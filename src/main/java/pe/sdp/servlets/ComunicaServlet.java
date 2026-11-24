package pe.sdp.servlets;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import pe.sdp.daos.ComunicaDao;
import pe.sdp.daos.ConfigDao;
import pe.sdp.util.Formato;
import pe.sdp.util.Sesion;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

/**
 * Módulo Comunica.
 *  /comunica                 tablero de proyectos (en desarrollo y publicados recientes)
 *  /comunica/cumpleanos?m=   rol de saludos de cumpleaños del mes
 *  /comunica/saludo?id=      un saludo: redactarlo, link del video, estado
 *  /comunica/ideas           banco de ideas y referencias (votos, etiquetas)
 *  /comunica/proyecto?id=    ver/editar un proyecto (sin id: nuevo)
 *  /comunica/archivo         proyectos pasados
 *  /comunica/plantillas      plantillas de flyers (vista previa + link de Canva)
 *  /comunica/plantilla?id=   editar una plantilla
 *  /comunica/plantilla-img?id=  la vista previa
 *  /proponer-idea            cualquier miembro le propone una idea a Comunica
 */
@WebServlet(urlPatterns = {"/comunica", "/comunica/*", "/proponer-idea"})
@MultipartConfig(maxFileSize = 8 * 1024 * 1024, maxRequestSize = 9 * 1024 * 1024)
public class ComunicaServlet extends BaseServlet {

    private static final String PERM = "GESTIONAR_COMUNICA";
    private static final String MOD = "MODERAR_COMUNICA";
    private final ComunicaDao dao = new ComunicaDao();
    private final ConfigDao config = new ConfigDao();

    private String ruta(HttpServletRequest req) {
        if ("/proponer-idea".equals(req.getServletPath())) return "/proponer-idea";
        return req.getPathInfo() == null ? "/" : req.getPathInfo();
    }

    private static LocalDate hoy() { return LocalDate.now(Formato.ZONA); }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int yo = Sesion.idUsuario(req);
        try {
            String r = ruta(req);
            if ("/proponer-idea".equals(r)) { proponer(req, resp, yo); return; }
            if (!Sesion.requiere(req, resp, PERM)) return;
            req.setAttribute("puedeModerar", Sesion.tiene(req, MOD));
            req.setAttribute("tipos", ComunicaDao.TIPOS);
            req.setAttribute("redes", ComunicaDao.REDES);
            switch (r) {
                case "/" -> {
                    req.setAttribute("enDesarrollo", dao.proyectos(yo, List.of("EN_DESARROLLO"), null));
                    List<Map<String, Object>> pub = dao.proyectos(yo, List.of("PUBLICADO"), null);
                    req.setAttribute("publicados", pub.size() > 8 ? pub.subList(0, 8) : pub);
                    req.setAttribute("hayMasPublicados", pub.size() > 8);
                    List<Map<String, Object>> ideas = dao.proyectos(yo, List.of("IDEA"), null);
                    req.setAttribute("ideasTop", ideas.size() > 5 ? ideas.subList(0, 5) : ideas);
                    req.setAttribute("totalIdeas", ideas.size());
                    dao.generarSiHaceFalta();
                    req.setAttribute("misSaludos", dao.misSaludosPendientes(yo, 7));
                    req.setAttribute("resumen", dao.resumen());
                    vista(req, resp, "comunica/tablero");
                }
                case "/cumpleanos" -> {
                    dao.generar();
                    YearMonth ym = YearMonth.from(hoy());
                    try { if (texto(req, "m") != null) ym = YearMonth.parse(texto(req, "m")); } catch (Exception ignored) { }
                    List<Map<String, Object>> saludos = dao.saludosDelMes(ym);
                    int publicados = 0;
                    for (Map<String, Object> s : saludos) if ("PUBLICADO".equals(s.get("estado"))) publicados++;
                    req.setAttribute("saludos", saludos);
                    req.setAttribute("publicados", publicados);
                    req.setAttribute("mes", ym.toString());
                    req.setAttribute("mesTexto", Formato.capital(ym.getMonth().getDisplayName(java.time.format.TextStyle.FULL, Formato.ES)) + " " + ym.getYear());
                    req.setAttribute("mesAnterior", ym.minusMonths(1).toString());
                    req.setAttribute("mesSiguiente", ym.plusMonths(1).toString());
                    req.setAttribute("esMesActual", ym.equals(YearMonth.from(hoy())));
                    req.setAttribute("equipo", dao.equipo());
                    req.setAttribute("carga", dao.cargaDelAnio(ym.getYear()));
                    req.setAttribute("anio", ym.getYear());
                    req.setAttribute("plantillaSaludo", config.valor(ComunicaDao.PLANTILLA_SALUDO, ""));
                    vista(req, resp, "comunica/cumpleanos");
                }
                case "/saludo" -> {
                    Integer id = entero(req, "id");
                    Map<String, Object> s = id == null ? null : dao.saludo(id);
                    if (s == null) { Sesion.error(req, "Ese saludo no existe."); ir(req, resp, "/comunica/cumpleanos"); return; }
                    if (Objects.equals(s.get("idMiembro"), yo)) { Sesion.error(req, "Tu saludo es sorpresa 🎂"); ir(req, resp, "/comunica/cumpleanos"); return; }
                    req.setAttribute("s", s);
                    req.setAttribute("estados", ComunicaDao.ESTADOS_SALUDO);
                    req.setAttribute("equipo", dao.equipo());
                    req.setAttribute("sugerido", rellenar(config.valor(ComunicaDao.PLANTILLA_SALUDO, ""), s));
                    req.setAttribute("esMio", Objects.equals(s.get("idResponsable"), yo));
                    vista(req, resp, "comunica/saludo");
                }
                case "/ideas" -> {
                    String tag = texto(req, "tag");
                    req.setAttribute("ideas", dao.proyectos(yo, List.of("IDEA"), tag));
                    req.setAttribute("tag", tag);
                    req.setAttribute("etiquetas", dao.etiquetas());
                    vista(req, resp, "comunica/ideas");
                }
                case "/archivo" -> {
                    String tag = texto(req, "tag");
                    req.setAttribute("proyectos", dao.proyectos(yo, List.of("PUBLICADO", "ARCHIVADO"), tag));
                    req.setAttribute("tag", tag);
                    req.setAttribute("etiquetas", dao.etiquetas());
                    vista(req, resp, "comunica/archivo");
                }
                case "/proyecto" -> {
                    Integer id = entero(req, "id");
                    Map<String, Object> p;
                    if (id == null) {
                        p = new HashMap<>();
                        String est = en(ComunicaDao.ESTADOS_PROYECTO, texto(req, "estado")) ? texto(req, "estado") : "EN_DESARROLLO";
                        p.put("estado", est); p.put("tipo", "TIKTOK"); p.put("red", "INSTAGRAM");
                        p.put("idResponsable", "IDEA".equals(est) ? null : yo);
                        p.put("nuevo", true);
                    } else {
                        p = dao.proyecto(id, yo);
                        if (p == null) { Sesion.error(req, "Ese proyecto no existe."); ir(req, resp, "/comunica"); return; }
                    }
                    req.setAttribute("p", p);
                    req.setAttribute("estadosProyecto", ComunicaDao.ESTADOS_PROYECTO);
                    req.setAttribute("equipo", dao.equipo());
                    req.setAttribute("etiquetas", dao.etiquetas());
                    vista(req, resp, "comunica/proyecto");
                }
                case "/plantillas" -> {
                    boolean archivo = "1".equals(req.getParameter("archivo"));
                    req.setAttribute("plantillas", dao.plantillas(!archivo));
                    req.setAttribute("verArchivo", archivo);
                    req.setAttribute("tiposPlantilla", ComunicaDao.TIPOS_PLANTILLA);
                    req.setAttribute("cicloActual", cicloActual());
                    vista(req, resp, "comunica/plantillas");
                }
                case "/plantilla" -> {
                    Integer id = entero(req, "id");
                    Map<String, Object> pl = null;
                    if (id != null) for (boolean act : new boolean[]{true, false}) for (Map<String, Object> x : dao.plantillas(act)) if (id.equals(x.get("id"))) pl = x;
                    if (pl == null) { Sesion.error(req, "Esa plantilla no existe."); ir(req, resp, "/comunica/plantillas"); return; }
                    req.setAttribute("pl", pl);
                    req.setAttribute("tiposPlantilla", ComunicaDao.TIPOS_PLANTILLA);
                    vista(req, resp, "comunica/plantilla");
                }
                case "/plantilla-img" -> {
                    Integer id = entero(req, "id");
                    byte[] img = id == null ? null : dao.imagenPlantilla(id);
                    if (img == null) { resp.sendError(404); return; }
                    resp.setContentType("image/jpeg");
                    resp.setHeader("Cache-Control", "private, max-age=3600");
                    resp.setHeader("X-Content-Type-Options", "nosniff");
                    resp.setContentLength(img.length);
                    resp.getOutputStream().write(img);
                }
                default -> resp.sendError(404);
            }
        } catch (SQLException e) { fallo(req, resp, e); }
    }

    /** Ciclo PUCP aproximado: enero-febrero = 0 (verano), marzo-julio = 1, agosto-diciembre = 2. */
    static String cicloActual() {
        LocalDate h = hoy();
        int m = h.getMonthValue();
        return h.getYear() + "-" + (m <= 2 ? "0" : m <= 7 ? "1" : "2");
    }

    /** Pone el apodo/nombre del cumpleañero en la plantilla del saludo. */
    static String rellenar(String plantilla, Map<String, Object> s) {
        if (plantilla == null) return "";
        String nombre = (String) s.get("nombre");
        return plantilla.replace("{apodo}", String.valueOf(s.get("comoLlamarlo")))
                        .replace("{nombre}", nombre == null ? "" : nombre.split(" ")[0]);
    }

    private void proponer(HttpServletRequest req, HttpServletResponse resp, int yo) throws SQLException, ServletException, IOException {
        req.setAttribute("tipos", ComunicaDao.TIPOS);
        req.setAttribute("misPropuestas", dao.misPropuestas(yo));
        vista(req, resp, "comunica/proponer");
    }

    // ======================================================================

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int yo = Sesion.idUsuario(req);
        try {
            String r = ruta(req);
            if ("/proponer-idea".equals(r)) { postProponer(req, resp, yo); return; }
            if (!Sesion.requiere(req, resp, PERM)) return;
            boolean mod = Sesion.tiene(req, MOD);
            switch (r) {
                case "/cumpleanos" -> {
                    if ("plantilla".equals(accion(req, "")) && mod) {
                        String t = texto(req, "plantilla");
                        config.guardar(ComunicaDao.PLANTILLA_SALUDO, t == null ? "" : recortar(t, 500));
                        Sesion.ok(req, "Plantilla del saludo guardada.");
                    }
                    ir(req, resp, "/comunica/cumpleanos" + (texto(req, "m") == null ? "" : "?m=" + mesSeguro(texto(req, "m"))));
                }
                case "/saludo" -> postSaludo(req, resp, yo, mod);
                case "/proyecto" -> postProyecto(req, resp, yo, mod);
                case "/ideas" -> {
                    Integer id = entero(req, "id");
                    String a = accion(req, "");
                    if (id != null && "votar".equals(a)) dao.votar(id, yo);
                    else if (id != null && "desarrollar".equals(a)) {
                        Map<String, Object> p = dao.proyecto(id, yo);
                        if (p != null && "IDEA".equals(p.get("estado"))) {
                            dao.cambiarEstado(id, "EN_DESARROLLO", yo);
                            if (p.get("idResponsable") == null) asignarProyecto(id, p, yo, yo);
                            Sesion.ok(req, "«" + p.get("titulo") + "» pasó a En desarrollo. Ahora está en el tablero.");
                            ir(req, resp, "/comunica/proyecto?id=" + id); return;
                        }
                    }
                    String tag = texto(req, "tag");
                    ir(req, resp, "/comunica/ideas" + (tag == null ? "" : "?tag=" + java.net.URLEncoder.encode(tag, java.nio.charset.StandardCharsets.UTF_8)));
                }
                case "/plantillas", "/plantilla" -> postPlantilla(req, resp, yo, mod);
                default -> resp.sendError(404);
            }
        } catch (SQLException e) { fallo(req, resp, e); }
    }

    private static String mesSeguro(String m) {
        try { return YearMonth.parse(m).toString(); } catch (Exception e) { return YearMonth.now(Formato.ZONA).toString(); }
    }

    /** List.of(...).contains(null) lanza NPE: un parámetro que no vino simplemente no está en la lista. */
    private static boolean en(List<String> l, String v) { return v != null && l.contains(v); }

    private static String recortar(String s, int max) { return s == null ? null : (s.length() > max ? s.substring(0, max) : s); }

    /** Un link: solo http(s) y máx. 500 caracteres. "" si vino vacío, null si no es un link válido. */
    static String link(String v) {
        if (v == null || v.isBlank()) return "";
        v = v.trim();
        if (!v.matches("(?i)^https?://\\S+$") || v.length() > 500) return null;
        return v;
    }

    private void postSaludo(HttpServletRequest req, HttpServletResponse resp, int yo, boolean mod) throws SQLException, IOException {
        Integer id = entero(req, "id");
        Map<String, Object> s = id == null ? null : dao.saludo(id);
        if (s == null) { Sesion.error(req, "Ese saludo no existe."); ir(req, resp, "/comunica/cumpleanos"); return; }
        String a = accion(req, "guardar");
        if ("asignar".equals(a)) {
            if (!mod) { Sesion.error(req, "Solo el admin o moderador de Comunica reasigna saludos."); }
            else {
                Integer r = entero(req, "responsable");
                if (r != null && (!dao.esDelEquipo(r) || r.equals(s.get("idMiembro")))) Sesion.error(req, "Elige a alguien del team Comunica (que no sea el cumpleañero).");
                else { dao.asignarSaludo(id, r, yo); Sesion.ok(req, "Saludo de " + s.get("comoLlamarlo") + " reasignado."); }
            }
        } else if ("tomar".equals(a)) {
            // tomar un saludo que no tiene responsable (cualquiera del team, salvo el cumpleañero)
            if (s.get("idResponsable") != null && !mod) Sesion.error(req, "Ese saludo ya tiene responsable.");
            else if (Objects.equals(s.get("idMiembro"), yo)) Sesion.error(req, "No puedes redactar tu propio saludo 😄");
            else { dao.asignarSaludo(id, yo, yo); Sesion.ok(req, "Te quedaste con el saludo de " + s.get("comoLlamarlo") + "."); }
        } else {
            if (Objects.equals(s.get("idMiembro"), yo)) { Sesion.error(req, "Tu propio saludo lo prepara otra persona del team 🎂"); ir(req, resp, "/comunica/cumpleanos"); return; }
            String estado = en(ComunicaDao.ESTADOS_SALUDO, texto(req, "estado")) ? texto(req, "estado") : (String) s.get("estado");
            String video = link(texto(req, "linkVideo"));
            if (video == null) { Sesion.error(req, "El link del video tiene que empezar con https:// (por ejemplo, el link de Drive)."); ir(req, resp, "/comunica/saludo?id=" + id); return; }
            String publicadoEn = String.join(", ", valores(req, "publicadoEn", List.of("Historia de Instagram", "Post de Instagram", "Telegram", "TikTok")));
            dao.guardarSaludo(id, recortar(texto(req, "texto"), 3000), video, publicadoEn, estado, yo);
            Sesion.ok(req, "Saludo de " + s.get("comoLlamarlo") + ": " + ComunicaDao.estadoSaludoTexto(estado).toLowerCase() + ".");
        }
        String volver = "lista".equals(req.getParameter("volver")) ? "/comunica/cumpleanos?m=" + YearMonth.from((LocalDate) s.get("fecha")) : "/comunica/saludo?id=" + id;
        ir(req, resp, volver);
    }

    private static List<String> valores(HttpServletRequest req, String nombre, List<String> permitidos) {
        List<String> l = new ArrayList<>();
        String[] vs = req.getParameterValues(nombre);
        if (vs != null) for (String v : vs) if (permitidos.contains(v) && !l.contains(v)) l.add(v);
        return l;
    }

    /** Lee el formulario de proyecto. Devuelve null (y deja el error) si algo no cuadra. */
    private Map<String, Object> leerProyecto(HttpServletRequest req, String estadoPorDefecto) throws SQLException {
        Map<String, Object> d = new HashMap<>();
        String titulo = texto(req, "titulo");
        if (titulo == null) { Sesion.error(req, "Ponle un título."); return null; }
        d.put("titulo", recortar(titulo, 120));
        d.put("tipo", en(ComunicaDao.TIPOS, texto(req, "tipo")) ? texto(req, "tipo") : "OTRO");
        d.put("red", en(ComunicaDao.REDES, texto(req, "red")) ? texto(req, "red") : "INSTAGRAM");
        d.put("estado", en(ComunicaDao.ESTADOS_PROYECTO, texto(req, "estado")) ? texto(req, "estado") : estadoPorDefecto);
        d.put("descripcion", recortar(texto(req, "descripcion"), 3000));
        d.put("etiquetas", limpiarEtiquetas(texto(req, "etiquetas")));
        Integer resp = entero(req, "responsable");
        if (resp != null && !dao.esDelEquipo(resp)) { Sesion.error(req, "El responsable tiene que ser del team Comunica."); return null; }
        d.put("idResponsable", resp);
        d.put("fechaObjetivo", fecha(req, "fechaObjetivo"));
        for (String k : List.of("linkReferencia", "linkDrive", "linkPublicacion")) {
            String l = link(texto(req, k));
            if (l == null) { Sesion.error(req, "Los links tienen que empezar con https:// (TikTok, Instagram, Drive, Canva…)."); return null; }
            d.put(k, l.isEmpty() ? null : l);
        }
        return d;
    }

    /** "Tendencia, meme ,  Palacios" → "tendencia, meme, palacios" (máx. 8, sin repetir). */
    static String limpiarEtiquetas(String s) {
        if (s == null) return null;
        LinkedHashSet<String> l = new LinkedHashSet<>();
        for (String t : s.split(",")) {
            String x = t.trim().toLowerCase(Formato.ES).replaceAll("[<>\"]", "");
            if (!x.isEmpty() && x.length() <= 25) l.add(x);
            if (l.size() == 8) break;
        }
        String r = String.join(", ", l);
        return r.isEmpty() ? null : recortar(r, 200);
    }

    private void asignarProyecto(int id, Map<String, Object> p, int responsable, int yo) throws SQLException {
        Map<String, Object> d = new HashMap<>(p);
        d.put("idResponsable", responsable);
        d.put("estado", "EN_DESARROLLO");
        d.put("linkReferencia", p.get("linkReferencia")); d.put("linkDrive", p.get("linkDrive")); d.put("linkPublicacion", p.get("linkPublicacion"));
        if (dao.esDelEquipo(responsable)) dao.actualizar(id, d, yo);
    }

    private void postProyecto(HttpServletRequest req, HttpServletResponse resp, int yo, boolean mod) throws SQLException, IOException {
        String a = accion(req, "guardar");
        Integer id = entero(req, "id");
        if ("crear".equals(a)) {
            Map<String, Object> d = leerProyecto(req, "EN_DESARROLLO");
            if (d == null) { ir(req, resp, "/comunica/proyecto" + (texto(req, "estado") == null ? "" : "?estado=" + mesOEstado(texto(req, "estado")))); return; }
            int nuevo = dao.crear(d, yo);
            Sesion.ok(req, "IDEA".equals(d.get("estado")) ? "Idea guardada en el banco." : "Proyecto creado.");
            ir(req, resp, "/comunica/proyecto?id=" + nuevo); return;
        }
        Map<String, Object> p = id == null ? null : dao.proyecto(id, yo);
        if (p == null) { Sesion.error(req, "Ese proyecto no existe."); ir(req, resp, "/comunica"); return; }
        switch (a) {
            case "guardar" -> {
                Map<String, Object> d = leerProyecto(req, (String) p.get("estado"));
                if (d != null) { dao.actualizar(id, d, yo); Sesion.ok(req, "Cambios guardados."); }
            }
            case "estado" -> {
                String e = texto(req, "estado");
                if (en(ComunicaDao.ESTADOS_PROYECTO, e)) {
                    dao.cambiarEstado(id, e, yo);
                    Sesion.ok(req, "«" + p.get("titulo") + "» → " + ComunicaDao.estadoProyectoTexto(e) + ".");
                }
                if ("tablero".equals(req.getParameter("volver"))) { ir(req, resp, "/comunica"); return; }
            }
            case "tarea" -> {
                String t = texto(req, "tarea");
                if (t != null) dao.agregarTarea(id, recortar(t, 200));
            }
            case "marcar" -> { Integer t = entero(req, "idTarea"); if (t != null) dao.marcarTarea(t, id); }
            case "quitar" -> { Integer t = entero(req, "idTarea"); if (t != null) dao.borrarTarea(t, id); }
            case "eliminar" -> {
                // borra el admin/moderador, o quien propuso la idea mientras siga siendo idea
                boolean propia = Objects.equals(p.get("idPropuestoPor"), yo) && "IDEA".equals(p.get("estado"));
                if (!mod && !propia) Sesion.error(req, "Solo el admin o moderador de Comunica puede borrar proyectos. Puedes archivarlo.");
                else { dao.eliminarProyecto(id); Sesion.ok(req, "«" + p.get("titulo") + "» eliminado."); ir(req, resp, "IDEA".equals(p.get("estado")) ? "/comunica/ideas" : "/comunica"); return; }
            }
            default -> { }
        }
        ir(req, resp, "/comunica/proyecto?id=" + id);
    }

    private static String mesOEstado(String e) { return en(ComunicaDao.ESTADOS_PROYECTO, e) ? e : "EN_DESARROLLO"; }

    private void postProponer(HttpServletRequest req, HttpServletResponse resp, int yo) throws SQLException, IOException {
        String titulo = texto(req, "titulo");
        if (titulo == null) { Sesion.error(req, "Cuéntanos la idea en una línea."); ir(req, resp, "/proponer-idea"); return; }
        String ref = link(texto(req, "linkReferencia"));
        if (ref == null) { Sesion.error(req, "El link de referencia tiene que empezar con https:// (por ejemplo, un TikTok)."); ir(req, resp, "/proponer-idea"); return; }
        Map<String, Object> d = new HashMap<>();
        d.put("titulo", recortar(titulo, 120));
        d.put("tipo", en(ComunicaDao.TIPOS, texto(req, "tipo")) ? texto(req, "tipo") : "OTRO");
        d.put("red", "TIKTOK".equals(d.get("tipo")) ? "TIKTOK" : "INSTAGRAM");
        d.put("estado", "IDEA");
        d.put("descripcion", recortar(texto(req, "descripcion"), 1500));
        d.put("etiquetas", null);
        d.put("idResponsable", null);
        d.put("fechaObjetivo", null);
        d.put("linkReferencia", ref.isEmpty() ? null : ref);
        d.put("linkDrive", null); d.put("linkPublicacion", null);
        dao.crear(d, yo);
        Sesion.ok(req, "¡Gracias! Tu idea le llegó a Comunica 💡");
        ir(req, resp, "/proponer-idea");
    }

    private void postPlantilla(HttpServletRequest req, HttpServletResponse resp, int yo, boolean mod) throws SQLException, IOException, ServletException {
        String a = accion(req, "agregar");
        Integer id = entero(req, "id");
        switch (a) {
            case "agregar", "guardar" -> {
                String nombre = texto(req, "nombre");
                String tipo = en(ComunicaDao.TIPOS_PLANTILLA, texto(req, "tipo")) ? texto(req, "tipo") : "Otro";
                String ciclo = texto(req, "ciclo");
                if (ciclo != null && !ciclo.matches("\\d{4}-[0-2]")) ciclo = recortar(ciclo, 10);
                String canva = link(texto(req, "linkCanva"));
                String volver = "guardar".equals(a) && id != null ? "/comunica/plantilla?id=" + id : "/comunica/plantillas";
                if (nombre == null) { Sesion.error(req, "Ponle un nombre a la plantilla."); ir(req, resp, volver); return; }
                if (canva == null) { Sesion.error(req, "El link de Canva tiene que empezar con https://"); ir(req, resp, volver); return; }
                byte[] img = null;
                Part foto;
                try { foto = req.getPart("imagen"); } catch (IllegalStateException e) { Sesion.error(req, "La imagen es muy pesada (máximo 8 MB)."); ir(req, resp, volver); return; }
                if (foto != null && foto.getSize() > 0) {
                    try (InputStream in = foto.getInputStream()) { img = FinanzasServlet.comprimir(in.readAllBytes()); }
                    if (img == null) { Sesion.error(req, "No pude leer la imagen. Sube la vista previa en JPG o PNG."); ir(req, resp, volver); return; }
                    if (img == FinanzasServlet.DEMASIADO_GRANDE) { Sesion.error(req, "Esa imagen es demasiado grande. Exporta el flyer en tamaño normal."); ir(req, resp, volver); return; }
                    if (img == FinanzasServlet.OCUPADO) { Sesion.error(req, "Hay varias imágenes subiéndose a la vez. Inténtalo en un momento."); ir(req, resp, volver); return; }
                }
                if ("agregar".equals(a)) {
                    if (img == null && canva.isEmpty()) { Sesion.error(req, "Sube la vista previa o pon el link de Canva (mejor los dos)."); ir(req, resp, volver); return; }
                    dao.agregarPlantilla(recortar(nombre, 100), tipo, ciclo, canva.isEmpty() ? null : canva, recortar(texto(req, "notas"), 300), img, yo);
                    Sesion.ok(req, "Plantilla «" + recortar(nombre, 100) + "» agregada.");
                } else if (id != null) {
                    dao.editarPlantilla(id, recortar(nombre, 100), tipo, ciclo, canva.isEmpty() ? null : canva, recortar(texto(req, "notas"), 300), img);
                    Sesion.ok(req, "Plantilla guardada.");
                }
                ir(req, resp, volver);
            }
            case "archivar", "activar" -> {
                if (id != null) { dao.activarPlantilla(id, "activar".equals(a)); Sesion.ok(req, "archivar".equals(a) ? "Plantilla archivada (ya no sale en la galería)." : "Plantilla de vuelta en la galería."); }
                ir(req, resp, "/comunica/plantillas" + ("activar".equals(a) ? "?archivo=1" : ""));
            }
            case "eliminar" -> {
                if (!mod) Sesion.error(req, "Solo el admin o moderador de Comunica puede borrar plantillas. Puedes archivarla.");
                else if (id != null) { dao.eliminarPlantilla(id); Sesion.ok(req, "Plantilla eliminada."); }
                ir(req, resp, "/comunica/plantillas");
            }
            default -> ir(req, resp, "/comunica/plantillas");
        }
    }
}
