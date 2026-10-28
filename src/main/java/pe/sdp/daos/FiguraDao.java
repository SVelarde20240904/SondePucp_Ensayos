package pe.sdp.daos;

import pe.sdp.beans.Figura;

import java.sql.*;
import java.util.*;

/** Mano de figuras: listados, alta, edición, desactivar/reactivar, eliminar y reglas de conteo. */
public class FiguraDao extends BaseDao {

    /** % de los que cuentan para el color que la marcan VERDE o AZUL. */
    private static final String SELECT =
        "SELECT v.*, f.descripcion, f.video_url, b.nombre AS baja_nombre, " +
        " (SELECT ROUND(100 * SUM(e.color IN ('VERDE','AZUL')) / NULLIF(COUNT(*),0)) FROM evaluacion e " +
        "   JOIN v_miembro_cuenta_color cc ON cc.id_miembro = e.id_miembro WHERE e.id_figura = v.id_figura) AS pct " +
        "FROM v_figura_admin v JOIN figura f ON f.id_figura = v.id_figura LEFT JOIN miembro b ON b.id_miembro = f.baja_por ";

    private Figura mapear(ResultSet rs) throws SQLException {
        Figura f = new Figura();
        f.setIdFigura(rs.getInt("id_figura"));
        f.setNombre(rs.getString("nombre"));
        f.setOrden(rs.getInt("orden"));
        f.setActiva(rs.getBoolean("activa"));
        f.setDescripcion(rs.getString("descripcion"));
        f.setVideoUrl(rs.getString("video_url"));
        f.setFechaBaja(fechaHora(rs, "fecha_baja"));
        f.setBajaPor(rs.getString("baja_nombre"));
        f.setColor(rs.getString("color"));
        f.setNEvaluaciones(rs.getInt("n_evaluaciones"));
        f.setNEnsenada(rs.getInt("n_ensenada"));
        f.setPuedeEliminar(rs.getBoolean("puede_eliminar"));
        f.setPorcentajeSaben(rs.getInt("pct"));
        return f;
    }

    private List<Figura> consultar(String where) throws SQLException {
        try (Connection c = getConnection(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(SELECT + where + " ORDER BY v.orden")) {
            List<Figura> l = new ArrayList<>();
            while (rs.next()) l.add(mapear(rs));
            return l;
        }
    }

    public List<Figura> listarActivas() throws SQLException { return consultar("WHERE v.activa = 1"); }

    public List<Figura> listarInactivas() throws SQLException { return consultar("WHERE v.activa = 0"); }

    /** Activas agrupadas por color (AZUL, VERDE, AMARILLO, ROJO y SIN para las que aún no tienen color). */
    public Map<String, List<Figura>> activasPorNivel() throws SQLException {
        Map<String, List<Figura>> m = new LinkedHashMap<>();
        for (String k : new String[]{"AZUL", "VERDE", "AMARILLO", "ROJO", "SIN"}) m.put(k, new ArrayList<>());
        for (Figura f : listarActivas()) m.get(f.getColor() == null ? "SIN" : f.getColor()).add(f);
        for (List<Figura> l : m.values()) l.sort((a, b) -> b.getPorcentajeSaben() - a.getPorcentajeSaben());
        return m;
    }

    public Figura obtener(int id) throws SQLException {
        List<Figura> l = consultar("WHERE v.id_figura = " + id);
        return l.isEmpty() ? null : l.get(0);
    }

    /**
     * Alta de figura. despuesDe = id de la figura tras la cual va (null = al final).
     * Si el nombre ya existía (inactiva), se reactiva en vez de duplicarse.
     */
    public int crear(String nombre, String descripcion, String videoUrl, Integer despuesDe, int idAdmin) throws SQLException {
        try (Connection c = getConnection()) {
            c.setAutoCommit(false);
            try {
                Integer orden = despuesDe == null ? null : hacerEspacio(c, despuesDe);
                String sql = "INSERT INTO figura (nombre, descripcion, video_url, orden, creado_por) VALUES (?,?,?,?,?) " +
                             "ON DUPLICATE KEY UPDATE activa = 1, descripcion = VALUES(descripcion), video_url = VALUES(video_url), id_figura = LAST_INSERT_ID(id_figura)";
                int id;
                try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, nombre.trim());
                    setTexto(ps, 2, descripcion);
                    setTexto(ps, 3, videoUrl);
                    setEntero(ps, 4, orden);
                    ps.setInt(5, idAdmin);
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) { rs.next(); id = rs.getInt(1); }
                }
                c.commit();
                return id;
            } catch (SQLException e) { c.rollback(); throw e; }
        }
    }

    public void actualizar(int id, String nombre, String descripcion, String videoUrl, Integer despuesDe) throws SQLException {
        try (Connection c = getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement("UPDATE figura SET nombre=?, descripcion=?, video_url=? WHERE id_figura=?")) {
                    ps.setString(1, nombre.trim()); setTexto(ps, 2, descripcion); setTexto(ps, 3, videoUrl); ps.setInt(4, id); ps.executeUpdate();
                }
                if (despuesDe != null && despuesDe != id) {
                    int orden = hacerEspacio(c, despuesDe);
                    try (PreparedStatement ps = c.prepareStatement("UPDATE figura SET orden=? WHERE id_figura=?")) {
                        ps.setInt(1, orden); ps.setInt(2, id); ps.executeUpdate();
                    }
                }
                c.commit();
            } catch (SQLException e) { c.rollback(); throw e; }
        }
    }

    /** Corre una posición a las figuras que van después de 'despuesDe' y devuelve el orden libre. */
    private int hacerEspacio(Connection c, int despuesDe) throws SQLException {
        int base;
        try (PreparedStatement ps = c.prepareStatement("SELECT orden FROM figura WHERE id_figura=?")) {
            ps.setInt(1, despuesDe);
            try (ResultSet rs = ps.executeQuery()) { if (!rs.next()) return 9999; base = rs.getInt(1); }
        }
        try (PreparedStatement ps = c.prepareStatement("UPDATE figura SET orden = orden + 1 WHERE orden > ?")) {
            ps.setInt(1, base); ps.executeUpdate();
        }
        return base + 1;
    }

    public void desactivar(int id, int idAdmin) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("UPDATE figura SET activa=0, baja_por=? WHERE id_figura=?")) {
            ps.setInt(1, idAdmin); ps.setInt(2, id); ps.executeUpdate();
        }
    }

    public void reactivar(int id) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("UPDATE figura SET activa=1 WHERE id_figura=?")) {
            ps.setInt(1, id); ps.executeUpdate();
        }
    }

    /** Solo si nunca se evaluó ni se enseñó. Devuelve false si no se pudo. */
    public boolean eliminar(int id) throws SQLException {
        String sql = "DELETE FROM figura WHERE id_figura=? AND id_figura IN (SELECT id_figura FROM (SELECT id_figura FROM v_figura_admin WHERE puede_eliminar=1) x)";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() == 1;
        }
    }

    /** Ids de las figuras activas que tienen video (para mostrar "▶ ver" en la autoevaluación). */
    public java.util.Set<Integer> idsConVideo() throws SQLException {
        java.util.Set<Integer> s = new java.util.HashSet<>();
        try (Connection c = getConnection(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT id_figura FROM figura WHERE activa=1 AND video_url IS NOT NULL AND video_url<>''")) {
            while (rs.next()) s.add(rs.getInt(1));
        }
        return s;
    }

    public boolean existeNombre(String nombre, int exceptoId) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("SELECT 1 FROM figura WHERE nombre=? AND id_figura<>? AND activa=1")) {
            ps.setString(1, nombre.trim()); ps.setInt(2, exceptoId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    // ---------- Reglas: quién cuenta para el color ----------
    public Map<String, Boolean> reglas() throws SQLException {
        Map<String, Boolean> m = new LinkedHashMap<>();
        try (Connection c = getConnection(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT clave, cuenta FROM regla_conteo_color ORDER BY FIELD(clave,'REGULAR','OCASIONAL','EVENTUAL','EQUIPO_ENSAYOS')")) {
            while (rs.next()) m.put(rs.getString(1), rs.getBoolean(2));
        }
        return m;
    }

    public void guardarReglas(Map<String, Boolean> reglas) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("UPDATE regla_conteo_color SET cuenta=? WHERE clave=?")) {
            for (Map.Entry<String, Boolean> e : reglas.entrySet()) {
                ps.setBoolean(1, e.getValue()); ps.setString(2, e.getKey()); ps.addBatch();
            }
            ps.executeBatch();
        }
    }
}
