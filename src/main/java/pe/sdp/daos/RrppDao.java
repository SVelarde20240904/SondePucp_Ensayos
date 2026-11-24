package pe.sdp.daos;

import pe.sdp.util.Formato;

import java.sql.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/**
 * Módulo RRPP:
 *  - Externos del taller del jueves: una fila por semana con el link del Form, quién lo publicó y cuándo se envió a la DAES.
 *    Las fechas del ciclo (publicar / cierre / envío) salen de config y se calculan hacia atrás desde el jueves.
 *  - Visitas formales (vamos todos): convocatoria, "Voy / No puedo", checklist de RRPP.
 *  - Directorio de contactos (ruedas, academias, centros federados, H's y T's, autoridades) con historial.
 */
public class RrppDao extends BaseDao {

    public static final List<String> CATEGORIAS = List.of("RUEDA", "ACADEMIA", "CENTRO_FEDERADO", "H_T", "AUTORIDAD", "OTRO");
    public static final List<String> ESTADOS_CONTACTO = List.of("SIN_CONTACTAR", "CONTACTADO", "RESPONDIO", "ALIADO");
    public static final List<String> CHECKLIST = List.of("AVISAR", "PUBLICAR", "RECORDAR", "FOTO", "AGRADECER");
    public static final List<String> ESTADOS_VISITA = List.of("BORRADOR", "CONVOCADA", "REALIZADA", "CANCELADA");

    private static LocalDate hoy() { return LocalDate.now(Formato.ZONA); }
    private static LocalDateTime ahora() { return LocalDateTime.now(Formato.ZONA); }
    private final ConfigDao config = new ConfigDao();

    public static String categoriaTexto(String c) {
        return switch (c == null ? "" : c) {
            case "RUEDA" -> "Ruedas"; case "ACADEMIA" -> "Academias"; case "CENTRO_FEDERADO" -> "Centros federados";
            case "H_T" -> "H's y T's"; case "AUTORIDAD" -> "Autoridades y oficinas"; default -> "Otros";
        };
    }

    public static String estadoContactoTexto(String e) {
        return switch (e == null ? "" : e) {
            case "SIN_CONTACTAR" -> "Sin contactar"; case "CONTACTADO" -> "Contactado"; case "RESPONDIO" -> "Respondió"; case "ALIADO" -> "Aliado"; default -> e;
        };
    }

    public static String checkTexto(String c) {
        return switch (c) {
            case "AVISAR" -> "Avisar a la rueda que vamos"; case "PUBLICAR" -> "Publicar la convocatoria en el grupo";
            case "RECORDAR" -> "Recordatorio el día anterior"; case "FOTO" -> "Foto grupal con el polo"; case "AGRADECER" -> "Post o mensaje de agradecimiento";
            default -> c;
        };
    }

    // ======================================================================
    //  Equipo RRPP
    // ======================================================================

    public List<Map<String, Object>> equipo() throws SQLException {
        List<Map<String, Object>> l = new ArrayList<>();
        try (Connection c = getConnection(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(
                "SELECT m.id_miembro, m.nombre FROM miembro m JOIN miembro_team mt ON mt.id_miembro=m.id_miembro JOIN team t ON t.id_team=mt.id_team " +
                "WHERE t.codigo='RRPP' AND m.estado='ACTIVO' ORDER BY m.nombre")) {
            while (rs.next()) { Map<String, Object> m = new LinkedHashMap<>(); m.put("id", rs.getInt(1)); m.put("nombre", rs.getString(2)); l.add(m); }
        }
        return l;
    }

    public boolean esDelEquipo(int id) throws SQLException {
        for (Map<String, Object> m : equipo()) if ((Integer) m.get("id") == id) return true;
        return false;
    }

    /** Moderador o admin de RRPP (o Owner): recibe los avisos de atraso de externos. */
    public boolean esModerador(int id) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(
                "SELECT 1 FROM miembro_rol WHERE id_miembro=? AND id_rol IN (16,17)")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    // ======================================================================
    //  Externos del jueves
    // ======================================================================

    public Map<String, String> configExternos() throws SQLException {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("diaPublicar", config.valor("rrpp.ext.dia_publicar", "6"));
        m.put("diaCierre", config.valor("rrpp.ext.dia_cierre", "1"));
        m.put("horaCierre", config.valor("rrpp.ext.hora_cierre", "23:59"));
        m.put("diaEnvio", config.valor("rrpp.ext.dia_envio", "2"));
        m.put("encargado", config.valor("rrpp.ext.encargado", ""));
        m.put("metaVisitas", config.valor("rrpp.meta_visitas", "1"));
        return m;
    }

    public void guardarConfigExternos(int pub, int cierre, LocalTime horaCierre, int envio, Integer encargado, int meta) throws SQLException {
        config.guardar("rrpp.ext.dia_publicar", String.valueOf(pub));
        config.guardar("rrpp.ext.dia_cierre", String.valueOf(cierre));
        config.guardar("rrpp.ext.hora_cierre", horaCierre.toString());
        config.guardar("rrpp.ext.dia_envio", String.valueOf(envio));
        config.guardar("rrpp.ext.encargado", encargado == null ? "" : String.valueOf(encargado));
        config.guardar("rrpp.meta_visitas", String.valueOf(meta));
    }

    private static int dia(String v, int porDefecto) {
        try { int d = Integer.parseInt(v); return d >= 1 && d <= 7 && d != 4 ? d : porDefecto; } catch (Exception e) { return porDefecto; }
    }

    /** El último día con ese día de la semana que sea <= tope. */
    private static LocalDate ultimo(LocalDate tope, int diaSemana) {
        return tope.with(TemporalAdjusters.previousOrSame(DayOfWeek.of(diaSemana)));
    }

    /** El jueves de "esta semana": hoy si es jueves, si no el próximo. */
    public static LocalDate juevesActual() { return hoy().with(TemporalAdjusters.nextOrSame(DayOfWeek.THURSDAY)); }

    /** Fechas del ciclo de un jueves: envío antes del jueves, cierre <= envío, publicar <= cierre. */
    public Map<String, Object> ciclo(LocalDate jueves) throws SQLException {
        Map<String, String> cf = configExternos();
        int dPub = dia(cf.get("diaPublicar"), 6), dCierre = dia(cf.get("diaCierre"), 1), dEnvio = dia(cf.get("diaEnvio"), 2);
        LocalTime hCierre;
        try { hCierre = LocalTime.parse(cf.get("horaCierre")); } catch (Exception e) { hCierre = LocalTime.of(23, 59); }
        LocalDate envio = ultimo(jueves.minusDays(1), dEnvio);
        LocalDate cierre = ultimo(envio, dCierre);
        LocalDate publicar = ultimo(cierre, dPub);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("publicar", publicar); m.put("cierre", cierre.atTime(hCierre)); m.put("envio", envio);
        m.put("publicarTexto", Formato.fechaLarga(publicar));
        m.put("cierreTexto", Formato.fechaLarga(cierre) + " " + Formato.hora(hCierre));
        m.put("cierreCorto", Formato.diaLargo(cierre).toLowerCase() + " " + Formato.hora(hCierre));
        m.put("envioTexto", Formato.fechaLarga(envio));
        return m;
    }

    /** La semana de ese jueves (la crea con su encargado si no existe) con sus fechas y en qué va. */
    public Map<String, Object> semana(LocalDate jueves) throws SQLException {
        asegurarSemana(jueves);
        Map<String, Object> m = new LinkedHashMap<>();
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(
                "SELECT x.*, e.nombre AS encargado, p.nombre AS publicador, s.nombre AS enviador FROM rrpp_externos x " +
                "LEFT JOIN miembro e ON e.id_miembro=x.id_encargado LEFT JOIN miembro p ON p.id_miembro=x.publicado_por " +
                "LEFT JOIN miembro s ON s.id_miembro=x.enviado_por WHERE x.jueves=?")) {
            setFecha(ps, 1, jueves);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) llenarSemana(m, rs); }
        }
        completar(m);
        return m;
    }

    private void llenarSemana(Map<String, Object> m, ResultSet rs) throws SQLException {
        LocalDate j = fecha(rs, "jueves");
        m.put("id", rs.getInt("id"));
        m.put("jueves", j);
        m.put("juevesTexto", Formato.fechaLarga(j));
        m.put("juevesCorto", Formato.ddmm(j));
        m.put("link", rs.getString("link_form"));
        m.put("idEncargado", entero(rs, "id_encargado"));
        m.put("encargado", rs.getString("encargado"));
        LocalDateTime pub = fechaHora(rs, "publicado_en"), env = fechaHora(rs, "enviado_en");
        m.put("publicadoPor", rs.getString("publicador"));
        m.put("publicadoEn", pub == null ? null : Formato.ddmmHora(pub));
        m.put("enviadoPor", rs.getString("enviador"));
        m.put("enviadoEn", env == null ? null : Formato.ddmmHora(env));
        m.put("enviado", env != null);
        m.put("sinTaller", rs.getBoolean("sin_taller"));
    }

    /** Agrega las fechas del ciclo y las banderas de en qué va (las usan RRPP, Inicio y Pendientes). */
    private void completar(Map<String, Object> m) throws SQLException {
        LocalDate j = (LocalDate) m.get("jueves");
        Map<String, Object> cy = ciclo(j);
        m.putAll(cy);
        LocalDate h = hoy(); LocalDateTime now = ahora();
        LocalDate publicar = (LocalDate) cy.get("publicar"), envio = (LocalDate) cy.get("envio");
        LocalDateTime cierre = (LocalDateTime) cy.get("cierre");
        boolean sinTaller = Boolean.TRUE.equals(m.get("sinTaller"));
        boolean hayLink = m.get("link") != null, enviado = Boolean.TRUE.equals(m.get("enviado"));
        boolean activa = !sinTaller && !h.isAfter(j);
        m.put("abierto", activa && hayLink && !h.isBefore(publicar) && now.isBefore(cierre));
        m.put("cerrado", !now.isBefore(cierre));
        m.put("faltaLink", activa && !hayLink && !h.isBefore(publicar) && now.isBefore(cierre));
        m.put("linkAtrasado", activa && !hayLink && h.isAfter(publicar));
        m.put("faltaEnvio", activa && !enviado && !h.isBefore(envio));
        m.put("envioAtrasado", activa && !enviado && h.isAfter(envio));
        m.put("hayLink", hayLink);
    }

    private void asegurarSemana(LocalDate jueves) throws SQLException {
        try (Connection c = getConnection()) {
            try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM rrpp_externos WHERE jueves=?")) {
                setFecha(ps, 1, jueves);
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return; }
            }
            Integer enc = null;
            String fijo = configExternos().get("encargado");
            Integer f = Formato.entero(fijo);
            if (f != null && esDelEquipo(f)) enc = f;
            else {
                // rota: le toca a quien menos semanas lleva como encargado
                try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(
                        "SELECT m.id_miembro FROM miembro m JOIN miembro_team mt ON mt.id_miembro=m.id_miembro JOIN team t ON t.id_team=mt.id_team " +
                        "WHERE t.codigo='RRPP' AND m.estado='ACTIVO' " +
                        "ORDER BY (SELECT COUNT(*) FROM rrpp_externos x WHERE x.id_encargado=m.id_miembro), m.nombre LIMIT 1")) {
                    if (rs.next()) enc = rs.getInt(1);
                }
            }
            try (PreparedStatement ins = c.prepareStatement("INSERT IGNORE INTO rrpp_externos (jueves, id_encargado) VALUES (?,?)")) {
                setFecha(ins, 1, jueves); setEntero(ins, 2, enc); ins.executeUpdate();
            }
        }
    }

    /** Las últimas semanas (para el historial). */
    public List<Map<String, Object>> historial(int n) throws SQLException {
        List<Map<String, Object>> l = new ArrayList<>();
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(
                "SELECT x.*, e.nombre AS encargado, p.nombre AS publicador, s.nombre AS enviador FROM rrpp_externos x " +
                "LEFT JOIN miembro e ON e.id_miembro=x.id_encargado LEFT JOIN miembro p ON p.id_miembro=x.publicado_por " +
                "LEFT JOIN miembro s ON s.id_miembro=x.enviado_por WHERE x.jueves <= ? ORDER BY x.jueves DESC LIMIT ?")) {
            setFecha(ps, 1, juevesActual()); ps.setInt(2, n);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) { Map<String, Object> m = new LinkedHashMap<>(); llenarSemana(m, rs); completar(m); l.add(m); }
            }
        }
        return l;
    }

    public void publicarLink(LocalDate jueves, String link, int yo) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(
                "UPDATE rrpp_externos SET link_form=?, publicado_por=?, publicado_en=COALESCE(publicado_en, ?) WHERE jueves=?")) {
            setTexto(ps, 1, link); ps.setInt(2, yo); setFechaHora(ps, 3, ahora()); setFecha(ps, 4, jueves); ps.executeUpdate();
        }
    }

    public void marcarEnviado(LocalDate jueves, boolean enviado, int yo) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(
                "UPDATE rrpp_externos SET enviado_por=?, enviado_en=? WHERE jueves=?")) {
            if (enviado) { ps.setInt(1, yo); setFechaHora(ps, 2, ahora()); } else { ps.setNull(1, Types.INTEGER); ps.setNull(2, Types.TIMESTAMP); }
            setFecha(ps, 3, jueves); ps.executeUpdate();
        }
    }

    public void sinTaller(LocalDate jueves, boolean sin) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("UPDATE rrpp_externos SET sin_taller=? WHERE jueves=?")) {
            ps.setBoolean(1, sin); setFecha(ps, 2, jueves); ps.executeUpdate();
        }
    }

    public void cambiarEncargado(LocalDate jueves, Integer id) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("UPDATE rrpp_externos SET id_encargado=? WHERE jueves=?")) {
            setEntero(ps, 1, id); setFecha(ps, 2, jueves); ps.executeUpdate();
        }
    }

    // ======================================================================
    //  Directorio
    // ======================================================================

    private Map<String, Object> contacto(ResultSet rs) throws SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        String cat = rs.getString("categoria"), est = rs.getString("estado");
        m.put("id", rs.getInt("id"));
        m.put("categoria", cat); m.put("categoriaTexto", categoriaTexto(cat));
        for (String k : List.of("nombre", "persona", "cargo", "telefono", "correo", "instagram", "ubicacion", "horario", "ciclo", "notas"))
            m.put(k, rs.getString(k));
        m.put("linkMapa", rs.getString("link_mapa"));
        m.put("estado", est); m.put("estadoTexto", estadoContactoTexto(est));
        m.put("tono", switch (est) { case "ALIADO" -> "ok"; case "RESPONDIO" -> "info"; case "CONTACTADO" -> "warn"; default -> "neutral"; });
        LocalDate u = fecha(rs, "ultimo_contacto");
        m.put("ultimoContacto", u);
        long dias = u == null ? -1 : ChronoUnit.DAYS.between(u, hoy());
        m.put("ultimoTexto", u == null ? "Nunca" : dias == 0 ? "Hoy" : dias == 1 ? "Ayer" : "Hace " + dias + " días");
        m.put("olvidado", u == null || dias > 60);
        m.put("idResponsable", entero(rs, "id_responsable"));
        m.put("responsable", rs.getString("responsable"));
        String ig = rs.getString("instagram");
        m.put("igLink", ig == null ? null : "https://www.instagram.com/" + ig.replace("@", "").trim() + "/");
        String tel = rs.getString("telefono");
        String dig = tel == null ? "" : tel.replaceAll("\\D", "");
        if (dig.length() == 9) dig = "51" + dig;
        m.put("waLink", dig.length() >= 10 ? "https://wa.me/" + dig : null);
        return m;
    }

    private static final String CONT_SQL = "SELECT c.*, r.nombre AS responsable FROM rrpp_contacto c LEFT JOIN miembro r ON r.id_miembro=c.id_responsable ";

    public List<Map<String, Object>> contactos(String categoria, String q, String ciclo) throws SQLException {
        List<Map<String, Object>> l = new ArrayList<>();
        StringBuilder sql = new StringBuilder(CONT_SQL + "WHERE 1=1 ");
        List<String> ps = new ArrayList<>();
        if (categoria != null) { sql.append("AND c.categoria=? "); ps.add(categoria); }
        if (ciclo != null) { sql.append("AND c.ciclo=? "); ps.add(ciclo); }
        if (q != null) {
            sql.append("AND (c.nombre LIKE ? OR c.persona LIKE ? OR c.notas LIKE ? OR c.instagram LIKE ?) ");
            String like = "%" + q.replace("%", "").replace("_", "") + "%";
            for (int i = 0; i < 4; i++) ps.add(like);
        }
        sql.append("ORDER BY c.categoria, c.nombre");
        try (Connection c = getConnection(); PreparedStatement st = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < ps.size(); i++) st.setString(i + 1, ps.get(i));
            try (ResultSet rs = st.executeQuery()) { while (rs.next()) l.add(contacto(rs)); }
        }
        return l;
    }

    public Map<String, Object> contacto(int id) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(CONT_SQL + "WHERE c.id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Map<String, Object> m = contacto(rs);
                List<Map<String, Object>> hist = new ArrayList<>();
                try (PreparedStatement ph = c.prepareStatement(
                        "SELECT i.fecha, i.nota, m.nombre FROM rrpp_interaccion i LEFT JOIN miembro m ON m.id_miembro=i.registrado_por " +
                        "WHERE i.id_contacto=? ORDER BY i.fecha DESC, i.id DESC")) {
                    ph.setInt(1, id);
                    try (ResultSet r2 = ph.executeQuery()) {
                        while (r2.next()) {
                            Map<String, Object> h = new LinkedHashMap<>();
                            h.put("fechaTexto", Formato.ddmmyyyy(fecha(r2, "fecha"))); h.put("nota", r2.getString("nota")); h.put("quien", r2.getString("nombre"));
                            hist.add(h);
                        }
                    }
                }
                m.put("historial", hist);
                // visitas formales a esta rueda
                List<Map<String, Object>> vis = new ArrayList<>();
                try (PreparedStatement pv = c.prepareStatement("SELECT id, titulo, fecha, estado FROM rrpp_visita WHERE id_contacto=? ORDER BY fecha DESC")) {
                    pv.setInt(1, id);
                    try (ResultSet r3 = pv.executeQuery()) {
                        while (r3.next()) {
                            Map<String, Object> v = new LinkedHashMap<>();
                            v.put("id", r3.getInt(1)); v.put("titulo", r3.getString(2)); v.put("fechaTexto", Formato.ddmmyyyy(fecha(r3, "fecha"))); v.put("estado", r3.getString(4));
                            vis.add(v);
                        }
                    }
                }
                m.put("visitas", vis);
                return m;
            }
        }
    }

    private static final String[] CAMPOS = {"categoria", "nombre", "persona", "cargo", "telefono", "correo", "instagram", "ubicacion", "linkMapa", "horario", "ciclo", "estado", "notas"};

    private static void llenarContacto(PreparedStatement ps, Map<String, Object> d) throws SQLException {
        for (int i = 0; i < CAMPOS.length; i++) setTexto(ps, i + 1, (String) d.get(CAMPOS[i]));
        setEntero(ps, CAMPOS.length + 1, (Integer) d.get("idResponsable"));
    }

    public int crearContacto(Map<String, Object> d, int yo) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(
                "INSERT INTO rrpp_contacto (categoria, nombre, persona, cargo, telefono, correo, instagram, ubicacion, link_mapa, horario, ciclo, estado, notas, " +
                "id_responsable, creado_por) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
            llenarContacto(ps, d); ps.setInt(15, yo); ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { k.next(); return k.getInt(1); }
        }
    }

    public void actualizarContacto(int id, Map<String, Object> d) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(
                "UPDATE rrpp_contacto SET categoria=?, nombre=?, persona=?, cargo=?, telefono=?, correo=?, instagram=?, ubicacion=?, link_mapa=?, horario=?, " +
                "ciclo=?, estado=?, notas=?, id_responsable=? WHERE id=?")) {
            llenarContacto(ps, d); ps.setInt(15, id); ps.executeUpdate();
        }
    }

    public void eliminarContacto(int id) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("DELETE FROM rrpp_contacto WHERE id=?")) { ps.setInt(1, id); ps.executeUpdate(); }
    }

    /** Anota una interacción; actualiza el último contacto y, si se indica, el estado. */
    public void agregarInteraccion(int id, LocalDate fecha, String nota, String nuevoEstado, int yo) throws SQLException {
        try (Connection c = getConnection()) {
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO rrpp_interaccion (id_contacto, fecha, nota, registrado_por) VALUES (?,?,?,?)")) {
                ps.setInt(1, id); setFecha(ps, 2, fecha); ps.setString(3, nota); ps.setInt(4, yo); ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE rrpp_contacto SET ultimo_contacto = GREATEST(COALESCE(ultimo_contacto, ?), ?), " +
                    "estado = COALESCE(?, IF(estado='SIN_CONTACTAR','CONTACTADO',estado)) WHERE id=?")) {
                setFecha(ps, 1, fecha); setFecha(ps, 2, fecha); setTexto(ps, 3, nuevoEstado); ps.setInt(4, id); ps.executeUpdate();
            }
        }
    }

    /** Cuántos contactos hay por categoría, y cuántos llevan más de 60 días sin contacto. */
    public Map<String, Object> resumenDirectorio(String ciclo) throws SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        Map<String, Integer> porCat = new LinkedHashMap<>();
        for (String k : CATEGORIAS) porCat.put(k, 0);
        try (Connection c = getConnection()) {
            try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT categoria, COUNT(*) FROM rrpp_contacto GROUP BY categoria")) {
                while (rs.next()) porCat.put(rs.getString(1), rs.getInt(2));
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT COUNT(*), SUM(estado <> 'SIN_CONTACTAR') FROM rrpp_contacto WHERE categoria='H_T' AND ciclo=?")) {
                ps.setString(1, ciclo);
                try (ResultSet rs = ps.executeQuery()) { rs.next(); m.put("htTotal", rs.getInt(1)); m.put("htContactados", rs.getInt(2)); }
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT COUNT(*) FROM rrpp_contacto WHERE categoria IN ('RUEDA','ACADEMIA') AND (ultimo_contacto IS NULL OR ultimo_contacto < ?)")) {
                setFecha(ps, 1, hoy().minusDays(60));
                try (ResultSet rs = ps.executeQuery()) { rs.next(); m.put("ruedasOlvidadas", rs.getInt(1)); }
            }
        }
        List<Map<String, Object>> cats = new ArrayList<>();
        for (Map.Entry<String, Integer> e : porCat.entrySet()) {
            Map<String, Object> x = new LinkedHashMap<>(); x.put("codigo", e.getKey()); x.put("texto", categoriaTexto(e.getKey())); x.put("n", e.getValue()); cats.add(x);
        }
        m.put("categorias", cats);
        return m;
    }

    /** Ruedas y academias para elegir en una visita. {id, nombre} */
    public List<Map<String, Object>> ruedas() throws SQLException {
        List<Map<String, Object>> l = new ArrayList<>();
        try (Connection c = getConnection(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(
                "SELECT id, nombre, ubicacion, link_mapa FROM rrpp_contacto WHERE categoria IN ('RUEDA','ACADEMIA') ORDER BY nombre")) {
            while (rs.next()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", rs.getInt(1)); m.put("nombre", rs.getString(2)); m.put("ubicacion", rs.getString(3)); m.put("linkMapa", rs.getString(4)); l.add(m);
            }
        }
        return l;
    }

    // ======================================================================
    //  Visitas formales
    // ======================================================================

    private static final String VIS_SQL =
            "SELECT v.*, k.nombre AS rueda, co.nombre AS coordinador, " +
            " (SELECT COUNT(*) FROM rrpp_visita_resp r WHERE r.id_visita=v.id AND r.respuesta='VOY') AS van, " +
            " (SELECT COUNT(*) FROM rrpp_visita_resp r WHERE r.id_visita=v.id AND r.respuesta='NO_PUEDO') AS no_pueden, " +
            " (SELECT r.respuesta FROM rrpp_visita_resp r WHERE r.id_visita=v.id AND r.id_miembro=?) AS mi_respuesta " +
            "FROM rrpp_visita v LEFT JOIN rrpp_contacto k ON k.id=v.id_contacto LEFT JOIN miembro co ON co.id_miembro=v.id_coordinador ";

    private Map<String, Object> visita(ResultSet rs) throws SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        LocalDate f = fecha(rs, "fecha");
        LocalTime h = hora(rs, "hora"), he = hora(rs, "hora_encuentro");
        String estado = rs.getString("estado");
        m.put("id", rs.getInt("id"));
        m.put("titulo", rs.getString("titulo"));
        m.put("idContacto", entero(rs, "id_contacto"));
        m.put("rueda", rs.getString("rueda"));
        m.put("lugar", rs.getString("lugar"));
        m.put("linkMapa", rs.getString("link_mapa"));
        m.put("fecha", f); m.put("fechaTexto", Formato.fechaLarga(f));
        m.put("hora", h); m.put("horaTexto", Formato.hora(h));
        m.put("puntoEncuentro", rs.getString("punto_encuentro"));
        m.put("horaEncuentro", he); m.put("horaEncuentroTexto", Formato.hora(he));
        m.put("comoLlegar", rs.getString("como_llegar"));
        m.put("costo", rs.getString("costo"));
        m.put("dressCode", rs.getString("dress_code"));
        m.put("notas", rs.getString("notas"));
        m.put("estado", estado);
        m.put("estadoTexto", switch (estado) { case "BORRADOR" -> "Borrador"; case "CONVOCADA" -> "Convocada"; case "REALIZADA" -> "Realizada"; default -> "Cancelada"; });
        m.put("tono", switch (estado) { case "CONVOCADA" -> "info"; case "REALIZADA" -> "ok"; case "CANCELADA" -> "bad"; default -> "neutral"; });
        String chk = rs.getString("checklist");
        Set<String> hechos = new HashSet<>();
        if (chk != null) for (String s : chk.split(",")) if (!s.isBlank()) hechos.add(s);
        List<Map<String, Object>> pasos = new ArrayList<>();
        for (String p : CHECKLIST) { Map<String, Object> x = new LinkedHashMap<>(); x.put("codigo", p); x.put("texto", checkTexto(p)); x.put("hecho", hechos.contains(p)); pasos.add(x); }
        m.put("checklist", pasos);
        m.put("checkHechos", hechos.size());
        m.put("idCoordinador", entero(rs, "id_coordinador"));
        m.put("coordinador", rs.getString("coordinador"));
        m.put("linkFoto", rs.getString("link_foto"));
        m.put("idEncuesta", entero(rs, "id_encuesta"));
        m.put("idProyecto", entero(rs, "id_proyecto"));
        m.put("van", rs.getInt("van"));
        m.put("noPueden", rs.getInt("no_pueden"));
        m.put("miRespuesta", rs.getString("mi_respuesta"));
        long dias = ChronoUnit.DAYS.between(hoy(), f);
        m.put("dias", dias);
        m.put("cuando", dias == 0 ? "¡Hoy!" : dias == 1 ? "Mañana" : dias > 1 ? "En " + dias + " días" : "");
        m.put("puedeResponder", "CONVOCADA".equals(estado) && dias >= 0);
        return m;
    }

    /** Para RRPP: próximas (borradores y convocadas desde hoy) o pasadas/cerradas. */
    public List<Map<String, Object>> visitas(boolean proximas, int yo) throws SQLException {
        List<Map<String, Object>> l = new ArrayList<>();
        String w = proximas ? "WHERE v.fecha >= ? AND v.estado IN ('BORRADOR','CONVOCADA') ORDER BY v.fecha"
                            : "WHERE v.fecha < ? OR v.estado IN ('REALIZADA','CANCELADA') ORDER BY v.fecha DESC LIMIT 40";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(VIS_SQL + w)) {
            ps.setInt(1, yo); setFecha(ps, 2, hoy());
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) l.add(visita(rs)); }
        }
        return l;
    }

    /** Para todos: convocadas desde hoy y las últimas realizadas. */
    public List<Map<String, Object>> visitasParaTodos(int yo) throws SQLException {
        List<Map<String, Object>> l = new ArrayList<>();
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(VIS_SQL +
                "WHERE (v.estado='CONVOCADA' AND v.fecha >= ?) OR (v.estado='REALIZADA' AND v.fecha >= ?) ORDER BY v.estado='REALIZADA', v.fecha")) {
            ps.setInt(1, yo); setFecha(ps, 2, hoy()); setFecha(ps, 3, hoy().minusDays(90));
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) l.add(visita(rs)); }
        }
        return l;
    }

    public Map<String, Object> visita(int id, int yo) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(VIS_SQL + "WHERE v.id=?")) {
            ps.setInt(1, yo); ps.setInt(2, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? visita(rs) : null; }
        }
    }

    /** Quiénes respondieron: {voy:[nombres], noPuedo:[nombres]} */
    public Map<String, List<String>> respuestas(int id) throws SQLException {
        Map<String, List<String>> m = new LinkedHashMap<>();
        m.put("VOY", new ArrayList<>()); m.put("NO_PUEDO", new ArrayList<>());
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(
                "SELECT r.respuesta, m.nombre, m.apodo FROM rrpp_visita_resp r JOIN miembro m ON m.id_miembro=r.id_miembro WHERE r.id_visita=? ORDER BY m.nombre")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) { String ap = rs.getString(3); m.get(rs.getString(1)).add(rs.getString(2) + (ap == null || ap.isBlank() ? "" : " (" + ap + ")")); }
            }
        }
        return m;
    }

    public int activos() throws SQLException {
        try (Connection c = getConnection(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM miembro WHERE estado='ACTIVO'")) {
            rs.next(); return rs.getInt(1);
        }
    }

    private static final String[] VCAMPOS = {"titulo", "lugar", "linkMapa", "puntoEncuentro", "comoLlegar", "costo", "dressCode", "notas", "linkFoto"};

    private static void llenarVisita(PreparedStatement ps, Map<String, Object> d) throws SQLException {
        for (int i = 0; i < VCAMPOS.length; i++) setTexto(ps, i + 1, (String) d.get(VCAMPOS[i]));
        setEntero(ps, 10, (Integer) d.get("idContacto"));
        setFecha(ps, 11, (LocalDate) d.get("fecha"));
        LocalTime h = (LocalTime) d.get("hora"), he = (LocalTime) d.get("horaEncuentro");
        if (h == null) ps.setNull(12, Types.TIME); else ps.setTime(12, Time.valueOf(h));
        if (he == null) ps.setNull(13, Types.TIME); else ps.setTime(13, Time.valueOf(he));
        setEntero(ps, 14, (Integer) d.get("idCoordinador"));
    }

    public int crearVisita(Map<String, Object> d, Integer idEncuesta, int yo) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(
                "INSERT INTO rrpp_visita (titulo, lugar, link_mapa, punto_encuentro, como_llegar, costo, dress_code, notas, link_foto, id_contacto, fecha, hora, " +
                "hora_encuentro, id_coordinador, id_encuesta, creado_por) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
            llenarVisita(ps, d); setEntero(ps, 15, idEncuesta); ps.setInt(16, yo); ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { k.next(); return k.getInt(1); }
        }
    }

    public void actualizarVisita(int id, Map<String, Object> d) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(
                "UPDATE rrpp_visita SET titulo=?, lugar=?, link_mapa=?, punto_encuentro=?, como_llegar=?, costo=?, dress_code=?, notas=?, link_foto=?, " +
                "id_contacto=?, fecha=?, hora=?, hora_encuentro=?, id_coordinador=? WHERE id=?")) {
            llenarVisita(ps, d); ps.setInt(15, id); ps.executeUpdate();
        }
    }

    public void estadoVisita(int id, String estado) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("UPDATE rrpp_visita SET estado=? WHERE id=?")) {
            ps.setString(1, estado); ps.setInt(2, id); ps.executeUpdate();
        }
    }

    public void ponerProyecto(int id, int idProyecto) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("UPDATE rrpp_visita SET id_proyecto=? WHERE id=? AND id_proyecto IS NULL")) {
            ps.setInt(1, idProyecto); ps.setInt(2, id); ps.executeUpdate();
        }
    }

    /** Marca o desmarca un paso del checklist. */
    public void check(int id, String paso, boolean hecho) throws SQLException {
        if (!CHECKLIST.contains(paso)) return;
        String sql = hecho ? "UPDATE rrpp_visita SET checklist = CONCAT_WS(',', NULLIF(checklist,''), ?) WHERE id=? AND NOT FIND_IN_SET(?, checklist)"
                           : "UPDATE rrpp_visita SET checklist = TRIM(BOTH ',' FROM REPLACE(CONCAT(',', checklist, ','), CONCAT(',', ?, ','), ',')) WHERE id=? AND ? IS NOT NULL";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, paso); ps.setInt(2, id); ps.setString(3, paso); ps.executeUpdate();
        }
    }

    public void eliminarVisita(int id) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("DELETE FROM rrpp_visita WHERE id=?")) { ps.setInt(1, id); ps.executeUpdate(); }
    }

    /** Responder a una convocatoria (solo si está convocada y no pasó). */
    public boolean responder(int id, int yo, String respuesta) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(
                "INSERT INTO rrpp_visita_resp (id_visita, id_miembro, respuesta) SELECT id, ?, ? FROM rrpp_visita WHERE id=? AND estado='CONVOCADA' AND fecha >= ? " +
                "ON DUPLICATE KEY UPDATE respuesta=VALUES(respuesta), fecha=NOW()")) {
            ps.setInt(1, yo); ps.setString(2, respuesta); ps.setInt(3, id); setFecha(ps, 4, hoy());
            return ps.executeUpdate() > 0;
        }
    }

    /** Visitas formales realizadas (y convocadas) en un mes, para la meta. */
    public int[] visitasDelMes(YearMonth ym) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(
                "SELECT SUM(estado='REALIZADA'), SUM(estado='CONVOCADA') FROM rrpp_visita WHERE fecha BETWEEN ? AND ?")) {
            setFecha(ps, 1, ym.atDay(1)); setFecha(ps, 2, ym.atEndOfMonth());
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return new int[]{rs.getInt(1), rs.getInt(2)}; }
        }
    }

    /** Convocadas desde hoy donde no respondí, y las que voy y son hoy o mañana (para Pendientes). */
    public List<Map<String, Object>> visitasPendientes(int yo) throws SQLException {
        List<Map<String, Object>> l = new ArrayList<>();
        for (Map<String, Object> v : visitasParaTodos(yo)) {
            if (!"CONVOCADA".equals(v.get("estado"))) continue;
            long d = (Long) v.get("dias");
            if (v.get("miRespuesta") == null || ("VOY".equals(v.get("miRespuesta")) && d <= 1)) l.add(v);
        }
        return l;
    }
}
