package pe.sdp.daos;

import pe.sdp.beans.Evaluacion;
import pe.sdp.beans.HistorialEvaluacion;
import pe.sdp.util.Formato;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Autoevaluaciones y correcciones.
 *  - Mensual: REEMPLAZA el color (puede subir o bajar).
 *  - Del ensayo: SOLO SUBE (si marca igual o menos, queda en el historial con aplicado = 0).
 *  - Corrección del admin: reemplaza y deja motivo.
 */
public class EvaluacionDao extends BaseDao {

    /** Todas las figuras activas con el color del miembro (null si no la evaluó) y el color de la figura. */
    public List<Evaluacion> deMiembro(int idMiembro) throws SQLException {
        String sql = "SELECT cf.id_figura, cf.nombre, cf.color AS color_figura, e.color, e.fecha_actualizacion, " +
                     " (SELECT h.fuente FROM historial_evaluacion h WHERE h.id_miembro=? AND h.id_figura=cf.id_figura AND h.aplicado=1 " +
                     "   ORDER BY h.fecha DESC LIMIT 1) AS fuente " +
                     "FROM v_color_figura cf LEFT JOIN evaluacion e ON e.id_figura = cf.id_figura AND e.id_miembro = ? " +
                     "ORDER BY cf.nombre";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idMiembro); ps.setInt(2, idMiembro);
            List<Evaluacion> l = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Evaluacion e = new Evaluacion();
                    e.setIdMiembro(idMiembro);
                    e.setIdFigura(rs.getInt("id_figura"));
                    e.setFigura(rs.getString("nombre"));
                    e.setColor(rs.getString("color"));
                    e.setColorFigura(rs.getString("color_figura"));
                    e.setFechaActualizacion(fechaHora(rs, "fecha_actualizacion"));
                    e.setFuente(rs.getString("fuente"));
                    l.add(e);
                }
            }
            return l;
        }
    }

    /** Figuras que le faltan (ROJO/AMARILLO), primero las de su nivel. */
    public List<Evaluacion> queLeFaltan(int idMiembro) throws SQLException {
        List<Evaluacion> l = new ArrayList<>();
        for (Evaluacion e : deMiembro(idMiembro)) if (e.isLeFalta() && e.getColorFigura() != null) l.add(e);
        l.sort(Comparator.comparingInt((Evaluacion e) -> -Formato.valor(e.getColorFigura())).thenComparing(Evaluacion::getFigura));
        return l;
    }

    // ---------------- Periodo mensual ----------------

    /** Periodo del mes actual (lo crea si no existe: del día 1 al 7). Devuelve su id. */
    public int periodoActual() throws SQLException {
        LocalDate hoy = LocalDate.now();
        try (Connection c = getConnection()) {
            try (PreparedStatement ps = c.prepareStatement("SELECT id_periodo FROM periodo_autoevaluacion WHERE anio=? AND mes=?")) {
                ps.setInt(1, hoy.getYear()); ps.setInt(2, hoy.getMonthValue());
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getInt(1); }
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO periodo_autoevaluacion (anio, mes, fecha_apertura, fecha_cierre) VALUES (?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, hoy.getYear()); ps.setInt(2, hoy.getMonthValue());
                ps.setTimestamp(3, Timestamp.valueOf(hoy.withDayOfMonth(1).atStartOfDay()));
                ps.setTimestamp(4, Timestamp.valueOf(hoy.withDayOfMonth(7).atTime(23, 59, 59)));
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) { rs.next(); return rs.getInt(1); }
            }
        }
    }

    /** [apertura, cierre] del periodo. */
    public LocalDateTime[] ventanaPeriodo(int idPeriodo) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("SELECT fecha_apertura, fecha_cierre FROM periodo_autoevaluacion WHERE id_periodo=?")) {
            ps.setInt(1, idPeriodo);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return new LocalDateTime[]{fechaHora(rs, "fecha_apertura"), fechaHora(rs, "fecha_cierre")}; }
        }
    }

    public boolean respondioPeriodo(int idMiembro, int idPeriodo) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(
                "SELECT 1 FROM historial_evaluacion WHERE id_miembro=? AND id_periodo=? AND fuente='MENSUAL' LIMIT 1")) {
            ps.setInt(1, idMiembro); ps.setInt(2, idPeriodo);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    // ---------------- Guardar ----------------

    /** Mensual: reemplaza todos los colores enviados. */
    public int guardarMensual(int idMiembro, Map<Integer, String> colores, int idPeriodo) throws SQLException {
        return guardar(idMiembro, colores, "MENSUAL", null, idPeriodo, idMiembro, null, false);
    }

    /** Del ensayo: solo sube. Devuelve cuántas figuras mejoraron. */
    public int guardarEnsayo(int idMiembro, Map<Integer, String> colores, int idEnsayo) throws SQLException {
        return guardar(idMiembro, colores, "ENSAYO", idEnsayo, null, idMiembro, null, true);
    }

    /** Corrección del admin. */
    public void corregir(int idMiembro, int idFigura, String color, String motivo, int idAdmin) throws SQLException {
        guardar(idMiembro, Map.of(idFigura, color), "CORRECCION", null, null, idAdmin, motivo, false);
    }

    private int guardar(int idMiembro, Map<Integer, String> colores, String fuente, Integer idEnsayo, Integer idPeriodo,
                        int registradoPor, String motivo, boolean soloSube) throws SQLException {
        int aplicados = 0;
        try (Connection c = getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement sel = c.prepareStatement("SELECT color FROM evaluacion WHERE id_miembro=? AND id_figura=? FOR UPDATE");
                 PreparedStatement up = c.prepareStatement("INSERT INTO evaluacion (id_miembro, id_figura, color, fecha_actualizacion) VALUES (?,?,?,NOW()) " +
                                                           "ON DUPLICATE KEY UPDATE color=VALUES(color), fecha_actualizacion=NOW()");
                 PreparedStatement hi = c.prepareStatement("INSERT INTO historial_evaluacion (id_miembro, id_figura, color_anterior, color_nuevo, fuente, aplicado, " +
                                                           "id_ensayo, id_periodo, registrado_por, motivo) VALUES (?,?,?,?,?,?,?,?,?,?)")) {
                for (Map.Entry<Integer, String> e : colores.entrySet()) {
                    String nuevo = e.getValue();
                    if (!Formato.esColor(nuevo)) continue;
                    sel.setInt(1, idMiembro); sel.setInt(2, e.getKey());
                    String actual = null;
                    try (ResultSet rs = sel.executeQuery()) { if (rs.next()) actual = rs.getString(1); }
                    if (!soloSube && nuevo.equals(actual) && "MENSUAL".equals(fuente)) {
                        // sin cambio: igual se registra para saber que respondió el periodo
                    }
                    boolean aplica = !soloSube || Formato.valor(nuevo) > Formato.valor(actual);
                    if (aplica && !nuevo.equals(actual)) {
                        up.setInt(1, idMiembro); up.setInt(2, e.getKey()); up.setString(3, nuevo); up.executeUpdate();
                        aplicados++;
                    }
                    hi.setInt(1, idMiembro); hi.setInt(2, e.getKey());
                    setTexto(hi, 3, actual); hi.setString(4, nuevo); hi.setString(5, fuente);
                    hi.setBoolean(6, aplica);
                    setEntero(hi, 7, idEnsayo); setEntero(hi, 8, idPeriodo); hi.setInt(9, registradoPor); setTexto(hi, 10, motivo);
                    hi.executeUpdate();
                }
                c.commit();
            } catch (SQLException ex) { c.rollback(); throw ex; }
        }
        return aplicados;
    }

    // ---------------- Historial ----------------

    private static final String SELECT_HIST =
        "SELECT h.*, m.nombre AS miembro, f.nombre AS figura, r.nombre AS reg " +
        "FROM historial_evaluacion h JOIN miembro m ON m.id_miembro=h.id_miembro JOIN figura f ON f.id_figura=h.id_figura " +
        "LEFT JOIN miembro r ON r.id_miembro=h.registrado_por ";

    private HistorialEvaluacion mapearH(ResultSet rs) throws SQLException {
        HistorialEvaluacion h = new HistorialEvaluacion();
        h.setIdHistorial(rs.getInt("id_historial"));
        h.setIdMiembro(rs.getInt("id_miembro"));
        h.setMiembro(rs.getString("miembro"));
        h.setIdFigura(rs.getInt("id_figura"));
        h.setFigura(rs.getString("figura"));
        h.setColorAnterior(rs.getString("color_anterior"));
        h.setColorNuevo(rs.getString("color_nuevo"));
        h.setFuente(rs.getString("fuente"));
        h.setAplicado(rs.getBoolean("aplicado"));
        h.setIdEnsayo(entero(rs, "id_ensayo"));
        String reg = rs.getString("reg");
        h.setRegistradoPor(reg == null ? ("CARGA_INICIAL".equals(h.getFuente()) ? "Migración Excel" : "—") : (reg.equals(h.getMiembro()) ? "La misma persona" : reg));
        h.setMotivo(rs.getString("motivo"));
        h.setFecha(fechaHora(rs, "fecha"));
        return h;
    }

    public List<HistorialEvaluacion> historialDe(int idMiembro, String fuente) throws SQLException {
        String sql = SELECT_HIST + "WHERE h.id_miembro=? " + (fuente != null ? "AND h.fuente=? " : "") + "ORDER BY h.fecha DESC, h.id_historial DESC LIMIT 200";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idMiembro);
            if (fuente != null) ps.setString(2, fuente);
            List<HistorialEvaluacion> l = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) l.add(mapearH(rs)); }
            return l;
        }
    }

    /** Búsqueda del panel Histórico. Devuelve la página pedida (tamaño fijo). */
    public List<HistorialEvaluacion> buscar(Integer idMiembro, Integer idFigura, String fuente, LocalDate desde, LocalDate hasta, int pagina, int tam) throws SQLException {
        StringBuilder w = new StringBuilder();
        List<Object> p = filtros(w, idMiembro, idFigura, fuente, desde, hasta);
        String sql = SELECT_HIST + w + " ORDER BY h.fecha DESC, h.id_historial DESC LIMIT ? OFFSET ?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            int i = poner(ps, p);
            ps.setInt(i++, tam); ps.setInt(i, (pagina - 1) * tam);
            List<HistorialEvaluacion> l = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) l.add(mapearH(rs)); }
            return l;
        }
    }

    /** [total, mensual, ensayo, correcciones] con los mismos filtros. */
    public int[] contar(Integer idMiembro, Integer idFigura, String fuente, LocalDate desde, LocalDate hasta) throws SQLException {
        StringBuilder w = new StringBuilder();
        List<Object> p = filtros(w, idMiembro, idFigura, fuente, desde, hasta);
        String sql = "SELECT COUNT(*), SUM(h.fuente='MENSUAL'), SUM(h.fuente='ENSAYO'), SUM(h.fuente='CORRECCION') FROM historial_evaluacion h " + w;
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            poner(ps, p);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return new int[]{rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getInt(4)}; }
        }
    }

    private List<Object> filtros(StringBuilder w, Integer idMiembro, Integer idFigura, String fuente, LocalDate desde, LocalDate hasta) {
        List<Object> p = new ArrayList<>();
        w.append("WHERE 1=1 ");
        if (idMiembro != null) { w.append("AND h.id_miembro=? "); p.add(idMiembro); }
        if (idFigura != null) { w.append("AND h.id_figura=? "); p.add(idFigura); }
        if (fuente != null) { w.append("AND h.fuente=? "); p.add(fuente); }
        if (desde != null) { w.append("AND h.fecha >= ? "); p.add(Timestamp.valueOf(desde.atStartOfDay())); }
        if (hasta != null) { w.append("AND h.fecha < ? "); p.add(Timestamp.valueOf(hasta.plusDays(1).atStartOfDay())); }
        return p;
    }

    private int poner(PreparedStatement ps, List<Object> p) throws SQLException {
        int i = 1;
        for (Object o : p) ps.setObject(i++, o);
        return i;
    }

    /** [figuras que sabe (VERDE/AZUL), figuras activas, mejoras del mes] */
    public int[] resumenMiembro(int idMiembro) throws SQLException {
        String sql = "SELECT (SELECT COUNT(*) FROM evaluacion e JOIN figura f ON f.id_figura=e.id_figura AND f.activa=1 WHERE e.id_miembro=? AND e.color IN ('VERDE','AZUL')), " +
                     "(SELECT COUNT(*) FROM figura WHERE activa=1), " +
                     "(SELECT COUNT(*) FROM historial_evaluacion WHERE id_miembro=? AND aplicado=1 AND fuente<>'CARGA_INICIAL' " +
                     "   AND color_anterior IS NOT NULL AND FIELD(color_nuevo,'ROJO','AMARILLO','VERDE','AZUL') > FIELD(color_anterior,'ROJO','AMARILLO','VERDE','AZUL') " +
                     "   AND fecha >= DATE_FORMAT(CURDATE(),'%Y-%m-01'))";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idMiembro); ps.setInt(2, idMiembro);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return new int[]{rs.getInt(1), rs.getInt(2), rs.getInt(3)}; }
        }
    }
}
