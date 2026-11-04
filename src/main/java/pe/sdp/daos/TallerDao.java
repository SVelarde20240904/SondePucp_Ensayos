package pe.sdp.daos;

import pe.sdp.beans.*;
import pe.sdp.util.Formato;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;

/**
 * Talleres: cronograma (bloques + pasos planificados + dictado planeado), dictado final,
 * formulario post-taller (checklist, feedback, sugerencias) y resúmenes anónimos de feedback.
 */
public class TallerDao extends BaseDao {

    /** Quién participó de verdad: lo planeado menos los que no se presentaron, más los que se sumaron. */
    private static final String EFECTIVO = "(p.asistencia IS NULL OR p.asistencia <> 'NO_SE_PRESENTO')";

    // ===================== LISTA Y DETALLE =====================

    public List<Taller> listar() throws SQLException {
        String sql = "SELECT t.*, " +
                " (SELECT COUNT(DISTINCT p.id_miembro) FROM taller_persona p WHERE p.id_taller=t.id_taller AND p.funcion IN ('TALLERISTA','APOYO') AND " + EFECTIVO + ") AS esperados, " +
                " (SELECT COUNT(*) FROM taller_respuesta r WHERE r.id_taller=t.id_taller) AS respondieron, " +
                " (SELECT COUNT(*) FROM taller_feedback f WHERE f.id_taller=t.id_taller AND f.tipo<>'TALLER') AS feedbacks, " +
                " (SELECT COUNT(DISTINCT f.para_miembro) FROM taller_feedback f WHERE f.id_taller=t.id_taller AND f.tipo<>'TALLER') AS receptores, " +
                " (SELECT COUNT(*) FROM taller_resumen s WHERE s.id_taller=t.id_taller AND s.enviado=1) AS enviados " +
                "FROM taller t ORDER BY t.fecha DESC";
        try (Connection c = getConnection(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            List<Taller> l = new ArrayList<>();
            while (rs.next()) {
                Taller t = mapear(rs);
                t.setEsperados(rs.getInt("esperados")); t.setRespondieron(rs.getInt("respondieron"));
                t.setFeedbacks(rs.getInt("feedbacks")); t.setReceptores(rs.getInt("receptores")); t.setResumenesEnviados(rs.getInt("enviados"));
                l.add(t);
            }
            return l;
        }
    }

    private Taller mapear(ResultSet rs) throws SQLException {
        Taller t = new Taller();
        t.setIdTaller(rs.getInt("id_taller")); t.setCiclo(rs.getString("ciclo"));
        int n = rs.getInt("numero"); t.setNumero(rs.wasNull() ? null : n);
        t.setFecha(fecha(rs, "fecha"));
        Time hi = rs.getTime("hora_inicio"), hf = rs.getTime("hora_fin");
        t.setHoraInicio(hi == null ? null : hi.toLocalTime()); t.setHoraFin(hf == null ? null : hf.toLocalTime());
        t.setLugar(rs.getString("lugar")); t.setEstado(rs.getString("estado"));
        t.setMusicaTexto(rs.getString("musica_texto")); t.setNotas(rs.getString("notas"));
        t.setFinalFecha(fechaHora(rs, "final_fecha"));
        return t;
    }

    /** Taller completo: bloques, pasos y personas. */
    public Taller obtener(int id) throws SQLException {
        try (Connection c = getConnection()) {
            Taller t;
            try (PreparedStatement ps = c.prepareStatement("SELECT t.*, m.nombre AS final_nombre FROM taller t LEFT JOIN miembro m ON m.id_miembro=t.final_por WHERE t.id_taller=?")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) { if (!rs.next()) return null; t = mapear(rs); t.setFinalPor(rs.getString("final_nombre")); }
            }
            try (PreparedStatement ps = c.prepareStatement("SELECT nivel, hora, actividad, duracion FROM taller_bloque WHERE id_taller=? ORDER BY nivel, orden")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) { while (rs.next()) t.getBloques().get(rs.getString(1)).add(new String[]{nz(rs.getString(2)), rs.getString(3), nz(rs.getString(4))}); }
            }
            try (PreparedStatement ps = c.prepareStatement("SELECT tp.*, p.nombre FROM taller_paso tp JOIN paso_taller p ON p.id_paso=tp.id_paso " +
                    "WHERE tp.id_taller=? ORDER BY tp.nivel, tp.planificado DESC, tp.seccion, tp.orden")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        TallerPaso p = new TallerPaso(rs.getString("nombre"), rs.getInt("seccion"));
                        p.setIdPaso(rs.getInt("id_paso")); p.setNivel(rs.getString("nivel")); p.setOrden(rs.getInt("orden"));
                        p.setPlanificado(rs.getBoolean("planificado"));
                        boolean e = rs.getBoolean("ensenado"); p.setEnsenado(rs.wasNull() ? null : e);
                        t.getPasos().get(p.getNivel()).add(p);
                    }
                }
            }
            try (PreparedStatement ps = c.prepareStatement("SELECT tc.nivel, m.nombre FROM taller_checklist tc JOIN miembro m ON m.id_miembro=tc.respondido_por WHERE tc.id_taller=?")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) { while (rs.next()) t.getChecklistPor().put(rs.getString(1), rs.getString(2)); }
            }
            t.getPersonas().addAll(personas(c, id));
            return t;
        }
    }

    private List<TallerPersona> personas(Connection c, int idTaller) throws SQLException {
        String sql = "SELECT p.*, m.nombre, r.nombre AS reemplaza FROM taller_persona p JOIN miembro m ON m.id_miembro=p.id_miembro " +
                     "LEFT JOIN miembro r ON r.id_miembro=p.reemplaza_a WHERE p.id_taller=? " +
                     "ORDER BY p.planeado DESC, FIELD(p.funcion,'TALLERISTA','APOYO','MAESTRO','MUSICA'), " +
                     "FIELD(p.nivel,'PRINCIPIANTE','BASICO','INTRO_RUEDA','RUEDA_BASICA'), p.id";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idTaller);
            List<TallerPersona> l = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TallerPersona p = new TallerPersona(rs.getInt("id_miembro"), rs.getString("funcion"), rs.getString("nivel"));
                    p.setId(rs.getInt("id")); p.setIdTaller(idTaller); p.setNombre(rs.getString("nombre"));
                    p.setPlaneado(rs.getBoolean("planeado")); p.setAsistencia(rs.getString("asistencia"));
                    p.setReemplazaA(entero(rs, "reemplaza_a")); p.setReemplazaNombre(rs.getString("reemplaza"));
                    l.add(p);
                }
            }
            return l;
        }
    }

    /** El taller anterior (para copiar su cronograma como plantilla). */
    public Taller ultimo() throws SQLException {
        try (Connection c = getConnection(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT id_taller FROM taller WHERE estado<>'CANCELADO' AND EXISTS (SELECT 1 FROM taller_bloque b WHERE b.id_taller=taller.id_taller) ORDER BY fecha DESC LIMIT 1")) {
            return rs.next() ? obtener(rs.getInt(1)) : null;
        }
    }

    /** Siguiente número de taller del ciclo. */
    public int siguienteNumero(String ciclo) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("SELECT COALESCE(MAX(numero),0)+1 FROM taller WHERE ciclo=?")) {
            ps.setString(1, ciclo);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        }
    }

    public boolean existeFecha(LocalDate fecha, int exceptoId) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("SELECT 1 FROM taller WHERE fecha=? AND id_taller<>?")) {
            setFecha(ps, 1, fecha); ps.setInt(2, exceptoId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    // ===================== CRONOGRAMA (PLAN) =====================

    /**
     * Guarda el cronograma: datos, bloques por nivel, pasos planificados por nivel y el dictado planeado.
     * Al editar, se conserva lo ya registrado (asistencia de quien sigue en el plan, ✅/❌ de pasos que siguen, pasos agregados).
     */
    public int guardar(Taller t, List<TallerPersona> plan, int yo) throws SQLException {
        try (Connection c = getConnection()) {
            c.setAutoCommit(false);
            try {
                int id = t.getIdTaller();
                if (id == 0) {
                    try (PreparedStatement ps = c.prepareStatement("INSERT INTO taller (ciclo, numero, fecha, hora_inicio, hora_fin, lugar, estado, musica_texto, notas, creado_por) " +
                            "VALUES (?,?,?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                        llenar(ps, t); ps.setInt(10, yo); ps.executeUpdate();
                        try (ResultSet rs = ps.getGeneratedKeys()) { rs.next(); id = rs.getInt(1); }
                    }
                } else {
                    try (PreparedStatement ps = c.prepareStatement("UPDATE taller SET ciclo=?, numero=?, fecha=?, hora_inicio=?, hora_fin=?, lugar=?, estado=?, musica_texto=?, notas=? WHERE id_taller=?")) {
                        llenar(ps, t); ps.setInt(10, id); ps.executeUpdate();
                    }
                }
                // bloques: se reemplazan
                try (PreparedStatement del = c.prepareStatement("DELETE FROM taller_bloque WHERE id_taller=?")) { del.setInt(1, id); del.executeUpdate(); }
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO taller_bloque (id_taller, nivel, orden, hora, actividad, duracion) VALUES (?,?,?,?,?,?)")) {
                    for (String nivel : Taller.NIVELES) {
                        int o = 0;
                        for (String[] b : t.getBloques().get(nivel)) {
                            ps.setInt(1, id); ps.setString(2, nivel); ps.setInt(3, ++o); setTexto(ps, 4, b[0]); ps.setString(5, b[1]); setTexto(ps, 6, b[2]);
                            ps.addBatch();
                        }
                    }
                    ps.executeBatch();
                }
                // pasos planificados
                for (String nivel : Taller.NIVELES) guardarPlanPasos(c, id, nivel, t.getPasos().get(nivel));
                // dictado planeado: se quitan los que ya no están y se agregan los nuevos (los que siguen conservan su asistencia)
                List<TallerPersona> actuales = new ArrayList<>();
                for (TallerPersona p : personas(c, id)) if (p.isPlaneado()) actuales.add(p);
                for (TallerPersona a : actuales) {
                    boolean sigue = plan.stream().anyMatch(n -> mismo(n, a));
                    if (!sigue) try (PreparedStatement ps = c.prepareStatement("DELETE FROM taller_persona WHERE id=?")) { ps.setInt(1, a.getId()); ps.executeUpdate(); }
                }
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO taller_persona (id_taller, id_miembro, funcion, nivel, planeado) VALUES (?,?,?,?,1)")) {
                    Set<String> vistos = new HashSet<>();
                    for (TallerPersona n : plan) {
                        if (!vistos.add(n.getIdMiembro() + n.getFuncion() + n.getNivel())) continue;
                        if (actuales.stream().anyMatch(a -> mismo(n, a))) continue;
                        ps.setInt(1, id); ps.setInt(2, n.getIdMiembro()); ps.setString(3, n.getFuncion()); setTexto(ps, 4, n.getNivel()); ps.addBatch();
                    }
                    ps.executeBatch();
                }
                c.commit();
                return id;
            } catch (SQLException e) { c.rollback(); throw e; }
        }
    }

    private static boolean mismo(TallerPersona a, TallerPersona b) {
        return a.getIdMiembro() == b.getIdMiembro() && a.getFuncion().equals(b.getFuncion()) && Objects.equals(a.getNivel(), b.getNivel());
    }

    private void llenar(PreparedStatement ps, Taller t) throws SQLException {
        ps.setString(1, t.getCiclo()); setEntero(ps, 2, t.getNumero()); setFecha(ps, 3, t.getFecha());
        ps.setTime(4, Time.valueOf(t.getHoraInicio())); ps.setTime(5, Time.valueOf(t.getHoraFin()));
        setTexto(ps, 6, t.getLugar()); ps.setString(7, t.getEstado()); setTexto(ps, 8, t.getMusicaTexto()); setTexto(ps, 9, t.getNotas());
    }

    private void guardarPlanPasos(Connection c, int id, String nivel, List<TallerPaso> pasos) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        try (PreparedStatement up = c.prepareStatement("INSERT INTO taller_paso (id_taller, nivel, id_paso, orden, seccion, planificado) VALUES (?,?,?,?,?,1) " +
                "ON DUPLICATE KEY UPDATE orden=VALUES(orden), seccion=VALUES(seccion), planificado=1")) {
            int o = 0;
            for (TallerPaso p : pasos) {
                int idPaso = asegurarPaso(c, p.getNombre(), nivel);
                if (ids.contains(idPaso)) continue;
                ids.add(idPaso);
                up.setInt(1, id); up.setString(2, nivel); up.setInt(3, idPaso); up.setInt(4, ++o); up.setInt(5, p.getSeccion()); up.executeUpdate();
            }
        }
        // los planificados que ya no están en el plan: se borran si nadie los marcó; si ya tenían ✅/❌ quedan como "agregados"
        StringBuilder sql = new StringBuilder("SELECT id_paso, ensenado FROM taller_paso WHERE id_taller=? AND nivel=? AND planificado=1");
        try (PreparedStatement ps = c.prepareStatement(sql.toString())) {
            ps.setInt(1, id); ps.setString(2, nivel);
            List<int[]> fuera = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) if (!ids.contains(rs.getInt(1))) { rs.getBoolean(2); fuera.add(new int[]{rs.getInt(1), rs.wasNull() ? 0 : 1}); } }
            for (int[] f : fuera) {
                String q = f[1] == 0 ? "DELETE FROM taller_paso WHERE id_taller=? AND nivel=? AND id_paso=?"
                                     : "UPDATE taller_paso SET planificado=0 WHERE id_taller=? AND nivel=? AND id_paso=?";
                try (PreparedStatement d = c.prepareStatement(q)) { d.setInt(1, id); d.setString(2, nivel); d.setInt(3, f[0]); d.executeUpdate(); }
            }
        }
    }

    /** Busca el paso en el catálogo del nivel (sin importar mayúsculas ni tildes); si no está, lo crea. */
    private int asegurarPaso(Connection c, String nombre, String nivel) throws SQLException {
        String limpio = nombre.trim().replaceAll("\\s+", " ");
        try (PreparedStatement ps = c.prepareStatement("SELECT id_paso, activo FROM paso_taller WHERE nivel=? AND nombre=?")) {   // la colación ya ignora mayúsculas y tildes
            ps.setString(1, nivel); ps.setString(2, limpio);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    if (!rs.getBoolean(2)) try (PreparedStatement a = c.prepareStatement("UPDATE paso_taller SET activo=1 WHERE id_paso=?")) { a.setInt(1, id); a.executeUpdate(); }
                    return id;
                }
            }
        }
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO paso_taller (nombre, nivel) VALUES (?,?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, limpio.length() > 80 ? limpio.substring(0, 80) : limpio); ps.setString(2, nivel); ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) { rs.next(); return rs.getInt(1); }
        }
    }

    public void cambiarEstado(int id, String estado) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("UPDATE taller SET estado=? WHERE id_taller=?")) {
            ps.setString(1, estado); ps.setInt(2, id); ps.executeUpdate();
        }
    }

    public void eliminar(int id) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("DELETE FROM taller WHERE id_taller=?")) {
            ps.setInt(1, id); ps.executeUpdate();
        }
    }

    // ===================== DICTADO FINAL =====================

    /** asistencias: id de taller_persona -> A_TIEMPO/TARDE/NO_SE_PRESENTO · extras: personas que se sumaron o reemplazaron. */
    public void registrarFinal(int idTaller, Map<Integer, String> asistencias, List<TallerPersona> extras, int yo) throws SQLException {
        try (Connection c = getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement("UPDATE taller_persona SET asistencia=? WHERE id=? AND id_taller=?")) {
                    for (Map.Entry<Integer, String> e : asistencias.entrySet()) { ps.setString(1, e.getValue()); ps.setInt(2, e.getKey()); ps.setInt(3, idTaller); ps.addBatch(); }
                    ps.executeBatch();
                }
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO taller_persona (id_taller, id_miembro, funcion, nivel, planeado, asistencia, reemplaza_a) VALUES (?,?,?,?,0,?,?)")) {
                    for (TallerPersona p : extras) {
                        ps.setInt(1, idTaller); ps.setInt(2, p.getIdMiembro()); ps.setString(3, p.getFuncion()); setTexto(ps, 4, p.getNivel());
                        ps.setString(5, p.getAsistencia() == null ? "A_TIEMPO" : p.getAsistencia()); setEntero(ps, 6, p.getReemplazaA()); ps.addBatch();
                    }
                    ps.executeBatch();
                }
                try (PreparedStatement ps = c.prepareStatement("UPDATE taller SET final_por=?, final_fecha=NOW(), estado=IF(estado='CANCELADO', estado, 'REALIZADO') WHERE id_taller=?")) {
                    ps.setInt(1, yo); ps.setInt(2, idTaller); ps.executeUpdate();
                }
                c.commit();
            } catch (SQLException e) { c.rollback(); throw e; }
        }
    }

    public void quitarExtra(int idTaller, int idPersona) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("DELETE FROM taller_persona WHERE id=? AND id_taller=? AND planeado=0")) {
            ps.setInt(1, idPersona); ps.setInt(2, idTaller); ps.executeUpdate();
        }
    }

    // ===================== FORMULARIO POST-TALLER =====================

    /** Mis funciones efectivas en un taller (tallerista/apoyo), para armar mi formulario. */
    public List<TallerPersona> misFunciones(int idTaller, int yo) throws SQLException {
        try (Connection c = getConnection()) {
            List<TallerPersona> l = new ArrayList<>();
            for (TallerPersona p : personas(c, idTaller))
                if (p.getIdMiembro() == yo && p.isEfectivo() && ("TALLERISTA".equals(p.getFuncion()) || "APOYO".equals(p.getFuncion()))) l.add(p);
            return l;
        }
    }

    public boolean respondio(int idTaller, int yo) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("SELECT 1 FROM taller_respuesta WHERE id_taller=? AND id_miembro=?")) {
            ps.setInt(1, idTaller); ps.setInt(2, yo);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    /** Talleres ya realizados (o de hoy) cuyo formulario me falta, de los últimos 14 días. */
    public List<Taller> formulariosPendientes(int yo) throws SQLException {
        String sql = "SELECT DISTINCT t.* FROM taller t JOIN taller_persona p ON p.id_taller=t.id_taller " +
                     "WHERE p.id_miembro=? AND p.funcion IN ('TALLERISTA','APOYO') AND " + EFECTIVO + " AND t.estado IN ('PROGRAMADO','REALIZADO') " +
                     "AND t.fecha <= CURDATE() AND t.fecha >= CURDATE() - INTERVAL 14 DAY " +
                     "AND NOT EXISTS (SELECT 1 FROM taller_respuesta r WHERE r.id_taller=t.id_taller AND r.id_miembro=p.id_miembro) ORDER BY t.fecha DESC";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, yo);
            List<Taller> l = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) l.add(mapear(rs)); }
            return l;
        }
    }

    /**
     * Guarda el formulario de una persona.
     * checklist: idPaso -> enseñado (solo talleristas; lo llena uno por nivel, el otro puede corregirlo).
     * extras: pasos agregados por nivel · feedback: lista (para, tipo, texto) · sugerencia: para el taller.
     */
    public void guardarFormulario(int idTaller, int yo, Map<String, Map<Integer, Boolean>> checklist, Map<String, List<String>> extras,
                                  List<TallerFeedback> feedback, String sugerencia) throws SQLException {
        try (Connection c = getConnection()) {
            c.setAutoCommit(false);
            try {
                for (Map.Entry<String, Map<Integer, Boolean>> nv : checklist.entrySet()) {
                    String nivel = nv.getKey();
                    try (PreparedStatement ps = c.prepareStatement("UPDATE taller_paso SET ensenado=? WHERE id_taller=? AND nivel=? AND id_paso=?")) {
                        for (Map.Entry<Integer, Boolean> e : nv.getValue().entrySet()) { ps.setBoolean(1, e.getValue()); ps.setInt(2, idTaller); ps.setString(3, nivel); ps.setInt(4, e.getKey()); ps.addBatch(); }
                        ps.executeBatch();
                    }
                    for (String nombre : extras.getOrDefault(nivel, List.of())) {
                        int idPaso = asegurarPaso(c, nombre, nivel);
                        try (PreparedStatement ps = c.prepareStatement("INSERT INTO taller_paso (id_taller, nivel, id_paso, orden, seccion, planificado, ensenado) " +
                                "VALUES (?,?,?,999,99,0,1) ON DUPLICATE KEY UPDATE ensenado=1")) {
                            ps.setInt(1, idTaller); ps.setString(2, nivel); ps.setInt(3, idPaso); ps.executeUpdate();
                        }
                    }
                    try (PreparedStatement ps = c.prepareStatement("INSERT INTO taller_checklist (id_taller, nivel, respondido_por) VALUES (?,?,?) " +
                            "ON DUPLICATE KEY UPDATE respondido_por=VALUES(respondido_por), fecha=NOW()")) {
                        ps.setInt(1, idTaller); ps.setString(2, nivel); ps.setInt(3, yo); ps.executeUpdate();
                    }
                }
                // si vuelve a mandar el formulario, su feedback anterior se reemplaza
                try (PreparedStatement ps = c.prepareStatement("DELETE FROM taller_feedback WHERE id_taller=? AND de_miembro=?")) { ps.setInt(1, idTaller); ps.setInt(2, yo); ps.executeUpdate(); }
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO taller_feedback (id_taller, de_miembro, para_miembro, tipo, texto) VALUES (?,?,?,?,?)")) {
                    for (TallerFeedback f : feedback) {
                        ps.setInt(1, idTaller); ps.setInt(2, yo); setEntero(ps, 3, f.getParaMiembro()); ps.setString(4, f.getTipo()); ps.setString(5, f.getTexto()); ps.addBatch();
                    }
                    if (sugerencia != null) { ps.setInt(1, idTaller); ps.setInt(2, yo); ps.setNull(3, Types.INTEGER); ps.setString(4, "TALLER"); ps.setString(5, sugerencia); ps.addBatch(); }
                    ps.executeBatch();
                }
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO taller_respuesta (id_taller, id_miembro) VALUES (?,?) ON DUPLICATE KEY UPDATE fecha=NOW()")) {
                    ps.setInt(1, idTaller); ps.setInt(2, yo); ps.executeUpdate();
                }
                c.commit();
            } catch (SQLException e) { c.rollback(); throw e; }
        }
    }

    /** Lo que esta persona escribió antes (para mostrarlo si vuelve a abrir el formulario). */
    public List<TallerFeedback> feedbackEscritoPor(int idTaller, int yo) throws SQLException {
        return feedback(idTaller, "AND f.de_miembro=?", yo);
    }

    /** Quiénes ya respondieron el formulario. */
    public Map<Integer, String> respuestas(int idTaller) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("SELECT id_miembro, fecha FROM taller_respuesta WHERE id_taller=?")) {
            ps.setInt(1, idTaller);
            Map<Integer, String> m = new HashMap<>();
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) m.put(rs.getInt(1), Formato.ddmmHora(rs.getTimestamp(2).toLocalDateTime())); }
            return m;
        }
    }

    // ===================== FEEDBACK Y RESÚMENES =====================

    public List<TallerFeedback> feedbackDe(int idTaller) throws SQLException { return feedback(idTaller, "", null); }

    private List<TallerFeedback> feedback(int idTaller, String extra, Integer de) throws SQLException {
        String sql = "SELECT f.*, d.nombre AS de_nombre, p.nombre AS para_nombre FROM taller_feedback f JOIN miembro d ON d.id_miembro=f.de_miembro " +
                     "LEFT JOIN miembro p ON p.id_miembro=f.para_miembro WHERE f.id_taller=? " + extra + " ORDER BY p.nombre, f.tipo, f.fecha";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idTaller); if (de != null) ps.setInt(2, de);
            List<TallerFeedback> l = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TallerFeedback f = new TallerFeedback();
                    f.setId(rs.getInt("id")); f.setIdTaller(idTaller); f.setDeMiembro(rs.getInt("de_miembro")); f.setDe(rs.getString("de_nombre"));
                    f.setParaMiembro(entero(rs, "para_miembro")); f.setPara(rs.getString("para_nombre")); f.setTipo(rs.getString("tipo"));
                    f.setTexto(rs.getString("texto")); f.setFecha(fechaHora(rs, "fecha"));
                    l.add(f);
                }
            }
            return l;
        }
    }

    /** Resúmenes de un taller: para -> resumen. */
    public Map<Integer, TallerFeedback> resumenes(int idTaller) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("SELECT s.*, m.nombre FROM taller_resumen s JOIN miembro m ON m.id_miembro=s.escrito_por WHERE s.id_taller=?")) {
            ps.setInt(1, idTaller);
            Map<Integer, TallerFeedback> m = new HashMap<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TallerFeedback f = new TallerFeedback();
                    f.setIdTaller(idTaller); f.setParaMiembro(rs.getInt("para_miembro")); f.setTexto(rs.getString("texto")); f.setDe(rs.getString("nombre"));
                    f.setEnviado(rs.getBoolean("enviado")); f.setFecha(fechaHora(rs, "fecha_actualizacion")); f.setTipo("RESUMEN");
                    m.put(f.getParaMiembro(), f);
                }
            }
            return m;
        }
    }

    public void guardarResumen(int idTaller, int para, String texto, int yo, boolean enviar) throws SQLException {
        String sql = "INSERT INTO taller_resumen (id_taller, para_miembro, texto, escrito_por, enviado, fecha_envio) VALUES (?,?,?,?,?,?) " +
                     "ON DUPLICATE KEY UPDATE texto=VALUES(texto), escrito_por=VALUES(escrito_por), fecha_actualizacion=NOW(), " +
                     "enviado=GREATEST(enviado, VALUES(enviado)), fecha_envio=COALESCE(fecha_envio, VALUES(fecha_envio))";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idTaller); ps.setInt(2, para); ps.setString(3, texto); ps.setInt(4, yo); ps.setBoolean(5, enviar);
            if (enviar) ps.setTimestamp(6, new Timestamp(System.currentTimeMillis())); else ps.setNull(6, Types.TIMESTAMP);
            ps.executeUpdate();
        }
    }

    /** Resúmenes que me enviaron (anónimos), del más reciente al más antiguo. */
    public List<TallerFeedback> misResumenes(int yo) throws SQLException {
        String sql = "SELECT s.texto, s.fecha_envio, t.id_taller, t.numero, t.ciclo, t.fecha FROM taller_resumen s JOIN taller t ON t.id_taller=s.id_taller " +
                     "WHERE s.para_miembro=? AND s.enviado=1 ORDER BY t.fecha DESC";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, yo);
            List<TallerFeedback> l = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TallerFeedback f = new TallerFeedback();
                    f.setIdTaller(rs.getInt("id_taller")); f.setTexto(rs.getString("texto")); f.setFecha(fechaHora(rs, "fecha_envio"));
                    int n = rs.getInt("numero"); boolean sinNum = rs.wasNull();
                    f.setTallerTexto((sinNum ? "" : n + "º ") + "taller " + rs.getString("ciclo") + " · " + Formato.ddmmyyyy(fecha(rs, "fecha")));
                    l.add(f);
                }
            }
            return l;
        }
    }

    // ===================== CATÁLOGO DE PASOS =====================

    /** Pasos de un nivel (o todos con nivel null), con cuántas veces se planificaron/enseñaron. */
    public List<TallerPaso> catalogo(String nivel, boolean soloActivos) throws SQLException {
        String sql = "SELECT p.*, (SELECT COUNT(*) FROM taller_paso tp WHERE tp.id_paso=p.id_paso) AS usos FROM paso_taller p WHERE 1=1 " +
                     (nivel != null ? "AND p.nivel=? " : "") + (soloActivos ? "AND p.activo=1 " : "") +
                     "ORDER BY FIELD(p.nivel,'PRINCIPIANTE','BASICO','INTRO_RUEDA','RUEDA_BASICA'), usos DESC, p.nombre";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (nivel != null) ps.setString(1, nivel);
            List<TallerPaso> l = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TallerPaso p = new TallerPaso(rs.getString("nombre"), 1);
                    p.setIdPaso(rs.getInt("id_paso")); p.setNivel(rs.getString("nivel")); p.setActivo(rs.getBoolean("activo")); p.setUsos(rs.getInt("usos"));
                    l.add(p);
                }
            }
            return l;
        }
    }

    public void agregarPaso(String nombre, String nivel) throws SQLException {
        try (Connection c = getConnection()) { asegurarPaso(c, nombre, nivel); }
    }

    public void activarPaso(int idPaso, boolean activo) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("UPDATE paso_taller SET activo=? WHERE id_paso=?")) {
            ps.setBoolean(1, activo); ps.setInt(2, idPaso); ps.executeUpdate();
        }
    }

    public boolean renombrarPaso(int idPaso, String nombre) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(
                "UPDATE paso_taller SET nombre=? WHERE id_paso=? AND NOT EXISTS (SELECT 1 FROM (SELECT p2.id_paso FROM paso_taller p2 JOIN paso_taller p1 ON p1.id_paso=? " +
                "WHERE p2.nivel=p1.nivel AND p2.nombre=? AND p2.id_paso<>?) x)")) {
            ps.setString(1, nombre); ps.setInt(2, idPaso); ps.setInt(3, idPaso); ps.setString(4, nombre); ps.setInt(5, idPaso);
            return ps.executeUpdate() == 1;
        }
    }

    // ===================== HISTORIAL =====================

    /** Matriz de dictado (como la hoja DICTADO DE NIVELES): miembro -> id_taller -> código (con * si llegó tarde, ✗ si no fue). */
    public Map<String, Map<Integer, String>> dictado(List<Taller> talleres, Map<String, Integer> idMiembro) throws SQLException {
        Map<String, Map<Integer, String>> m = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        String sql = "SELECT p.*, mi.nombre FROM taller_persona p JOIN miembro mi ON mi.id_miembro=p.id_miembro JOIN taller t ON t.id_taller=p.id_taller " +
                     "WHERE t.estado<>'CANCELADO' ORDER BY p.planeado";
        try (Connection c = getConnection(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String nom = rs.getString("nombre");
                idMiembro.put(nom, rs.getInt("id_miembro"));
                String cod = Taller.codigo(rs.getString("funcion"), rs.getString("nivel"));
                String asis = rs.getString("asistencia");
                if ("NO_SE_PRESENTO".equals(asis)) cod = "✗" + cod;
                else if ("TARDE".equals(asis)) cod = cod + "*";
                m.computeIfAbsent(nom, k -> new HashMap<>()).merge(rs.getInt("id_taller"), cod, (a, b) -> a + " " + b);
            }
        }
        return m;
    }

    /** Resumen de una persona: [veces tallerista, veces apoyo, veces maestro, tardanzas, faltas, reemplazos que hizo]. */
    public int[] resumenPersona(int idMiembro) throws SQLException {
        String sql = "SELECT SUM(p.funcion='TALLERISTA' AND " + EFECTIVO + "), SUM(p.funcion='APOYO' AND " + EFECTIVO + "), SUM(p.funcion='MAESTRO' AND " + EFECTIVO + "), " +
                     "SUM(p.asistencia='TARDE'), SUM(p.asistencia='NO_SE_PRESENTO'), SUM(p.reemplaza_a IS NOT NULL) " +
                     "FROM taller_persona p JOIN taller t ON t.id_taller=p.id_taller WHERE p.id_miembro=? AND t.estado<>'CANCELADO'";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idMiembro);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); int[] r = new int[6]; for (int i = 0; i < 6; i++) r[i] = rs.getInt(i + 1); return r; }
        }
    }

    private static String nz(String s) { return s == null ? "" : s; }
}
