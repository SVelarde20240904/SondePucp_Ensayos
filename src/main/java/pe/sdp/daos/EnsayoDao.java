package pe.sdp.daos;

import pe.sdp.beans.*;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/** Ensayos: creación, listados, confirmaciones, asistencia real, figuras enseñadas, avances y cronograma. */
public class EnsayoDao extends BaseDao {

    private static final String SELECT =
        "SELECT e.*, na.nombre AS notas_nombre, r.confirmados, r.sin_respuesta, r.asistieron, r.puntuales, r.tardanzas, r.faltas, " +
        " r.figuras_ensenadas, r.avances, " +
        " (SELECT COUNT(*) FROM asistencia a WHERE a.id_ensayo=e.id_ensayo AND a.estado_real='FJ') AS faltas_j, " +
        " (SELECT COUNT(*) FROM asistencia a WHERE a.id_ensayo=e.id_ensayo AND (a.confirmacion='SI' OR a.estado_real IS NOT NULL)) AS esperados, " +
        " (SELECT COUNT(DISTINCT h.id_miembro) FROM historial_evaluacion h WHERE h.id_ensayo=e.id_ensayo) AS autoevaluaron, " +
        " (SELECT GROUP_CONCAT(DISTINCT ef.nivel ORDER BY FIELD(ef.nivel,'AZUL','VERDE','AMARILLO','ROJO')) FROM ensayo_figura ef WHERE ef.id_ensayo=e.id_ensayo) AS niveles, " +
        " (SELECT GROUP_CONCAT(m.nombre ORDER BY m.nombre SEPARATOR ', ') FROM ensayo_dictante d JOIN miembro m ON m.id_miembro=d.id_miembro WHERE d.id_ensayo=e.id_ensayo) AS dictan " +
        "FROM ensayo e JOIN v_ensayo_resumen r ON r.id_ensayo = e.id_ensayo LEFT JOIN miembro na ON na.id_miembro = e.notas_autor ";

    private Ensayo mapear(ResultSet rs) throws SQLException {
        Ensayo e = new Ensayo();
        e.setIdEnsayo(rs.getInt("id_ensayo"));
        e.setTipo(rs.getString("tipo"));
        e.setFecha(fecha(rs, "fecha"));
        e.setHoraInicio(hora(rs, "hora_inicio"));
        e.setHoraFin(hora(rs, "hora_fin"));
        e.setLugar(rs.getString("lugar"));
        e.setCierreConfirmacion(fechaHora(rs, "cierre_confirmacion"));
        e.setEstado(rs.getString("estado"));
        e.setNotas(rs.getString("notas"));
        e.setNotasAutor(rs.getString("notas_nombre"));
        e.setConfirmados(rs.getInt("confirmados"));
        e.setSinRespuesta(rs.getInt("sin_respuesta"));
        e.setAsistieron(rs.getInt("asistieron"));
        e.setPuntuales(rs.getInt("puntuales"));
        e.setTardanzas(rs.getInt("tardanzas"));
        e.setFaltas(rs.getInt("faltas"));
        e.setFaltasJustificadas(rs.getInt("faltas_j"));
        e.setEsperados(rs.getInt("esperados"));
        e.setFigurasEnsenadas(rs.getInt("figuras_ensenadas"));
        e.setAvances(rs.getInt("avances"));
        e.setAutoevaluaron(rs.getInt("autoevaluaron"));
        e.setNivelesEnsenados(rs.getString("niveles"));
        e.setDictantesTexto(rs.getString("dictan"));
        return e;
    }

    public List<Ensayo> listar(String tipo) throws SQLException {
        String sql = SELECT + (tipo != null ? "WHERE e.tipo=? " : "") + "ORDER BY e.fecha DESC, e.hora_inicio DESC";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (tipo != null) ps.setString(1, tipo);
            List<Ensayo> l = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) l.add(mapear(rs)); }
            return l;
        }
    }

    public Ensayo obtener(int id) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(SELECT + "WHERE e.id_ensayo=?")) {
            ps.setInt(1, id);
            Ensayo e;
            try (ResultSet rs = ps.executeQuery()) { if (!rs.next()) return null; e = mapear(rs); }
            e.setIndicaciones(listaTexto(c, "SELECT texto FROM ensayo_indicacion WHERE id_ensayo=? ORDER BY orden", id));
            List<Integer> dict = new ArrayList<>();
            try (PreparedStatement p2 = c.prepareStatement("SELECT id_miembro FROM ensayo_dictante WHERE id_ensayo=?")) {
                p2.setInt(1, id);
                try (ResultSet rs = p2.executeQuery()) { while (rs.next()) dict.add(rs.getInt(1)); }
            }
            e.setDictantes(dict);
            return e;
        }
    }

    private List<String> listaTexto(Connection c, String sql, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            List<String> l = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) l.add(rs.getString(1)); }
            return l;
        }
    }

    /** Próximos ensayos publicados (desde hoy), con la confirmación del miembro. */
    public List<Ensayo> proximos(int idMiembro, int limite) throws SQLException {
        String sql = SELECT.replace("FROM ensayo e", ", a.confirmacion AS mi_conf, a.motivo AS mi_motivo FROM ensayo e") +
                     "LEFT JOIN asistencia a ON a.id_ensayo=e.id_ensayo AND a.id_miembro=? " +
                     "WHERE e.estado='PROGRAMADO' AND e.fecha >= CURDATE() ORDER BY e.fecha, e.hora_inicio LIMIT ?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idMiembro); ps.setInt(2, limite);
            List<Ensayo> l = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Ensayo e = mapear(rs);
                    String conf = rs.getString("mi_conf");
                    e.setMiConfirmacion(conf == null ? "SIN_RESPUESTA" : conf);
                    e.setMiMotivo(rs.getString("mi_motivo"));
                    l.add(e);
                }
            }
            for (Ensayo e : l) e.setIndicaciones(obtenerIndicaciones(e.getIdEnsayo()));
            return l;
        }
    }

    private List<String> obtenerIndicaciones(int id) throws SQLException {
        try (Connection c = getConnection()) { return listaTexto(c, "SELECT texto FROM ensayo_indicacion WHERE id_ensayo=? ORDER BY orden", id); }
    }

    /** Ensayo más cercano (hoy o después) o, si no hay, el último realizado. */
    public Ensayo actualOProximo() throws SQLException {
        try (Connection c = getConnection(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT id_ensayo FROM ensayo WHERE estado IN ('PROGRAMADO','CERRADO') " +
                     "ORDER BY (fecha < CURDATE()), IF(fecha >= CURDATE(), DATEDIFF(fecha, CURDATE()), DATEDIFF(CURDATE(), fecha)) LIMIT 1")) {
            return rs.next() ? obtener(rs.getInt(1)) : null;
        }
    }

    // ---------------- Crear / editar ----------------

    /** Guarda el ensayo (id = 0 → nuevo). Al publicarlo crea la fila "Sin respuesta" de cada miembro activo. */
    public int guardar(Ensayo e, List<String> indicaciones, List<Integer> dictantes, int idUsuario) throws SQLException {
        try (Connection c = getConnection()) {
            c.setAutoCommit(false);
            try {
                int id = e.getIdEnsayo();
                if (id == 0) {
                    String sql = "INSERT INTO ensayo (tipo, fecha, hora_inicio, hora_fin, lugar, cierre_confirmacion, estado, creado_por) VALUES (?,?,?,?,?,?,?,?)";
                    try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                        llenar(ps, e); ps.setInt(8, idUsuario);
                        ps.executeUpdate();
                        try (ResultSet rs = ps.getGeneratedKeys()) { rs.next(); id = rs.getInt(1); }
                    }
                } else {
                    String sql = "UPDATE ensayo SET tipo=?, fecha=?, hora_inicio=?, hora_fin=?, lugar=?, cierre_confirmacion=?, estado=? WHERE id_ensayo=?";
                    try (PreparedStatement ps = c.prepareStatement(sql)) { llenar(ps, e); ps.setInt(8, id); ps.executeUpdate(); }
                    try (PreparedStatement ps = c.prepareStatement("DELETE FROM ensayo_indicacion WHERE id_ensayo=?")) { ps.setInt(1, id); ps.executeUpdate(); }
                    try (PreparedStatement ps = c.prepareStatement("DELETE FROM ensayo_dictante WHERE id_ensayo=?")) { ps.setInt(1, id); ps.executeUpdate(); }
                }
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO ensayo_indicacion (id_ensayo, texto, orden) VALUES (?,?,?)")) {
                    int o = 1;
                    for (String t : indicaciones) { if (t == null || t.isBlank()) continue; ps.setInt(1, id); ps.setString(2, t.trim()); ps.setInt(3, o++); ps.addBatch(); }
                    ps.executeBatch();
                }
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO ensayo_dictante (id_ensayo, id_miembro) VALUES (?,?)")) {
                    for (Integer d : dictantes) { ps.setInt(1, id); ps.setInt(2, d); ps.addBatch(); }
                    ps.executeBatch();
                }
                if ("PROGRAMADO".equals(e.getEstado())) {
                    try (PreparedStatement ps = c.prepareStatement("INSERT IGNORE INTO asistencia (id_ensayo, id_miembro) SELECT ?, id_miembro FROM miembro WHERE estado='ACTIVO'")) {
                        ps.setInt(1, id); ps.executeUpdate();
                    }
                }
                c.commit();
                return id;
            } catch (SQLException ex) { c.rollback(); throw ex; }
        }
    }

    private void llenar(PreparedStatement ps, Ensayo e) throws SQLException {
        ps.setString(1, e.getTipo());
        setFecha(ps, 2, e.getFecha());
        ps.setTime(3, Time.valueOf(e.getHoraInicio()));
        ps.setTime(4, Time.valueOf(e.getHoraFin()));
        ps.setString(5, e.getLugar());
        setFechaHora(ps, 6, e.getCierreConfirmacion());
        ps.setString(7, e.getEstado());
    }

    public void cambiarEstado(int id, String estado) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("UPDATE ensayo SET estado=? WHERE id_ensayo=?")) {
            ps.setString(1, estado); ps.setInt(2, id); ps.executeUpdate();
        }
    }

    public void guardarNotas(int id, String notas, int autor) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("UPDATE ensayo SET notas=?, notas_autor=? WHERE id_ensayo=?")) {
            setTexto(ps, 1, notas); ps.setInt(2, autor); ps.setInt(3, id); ps.executeUpdate();
        }
    }

    // ---------------- Asistencia ----------------

    /** El miembro confirma (SI) o avisa que no va (NO + motivo). */
    public void confirmar(int idEnsayo, int idMiembro, String respuesta, String motivo) throws SQLException {
        String sql = "INSERT INTO asistencia (id_ensayo, id_miembro, confirmacion, motivo, fecha_confirmacion) VALUES (?,?,?,?,NOW()) " +
                     "ON DUPLICATE KEY UPDATE confirmacion=VALUES(confirmacion), motivo=VALUES(motivo), fecha_confirmacion=NOW()";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idEnsayo); ps.setInt(2, idMiembro); ps.setString(3, respuesta);
            setTexto(ps, 4, "NO".equals(respuesta) ? motivo : null);
            ps.executeUpdate();
        }
    }

    /** Lista para "Tomar asistencia": todos los activos, primero los que confirmaron. */
    public List<Asistencia> asistenciaDe(int idEnsayo) throws SQLException {
        String sql = "SELECT m.id_miembro, m.nombre, m.rol_baile, n.nivel, f.es_equipo_ensayos, " +
                     " COALESCE(a.confirmacion,'SIN_RESPUESTA') AS confirmacion, a.motivo, a.estado_real " +
                     "FROM miembro m JOIN v_miembro_flags f ON f.id_miembro=m.id_miembro JOIN v_nivel_miembro n ON n.id_miembro=m.id_miembro " +
                     "LEFT JOIN asistencia a ON a.id_miembro=m.id_miembro AND a.id_ensayo=? " +
                     "WHERE m.estado='ACTIVO' OR a.estado_real IS NOT NULL " +
                     "ORDER BY FIELD(COALESCE(a.confirmacion,'SIN_RESPUESTA'),'SI','NO','SIN_RESPUESTA'), m.nombre";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idEnsayo);
            List<Asistencia> l = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Asistencia a = new Asistencia();
                    a.setIdEnsayo(idEnsayo);
                    a.setIdMiembro(rs.getInt("id_miembro"));
                    a.setMiembro(rs.getString("nombre"));
                    a.setRolBaile(rs.getString("rol_baile"));
                    a.setNivel(rs.getString("nivel"));
                    a.setEquipoEnsayos(rs.getBoolean("es_equipo_ensayos"));
                    a.setConfirmacion(rs.getString("confirmacion"));
                    a.setMotivo(rs.getString("motivo"));
                    a.setEstadoReal(rs.getString("estado_real"));
                    l.add(a);
                }
            }
            return l;
        }
    }

    /** Guarda la asistencia real (AP/AN/TJ/TI/FJ/FI). codigo vacío = sin marcar. */
    public void registrarAsistencia(int idEnsayo, Map<Integer, String> codigos, int registrador) throws SQLException {
        String sql = "INSERT INTO asistencia (id_ensayo, id_miembro, estado_real, registrado_por) VALUES (?,?,?,?) " +
                     "ON DUPLICATE KEY UPDATE estado_real=VALUES(estado_real), registrado_por=VALUES(registrado_por)";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (Map.Entry<Integer, String> e : codigos.entrySet()) {
                ps.setInt(1, idEnsayo); ps.setInt(2, e.getKey()); setTexto(ps, 3, e.getValue()); ps.setInt(4, registrador);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /** Asistencias del miembro en los últimos N ensayos (para "Mi asistencia"). */
    public List<Asistencia> misAsistencias(int idMiembro, int n) throws SQLException {
        String sql = "SELECT e.id_ensayo, e.fecha, a.confirmacion, a.estado_real FROM ensayo e JOIN asistencia a ON a.id_ensayo=e.id_ensayo AND a.id_miembro=? " +
                     "WHERE e.fecha <= CURDATE() AND e.estado<>'CANCELADO' ORDER BY e.fecha DESC LIMIT ?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idMiembro); ps.setInt(2, n);
            List<Asistencia> l = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Asistencia a = new Asistencia();
                    a.setIdEnsayo(rs.getInt("id_ensayo")); a.setFecha(fecha(rs, "fecha"));
                    a.setConfirmacion(rs.getString("confirmacion")); a.setEstadoReal(rs.getString("estado_real"));
                    l.add(a);
                }
            }
            Collections.reverse(l);
            return l;
        }
    }

    /** % de asistencia del miembro en el ciclo (ensayos con asistencia registrada). */
    public int porcentajeAsistencia(int idMiembro) throws SQLException {
        String sql = "SELECT ROUND(100*SUM(estado_real IN ('AP','AN','TJ','TI'))/NULLIF(SUM(estado_real IS NOT NULL),0)) FROM asistencia WHERE id_miembro=?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idMiembro);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        }
    }

    /** Historial de asistencia de los últimos 6 ensayos hasta este (página 2 del reporte). */
    public List<ResumenPersona> historialAsistencia(int idEnsayo, List<LocalDate> fechasOut) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(
                "SELECT id_ensayo, fecha FROM ensayo WHERE estado<>'CANCELADO' AND fecha <= (SELECT fecha FROM ensayo WHERE id_ensayo=?) ORDER BY fecha DESC LIMIT 6")) {
            ps.setInt(1, idEnsayo);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) { ids.add(0, rs.getInt(1)); fechasOut.add(0, fecha(rs, "fecha")); } }
        }
        Map<Integer, ResumenPersona> filas = new LinkedHashMap<>();
        String in = ids.toString().replace('[', '(').replace(']', ')');
        if (ids.isEmpty()) return new ArrayList<>();
        String sql = "SELECT a.id_ensayo, m.id_miembro, m.nombre, m.rol_baile, n.nivel, f.es_equipo_ensayos, a.confirmacion, a.estado_real " +
                     "FROM asistencia a JOIN miembro m ON m.id_miembro=a.id_miembro JOIN v_nivel_miembro n ON n.id_miembro=m.id_miembro " +
                     "JOIN v_miembro_flags f ON f.id_miembro=m.id_miembro WHERE a.id_ensayo IN " + in +
                     " AND m.id_miembro IN (SELECT id_miembro FROM asistencia WHERE id_ensayo=? AND (confirmacion='SI' OR estado_real IS NOT NULL)) ORDER BY m.nombre";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idEnsayo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int idm = rs.getInt("id_miembro");
                    ResumenPersona r = filas.get(idm);
                    if (r == null) {
                        r = new ResumenPersona();
                        r.setIdMiembro(idm); r.setNombre(rs.getString("nombre"));
                        r.setRol("LEADER".equals(rs.getString("rol_baile")) ? "L" : "F");
                        r.setNivel(rs.getBoolean("es_equipo_ensayos") ? "Ensayos" : rs.getString("nivel"));
                        r.setCodigos(new ArrayList<>(Collections.nCopies(ids.size(), "")));
                        filas.put(idm, r);
                    }
                    String cod = rs.getString("estado_real");
                    if (cod == null) cod = "SIN_RESPUESTA".equals(rs.getString("confirmacion")) ? "SR" : "";
                    r.getCodigos().set(ids.indexOf(rs.getInt("id_ensayo")), cod);
                }
            }
        }
        for (ResumenPersona r : filas.values()) {
            long ok = r.getCodigos().stream().filter(x -> x.equals("AP") || x.equals("AN") || x.equals("TJ") || x.equals("TI")).count();
            long tot = r.getCodigos().stream().filter(x -> !x.isEmpty()).count();
            r.setPorcentaje(tot == 0 ? 0 : (int) Math.round(ok * 100.0 / tot));
            r.setA((int) r.getCodigos().stream().filter("FI"::equals).count());
            r.setB((int) r.getCodigos().stream().filter("SR"::equals).count());
        }
        return new ArrayList<>(filas.values());
    }

    // ---------------- Figuras enseñadas y avances ----------------

    public List<FiguraSugerida> figurasEnsenadas(int idEnsayo) throws SQLException {
        String sql = "SELECT ef.id_figura, f.nombre, ef.nivel, m.nombre AS dicto FROM ensayo_figura ef JOIN figura f ON f.id_figura=ef.id_figura " +
                     "LEFT JOIN miembro m ON m.id_miembro=ef.dicto WHERE ef.id_ensayo=? ORDER BY FIELD(ef.nivel,'AZUL','VERDE','AMARILLO','ROJO'), f.nombre";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idEnsayo);
            List<FiguraSugerida> l = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    FiguraSugerida f = new FiguraSugerida();
                    f.setIdFigura(rs.getInt(1)); f.setFigura(rs.getString(2)); f.setNivel(rs.getString(3)); f.setDicto(rs.getString(4)); f.setEnsenada(true);
                    l.add(f);
                }
            }
            return l;
        }
    }

    /** Reemplaza las figuras enseñadas (guarda el color que tenía cada figura en ese momento). */
    public void guardarFigurasEnsenadas(int idEnsayo, List<Integer> figuras, Integer dicto) throws SQLException {
        try (Connection c = getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement("DELETE FROM ensayo_figura WHERE id_ensayo=?")) { ps.setInt(1, idEnsayo); ps.executeUpdate(); }
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO ensayo_figura (id_ensayo, id_figura, nivel, dicto) SELECT ?, id_figura, color, ? FROM v_color_figura WHERE id_figura=?")) {
                    for (Integer f : figuras) { ps.setInt(1, idEnsayo); setEntero(ps, 2, dicto); ps.setInt(3, f); ps.addBatch(); }
                    ps.executeBatch();
                }
                c.commit();
            } catch (SQLException e) { c.rollback(); throw e; }
        }
    }

    /** Figuras enseñadas que el miembro puede autoevaluar en ese ensayo (asistió; primero las de su nivel). */
    public List<FiguraSugerida> figurasParaAutoevaluar(int idEnsayo, int idMiembro) throws SQLException {
        String sql = "SELECT ef.id_figura, f.nombre, ef.nivel, (ef.nivel = n.nivel) AS de_su_nivel FROM ensayo_figura ef " +
                     "JOIN figura f ON f.id_figura=ef.id_figura AND f.activa=1 " +
                     "JOIN asistencia a ON a.id_ensayo=ef.id_ensayo AND a.id_miembro=? AND a.estado_real IN ('AP','AN','TJ','TI') " +
                     "JOIN v_nivel_miembro n ON n.id_miembro=a.id_miembro WHERE ef.id_ensayo=? ORDER BY de_su_nivel DESC, f.nombre";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idMiembro); ps.setInt(2, idEnsayo);
            List<FiguraSugerida> todas = new ArrayList<>(), suNivel = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    FiguraSugerida f = new FiguraSugerida();
                    f.setIdFigura(rs.getInt(1)); f.setFigura(rs.getString(2)); f.setNivel(rs.getString(3));
                    todas.add(f);
                    if (rs.getBoolean(4)) suNivel.add(f);
                }
            }
            return suNivel.isEmpty() ? todas : suNivel;
        }
    }

    /** Último ensayo (máx. 7 días atrás) con figuras registradas que el miembro aún no autoevaluó. */
    public Ensayo autoevaluacionPendiente(int idMiembro) throws SQLException {
        String sql = "SELECT e.id_ensayo FROM ensayo e JOIN asistencia a ON a.id_ensayo=e.id_ensayo AND a.id_miembro=? AND a.estado_real IN ('AP','AN','TJ','TI') " +
                     "WHERE e.fecha BETWEEN CURDATE() - INTERVAL 7 DAY AND CURDATE() AND EXISTS (SELECT 1 FROM ensayo_figura ef WHERE ef.id_ensayo=e.id_ensayo) " +
                     "AND NOT EXISTS (SELECT 1 FROM historial_evaluacion h WHERE h.id_ensayo=e.id_ensayo AND h.id_miembro=?) ORDER BY e.fecha DESC LIMIT 1";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idMiembro); ps.setInt(2, idMiembro);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? obtener(rs.getInt(1)) : null; }
        }
    }

    public List<HistorialEvaluacion> avances(int idEnsayo) throws SQLException {
        String sql = "SELECT h.*, m.nombre AS miembro, f.nombre AS figura FROM historial_evaluacion h JOIN miembro m ON m.id_miembro=h.id_miembro " +
                     "JOIN figura f ON f.id_figura=h.id_figura WHERE h.id_ensayo=? AND h.aplicado=1 AND (h.color_anterior IS NULL OR h.color_anterior<>h.color_nuevo) " +
                     "ORDER BY f.nombre, m.nombre";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idEnsayo);
            List<HistorialEvaluacion> l = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    HistorialEvaluacion h = new HistorialEvaluacion();
                    h.setMiembro(rs.getString("miembro")); h.setFigura(rs.getString("figura"));
                    h.setColorAnterior(rs.getString("color_anterior")); h.setColorNuevo(rs.getString("color_nuevo"));
                    l.add(h);
                }
            }
            return l;
        }
    }

    // ---------------- Cronograma sugerido ----------------

    /** Quiénes cuentan como "asisten" para el cronograma: confirmaron SI o ya se les marcó asistencia. */
    private static final String ASISTEN =
        "SELECT f.id_miembro, f.nombre, f.rol_baile, f.es_equipo_ensayos, n.nivel FROM asistencia a " +
        "JOIN v_miembro_flags f ON f.id_miembro=a.id_miembro AND f.estado='ACTIVO' JOIN v_nivel_miembro n ON n.id_miembro=a.id_miembro " +
        "WHERE a.id_ensayo=? AND (a.estado_real IN ('AP','AN','TJ','TI') OR (a.estado_real IS NULL AND a.confirmacion='SI'))";

    /** Resumen por nivel: se abre con leader + follower, o con un solo rol + alguien del equipo de ensayos del rol contrario. */
    public List<NivelCronograma> resumenPorNivel(int idEnsayo, List<String> equipoOut, List<String> sinNivelOut) throws SQLException {
        Map<String, NivelCronograma> m = new LinkedHashMap<>();
        for (String n : new String[]{"AZUL", "VERDE", "AMARILLO", "ROJO"}) {
            NivelCronograma nc = new NivelCronograma(); nc.setNivel(n); nc.setNombresLeaders(""); nc.setNombresFollowers(""); m.put(n, nc);
        }
        int eqL = 0, eqF = 0;
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(ASISTEN + " ORDER BY f.nombre")) {
            ps.setInt(1, idEnsayo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String nombre = rs.getString("nombre"), rol = rs.getString("rol_baile"), nivel = rs.getString("nivel");
                    if (rs.getBoolean("es_equipo_ensayos")) {
                        if ("LEADER".equals(rol)) eqL++; else if ("FOLLOWER".equals(rol)) eqF++;
                        equipoOut.add(nombre + "|" + (rol == null ? "" : rol));
                        continue;
                    }
                    if (nivel == null) { sinNivelOut.add(nombre); continue; }
                    NivelCronograma nc = m.get(nivel);
                    if ("LEADER".equals(rol)) { nc.setLeaders(nc.getLeaders() + 1); nc.setNombresLeaders(sumar(nc.getNombresLeaders(), nombre)); }
                    else if ("FOLLOWER".equals(rol)) { nc.setFollowers(nc.getFollowers() + 1); nc.setNombresFollowers(sumar(nc.getNombresFollowers(), nombre)); }
                }
            }
        }
        for (NivelCronograma nc : m.values()) {
            int l = nc.getLeaders(), f = nc.getFollowers();
            if (l == 0 && f == 0) { nc.setSeAbre(false); nc.setPareja("Sin asistentes"); }
            else if (l > 0 && f > 0) { nc.setSeAbre(true); nc.setPareja("Completa"); }
            else if (l > 0) { nc.setSeAbre(eqF > 0); nc.setPareja(eqF > 0 ? "Con ensayos (follower)" : "Falta follower de ensayos"); }
            else { nc.setSeAbre(eqL > 0); nc.setPareja(eqL > 0 ? "Con ensayos (leader)" : "Falta leader de ensayos"); }
        }
        return new ArrayList<>(m.values());
    }

    private static String sumar(String lista, String nombre) {
        String corto = nombre.contains(" ") ? nombre.substring(0, nombre.indexOf(' ') + 2) + "." : nombre;
        return lista.isEmpty() ? corto : lista + ", " + corto;
    }

    /** Figuras que les faltan a los asistentes de los niveles abiertos, de la más necesitada a la menos. */
    public List<FiguraSugerida> figurasSugeridas(int idEnsayo, Set<String> nivelesAbiertos) throws SQLException {
        if (nivelesAbiertos.isEmpty()) return new ArrayList<>();
        String sql = "SELECT cf.id_figura, cf.nombre, cf.color, GROUP_CONCAT(x.nombre ORDER BY x.nombre SEPARATOR ', ') AS les_falta, COUNT(*) AS n, " +
                     " EXISTS(SELECT 1 FROM ensayo_figura ef WHERE ef.id_ensayo=? AND ef.id_figura=cf.id_figura) AS ensenada " +
                     "FROM (" + ASISTEN + ") x JOIN v_color_figura cf ON cf.color = x.nivel " +
                     "JOIN evaluacion e ON e.id_miembro=x.id_miembro AND e.id_figura=cf.id_figura AND e.color IN ('ROJO','AMARILLO') " +
                     "WHERE x.es_equipo_ensayos=0 GROUP BY cf.id_figura, cf.nombre, cf.color, cf.orden ORDER BY n DESC, cf.orden";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idEnsayo); ps.setInt(2, idEnsayo);
            List<FiguraSugerida> l = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (!nivelesAbiertos.contains(rs.getString("color"))) continue;
                    FiguraSugerida f = new FiguraSugerida();
                    f.setIdFigura(rs.getInt("id_figura")); f.setFigura(rs.getString("nombre")); f.setNivel(rs.getString("color"));
                    f.setLesFaltaA(rs.getString("les_falta")); f.setNAsistentes(rs.getInt("n")); f.setEnsenada(rs.getBoolean("ensenada"));
                    l.add(f);
                }
            }
            return l;
        }
    }

    /** Totales del ciclo (desde el primer ensayo del año académico en curso: marzo o agosto). [ensayos, % asistencia, figuras, avances] */
    public int[] resumenCiclo(LocalDate desde) throws SQLException {
        String sql = "SELECT COUNT(*), ROUND(AVG(100*r.asistieron/NULLIF((SELECT COUNT(*) FROM asistencia a WHERE a.id_ensayo=r.id_ensayo AND (a.confirmacion='SI' OR a.estado_real IS NOT NULL)),0))), " +
                     "SUM(r.figuras_ensenadas), SUM(r.avances) FROM v_ensayo_resumen r WHERE r.fecha BETWEEN ? AND CURDATE() AND r.estado<>'CANCELADO'";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            setFecha(ps, 1, desde);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return new int[]{rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getInt(4)}; }
        }
    }

    public static LocalDate inicioCiclo(LocalDate hoy) {
        return hoy.getMonthValue() >= 8 ? LocalDate.of(hoy.getYear(), 8, 1) : LocalDate.of(hoy.getYear(), 1, 1);
    }

    public static LocalDateTime cierrePorDefecto(LocalDate fecha) { return fecha.minusDays(1).atTime(23, 59); }
}
