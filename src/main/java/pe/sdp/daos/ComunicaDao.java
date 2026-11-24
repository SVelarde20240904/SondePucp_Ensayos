package pe.sdp.daos;

import pe.sdp.util.Formato;

import java.sql.*;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Módulo Comunica: saludos de cumpleaños (quién redacta cada uno y en qué va), proyectos de contenido
 * (las ideas son proyectos en estado IDEA, con votos) y plantillas de flyers.
 * Las filas se devuelven como mapas (como en CajaDao): son pantallas de lectura, sin lógica propia.
 */
public class ComunicaDao extends BaseDao {

    public static final List<String> ESTADOS_SALUDO = List.of("PENDIENTE", "REDACTADO", "VIDEO_LISTO", "PUBLICADO", "OMITIDO");
    public static final List<String> TIPOS = List.of("TIKTOK", "REEL", "POST", "HISTORIA", "DESTACADA", "FLYER", "MEME", "TUTORIAL", "COMUNICADO", "OTRO");
    public static final List<String> REDES = List.of("INSTAGRAM", "TIKTOK", "TELEGRAM", "VARIAS");
    public static final List<String> ESTADOS_PROYECTO = List.of("IDEA", "EN_DESARROLLO", "PUBLICADO", "ARCHIVADO");
    public static final List<String> TIPOS_PLANTILLA = List.of("Hoy no hay taller", "Taller", "Evento", "Cumpleaños", "Cuota", "Comunicado", "Otro");
    public static final String PLANTILLA_SALUDO = "comunica.plantilla_saludo";

    private static LocalDate hoy() { return LocalDate.now(Formato.ZONA); }

    // ======================================================================
    //  Saludos de cumpleaños
    // ======================================================================

    /** El cumpleaños de alguien en un año dado (29/02 → 28/02 si el año no es bisiesto). */
    static LocalDate cumpleEn(LocalDate nacimiento, int anio) {
        try { return nacimiento.withYear(anio); } catch (DateTimeException e) { return LocalDate.of(anio, 2, 28); }
    }

    /**
     * Crea los saludos que falten (de hoy hasta fin del mes siguiente), y reparte entre el team Comunica los que
     * no tienen responsable: le toca a quien menos saludos lleva en el año (nunca el propio cumpleañero).
     */
    public void generar() throws SQLException {
        LocalDate h = hoy();
        YearMonth desde = YearMonth.from(h), hasta = desde.plusMonths(1);
        try (Connection c = getConnection()) {
            c.setAutoCommit(false);
            try {
                // 1) filas que faltan
                try (Statement st = c.createStatement();
                     ResultSet rs = st.executeQuery("SELECT id_miembro, fecha_nacimiento FROM miembro WHERE estado='ACTIVO' AND fecha_nacimiento IS NOT NULL");
                     PreparedStatement ins = c.prepareStatement("INSERT IGNORE INTO com_saludo (id_miembro, anio, fecha) VALUES (?,?,?)")) {
                    while (rs.next()) {
                        LocalDate n = rs.getDate(2).toLocalDate();
                        for (YearMonth ym = desde; !ym.isAfter(hasta); ym = ym.plusMonths(1)) {
                            LocalDate f = cumpleEn(n, ym.getYear());
                            // solo de hoy en adelante: al estrenar el módulo (o al ingresar alguien) no aparecen saludos "atrasados" que ya se hicieron a mano
                            if (YearMonth.from(f).equals(ym) && !f.isBefore(h)) {
                                ins.setInt(1, rs.getInt(1)); ins.setInt(2, ym.getYear()); ins.setDate(3, java.sql.Date.valueOf(f)); ins.addBatch();
                            }
                        }
                    }
                    ins.executeBatch();
                }
                // 2) repartir los que no tienen responsable
                List<Integer> equipo = new ArrayList<>();
                try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(
                        "SELECT m.id_miembro FROM miembro m JOIN miembro_team mt ON mt.id_miembro=m.id_miembro JOIN team t ON t.id_team=mt.id_team " +
                        "WHERE t.codigo='COMUNICA' AND m.estado='ACTIVO' ORDER BY m.nombre")) {
                    while (rs.next()) equipo.add(rs.getInt(1));
                }
                if (!equipo.isEmpty()) {
                    List<int[]> sinResp = new ArrayList<>();       // {id, idMiembro, anio}
                    try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(
                            "SELECT id, id_miembro, anio FROM com_saludo WHERE id_responsable IS NULL AND estado <> 'OMITIDO' ORDER BY fecha")) {
                        while (rs.next()) sinResp.add(new int[]{rs.getInt(1), rs.getInt(2), rs.getInt(3)});
                    }
                    Map<String, Integer> carga = new HashMap<>();   // "anio|idResp" -> cuántos lleva
                    try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(
                            "SELECT anio, id_responsable, COUNT(*) FROM com_saludo WHERE id_responsable IS NOT NULL GROUP BY anio, id_responsable")) {
                        while (rs.next()) carga.put(rs.getInt(1) + "|" + rs.getInt(2), rs.getInt(3));
                    }
                    try (PreparedStatement up = c.prepareStatement("UPDATE com_saludo SET id_responsable=? WHERE id=? AND id_responsable IS NULL")) {
                        for (int[] s : sinResp) {
                            Integer mejor = null; int min = Integer.MAX_VALUE;
                            for (int r : equipo) {
                                if (r == s[1]) continue;                                   // nadie redacta su propio saludo
                                int n = carga.getOrDefault(s[2] + "|" + r, 0);
                                if (n < min) { min = n; mejor = r; }
                            }
                            if (mejor == null) continue;
                            carga.merge(s[2] + "|" + mejor, 1, Integer::sum);
                            up.setInt(1, mejor); up.setInt(2, s[0]); up.addBatch();
                        }
                        up.executeBatch();
                    }
                }
                c.commit();
            } catch (SQLException e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
    }

    private static volatile LocalDate ultimaGeneracion;

    /** generar() una vez al día (para Inicio/Pendientes, que se abren a cada rato). */
    public void generarSiHaceFalta() throws SQLException {
        LocalDate h = hoy();
        if (h.equals(ultimaGeneracion)) return;
        generar();
        ultimaGeneracion = h;
    }

    private static final String SALUDO_SQL =
            "SELECT s.*, m.nombre, m.apodo, m.estado AS estado_miembro, r.nombre AS responsable, a.nombre AS actualizado_nombre " +
            "FROM com_saludo s JOIN miembro m ON m.id_miembro=s.id_miembro LEFT JOIN miembro r ON r.id_miembro=s.id_responsable " +
            "LEFT JOIN miembro a ON a.id_miembro=s.actualizado_por ";

    private Map<String, Object> saludo(ResultSet rs) throws SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        LocalDate f = fecha(rs, "fecha");
        String estado = rs.getString("estado");
        m.put("id", rs.getInt("id"));
        m.put("idMiembro", rs.getInt("id_miembro"));
        m.put("nombre", rs.getString("nombre"));
        String apodo = rs.getString("apodo");
        m.put("apodo", apodo);
        m.put("comoLlamarlo", apodo != null && !apodo.isBlank() ? apodo : rs.getString("nombre").split(" ")[0]);
        m.put("iniciales", Formato.iniciales(rs.getString("nombre")));
        m.put("fecha", f);
        m.put("fechaTexto", Formato.diaMes(f));
        m.put("diaSemana", Formato.fechaLarga(f));
        long dias = ChronoUnit.DAYS.between(hoy(), f);
        m.put("dias", dias);
        boolean listo = "PUBLICADO".equals(estado) || "OMITIDO".equals(estado);
        m.put("cuando", dias == 0 ? "¡Hoy!" : dias == 1 ? "Mañana" : dias > 1 ? "En " + dias + " días" : (listo ? "Pasó" : "Pasó hace " + (-dias) + (dias == -1 ? " día" : " días")));
        m.put("atrasado", dias < 0 && !listo);
        m.put("urgente", dias >= 0 && dias <= 2 && !listo);
        m.put("idResponsable", entero(rs, "id_responsable"));
        m.put("responsable", rs.getString("responsable"));
        m.put("estado", estado);
        m.put("estadoTexto", estadoSaludoTexto(estado));
        m.put("tono", switch (estado) { case "PUBLICADO" -> "ok"; case "OMITIDO" -> "neutral"; case "PENDIENTE" -> "warn"; default -> "info"; });
        m.put("paso", ESTADOS_SALUDO.indexOf(estado));
        m.put("texto", rs.getString("texto"));
        m.put("linkVideo", rs.getString("link_video"));
        m.put("publicadoEn", rs.getString("publicado_en"));
        m.put("actualizadoPor", rs.getString("actualizado_nombre"));
        m.put("miembroActivo", "ACTIVO".equals(rs.getString("estado_miembro")));
        return m;
    }

    public static String estadoSaludoTexto(String e) {
        return switch (e == null ? "" : e) {
            case "PENDIENTE" -> "Por redactar";
            case "REDACTADO" -> "Redactado";
            case "VIDEO_LISTO" -> "Video listo";
            case "PUBLICADO" -> "Publicado";
            case "OMITIDO" -> "No se publica";
            default -> e;
        };
    }

    /** Saludos de un mes, en orden de fecha. */
    public List<Map<String, Object>> saludosDelMes(YearMonth ym) throws SQLException {
        List<Map<String, Object>> l = new ArrayList<>();
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(SALUDO_SQL + "WHERE s.fecha BETWEEN ? AND ? ORDER BY s.fecha, m.nombre")) {
            setFecha(ps, 1, ym.atDay(1)); setFecha(ps, 2, ym.atEndOfMonth());
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) l.add(saludo(rs)); }
        }
        return l;
    }

    public Map<String, Object> saludo(int id) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(SALUDO_SQL + "WHERE s.id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? saludo(rs) : null; }
        }
    }

    /** Mis saludos sin publicar: los atrasados y los de los próximos "dias" días. */
    public List<Map<String, Object>> misSaludosPendientes(int yo, int dias) throws SQLException {
        List<Map<String, Object>> l = new ArrayList<>();
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(SALUDO_SQL +
                "WHERE s.id_responsable=? AND s.estado NOT IN ('PUBLICADO','OMITIDO') AND s.fecha <= ? AND s.fecha >= ? ORDER BY s.fecha")) {
            ps.setInt(1, yo); setFecha(ps, 2, hoy().plusDays(dias)); setFecha(ps, 3, hoy().minusDays(30));
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) l.add(saludo(rs)); }
        }
        return l;
    }

    /** Cuántos saludos lleva cada miembro del team en el año (para ver que el reparto sea parejo). */
    public List<Map<String, Object>> cargaDelAnio(int anio) throws SQLException {
        List<Map<String, Object>> l = new ArrayList<>();
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(
                "SELECT m.id_miembro, m.nombre, COUNT(s.id) AS total, SUM(s.estado='PUBLICADO') AS publicados " +
                "FROM miembro m JOIN miembro_team mt ON mt.id_miembro=m.id_miembro JOIN team t ON t.id_team=mt.id_team AND t.codigo='COMUNICA' " +
                "LEFT JOIN com_saludo s ON s.id_responsable=m.id_miembro AND s.anio=? WHERE m.estado='ACTIVO' GROUP BY m.id_miembro, m.nombre ORDER BY m.nombre")) {
            ps.setInt(1, anio);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", rs.getInt(1)); m.put("nombre", rs.getString(2)); m.put("total", rs.getInt(3)); m.put("publicados", rs.getInt(4));
                    l.add(m);
                }
            }
        }
        return l;
    }

    public void guardarSaludo(int id, String texto, String linkVideo, String publicadoEn, String estado, int yo) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(
                "UPDATE com_saludo SET texto=?, link_video=?, publicado_en=?, estado=?, actualizado_por=?, fecha_actualizacion=NOW() WHERE id=?")) {
            setTexto(ps, 1, texto); setTexto(ps, 2, linkVideo); setTexto(ps, 3, publicadoEn); ps.setString(4, estado);
            ps.setInt(5, yo); ps.setInt(6, id); ps.executeUpdate();
        }
    }

    public void asignarSaludo(int id, Integer idResponsable, int yo) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(
                "UPDATE com_saludo SET id_responsable=?, actualizado_por=?, fecha_actualizacion=NOW() WHERE id=?")) {
            setEntero(ps, 1, idResponsable); ps.setInt(2, yo); ps.setInt(3, id); ps.executeUpdate();
        }
    }

    /** Miembros activos del team Comunica (para asignar responsables). {id, nombre} */
    public List<Map<String, Object>> equipo() throws SQLException {
        List<Map<String, Object>> l = new ArrayList<>();
        try (Connection c = getConnection(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(
                "SELECT m.id_miembro, m.nombre FROM miembro m JOIN miembro_team mt ON mt.id_miembro=m.id_miembro JOIN team t ON t.id_team=mt.id_team " +
                "WHERE t.codigo='COMUNICA' AND m.estado='ACTIVO' ORDER BY m.nombre")) {
            while (rs.next()) { Map<String, Object> m = new LinkedHashMap<>(); m.put("id", rs.getInt(1)); m.put("nombre", rs.getString(2)); l.add(m); }
        }
        return l;
    }

    public boolean esDelEquipo(int idMiembro) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(
                "SELECT 1 FROM miembro m JOIN miembro_team mt ON mt.id_miembro=m.id_miembro JOIN team t ON t.id_team=mt.id_team " +
                "WHERE t.codigo='COMUNICA' AND m.estado='ACTIVO' AND m.id_miembro=?")) {
            ps.setInt(1, idMiembro);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    // ======================================================================
    //  Proyectos e ideas
    // ======================================================================

    private static final String PROY_SQL =
            "SELECT p.*, r.nombre AS responsable, pp.nombre AS propuesto_nombre, " +
            " (SELECT COUNT(*) FROM com_voto v WHERE v.id_proyecto=p.id) AS votos, " +
            " (SELECT COUNT(*) FROM com_voto v WHERE v.id_proyecto=p.id AND v.id_miembro=?) AS mi_voto, " +
            " (SELECT COUNT(*) FROM com_tarea t WHERE t.id_proyecto=p.id) AS tareas, " +
            " (SELECT COUNT(*) FROM com_tarea t WHERE t.id_proyecto=p.id AND t.hecha=1) AS tareas_hechas " +
            "FROM com_proyecto p LEFT JOIN miembro r ON r.id_miembro=p.id_responsable LEFT JOIN miembro pp ON pp.id_miembro=p.propuesto_por ";

    private Map<String, Object> proyecto(ResultSet rs) throws SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        String tipo = rs.getString("tipo"), red = rs.getString("red"), estado = rs.getString("estado");
        m.put("id", rs.getInt("id"));
        m.put("titulo", rs.getString("titulo"));
        m.put("tipo", tipo); m.put("tipoTexto", tipoTexto(tipo)); m.put("tipoIcono", tipoIcono(tipo));
        m.put("red", red); m.put("redTexto", redTexto(red));
        m.put("estado", estado); m.put("estadoTexto", estadoProyectoTexto(estado));
        m.put("descripcion", rs.getString("descripcion"));
        String et = rs.getString("etiquetas");
        m.put("etiquetas", et);
        List<String> tags = new ArrayList<>();
        if (et != null) for (String t : et.split(",")) if (!t.isBlank()) tags.add(t.trim());
        m.put("tags", tags);
        m.put("idResponsable", entero(rs, "id_responsable"));
        m.put("responsable", rs.getString("responsable"));
        LocalDate fo = fecha(rs, "fecha_objetivo");
        m.put("fechaObjetivo", fo);
        m.put("fechaObjetivoTexto", fo == null ? null : Formato.diaMes(fo));
        m.put("vencido", fo != null && fo.isBefore(hoy()) && "EN_DESARROLLO".equals(estado));
        m.put("linkReferencia", rs.getString("link_referencia"));
        m.put("linkDrive", rs.getString("link_drive"));
        m.put("linkPublicacion", rs.getString("link_publicacion"));
        m.put("propuestoPor", rs.getString("propuesto_nombre"));
        m.put("idPropuestoPor", entero(rs, "propuesto_por"));
        java.time.LocalDateTime fc = fechaHora(rs, "fecha_creacion");
        m.put("fechaCreacionTexto", fc == null ? "" : Formato.diaMes(fc.toLocalDate()));
        LocalDate fp = fecha(rs, "fecha_publicacion");
        m.put("fechaPublicacion", fp);
        m.put("fechaPublicacionTexto", fp == null ? null : Formato.ddmmyyyy(fp));
        m.put("votos", rs.getInt("votos"));
        m.put("miVoto", rs.getInt("mi_voto") > 0);
        m.put("tareas", rs.getInt("tareas"));
        m.put("tareasHechas", rs.getInt("tareas_hechas"));
        return m;
    }

    public static String tipoTexto(String t) {
        return switch (t == null ? "" : t) {
            case "TIKTOK" -> "TikTok"; case "REEL" -> "Reel"; case "POST" -> "Post"; case "HISTORIA" -> "Historia";
            case "DESTACADA" -> "Destacada"; case "FLYER" -> "Flyer"; case "MEME" -> "Meme"; case "TUTORIAL" -> "Tutorial";
            case "COMUNICADO" -> "Comunicado"; default -> "Otro";
        };
    }

    public static String tipoIcono(String t) {
        return switch (t == null ? "" : t) {
            case "TIKTOK", "REEL" -> "🎬"; case "POST" -> "🖼️"; case "HISTORIA", "DESTACADA" -> "⭕"; case "FLYER" -> "🎨";
            case "MEME" -> "😂"; case "TUTORIAL" -> "💃"; case "COMUNICADO" -> "📢"; default -> "✨";
        };
    }

    public static String redTexto(String r) {
        return switch (r == null ? "" : r) {
            case "INSTAGRAM" -> "Instagram"; case "TIKTOK" -> "TikTok"; case "TELEGRAM" -> "Telegram"; default -> "Varias redes";
        };
    }

    public static String estadoProyectoTexto(String e) {
        return switch (e == null ? "" : e) {
            case "IDEA" -> "Idea"; case "EN_DESARROLLO" -> "En desarrollo"; case "PUBLICADO" -> "Publicado"; case "ARCHIVADO" -> "Archivado"; default -> e;
        };
    }

    /** Proyectos en esos estados. Ideas: por votos; lo demás: por fecha. Si tag no es null, solo con esa etiqueta. */
    public List<Map<String, Object>> proyectos(int yo, List<String> estados, String tag) throws SQLException {
        List<Map<String, Object>> l = new ArrayList<>();
        String in = String.join(",", Collections.nCopies(estados.size(), "?"));
        String orden = estados.equals(List.of("IDEA")) ? "votos DESC, p.fecha_creacion DESC"
                : "FIELD(p.estado,'EN_DESARROLLO','PUBLICADO','ARCHIVADO','IDEA'), COALESCE(p.fecha_publicacion, p.fecha_objetivo, DATE(p.fecha_creacion)) DESC, p.id DESC";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(PROY_SQL + "WHERE p.estado IN (" + in + ") ORDER BY " + orden)) {
            int i = 1;
            ps.setInt(i++, yo);
            for (String e : estados) ps.setString(i++, e);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = proyecto(rs);
                    if (tag != null && !((List<?>) m.get("tags")).stream().anyMatch(x -> x.toString().equalsIgnoreCase(tag))) continue;
                    l.add(m);
                }
            }
        }
        return l;
    }

    public Map<String, Object> proyecto(int id, int yo) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(PROY_SQL + "WHERE p.id=?")) {
            ps.setInt(1, yo); ps.setInt(2, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Map<String, Object> m = proyecto(rs);
                m.put("listaTareas", tareas(c, id));
                return m;
            }
        }
    }

    private List<Map<String, Object>> tareas(Connection c, int idProyecto) throws SQLException {
        List<Map<String, Object>> l = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement("SELECT id, texto, hecha FROM com_tarea WHERE id_proyecto=? ORDER BY id")) {
            ps.setInt(1, idProyecto);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) { Map<String, Object> m = new LinkedHashMap<>(); m.put("id", rs.getInt(1)); m.put("texto", rs.getString(2)); m.put("hecha", rs.getBoolean(3)); l.add(m); }
            }
        }
        return l;
    }

    /** Etiquetas usadas (para filtros y sugerencias). */
    public List<String> etiquetas() throws SQLException {
        Set<String> s = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        try (Connection c = getConnection(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT etiquetas FROM com_proyecto WHERE etiquetas IS NOT NULL")) {
            while (rs.next()) for (String t : rs.getString(1).split(",")) if (!t.isBlank()) s.add(t.trim());
        }
        return new ArrayList<>(s);
    }

    /** Ideas que propuse yo (cualquier miembro), con su estado. */
    public List<Map<String, Object>> misPropuestas(int yo) throws SQLException {
        List<Map<String, Object>> l = new ArrayList<>();
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(PROY_SQL + "WHERE p.propuesto_por=? ORDER BY p.fecha_creacion DESC")) {
            ps.setInt(1, yo); ps.setInt(2, yo);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) l.add(proyecto(rs)); }
        }
        return l;
    }

    /** Crea un proyecto o idea. Devuelve el id. */
    public int crear(Map<String, Object> d, int yo) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(
                "INSERT INTO com_proyecto (titulo, tipo, red, estado, descripcion, etiquetas, id_responsable, fecha_objetivo, link_referencia, link_drive, " +
                "link_publicacion, propuesto_por, actualizado_por, fecha_actualizacion) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,NOW())", Statement.RETURN_GENERATED_KEYS)) {
            llenar(ps, d);
            ps.setInt(12, yo); ps.setInt(13, yo);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { k.next(); return k.getInt(1); }
        }
    }

    public void actualizar(int id, Map<String, Object> d, int yo) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(
                "UPDATE com_proyecto SET titulo=?, tipo=?, red=?, estado=?, descripcion=?, etiquetas=?, id_responsable=?, fecha_objetivo=?, " +
                "link_referencia=?, link_drive=?, link_publicacion=?, actualizado_por=?, fecha_actualizacion=NOW(), " +
                "fecha_publicacion = CASE WHEN ?='PUBLICADO' THEN COALESCE(fecha_publicacion, CURDATE()) WHEN ?='ARCHIVADO' THEN fecha_publicacion ELSE NULL END " +
                "WHERE id=?")) {
            llenar(ps, d);
            ps.setInt(12, yo); ps.setString(13, (String) d.get("estado")); ps.setString(14, (String) d.get("estado")); ps.setInt(15, id);
            ps.executeUpdate();
        }
    }

    private static void llenar(PreparedStatement ps, Map<String, Object> d) throws SQLException {
        ps.setString(1, (String) d.get("titulo"));
        ps.setString(2, (String) d.get("tipo"));
        ps.setString(3, (String) d.get("red"));
        ps.setString(4, (String) d.get("estado"));
        setTexto(ps, 5, (String) d.get("descripcion"));
        setTexto(ps, 6, (String) d.get("etiquetas"));
        setEntero(ps, 7, (Integer) d.get("idResponsable"));
        setFecha(ps, 8, (LocalDate) d.get("fechaObjetivo"));
        setTexto(ps, 9, (String) d.get("linkReferencia"));
        setTexto(ps, 10, (String) d.get("linkDrive"));
        setTexto(ps, 11, (String) d.get("linkPublicacion"));
    }

    /** Mover de columna en el tablero. */
    public void cambiarEstado(int id, String estado, int yo) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(
                "UPDATE com_proyecto SET estado=?, actualizado_por=?, fecha_actualizacion=NOW(), " +
                "fecha_publicacion = CASE WHEN ?='PUBLICADO' THEN COALESCE(fecha_publicacion, CURDATE()) WHEN ?='ARCHIVADO' THEN fecha_publicacion ELSE NULL END WHERE id=?")) {
            ps.setString(1, estado); ps.setInt(2, yo); ps.setString(3, estado); ps.setString(4, estado); ps.setInt(5, id);
            ps.executeUpdate();
        }
    }

    public void eliminarProyecto(int id) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("DELETE FROM com_proyecto WHERE id=?")) {
            ps.setInt(1, id); ps.executeUpdate();
        }
    }

    /** Votar o quitar el voto de una idea. */
    public void votar(int id, int yo) throws SQLException {
        try (Connection c = getConnection()) {
            int n;
            try (PreparedStatement del = c.prepareStatement("DELETE FROM com_voto WHERE id_proyecto=? AND id_miembro=?")) {
                del.setInt(1, id); del.setInt(2, yo); n = del.executeUpdate();
            }
            if (n == 0) try (PreparedStatement ins = c.prepareStatement(
                    "INSERT IGNORE INTO com_voto (id_proyecto, id_miembro) SELECT id, ? FROM com_proyecto WHERE id=? AND estado='IDEA'")) {
                ins.setInt(1, yo); ins.setInt(2, id); ins.executeUpdate();
            }
        }
    }

    public void agregarTarea(int idProyecto, String texto) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("INSERT INTO com_tarea (id_proyecto, texto) VALUES (?,?)")) {
            ps.setInt(1, idProyecto); ps.setString(2, texto); ps.executeUpdate();
        }
    }

    public void marcarTarea(int idTarea, int idProyecto) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("UPDATE com_tarea SET hecha = 1 - hecha WHERE id=? AND id_proyecto=?")) {
            ps.setInt(1, idTarea); ps.setInt(2, idProyecto); ps.executeUpdate();
        }
    }

    public void borrarTarea(int idTarea, int idProyecto) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("DELETE FROM com_tarea WHERE id=? AND id_proyecto=?")) {
            ps.setInt(1, idTarea); ps.setInt(2, idProyecto); ps.executeUpdate();
        }
    }

    /** Mis proyectos en desarrollo con fecha objetivo cercana o vencida (para Pendientes). */
    public List<Map<String, Object>> misProyectosPorVencer(int yo, int dias) throws SQLException {
        List<Map<String, Object>> l = new ArrayList<>();
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(PROY_SQL +
                "WHERE p.id_responsable=? AND p.estado='EN_DESARROLLO' AND p.fecha_objetivo IS NOT NULL AND p.fecha_objetivo <= ? ORDER BY p.fecha_objetivo")) {
            ps.setInt(1, yo); ps.setInt(2, yo); setFecha(ps, 3, hoy().plusDays(dias));
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) l.add(proyecto(rs)); }
        }
        return l;
    }

    // ======================================================================
    //  Plantillas de flyers
    // ======================================================================

    public List<Map<String, Object>> plantillas(boolean activas) throws SQLException {
        List<Map<String, Object>> l = new ArrayList<>();
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(
                "SELECT p.id, p.nombre, p.tipo, p.ciclo, p.link_canva, p.notas, p.imagen IS NOT NULL AS con_imagen, p.activa, p.fecha, m.nombre AS subido " +
                "FROM com_plantilla p LEFT JOIN miembro m ON m.id_miembro=p.subido_por WHERE p.activa=? " +
                "ORDER BY FIELD(p.tipo,'Hoy no hay taller','Taller','Evento','Cumpleaños','Cuota','Comunicado','Otro'), p.ciclo DESC, p.nombre")) {
            ps.setBoolean(1, activas);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", rs.getInt("id")); m.put("nombre", rs.getString("nombre")); m.put("tipo", rs.getString("tipo"));
                    m.put("ciclo", rs.getString("ciclo")); m.put("linkCanva", rs.getString("link_canva")); m.put("notas", rs.getString("notas"));
                    m.put("conImagen", rs.getBoolean("con_imagen")); m.put("activa", rs.getBoolean("activa"));
                    m.put("subidoPor", rs.getString("subido"));
                    java.time.LocalDateTime f = fechaHora(rs, "fecha");
                    m.put("fechaTexto", f == null ? "" : Formato.ddmmyyyy(f.toLocalDate()));
                    l.add(m);
                }
            }
        }
        return l;
    }

    public int agregarPlantilla(String nombre, String tipo, String ciclo, String linkCanva, String notas, byte[] imagen, int yo) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(
                "INSERT INTO com_plantilla (nombre, tipo, ciclo, link_canva, notas, imagen, subido_por) VALUES (?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nombre); ps.setString(2, tipo); setTexto(ps, 3, ciclo); setTexto(ps, 4, linkCanva); setTexto(ps, 5, notas);
            if (imagen == null) ps.setNull(6, Types.BLOB); else ps.setBytes(6, imagen);
            ps.setInt(7, yo);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { k.next(); return k.getInt(1); }
        }
    }

    public void editarPlantilla(int id, String nombre, String tipo, String ciclo, String linkCanva, String notas, byte[] imagen) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(
                "UPDATE com_plantilla SET nombre=?, tipo=?, ciclo=?, link_canva=?, notas=?" + (imagen == null ? "" : ", imagen=?") + " WHERE id=?")) {
            ps.setString(1, nombre); ps.setString(2, tipo); setTexto(ps, 3, ciclo); setTexto(ps, 4, linkCanva); setTexto(ps, 5, notas);
            int i = 6;
            if (imagen != null) ps.setBytes(i++, imagen);
            ps.setInt(i, id);
            ps.executeUpdate();
        }
    }

    public void activarPlantilla(int id, boolean activa) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("UPDATE com_plantilla SET activa=? WHERE id=?")) {
            ps.setBoolean(1, activa); ps.setInt(2, id); ps.executeUpdate();
        }
    }

    public void eliminarPlantilla(int id) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("DELETE FROM com_plantilla WHERE id=?")) {
            ps.setInt(1, id); ps.executeUpdate();
        }
    }

    public byte[] imagenPlantilla(int id) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("SELECT imagen FROM com_plantilla WHERE id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getBytes(1) : null; }
        }
    }

    /** Números para la portada de Comunica. */
    public Map<String, Integer> resumen() throws SQLException {
        Map<String, Integer> m = new LinkedHashMap<>();
        YearMonth ym = YearMonth.from(hoy());
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(
                "SELECT (SELECT COUNT(*) FROM com_proyecto WHERE estado='IDEA'), (SELECT COUNT(*) FROM com_proyecto WHERE estado='EN_DESARROLLO'), " +
                "(SELECT COUNT(*) FROM com_proyecto WHERE estado='PUBLICADO' AND fecha_publicacion BETWEEN ? AND ?), " +
                "(SELECT COUNT(*) FROM com_saludo WHERE fecha BETWEEN ? AND ? AND estado NOT IN ('PUBLICADO','OMITIDO') AND fecha < CURDATE()), " +
                "(SELECT COUNT(*) FROM com_plantilla WHERE activa=1)")) {
            setFecha(ps, 1, ym.atDay(1)); setFecha(ps, 2, ym.atEndOfMonth());
            setFecha(ps, 3, ym.minusMonths(1).atDay(1)); setFecha(ps, 4, ym.atEndOfMonth());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                m.put("ideas", rs.getInt(1)); m.put("enDesarrollo", rs.getInt(2)); m.put("publicadosMes", rs.getInt(3));
                m.put("saludosAtrasados", rs.getInt(4)); m.put("plantillas", rs.getInt(5));
            }
        }
        return m;
    }
}
