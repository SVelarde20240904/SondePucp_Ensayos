package pe.sdp.servlets;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pe.sdp.daos.ComunicaDao;
import pe.sdp.daos.EncuestaDao;
import pe.sdp.daos.RrppDao;
import pe.sdp.util.Formato;
import pe.sdp.util.Sesion;

import java.io.IOException;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.*;

/**
 * Módulo RRPP.
 *  /rrpp                    resumen (externos de la semana, próxima visita, meta, directorio)
 *  /rrpp/externos           ciclo semanal del Form de externos del taller del jueves
 *  /rrpp/visitas            visitas formales (próximas y pasadas)
 *  /rrpp/visita?id=         crear / editar / convocar una visita (?encuesta= la arma desde una encuesta)
 *  /rrpp/directorio         contactos por categoría
 *  /rrpp/contacto?id=       un contacto con su historial
 *  /visitas                 (todos) las visitas convocadas: "Voy / No puedo"
 *  /externos                (todos) lleva al Form de externos de esta semana si está abierto
 */
@WebServlet(urlPatterns = {"/rrpp", "/rrpp/*", "/visitas", "/externos"})
public class RrppServlet extends BaseServlet {

    private static final String PERM = "GESTIONAR_RRPP";
    private static final String MOD = "MODERAR_RRPP";
    private final RrppDao dao = new RrppDao();

    private String ruta(HttpServletRequest req) {
        String sp = req.getServletPath();
        if ("/visitas".equals(sp) || "/externos".equals(sp)) return "todos" + sp;   // no confundir con /rrpp/visitas y /rrpp/externos
        return req.getPathInfo() == null ? "/" : req.getPathInfo();
    }

    private static LocalDate hoy() { return LocalDate.now(Formato.ZONA); }
    private static boolean en(List<String> l, String v) { return v != null && l.contains(v); }
    private static String recortar(String s, int max) { return s == null ? null : (s.length() > max ? s.substring(0, max) : s); }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int yo = Sesion.idUsuario(req);
        try {
            String r = ruta(req);
            if ("todos/externos".equals(r)) { externosMiembro(req, resp); return; }
            if ("todos/visitas".equals(r)) {
                List<Map<String, Object>> vs = dao.visitasParaTodos(yo);
                Map<Integer, Map<String, List<String>>> resp2 = new HashMap<>();
                for (Map<String, Object> v : vs) resp2.put((Integer) v.get("id"), dao.respuestas((Integer) v.get("id")));
                req.setAttribute("visitas", vs);
                req.setAttribute("respuestas", resp2);
                vista(req, resp, "rrpp/visitas-todos");
                return;
            }
            if (!Sesion.requiere(req, resp, PERM)) return;
            req.setAttribute("puedeModerar", Sesion.tiene(req, MOD));
            switch (r) {
                case "/" -> {
                    req.setAttribute("sem", dao.semana(RrppDao.juevesActual()));
                    List<Map<String, Object>> prox = dao.visitas(true, yo);
                    req.setAttribute("proxima", prox.isEmpty() ? null : prox.get(0));
                    YearMonth ym = YearMonth.from(hoy());
                    int[] vm = dao.visitasDelMes(ym);
                    req.setAttribute("visitasHechas", vm[0]);
                    req.setAttribute("visitasConvocadas", vm[1]);
                    req.setAttribute("meta", Formato.entero(dao.configExternos().get("metaVisitas")) == null ? 1 : Formato.entero(dao.configExternos().get("metaVisitas")));
                    req.setAttribute("mesTexto", Formato.capital(ym.getMonth().getDisplayName(java.time.format.TextStyle.FULL, Formato.ES)));
                    String ciclo = ComunicaServlet.cicloActual();
                    req.setAttribute("ciclo", ciclo);
                    req.setAttribute("dir", dao.resumenDirectorio(ciclo));
                    req.setAttribute("encuestas", new EncuestaDao().paraMi(yo, true));
                    vista(req, resp, "rrpp/resumen");
                }
                case "/externos" -> {
                    req.setAttribute("sem", dao.semana(RrppDao.juevesActual()));
                    req.setAttribute("historial", dao.historial(12));
                    req.setAttribute("cfg", dao.configExternos());
                    req.setAttribute("equipo", dao.equipo());
                    req.setAttribute("dias", List.of(List.of("6", "Sábado"), List.of("7", "Domingo"), List.of("1", "Lunes"), List.of("2", "Martes"),
                            List.of("3", "Miércoles"), List.of("5", "Viernes")));
                    vista(req, resp, "rrpp/externos");
                }
                case "/visitas" -> {
                    req.setAttribute("proximas", dao.visitas(true, yo));
                    req.setAttribute("pasadas", dao.visitas(false, yo));
                    YearMonth ym = YearMonth.from(hoy());
                    int[] vm = dao.visitasDelMes(ym);
                    req.setAttribute("visitasHechas", vm[0]);
                    req.setAttribute("meta", Formato.entero(dao.configExternos().get("metaVisitas")) == null ? 1 : Formato.entero(dao.configExternos().get("metaVisitas")));
                    vista(req, resp, "rrpp/visitas");
                }
                case "/visita" -> {
                    Integer id = entero(req, "id");
                    Map<String, Object> v;
                    if (id == null) {
                        v = new HashMap<>();
                        v.put("nuevo", true);
                        v.put("dressCode", "Polo de Son de PUCP 👕");
                        v.put("idCoordinador", dao.esDelEquipo(yo) ? yo : null);
                        Integer enc = entero(req, "encuesta");
                        if (enc != null) {
                            Map<String, Object> e = new EncuestaDao().ver(enc, yo);
                            if (e != null) { v.put("idEncuesta", enc); v.put("titulo", recortar(((String) e.get("pregunta")).replaceAll("[¿?]", "").trim(), 120)); }
                        }
                    } else {
                        v = dao.visita(id, yo);
                        if (v == null) { Sesion.error(req, "Esa visita no existe."); ir(req, resp, "/rrpp/visitas"); return; }
                        req.setAttribute("resp", dao.respuestas(id));
                        req.setAttribute("activos", dao.activos());
                    }
                    req.setAttribute("v", v);
                    req.setAttribute("ruedas", dao.ruedas());
                    req.setAttribute("equipo", dao.equipo());
                    vista(req, resp, "rrpp/visita");
                }
                case "/directorio" -> {
                    String cat = en(RrppDao.CATEGORIAS, texto(req, "cat")) ? texto(req, "cat") : null;
                    String q = texto(req, "q");
                    String ciclo = texto(req, "ciclo");
                    if (ciclo != null && !ciclo.matches("\\d{4}-[0-2]")) ciclo = null;
                    req.setAttribute("contactos", dao.contactos(cat, q, ciclo));
                    req.setAttribute("cat", cat);
                    req.setAttribute("q", q);
                    req.setAttribute("cicloFiltro", ciclo);
                    req.setAttribute("cicloActual", ComunicaServlet.cicloActual());
                    req.setAttribute("dir", dao.resumenDirectorio(ComunicaServlet.cicloActual()));
                    vista(req, resp, "rrpp/directorio");
                }
                case "/contacto" -> {
                    Integer id = entero(req, "id");
                    Map<String, Object> k;
                    if (id == null) {
                        k = new HashMap<>();
                        k.put("nuevo", true);
                        String cat = en(RrppDao.CATEGORIAS, texto(req, "cat")) ? texto(req, "cat") : "RUEDA";
                        k.put("categoria", cat);
                        k.put("estado", "SIN_CONTACTAR");
                        if ("H_T".equals(cat)) k.put("ciclo", ComunicaServlet.cicloActual());
                        k.put("idResponsable", dao.esDelEquipo(yo) ? yo : null);
                    } else {
                        k = dao.contacto(id);
                        if (k == null) { Sesion.error(req, "Ese contacto no existe."); ir(req, resp, "/rrpp/directorio"); return; }
                    }
                    req.setAttribute("k", k);
                    req.setAttribute("categorias", RrppDao.CATEGORIAS);
                    req.setAttribute("estados", RrppDao.ESTADOS_CONTACTO);
                    req.setAttribute("equipo", dao.equipo());
                    req.setAttribute("hoy", hoy().toString());
                    vista(req, resp, "rrpp/contacto");
                }
                default -> resp.sendError(404);
            }
        } catch (SQLException e) { fallo(req, resp, e); }
    }

    /** /externos: si el registro está abierto, lleva al Form; si no, explica cuándo abre. */
    private void externosMiembro(HttpServletRequest req, HttpServletResponse resp) throws SQLException, ServletException, IOException {
        Map<String, Object> s = dao.semana(RrppDao.juevesActual());
        String link = (String) s.get("link");
        if (Boolean.TRUE.equals(s.get("abierto")) && link != null && link.matches("(?i)^https://\\S+$")) { resp.sendRedirect(link); return; }
        req.setAttribute("sem", s);
        vista(req, resp, "rrpp/externos-cerrado");
    }

    // ======================================================================

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int yo = Sesion.idUsuario(req);
        try {
            String r = ruta(req);
            if ("todos/visitas".equals(r)) {
                Integer id = entero(req, "id");
                String a = texto(req, "respuesta");
                if (id != null && ("VOY".equals(a) || "NO_PUEDO".equals(a))) {
                    if (dao.responder(id, yo, a)) Sesion.ok(req, "VOY".equals(a) ? "¡Anotado! Nos vemos ahí con el polo 👕" : "Listo, quedó anotado que no puedes.");
                    else Sesion.error(req, "Esa visita ya no recibe respuestas.");
                }
                ir(req, resp, "/visitas" + (id == null ? "" : "#v" + id));
                return;
            }
            if (!Sesion.requiere(req, resp, PERM)) return;
            boolean mod = Sesion.tiene(req, MOD);
            switch (r) {
                case "/externos" -> postExternos(req, resp, yo, mod);
                case "/visita" -> postVisita(req, resp, yo, mod);
                case "/contacto" -> postContacto(req, resp, yo, mod);
                default -> resp.sendError(404);
            }
        } catch (SQLException e) { fallo(req, resp, e); }
    }

    private void postExternos(HttpServletRequest req, HttpServletResponse resp, int yo, boolean mod) throws SQLException, IOException {
        String a = accion(req, "");
        if ("config".equals(a)) {
            if (!mod) Sesion.error(req, "Solo el admin o moderador de RRPP cambia el ciclo.");
            else {
                Integer pub = entero(req, "diaPublicar"), cie = entero(req, "diaCierre"), env = entero(req, "diaEnvio"), meta = entero(req, "meta");
                LocalTime hc = hora(req, "horaCierre");
                Integer enc = entero(req, "encargado");
                List<Integer> validos = List.of(1, 2, 3, 5, 6, 7);
                if (pub == null || cie == null || env == null || !validos.contains(pub) || !validos.contains(cie) || !validos.contains(env) || hc == null)
                    Sesion.error(req, "Revisa los días y la hora del ciclo.");
                else if (enc != null && !dao.esDelEquipo(enc)) Sesion.error(req, "El encargado fijo tiene que ser del team RRPP.");
                else {
                    dao.guardarConfigExternos(pub, cie, hc, env, enc, meta == null || meta < 0 || meta > 10 ? 1 : meta);
                    Sesion.ok(req, "Ciclo de externos guardado. Las fechas de esta semana ya se calculan así.");
                }
            }
            ir(req, resp, "/rrpp/externos"); return;
        }
        LocalDate j = fecha(req, "jueves");
        if (j == null || j.getDayOfWeek() != DayOfWeek.THURSDAY || j.isAfter(RrppDao.juevesActual().plusWeeks(1))) {
            Sesion.error(req, "Semana no válida."); ir(req, resp, "/rrpp/externos"); return;
        }
        dao.semana(j);   // se asegura de que exista
        switch (a) {
            case "publicar" -> {
                String l = ComunicaServlet.link(texto(req, "link"));
                if (l == null || l.isEmpty() || !l.toLowerCase().startsWith("https://")) Sesion.error(req, "Pega el link del Form (empieza con https://).");
                else { dao.publicarLink(j, l, yo); Sesion.ok(req, "Link publicado: a todos les aparece en Inicio hasta que cierre."); }
            }
            case "enviado" -> { dao.marcarEnviado(j, true, yo); Sesion.ok(req, "Marcado como enviado a la DAES ✓"); }
            case "deshacer" -> { dao.marcarEnviado(j, false, yo); Sesion.ok(req, "Se desmarcó el envío."); }
            case "sinTaller" -> { dao.sinTaller(j, true); Sesion.ok(req, "Esta semana no hay taller: no se piden externos."); }
            case "conTaller" -> { dao.sinTaller(j, false); Sesion.ok(req, "Semana activa otra vez."); }
            case "encargado" -> {
                Integer e = entero(req, "encargado");
                if (!mod) Sesion.error(req, "Solo el admin o moderador de RRPP cambia el encargado.");
                else if (e != null && !dao.esDelEquipo(e)) Sesion.error(req, "El encargado tiene que ser del team RRPP.");
                else { dao.cambiarEncargado(j, e); Sesion.ok(req, "Encargado de la semana actualizado."); }
            }
            default -> { }
        }
        ir(req, resp, "/rrpp/externos");
    }

    private Map<String, Object> leerVisita(HttpServletRequest req) throws SQLException {
        Map<String, Object> d = new HashMap<>();
        String titulo = texto(req, "titulo");
        LocalDate f = fecha(req, "fecha");
        if (titulo == null) { Sesion.error(req, "Ponle un título a la visita."); return null; }
        if (f == null) { Sesion.error(req, "¿Qué día es la visita?"); return null; }
        d.put("titulo", recortar(titulo, 120));
        d.put("fecha", f);
        d.put("hora", hora(req, "hora"));
        d.put("horaEncuentro", hora(req, "horaEncuentro"));
        for (String[] k : new String[][]{{"lugar", "200"}, {"puntoEncuentro", "200"}, {"comoLlegar", "500"}, {"costo", "80"}, {"dressCode", "120"}, {"notas", "1000"}})
            d.put(k[0], recortar(texto(req, k[0]), Integer.parseInt(k[1])));
        for (String k : List.of("linkMapa", "linkFoto")) {
            String l = ComunicaServlet.link(texto(req, k));
            if (l == null) { Sesion.error(req, "Los links tienen que empezar con https://"); return null; }
            d.put(k, l.isEmpty() ? null : l);
        }
        Integer cont = entero(req, "idContacto");
        boolean existe = false;
        if (cont != null) for (Map<String, Object> x : dao.ruedas()) if (cont.equals(x.get("id"))) existe = true;
        d.put("idContacto", existe ? cont : null);
        Integer coord = entero(req, "coordinador");
        if (coord != null && !dao.esDelEquipo(coord)) { Sesion.error(req, "El coordinador tiene que ser del team RRPP."); return null; }
        d.put("idCoordinador", coord);
        return d;
    }

    private void postVisita(HttpServletRequest req, HttpServletResponse resp, int yo, boolean mod) throws SQLException, IOException {
        String a = accion(req, "guardar");
        Integer id = entero(req, "id");
        if ("crear".equals(a)) {
            Map<String, Object> d = leerVisita(req);
            if (d == null) { ir(req, resp, "/rrpp/visita"); return; }
            Integer enc = entero(req, "idEncuesta");
            if (enc != null && new EncuestaDao().ver(enc, yo) == null) enc = null;
            int nuevo = dao.crearVisita(d, enc, yo);
            Sesion.ok(req, "Visita creada como borrador. Revísala y convócala cuando esté lista.");
            ir(req, resp, "/rrpp/visita?id=" + nuevo); return;
        }
        Map<String, Object> v = id == null ? null : dao.visita(id, yo);
        if (v == null) { Sesion.error(req, "Esa visita no existe."); ir(req, resp, "/rrpp/visitas"); return; }
        switch (a) {
            case "guardar" -> { Map<String, Object> d = leerVisita(req); if (d != null) { dao.actualizarVisita(id, d); Sesion.ok(req, "Cambios guardados."); } }
            case "estado" -> {
                String e = texto(req, "estado");
                if (en(RrppDao.ESTADOS_VISITA, e)) {
                    if ("CONVOCADA".equals(e) && ((LocalDate) v.get("fecha")).isBefore(hoy())) { Sesion.error(req, "La fecha ya pasó: cámbiala antes de convocar."); break; }
                    dao.estadoVisita(id, e);
                    if ("REALIZADA".equals(e) && v.get("idProyecto") == null) {
                        // el post de la visita le llega a Comunica como proyecto
                        Map<String, Object> p = new HashMap<>();
                        p.put("titulo", recortar("Post de la visita: " + v.get("titulo"), 120));
                        p.put("tipo", "POST"); p.put("red", "INSTAGRAM"); p.put("estado", "EN_DESARROLLO");
                        p.put("descripcion", "Visita formal del " + v.get("fechaTexto") + (v.get("rueda") == null ? "" : " a " + v.get("rueda"))
                                + ". Publicar las fotos con el polo y etiquetar a la rueda." + (v.get("linkFoto") == null ? " (RRPP todavía no subió el link de las fotos)" : ""));
                        p.put("etiquetas", "visita, rrpp");
                        p.put("idResponsable", null); p.put("fechaObjetivo", hoy().plusDays(3));
                        p.put("linkReferencia", null); p.put("linkDrive", v.get("linkFoto")); p.put("linkPublicacion", null);
                        int idp = new ComunicaDao().crear(p, yo);
                        dao.ponerProyecto(id, idp);
                        Sesion.ok(req, "Visita marcada como realizada. A Comunica le llegó el proyecto «" + p.get("titulo") + "».");
                    } else Sesion.ok(req, switch (e) {
                        case "CONVOCADA" -> "¡Convocada! A todos les aparece en Pendientes para responder «Voy / No puedo».";
                        case "CANCELADA" -> "Visita cancelada.";
                        case "BORRADOR" -> "Volvió a borrador (ya no se ve para los demás).";
                        default -> "Visita marcada como realizada.";
                    });
                }
            }
            case "check" -> { String p = texto(req, "paso"); dao.check(id, p, "1".equals(req.getParameter("hecho"))); }
            case "eliminar" -> {
                if (!mod) Sesion.error(req, "Solo el admin o moderador de RRPP borra visitas. Puedes cancelarla.");
                else { dao.eliminarVisita(id); Sesion.ok(req, "Visita eliminada."); ir(req, resp, "/rrpp/visitas"); return; }
            }
            default -> { }
        }
        ir(req, resp, "/rrpp/visita?id=" + id);
    }

    private Map<String, Object> leerContacto(HttpServletRequest req) throws SQLException {
        Map<String, Object> d = new HashMap<>();
        String nombre = texto(req, "nombre");
        if (nombre == null) { Sesion.error(req, "Ponle un nombre (la rueda, el centro federado, el H…)."); return null; }
        d.put("categoria", en(RrppDao.CATEGORIAS, texto(req, "categoria")) ? texto(req, "categoria") : "OTRO");
        d.put("nombre", recortar(nombre, 120));
        for (String[] k : new String[][]{{"persona", "120"}, {"cargo", "80"}, {"telefono", "30"}, {"correo", "120"}, {"ubicacion", "200"}, {"horario", "200"}, {"notas", "1000"}})
            d.put(k[0], recortar(texto(req, k[0]), Integer.parseInt(k[1])));
        String ig = texto(req, "instagram");
        if (ig != null) {
            ig = ig.replaceFirst("(?i)^https?://(www\\.)?instagram\\.com/", "").replaceAll("[/?].*$", "").replace("@", "").trim();
            if (!ig.matches("[A-Za-z0-9._]{1,30}")) { Sesion.error(req, "El Instagram va como usuario, por ejemplo @sondepucp."); return null; }
            ig = "@" + ig;
        }
        d.put("instagram", ig);
        String correo = (String) d.get("correo");
        if (correo != null && !correo.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) { Sesion.error(req, "Revisa el correo."); return null; }
        String l = ComunicaServlet.link(texto(req, "linkMapa"));
        if (l == null) { Sesion.error(req, "El link de Google Maps tiene que empezar con https://"); return null; }
        d.put("linkMapa", l.isEmpty() ? null : l);
        String ciclo = texto(req, "ciclo");
        d.put("ciclo", ciclo != null && ciclo.matches("\\d{4}-[0-2]") ? ciclo : null);
        d.put("estado", en(RrppDao.ESTADOS_CONTACTO, texto(req, "estado")) ? texto(req, "estado") : "SIN_CONTACTAR");
        Integer resp = entero(req, "responsable");
        if (resp != null && !dao.esDelEquipo(resp)) { Sesion.error(req, "El responsable tiene que ser del team RRPP."); return null; }
        d.put("idResponsable", resp);
        return d;
    }

    private void postContacto(HttpServletRequest req, HttpServletResponse resp, int yo, boolean mod) throws SQLException, IOException {
        String a = accion(req, "guardar");
        Integer id = entero(req, "id");
        if ("crear".equals(a)) {
            Map<String, Object> d = leerContacto(req);
            if (d == null) { ir(req, resp, "/rrpp/contacto?cat=" + (en(RrppDao.CATEGORIAS, texto(req, "categoria")) ? texto(req, "categoria") : "RUEDA")); return; }
            int nuevo = dao.crearContacto(d, yo);
            Sesion.ok(req, "Contacto agregado al directorio.");
            ir(req, resp, "/rrpp/contacto?id=" + nuevo); return;
        }
        if (id == null || dao.contacto(id) == null) { Sesion.error(req, "Ese contacto no existe."); ir(req, resp, "/rrpp/directorio"); return; }
        switch (a) {
            case "guardar" -> { Map<String, Object> d = leerContacto(req); if (d != null) { dao.actualizarContacto(id, d); Sesion.ok(req, "Contacto guardado."); } }
            case "interaccion" -> {
                String nota = texto(req, "nota");
                LocalDate f = fecha(req, "fechaNota");
                if (f == null || f.isAfter(hoy())) f = hoy();
                if (nota == null) Sesion.error(req, "Escribe qué pasó (ej. «Le escribimos por el taller, dijo que sí»).");
                else {
                    String est = en(RrppDao.ESTADOS_CONTACTO, texto(req, "nuevoEstado")) ? texto(req, "nuevoEstado") : null;
                    dao.agregarInteraccion(id, f, recortar(nota, 500), est, yo);
                    Sesion.ok(req, "Anotado en el historial.");
                }
            }
            case "eliminar" -> {
                if (!mod) Sesion.error(req, "Solo el admin o moderador de RRPP borra contactos.");
                else { dao.eliminarContacto(id); Sesion.ok(req, "Contacto eliminado."); ir(req, resp, "/rrpp/directorio"); return; }
            }
            default -> { }
        }
        ir(req, resp, "/rrpp/contacto?id=" + id);
    }
}
