package pe.sdp.daos;

import pe.sdp.util.Formato;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Encuestas: las crean los admins/moderadores de cualquier team (permiso CREAR_ENCUESTAS) para todo el grupo
 * o para un team. Se puede cambiar el voto hasta el cierre. "Anónima" = no se muestran los nombres
 * (el voto igual queda guardado con la persona, para que nadie vote dos veces).
 */
public class EncuestaDao extends BaseDao {

    public static final List<String> TIPOS = List.of("SI_NO", "UNA", "VARIAS", "FECHAS");

    private static LocalDateTime ahora() { return LocalDateTime.now(Formato.ZONA); }

    /** ¿Esta encuesta es para mí? (para todos, o soy del team, o la creé) */
    private static final String PARA_MI =
            "(e.id_team IS NULL OR e.creada_por = ? OR EXISTS (SELECT 1 FROM miembro_team mt WHERE mt.id_team = e.id_team AND mt.id_miembro = ?))";

    private static final String SQL =
            "SELECT e.*, m.nombre AS creador, t.nombre AS team, " +
            " (SELECT COUNT(DISTINCT v.id_miembro) FROM encuesta_voto v WHERE v.id_encuesta=e.id) AS votantes, " +
            " (SELECT COUNT(*) FROM encuesta_voto v WHERE v.id_encuesta=e.id AND v.id_miembro=?) AS mis_votos, " +
            " CASE WHEN e.id_team IS NULL THEN (SELECT COUNT(*) FROM miembro x WHERE x.estado='ACTIVO') " +
            "      ELSE (SELECT COUNT(*) FROM miembro_team mt JOIN miembro x ON x.id_miembro=mt.id_miembro WHERE mt.id_team=e.id_team AND x.estado='ACTIVO') END AS publico " +
            "FROM encuesta e LEFT JOIN miembro m ON m.id_miembro=e.creada_por LEFT JOIN team t ON t.id_team=e.id_team ";

    private Map<String, Object> fila(ResultSet rs) throws SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        LocalDateTime cierre = fechaHora(rs, "cierre");
        boolean abierta = cierre != null && ahora().isBefore(cierre);
        String tipo = rs.getString("tipo");
        m.put("id", rs.getInt("id"));
        m.put("pregunta", rs.getString("pregunta"));
        m.put("descripcion", rs.getString("descripcion"));
        m.put("tipo", tipo);
        m.put("multiple", "VARIAS".equals(tipo) || "FECHAS".equals(tipo));
        m.put("anonima", rs.getBoolean("anonima"));
        m.put("verAntes", rs.getBoolean("ver_antes"));
        m.put("idTeam", entero(rs, "id_team"));
        m.put("team", rs.getString("team"));
        m.put("cierre", cierre);
        m.put("cierreTexto", cierre == null ? "" : Formato.fechaLarga(cierre.toLocalDate()) + " a las " + Formato.hora(cierre.toLocalTime()));
        m.put("abierta", abierta);
        m.put("idCreador", entero(rs, "creada_por"));
        m.put("creador", rs.getString("creador"));
        int votantes = rs.getInt("votantes"), publico = rs.getInt("publico");
        m.put("votantes", votantes);
        m.put("publico", publico);
        m.put("participacion", publico == 0 ? 0 : Math.round(votantes * 100f / publico));
        m.put("yaVote", rs.getInt("mis_votos") > 0);
        return m;
    }

    /** Las encuestas que me tocan: abiertas primero (por cierre), luego cerradas recientes. */
    public List<Map<String, Object>> paraMi(int yo, boolean abiertas) throws SQLException {
        List<Map<String, Object>> l = new ArrayList<>();
        String sql = SQL + "WHERE " + PARA_MI + " AND e.cierre " + (abiertas ? "> ? ORDER BY e.cierre" : "<= ? ORDER BY e.cierre DESC LIMIT 30");
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, yo); ps.setInt(2, yo); ps.setInt(3, yo); setFechaHora(ps, 4, ahora());
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) l.add(fila(rs)); }
        }
        return l;
    }

    /** Abiertas donde todavía no voto (para Pendientes). */
    public List<Map<String, Object>> sinVotar(int yo) throws SQLException {
        List<Map<String, Object>> l = new ArrayList<>();
        for (Map<String, Object> e : paraMi(yo, true)) if (!Boolean.TRUE.equals(e.get("yaVote"))) l.add(e);
        return l;
    }

    /** Una encuesta, solo si es para mí (null si no existe o no me toca). Incluye opciones, conteos y mis votos. */
    public Map<String, Object> ver(int id, int yo) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(SQL + "WHERE e.id=? AND " + PARA_MI)) {
            ps.setInt(1, yo); ps.setInt(2, id); ps.setInt(3, yo); ps.setInt(4, yo);
            Map<String, Object> e;
            try (ResultSet rs = ps.executeQuery()) { if (!rs.next()) return null; e = fila(rs); }
            List<Map<String, Object>> ops = new ArrayList<>();
            Map<Integer, Map<String, Object>> porId = new HashMap<>();
            try (PreparedStatement po = c.prepareStatement(
                    "SELECT o.id, o.texto, (SELECT COUNT(*) FROM encuesta_voto v WHERE v.id_opcion=o.id) AS votos, " +
                    "(SELECT COUNT(*) FROM encuesta_voto v WHERE v.id_opcion=o.id AND v.id_miembro=?) AS mio " +
                    "FROM encuesta_opcion o WHERE o.id_encuesta=? ORDER BY o.orden, o.id")) {
                po.setInt(1, yo); po.setInt(2, id);
                try (ResultSet rs = po.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> o = new LinkedHashMap<>();
                        o.put("id", rs.getInt(1)); o.put("texto", rs.getString(2)); o.put("votos", rs.getInt(3)); o.put("mio", rs.getInt(4) > 0);
                        o.put("nombres", new ArrayList<String>());
                        ops.add(o); porId.put(rs.getInt(1), o);
                    }
                }
            }
            int max = 0;
            for (Map<String, Object> o : ops) max = Math.max(max, (Integer) o.get("votos"));
            int votantes = (Integer) e.get("votantes");
            for (Map<String, Object> o : ops) {
                int v = (Integer) o.get("votos");
                o.put("pct", votantes == 0 ? 0 : Math.round(v * 100f / votantes));
                o.put("gana", v > 0 && v == max);
            }
            if (!Boolean.TRUE.equals(e.get("anonima"))) {
                try (PreparedStatement pn = c.prepareStatement(
                        "SELECT v.id_opcion, m.nombre, m.apodo FROM encuesta_voto v JOIN miembro m ON m.id_miembro=v.id_miembro WHERE v.id_encuesta=? ORDER BY m.nombre")) {
                    pn.setInt(1, id);
                    try (ResultSet rs = pn.executeQuery()) {
                        while (rs.next()) {
                            Map<String, Object> o = porId.get(rs.getInt(1));
                            String ap = rs.getString(3);
                            if (o != null) ((List<String>) o.get("nombres")).add(rs.getString(2) + (ap == null || ap.isBlank() ? "" : " (" + ap + ")"));
                        }
                    }
                }
            }
            e.put("opciones", ops);
            return e;
        }
    }

    public int crear(String pregunta, String descripcion, String tipo, boolean anonima, boolean verAntes, Integer idTeam,
                     LocalDateTime cierre, List<String> opciones, int yo) throws SQLException {
        try (Connection c = getConnection()) {
            c.setAutoCommit(false);
            try {
                int id;
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO encuesta (pregunta, descripcion, tipo, anonima, ver_antes, id_team, cierre, creada_por) VALUES (?,?,?,?,?,?,?,?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, pregunta); setTexto(ps, 2, descripcion); ps.setString(3, tipo); ps.setBoolean(4, anonima); ps.setBoolean(5, verAntes);
                    if (idTeam == null) ps.setNull(6, Types.SMALLINT); else ps.setInt(6, idTeam);
                    setFechaHora(ps, 7, cierre); ps.setInt(8, yo);
                    ps.executeUpdate();
                    try (ResultSet k = ps.getGeneratedKeys()) { k.next(); id = k.getInt(1); }
                }
                try (PreparedStatement po = c.prepareStatement("INSERT INTO encuesta_opcion (id_encuesta, texto, orden) VALUES (?,?,?)")) {
                    int i = 0;
                    for (String o : opciones) { po.setInt(1, id); po.setString(2, o); po.setInt(3, i++); po.addBatch(); }
                    po.executeBatch();
                }
                c.commit();
                return id;
            } catch (SQLException e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
    }

    /** Reemplaza mis votos (solo opciones de esa encuesta, y solo si sigue abierta). Devuelve cuántas opciones quedaron marcadas. */
    public int votar(int idEncuesta, List<Integer> opciones, int yo) throws SQLException {
        try (Connection c = getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ab = c.prepareStatement("SELECT 1 FROM encuesta WHERE id=? AND cierre > ? FOR UPDATE")) {
                    ab.setInt(1, idEncuesta); setFechaHora(ab, 2, ahora());
                    try (ResultSet rs = ab.executeQuery()) { if (!rs.next()) { c.rollback(); return -1; } }
                }
                try (PreparedStatement del = c.prepareStatement("DELETE FROM encuesta_voto WHERE id_encuesta=? AND id_miembro=?")) {
                    del.setInt(1, idEncuesta); del.setInt(2, yo); del.executeUpdate();
                }
                int n = 0;
                try (PreparedStatement ins = c.prepareStatement(
                        "INSERT IGNORE INTO encuesta_voto (id_opcion, id_miembro, id_encuesta) SELECT id, ?, id_encuesta FROM encuesta_opcion WHERE id=? AND id_encuesta=?")) {
                    for (int o : opciones) { ins.setInt(1, yo); ins.setInt(2, o); ins.setInt(3, idEncuesta); n += ins.executeUpdate(); }
                }
                c.commit();
                return n;
            } catch (SQLException e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
    }

    public void cerrarYa(int id) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("UPDATE encuesta SET cierre=? WHERE id=? AND cierre > ?")) {
            setFechaHora(ps, 1, ahora()); ps.setInt(2, id); setFechaHora(ps, 3, ahora()); ps.executeUpdate();
        }
    }

    public void eliminar(int id) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("DELETE FROM encuesta WHERE id=?")) {
            ps.setInt(1, id); ps.executeUpdate();
        }
    }
}
