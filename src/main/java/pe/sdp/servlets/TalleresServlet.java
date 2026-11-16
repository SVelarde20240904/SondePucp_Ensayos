package pe.sdp.servlets;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pe.sdp.beans.*;
import pe.sdp.daos.MiembroDao;
import pe.sdp.daos.TallerDao;
import pe.sdp.util.Formato;
import pe.sdp.util.Sesion;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

/**
 * Team Talleres (permiso GESTIONAR_TALLERES, salvo el formulario y Mi feedback):
 *  /talleres                    lista de talleres
 *  /talleres/nuevo, /editar     cronograma: bloques y pasos por nivel + dictado planeado
 *  /talleres/detalle?id=        resumen, dictado final (asistencia, reemplazos), formularios y sugerencias
 *  /talleres/feedback?id=       feedback crudo por persona + resumen anónimo para enviar
 *  /talleres/flyer?id=          cronograma listo para compartir (+ /flyer-nivel: imagen por nivel para publicar)
 *  /talleres/lo-dictado?id=     lo que se dictó al final (el plan se conserva para comparar)
 *  /talleres/dictado            historial de dictado (como la hoja DICTADO DE NIVELES)
 *  /talleres/pasos              catálogo de pasos por nivel
 *  /talleres/formulario?id=     formulario post-taller (talleristas y apoyos de ese taller)
 *  /mi-feedback                 resúmenes anónimos que me enviaron + mi historial en talleres
 */
@WebServlet(urlPatterns = {"/talleres", "/talleres/*", "/mi-feedback"})
public class TalleresServlet extends BaseServlet {

    private static final String PERM = "GESTIONAR_TALLERES";
    private final TallerDao dao = new TallerDao();
    private final MiembroDao miembros = new MiembroDao();

    private static String ruta(HttpServletRequest req) {
        if ("/mi-feedback".equals(req.getServletPath())) return "/mi-feedback";
        return req.getPathInfo() == null ? "/" : req.getPathInfo();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int yo = Sesion.idUsuario(req);
        try {
            String r = ruta(req);
            if ("/mi-feedback".equals(r)) {
                req.setAttribute("resumenes", dao.misResumenes(yo));
                req.setAttribute("stats", dao.resumenPersona(yo));
                req.setAttribute("pendientes", dao.formulariosPendientes(yo));
                vista(req, resp, "talleres/mi-feedback");
                return;
            }
            if ("/formulario".equals(r)) { formulario(req, resp, yo); return; }
            if (!Sesion.requiere(req, resp, PERM)) return;
            switch (r) {
                case "/" -> {
                    List<Taller> lista = dao.listar();
                    Map<String, List<Taller>> porCiclo = new LinkedHashMap<>();
                    for (Taller t : lista) porCiclo.computeIfAbsent(t.getCiclo(), k -> new ArrayList<>()).add(t);
                    req.setAttribute("porCiclo", porCiclo);
                    req.setAttribute("total", lista.size());
                    vista(req, resp, "talleres/lista");
                }
                case "/nuevo", "/editar" -> {
                    Taller t;
                    if (entero(req, "id") != null) {
                        t = dao.obtener(entero(req, "id"));
                        if (t == null) { Sesion.error(req, "No encontré ese taller."); ir(req, resp, "/talleres"); return; }
                        for (String n : Taller.NIVELES) if (t.getPlan().get(n).isEmpty()) t.getPlan().get(n).addAll(TallerSeccion.plantilla());
                    } else {
                        t = new Taller();
                        LocalDate jueves = LocalDate.now(Formato.ZONA);
                        while (jueves.getDayOfWeek() != java.time.DayOfWeek.THURSDAY) jueves = jueves.plusDays(1);
                        t.setFecha(jueves); t.setCiclo(ciclo(jueves)); t.setNumero(dao.siguienteNumero(t.getCiclo()));
                        Taller ant = dao.ultimo();   // el cronograma del taller anterior sirve de plantilla
                        if (ant != null) {
                            for (String n : Taller.NIVELES) t.getPlan().get(n).addAll(ant.getPlan().get(n).isEmpty() ? TallerSeccion.plantilla() : ant.getPlan().get(n));
                            t.setLugar(ant.getLugar()); t.setHoraInicio(ant.getHoraInicio()); t.setHoraFin(ant.getHoraFin());
                            req.setAttribute("plantilla", ant);
                        } else for (String n : Taller.NIVELES) t.getPlan().get(n).addAll(TallerSeccion.plantilla());
                    }
                    datosFormulario(req, t);
                    vista(req, resp, "talleres/form");
                }
                case "/detalle" -> {
                    Taller t = pedido(req, resp); if (t == null) return;
                    req.setAttribute("t", t);
                    req.setAttribute("respuestas", dao.respuestas(t.getIdTaller()));
                    List<TallerFeedback> fb = dao.feedbackDe(t.getIdTaller());
                    List<TallerFeedback> sug = new ArrayList<>();
                    int nfb = 0;
                    for (TallerFeedback f : fb) { if ("TALLER".equals(f.getTipo())) sug.add(f); else nfb++; }
                    req.setAttribute("sugerencias", sug);
                    req.setAttribute("nFeedback", nfb);
                    req.setAttribute("activos", miembros.listarActivos());
                    req.setAttribute("yoEnTaller", !dao.misFunciones(t.getIdTaller(), yo).isEmpty());
                    req.setAttribute("confirmaciones", dao.confirmaciones(t.getIdTaller()));
                    vista(req, resp, "talleres/detalle");
                }
                case "/feedback" -> {
                    Taller t = pedido(req, resp); if (t == null) return;
                    List<TallerFeedback> fb = dao.feedbackDe(t.getIdTaller());
                    // a quién va dirigido: los talleristas que dictaron + cualquiera que haya recibido feedback
                    Map<Integer, String> receptores = new LinkedHashMap<>();
                    for (TallerPersona p : t.getPersonas()) if ("TALLERISTA".equals(p.getFuncion()) && p.isEfectivo()) receptores.put(p.getIdMiembro(), p.getNombre());
                    Map<Integer, List<TallerFeedback>> porPersona = new LinkedHashMap<>();
                    List<TallerFeedback> sug = new ArrayList<>();
                    for (TallerFeedback f : fb) {
                        if ("TALLER".equals(f.getTipo())) { sug.add(f); continue; }
                        receptores.putIfAbsent(f.getParaMiembro(), f.getPara());
                        porPersona.computeIfAbsent(f.getParaMiembro(), k -> new ArrayList<>()).add(f);
                    }
                    for (Integer id : receptores.keySet()) porPersona.putIfAbsent(id, new ArrayList<>());
                    req.setAttribute("t", t);
                    req.setAttribute("receptores", receptores);
                    req.setAttribute("porPersona", porPersona);
                    req.setAttribute("resumenes", dao.resumenes(t.getIdTaller()));
                    req.setAttribute("sugerencias", sug);
                    vista(req, resp, "talleres/feedback");
                }
                case "/flyer" -> {
                    Taller t = pedido(req, resp); if (t == null) return;
                    req.setAttribute("t", t);
                    req.getRequestDispatcher("/WEB-INF/jsp/talleres/flyer.jsp").forward(req, resp);
                }
                case "/flyer-nivel" -> {
                    Taller t = pedido(req, resp); if (t == null) return;
                    String n = req.getParameter("nivel");
                    if (!Taller.NIVELES.contains(n)) n = Taller.NIVELES.get(0);
                    boolean dict = "dictado".equals(req.getParameter("v")) && !t.getDictado().get(n).isEmpty();
                    req.setAttribute("t", t);
                    req.setAttribute("nivel", n);
                    req.setAttribute("datos", datosFlyer(t, n, dict));
                    vista(req, resp, "talleres/flyer-nivel");
                }
                case "/lo-dictado" -> {
                    Taller t = pedido(req, resp); if (t == null) return;
                    // se parte de lo ya registrado o, si no hay, del plan
                    Map<String, String> texto = new HashMap<>();
                    for (String n : Taller.NIVELES)
                        texto.put(n, TallerSeccion.escribir(t.getDictado().get(n).isEmpty() ? t.getPlan().get(n) : t.getDictado().get(n)));
                    req.setAttribute("t", t);
                    req.setAttribute("textoNivel", texto);
                    req.setAttribute("catalogo", catalogo());
                    vista(req, resp, "talleres/lo-dictado");
                }
                case "/dictado" -> {
                    List<Taller> talleres = new ArrayList<>(dao.listar());
                    talleres.removeIf(x -> "CANCELADO".equals(x.getEstado()));
                    String ciclo = texto(req, "ciclo");
                    Set<String> ciclos = new LinkedHashSet<>();
                    for (Taller x : talleres) ciclos.add(x.getCiclo());
                    if (ciclo == null && !ciclos.isEmpty()) ciclo = ciclos.iterator().next();
                    final String c = ciclo;
                    talleres.removeIf(x -> !x.getCiclo().equals(c));
                    Collections.reverse(talleres);
                    Map<String, Integer> ids = new HashMap<>();
                    req.setAttribute("matriz", dao.dictado(talleres, ids));
                    req.setAttribute("talleres", talleres);
                    req.setAttribute("ciclos", ciclos);
                    req.setAttribute("ciclo", ciclo);
                    vista(req, resp, "talleres/dictado");
                }
                case "/pasos" -> {
                    Map<String, List<TallerPaso>> porNivel = new LinkedHashMap<>();
                    for (String n : Taller.NIVELES) porNivel.put(n, new ArrayList<>());
                    for (TallerPaso p : dao.catalogo(null, false)) porNivel.get(p.getNivel()).add(p);
                    req.setAttribute("porNivel", porNivel);
                    vista(req, resp, "talleres/pasos");
                }
                default -> resp.sendError(404);
            }
        } catch (SQLException e) { fallo(req, resp, e); }
    }

    /** Ciclo PUCP de una fecha: enero-julio = -1, agosto-diciembre = -2. */
    static String ciclo(LocalDate d) { return d.getYear() + "-" + (d.getMonthValue() <= 7 ? 1 : 2); }

    private Taller pedido(HttpServletRequest req, HttpServletResponse resp) throws SQLException, IOException {
        Integer id = entero(req, "id");
        Taller t = id == null ? null : dao.obtener(id);
        if (t == null) { Sesion.error(req, "No encontré ese taller."); ir(req, resp, "/talleres"); }
        return t;
    }

    private void datosFormulario(HttpServletRequest req, Taller t) throws SQLException {
        req.setAttribute("t", t);
        List<Miembro> activos = miembros.listarActivos();
        // primero los del team Talleres
        activos.sort(Comparator.comparing((Miembro m) -> !m.getTeams().contains("Talleres")).thenComparing(Miembro::getNombre));
        req.setAttribute("activos", activos);
        req.setAttribute("catalogo", catalogo());
        req.setAttribute("textoNivel", t.getPlanTexto());
    }

    private Map<String, List<TallerPaso>> catalogo() throws SQLException {
        Map<String, List<TallerPaso>> catalogo = new LinkedHashMap<>();
        for (String n : Taller.NIVELES) catalogo.put(n, dao.catalogo(n, true));
        return catalogo;
    }

    /** Lo que dibuja la imagen del nivel (en el formato que se publica): JSON simple. */
    private String datosFlyer(Taller t, String n, boolean dictado) throws SQLException {
        StringBuilder sb = new StringBuilder("{");
        List<String> tal = new ArrayList<>(), apo = new ArrayList<>();
        for (TallerPersona p : t.personasDe("TALLERISTA", n)) if (p.isEfectivo() || !dictado) tal.add(p.getNombre().split(" ")[0]);
        for (TallerPersona p : t.personasDe("APOYO", n)) if (p.isEfectivo() || !dictado) apo.add(p.getNombre().split(" ")[0]);
        sb.append("\"nivel\":").append(json(Taller.nombreNivel(n)));
        sb.append(",\"columna\":").append(json(n.endsWith("RUEDA") || n.startsWith("RUEDA") ? "Rueda" : "Pasos"));
        sb.append(",\"talleristas\":").append(json(unir(tal)));
        sb.append(",\"apoyos\":").append(json(unir(apo)));
        sb.append(",\"fecha\":").append(json(t.getFechaTexto() + " · " + t.getHorario()));
        sb.append(",\"lema\":").append(json(new pe.sdp.daos.ConfigDao().valor("flyer.lema", "¡11 años repartiendo cubanía!")));
        sb.append(",\"secciones\":[");
        boolean primera = true;
        for (TallerSeccion sec : (dictado ? t.getDictado() : t.getPlan()).get(n)) {
            sb.append(primera ? "" : ",").append("{\"hora\":").append(json(sec.getHora())).append(",\"estilo\":").append(json(sec.getEstilo())).append(",\"items\":[");
            boolean pi = true;
            for (String[] it : sec.getItems()) { sb.append(pi ? "" : ",").append("[").append(json(it[0])).append(",").append(json(it[1])).append("]"); pi = false; }
            if (sec.getItems().isEmpty()) sb.append("[\"\",null]");
            sb.append("]}"); primera = false;
        }
        return sb.append("]}").toString();
    }

    private static String unir(List<String> l) {
        if (l.isEmpty()) return "";
        if (l.size() == 1) return l.get(0);
        return String.join(", ", l.subList(0, l.size() - 1)) + " y " + l.get(l.size() - 1);
    }

    /** Texto JSON seguro para meterlo dentro de un script (sin </script>). */
    private static String json(String v) {
        if (v == null) return "null";
        StringBuilder sb = new StringBuilder("\"");
        for (char ch : v.toCharArray()) {
            switch (ch) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '<' -> sb.append("\\u003c");
                case '>' -> sb.append("\\u003e");
                case '&' -> sb.append("\\u0026");
                default -> { if (ch < 0x20) sb.append(String.format("\\u%04x", (int) ch)); else sb.append(ch); }
            }
        }
        return sb.append('"').toString();
    }

    // ===================== FORMULARIO POST-TALLER =====================

    private void formulario(HttpServletRequest req, HttpServletResponse resp, int yo) throws SQLException, ServletException, IOException {
        Integer id = entero(req, "id");
        Taller t = id == null ? null : dao.obtener(id);
        List<TallerPersona> mias = t == null ? List.of() : dao.misFunciones(id, yo);
        if (t == null || mias.isEmpty()) { Sesion.error(req, "Ese formulario es para los talleristas y apoyos que estuvieron en el taller."); ir(req, resp, "/inicio"); return; }
        // una tarjeta por función y nivel (si alguien quedó dos veces en el dictado, no se repite)
        Map<String, TallerPersona> unicas = new LinkedHashMap<>();
        for (TallerPersona m : mias) unicas.putIfAbsent(m.getFuncion() + "|" + m.getNivel(), m);
        List<String[]> dest = destinos(t, yo);
        Set<String> conCompanero = new HashSet<>();     // niveles (texto) donde tuve compañero(a) de dictado
        for (String[] d : dest) if ("COMPANERO".equals(d[0])) conCompanero.add(d[3]);
        boolean soyTallerista = false, soyApoyo = false;
        for (TallerPersona m : unicas.values()) { soyTallerista |= "TALLERISTA".equals(m.getFuncion()); soyApoyo |= "APOYO".equals(m.getFuncion()); }
        req.setAttribute("t", t);
        req.setAttribute("mias", new ArrayList<>(unicas.values()));
        req.setAttribute("destinos", dest);
        req.setAttribute("conCompanero", conCompanero);
        req.setAttribute("soyTallerista", soyTallerista);
        req.setAttribute("soyApoyo", soyApoyo);
        Map<String, String> previo = new HashMap<>();   // "tipo_para" -> texto
        for (TallerFeedback f : dao.feedbackEscritoPor(id, yo)) previo.put(f.getTipo() + "_" + (f.getParaMiembro() == null ? "" : f.getParaMiembro()), f.getTexto());
        req.setAttribute("previo", previo);
        req.setAttribute("yaRespondio", dao.respondio(id, yo));
        vista(req, resp, "talleres/formulario");
    }

    /** A quién le doy feedback: tallerista → su(s) compañero(s) de nivel; apoyo → los talleristas de su nivel. [tipo, idPara, nombre, nivel] */
    private static List<String[]> destinos(Taller t, int yo) {
        List<String[]> l = new ArrayList<>();
        Set<String> vistos = new HashSet<>();
        for (TallerPersona mia : t.getPersonas()) {
            if (mia.getIdMiembro() != yo || !mia.isEfectivo()) continue;
            String tipo = "TALLERISTA".equals(mia.getFuncion()) ? "COMPANERO" : ("APOYO".equals(mia.getFuncion()) ? "APOYO" : null);
            if (tipo == null) continue;
            for (TallerPersona o : t.getPersonas()) {
                if (o.getIdMiembro() == yo || !"TALLERISTA".equals(o.getFuncion()) || !o.isEfectivo() || !Objects.equals(o.getNivel(), mia.getNivel())) continue;
                if (vistos.add(tipo + o.getIdMiembro())) l.add(new String[]{tipo, String.valueOf(o.getIdMiembro()), o.getNombre(), o.getNivelTexto()});
            }
        }
        return l;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int yo = Sesion.idUsuario(req);
        try {
            String r = ruta(req);
            if ("/formulario".equals(r)) { postFormulario(req, resp, yo); return; }
            if (!Sesion.requiere(req, resp, PERM)) return;
            switch (r) {
                case "/nuevo", "/editar" -> postCronograma(req, resp, yo);
                case "/lo-dictado" -> {
                    Taller t = pedido(req, resp); if (t == null) return;
                    Map<String, List<TallerSeccion>> d = new LinkedHashMap<>();
                    for (String n : Taller.NIVELES) d.put(n, TallerSeccion.leer(req.getParameter("crono_" + n)));
                    dao.guardarDictado(t.getIdTaller(), d);
                    Sesion.ok(req, "Guardado lo que se dictó. El plan se mantiene para comparar.");
                    ir(req, resp, "/talleres/detalle?id=" + t.getIdTaller() + "#cronograma");
                }
                case "/flyer-lema" -> {
                    String lema = texto(req, "lema");
                    if (lema != null) new pe.sdp.daos.ConfigDao().guardar("flyer.lema", lema.length() > 80 ? lema.substring(0, 80) : lema);
                    Sesion.ok(req, "Frase del pie guardada.");
                    ir(req, resp, "/talleres/flyer-nivel?id=" + entero(req, "id") + "&nivel=" + Optional.ofNullable(texto(req, "nivel")).orElse(""));
                }
                case "/detalle" -> {
                    Taller t = pedido(req, resp); if (t == null) return;
                    int id = t.getIdTaller();
                    String volver = "/talleres/detalle?id=" + id;
                    switch (accion(req, "")) {
                        case "final" -> {
                            Map<Integer, String> asis = new HashMap<>();
                            for (TallerPersona p : t.getPersonas()) {
                                String v = req.getParameter("asis_" + p.getId());
                                if (List.of("A_TIEMPO", "TARDE", "NO_SE_PRESENTO").contains(v)) asis.put(p.getId(), v);
                            }
                            List<TallerPersona> extras = new ArrayList<>();
                            // pueden venir varias filas (una por persona que se sumó o reemplazó)
                            String[] quienes = req.getParameterValues("extraMiembro"), fs = req.getParameterValues("extraFuncion"),
                                     ns = req.getParameterValues("extraNivel"), rs = req.getParameterValues("extraReemplaza"), as = req.getParameterValues("extraAsistencia");
                            Set<String> vistos = new HashSet<>();
                            // nadie puede estar dos veces con la misma función en un taller (ej. tallerista en el plan y otra vez como adicional)
                            for (TallerPersona p : t.getPersonas()) vistos.add(p.getIdMiembro() + "|" + p.getFuncion());
                            List<String> repetidos = new ArrayList<>();
                            for (int i = 0; quienes != null && i < quienes.length; i++) {
                                Integer quien;
                                try { quien = quienes[i] == null || quienes[i].isBlank() ? null : Integer.valueOf(quienes[i].trim()); } catch (NumberFormatException ex) { quien = null; }
                                if (quien == null) continue;
                                String f = val(fs, i), n = val(ns, i);
                                if (!List.of("TALLERISTA", "APOYO", "MAESTRO", "MUSICA").contains(f)) f = "APOYO";
                                if (!Taller.NIVELES.contains(n) || "MAESTRO".equals(f) || "MUSICA".equals(f)) n = null;
                                if (!vistos.add(quien + "|" + f)) { repetidos.add(nombreDe(t, quien)); continue; }
                                TallerPersona x = new TallerPersona(quien, f, n);
                                try { String re = val(rs, i); x.setReemplazaA(re == null || re.isBlank() ? null : Integer.valueOf(re.trim())); } catch (NumberFormatException ex) { x.setReemplazaA(null); }
                                x.setAsistencia("TARDE".equals(val(as, i)) ? "TARDE" : "A_TIEMPO");
                                extras.add(x);
                            }
                            dao.registrarFinal(id, asis, extras, yo);
                            Sesion.ok(req, extras.isEmpty() ? "Dictado final guardado." : "Dictado final guardado. Se agregó a " + extras.size() + (extras.size() == 1 ? " persona." : " personas."));
                            if (!repetidos.isEmpty())
                                Sesion.error(req, "No se agregó otra vez a " + String.join(", ", new LinkedHashSet<>(repetidos)) + ": ya está en este taller con esa función.");
                        }
                        case "quitarExtra" -> { dao.quitarExtra(id, entero(req, "idPersona")); Sesion.ok(req, "Se quitó del dictado final."); }
                        case "cancelar" -> { dao.cambiarEstado(id, "CANCELADO"); Sesion.ok(req, "Taller cancelado."); }
                        case "reactivar" -> { dao.cambiarEstado(id, t.isFinalRegistrado() ? "REALIZADO" : "PROGRAMADO"); Sesion.ok(req, "Taller reactivado."); }
                        case "eliminar" -> {
                            if (!Sesion.tiene(req, "ASIGNAR_RESPONSABILIDADES_TALLERES")) { Sesion.error(req, "Solo el admin o moderador de talleres puede eliminar talleres."); break; }
                            dao.eliminar(id); Sesion.ok(req, t.getTitulo() + " del " + t.getFechaCorta() + " eliminado."); ir(req, resp, "/talleres"); return;
                        }
                        default -> { }
                    }
                    ir(req, resp, volver);
                }
                case "/feedback" -> {
                    Taller t = pedido(req, resp); if (t == null) return;
                    Integer para = entero(req, "para");
                    String texto = texto(req, "resumen");
                    boolean enviar = "enviar".equals(accion(req, ""));
                    if (para == null || texto == null) Sesion.error(req, "Escribe el resumen antes de guardarlo.");
                    else {
                        if (texto.length() > 2000) texto = texto.substring(0, 2000);
                        dao.guardarResumen(t.getIdTaller(), para, texto, yo, enviar);
                        Sesion.ok(req, enviar ? "Resumen enviado: la persona ya lo ve en “Mi feedback de talleres”. Abajo está el mensaje para WhatsApp." : "Resumen guardado (todavía no se envía).");
                    }
                    ir(req, resp, "/talleres/feedback?id=" + t.getIdTaller() + (para == null ? "" : "#p" + para));
                }
                case "/pasos" -> {
                    String nivel = req.getParameter("nivel");
                    switch (accion(req, "")) {
                        case "agregar" -> {
                            String nom = texto(req, "nombre");
                            if (nom == null || !Taller.NIVELES.contains(nivel)) Sesion.error(req, "Escribe el paso y elige el nivel.");
                            else { dao.agregarPaso(nom, nivel); Sesion.ok(req, "Paso agregado a " + Taller.nombreNivel(nivel) + "."); }
                        }
                        case "activar", "desactivar" -> { dao.activarPaso(entero(req, "idPaso"), "activar".equals(accion(req, ""))); Sesion.ok(req, "Listo."); }
                        case "renombrar" -> {
                            String nom = texto(req, "nombre");
                            if (nom == null || !dao.renombrarPaso(entero(req, "idPaso"), nom)) Sesion.error(req, "No se pudo renombrar: ya hay un paso con ese nombre en el nivel.");
                            else Sesion.ok(req, "Paso renombrado.");
                        }
                        default -> { }
                    }
                    ir(req, resp, "/talleres/pasos" + (Taller.NIVELES.contains(nivel) ? "#" + nivel : ""));
                }
                default -> resp.sendError(404);
            }
        } catch (SQLException e) { fallo(req, resp, e); }
    }

    private void postCronograma(HttpServletRequest req, HttpServletResponse resp, int yo) throws SQLException, IOException, ServletException {
        Taller t = new Taller();
        Integer id = entero(req, "idTaller");
        t.setIdTaller(id == null ? 0 : id);
        t.setFecha(fecha(req, "fecha"));
        LocalTime hi = hora(req, "horaInicio"), hf = hora(req, "horaFin");
        t.setHoraInicio(hi == null ? LocalTime.of(19, 30) : hi); t.setHoraFin(hf == null ? LocalTime.of(21, 5) : hf);
        t.setLugar(texto(req, "lugar"));
        t.setNumero(entero(req, "numero"));
        t.setCiclo(texto(req, "ciclo"));
        t.setMusicaTexto(texto(req, "musicaTexto"));
        t.setNotas(texto(req, "notas"));
        String estado = req.getParameter("estado");
        t.setEstado(List.of("BORRADOR", "PROGRAMADO", "REALIZADO", "CANCELADO").contains(estado) ? estado : "PROGRAMADO");
        if ("borrador".equals(req.getParameter("publicar"))) t.setEstado("BORRADOR");
        else if ("BORRADOR".equals(t.getEstado())) t.setEstado("PROGRAMADO");
        List<TallerPersona> plan = new ArrayList<>();
        for (String n : Taller.NIVELES) {
            // cronograma por secciones (el editor lo manda como texto)
            t.getPlan().get(n).addAll(TallerSeccion.leer(req.getParameter("crono_" + n)));
            for (String v : valores(req, "tal_" + n)) plan.add(new TallerPersona(v.isEmpty() ? 0 : Integer.parseInt(v), "TALLERISTA", n));
            for (String v : valores(req, "apo_" + n)) plan.add(new TallerPersona(Integer.parseInt(v), "APOYO", n));
        }
        for (String v : valores(req, "maestro")) plan.add(new TallerPersona(Integer.parseInt(v), "MAESTRO", null));
        for (String v : valores(req, "musica")) plan.add(new TallerPersona(Integer.parseInt(v), "MUSICA", null));
        plan.removeIf(p -> p.getIdMiembro() <= 0);

        String error = null;
        if (t.getFecha() == null) error = "Pon la fecha del taller.";
        else if (!t.getHoraFin().isAfter(t.getHoraInicio())) error = "La hora de fin debe ser después de la de inicio.";
        else if (dao.existeFecha(t.getFecha(), t.getIdTaller())) error = "Ya hay un taller registrado ese día.";
        if (t.getCiclo() == null || !t.getCiclo().matches("\\d{4}-[12]")) t.setCiclo(t.getFecha() == null ? null : ciclo(t.getFecha()));
        if (error != null) {
            Sesion.error(req, error);
            // se vuelve a mostrar con lo escrito
            for (TallerPersona p : plan) t.getPersonas().add(p);
            datosFormulario(req, t);
            vista(req, resp, "talleres/form");
            return;
        }
        int nuevo = dao.guardar(t, plan, yo);
        Sesion.ok(req, "BORRADOR".equals(t.getEstado()) ? "Borrador guardado." : "Cronograma guardado. Ya puedes compartir el flyer.");
        ir(req, resp, "/talleres/detalle?id=" + nuevo);
    }

    private static List<String> valores(HttpServletRequest req, String nombre) {
        String[] v = req.getParameterValues(nombre);
        List<String> l = new ArrayList<>();
        if (v != null) for (String x : v) if (x != null && x.matches("\\d+")) l.add(x);
        return l;
    }

    private static String corta(String s, int max) {
        s = Formato.vacio(s);
        return s == null ? null : (s.length() > max ? s.substring(0, max) : s);
    }

    private void postFormulario(HttpServletRequest req, HttpServletResponse resp, int yo) throws SQLException, IOException {
        Integer id = entero(req, "id");
        Taller t = id == null ? null : dao.obtener(id);
        List<TallerPersona> mias = t == null ? List.of() : dao.misFunciones(id, yo);
        if (t == null || mias.isEmpty()) { Sesion.error(req, "Ese formulario es para los talleristas y apoyos que estuvieron en el taller."); ir(req, resp, "/inicio"); return; }
        // checklist: solo de los niveles donde dicté
        Map<String, Map<Integer, Boolean>> checklist = new HashMap<>();
        Map<String, List<String>> extras = new HashMap<>();
        for (TallerPersona m : mias) {
            if (!"TALLERISTA".equals(m.getFuncion()) || m.getNivel() == null) continue;
            String n = m.getNivel();
            Map<Integer, Boolean> marcas = new HashMap<>();
            for (TallerPaso p : t.getPasos().get(n)) {
                if (!p.isPlanificado()) continue;
                String v = req.getParameter("chk_" + n + "_" + p.getIdPaso());
                if ("1".equals(v) || "0".equals(v)) marcas.put(p.getIdPaso(), "1".equals(v));
            }
            List<String> ex = new ArrayList<>();
            String txt = req.getParameter("extra_" + n);
            if (txt != null) for (String l : txt.split("\\r?\\n")) { String x = corta(l.replaceFirst("^[•·\\-*]\\s*", ""), 80); if (x != null && !x.equalsIgnoreCase("ninguno")) ex.add(x); }
            if (!marcas.isEmpty() || !ex.isEmpty()) { checklist.put(n, marcas); extras.put(n, ex); }
        }
        // feedback: solo a quien me corresponde
        List<TallerFeedback> fb = new ArrayList<>();
        for (String[] d : destinos(t, yo)) {
            String txt = corta(req.getParameter("fb_" + d[0] + "_" + d[1]), 2000);
            if (txt == null) continue;
            TallerFeedback f = new TallerFeedback();
            f.setTipo(d[0]); f.setParaMiembro(Integer.parseInt(d[1])); f.setTexto(txt);
            fb.add(f);
        }
        String sug = corta(req.getParameter("sugerencia"), 2000);
        if (fb.isEmpty() && sug == null && checklist.isEmpty()) { Sesion.error(req, "El formulario está vacío."); ir(req, resp, "/talleres/formulario?id=" + id); return; }
        dao.guardarFormulario(id, yo, checklist, extras, fb, sug);
        Sesion.ok(req, "¡Gracias! Tu formulario del " + t.getTitulo() + " quedó guardado." + (t.isPlazoVencido() ? " (Llegó fuera de plazo.)" : ""));
        ir(req, resp, "/inicio");
    }

    private static String val(String[] a, int i) { return a != null && i < a.length ? a[i] : null; }

    private static String nombreDe(Taller t, int idMiembro) {
        return t.getPersonas().stream().filter(p -> p.getIdMiembro() == idMiembro).map(TallerPersona::getNombre).findFirst().orElse("esa persona");
    }
}
