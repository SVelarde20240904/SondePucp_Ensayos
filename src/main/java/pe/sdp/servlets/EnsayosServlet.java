package pe.sdp.servlets;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pe.sdp.beans.*;
import pe.sdp.daos.EnsayoDao;
import pe.sdp.daos.EvaluacionDao;
import pe.sdp.daos.FiguraDao;
import pe.sdp.daos.MiembroDao;
import pe.sdp.util.Formato;
import pe.sdp.util.Sesion;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

/**
 * Sección ENSAYOS:
 *  /ensayos                 historial del ciclo
 *  /ensayos/nuevo, /editar  formulario (GET) y guardar (POST)
 *  /ensayos/detalle         detalle + notas + cerrar/cancelar
 *  /ensayos/reporte         reporte imprimible A4
 *  /ensayos/cronograma      cronograma sugerido
 *  /ensayos/asistencia      tomar asistencia real
 *  /ensayos/figuras         figuras enseñadas (habilita la autoevaluación del ensayo)
 *  /ensayos/miembros        fichas de miembros (lista) · /ensayos/miembro?id= ficha completa
 */
@WebServlet(urlPatterns = {"/ensayos", "/ensayos/*"})
public class EnsayosServlet extends BaseServlet {

    private final EnsayoDao dao = new EnsayoDao();
    private final MiembroDao miembros = new MiembroDao();
    private final FiguraDao figuras = new FiguraDao();
    private final EvaluacionDao evaluaciones = new EvaluacionDao();

    private String ruta(HttpServletRequest req) { return req.getPathInfo() == null ? "/" : req.getPathInfo(); }

    /** Ensayo pedido (?id=) o el más cercano. */
    private Ensayo ensayoPedido(HttpServletRequest req) throws SQLException {
        Integer id = entero(req, "id");
        return id != null ? dao.obtener(id) : dao.actualOProximo();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            switch (ruta(req)) {
                case "/" -> {
                    if (!Sesion.requiere(req, resp, "GESTIONAR_ENSAYOS")) return;
                    String tipo = texto(req, "tipo");
                    List<Ensayo> lista = dao.listar(tipo);
                    LocalDate hoy = LocalDate.now();
                    List<Ensayo> grafico = new ArrayList<>();
                    for (Ensayo e : dao.listar(null)) if (e.isPasado() && !"CANCELADO".equals(e.getEstado()) && grafico.size() < 12) grafico.add(0, e);
                    int max = grafico.stream().mapToInt(Ensayo::getAsistieron).max().orElse(1);
                    req.setAttribute("ensayos", lista);
                    req.setAttribute("grafico", grafico);
                    req.setAttribute("maxGrafico", Math.max(max, 1));
                    req.setAttribute("tipo", tipo);
                    req.setAttribute("ciclo", dao.resumenCiclo(EnsayoDao.inicioCiclo(hoy)));
                    req.setAttribute("proximo", dao.proximos(Sesion.idUsuario(req), 1).stream().findFirst().orElse(null));
                    vista(req, resp, "ensayos/historial");
                }
                case "/nuevo", "/editar" -> {
                    if (!Sesion.requiere(req, resp, "GESTIONAR_ENSAYOS")) return;
                    Ensayo e;
                    if ("/editar".equals(ruta(req)) && entero(req, "id") != null) e = dao.obtener(entero(req, "id"));
                    else {
                        e = new Ensayo();
                        LocalDate sab = Formato.sabadoDe(LocalDate.now().plusDays(1));
                        e.setTipo("GENERAL"); e.setFecha(sab); e.setHoraInicio(LocalTime.of(14, 0)); e.setHoraFin(LocalTime.of(16, 0));
                        e.setLugar("Explanada del polideportivo"); e.setCierreConfirmacion(EnsayoDao.cierrePorDefecto(sab));
                        e.setIndicaciones(List.of("No olvidar llegar temprano", "No olvidar traer su agüita"));
                        e.setDictantes(List.of(Sesion.idUsuario(req)));
                    }
                    req.setAttribute("ensayo", e);
                    req.setAttribute("equipo", miembros.equipoEnsayos());
                    vista(req, resp, "ensayos/form");
                }
                case "/detalle", "/reporte" -> {
                    if (!Sesion.requiere(req, resp, "/reporte".equals(ruta(req)) ? "IMPRIMIR_REPORTES" : "GESTIONAR_ENSAYOS")) return;
                    Ensayo e = ensayoPedido(req);
                    if (e == null) { Sesion.error(req, "No hay ensayos registrados todavía."); ir(req, resp, "/ensayos"); return; }
                    cargarDetalle(req, e);
                    vista(req, resp, "/reporte".equals(ruta(req)) ? "ensayos/reporte" : "ensayos/detalle");
                }
                case "/cronograma" -> {
                    if (!Sesion.requiere(req, resp, "VER_CRONOGRAMA")) return;
                    Ensayo e = ensayoPedido(req);
                    if (e == null) { Sesion.error(req, "Primero crea un ensayo."); ir(req, resp, "/ensayos/nuevo"); return; }
                    cargarCronograma(req, e);
                    vista(req, resp, "ensayos/cronograma");
                }
                case "/asistencia" -> {
                    if (!Sesion.requiere(req, resp, "TOMAR_ASISTENCIA")) return;
                    Ensayo e = ensayoPedido(req);
                    if (e == null) { Sesion.error(req, "Primero crea un ensayo."); ir(req, resp, "/ensayos/nuevo"); return; }
                    req.setAttribute("ensayo", e);
                    req.setAttribute("filas", dao.asistenciaDe(e.getIdEnsayo()));
                    vista(req, resp, "ensayos/asistencia");
                }
                case "/figuras" -> {
                    if (!Sesion.requiere(req, resp, "REGISTRAR_FIGURAS_ENSENADAS")) return;
                    Ensayo e = ensayoPedido(req);
                    if (e == null) { Sesion.error(req, "Primero crea un ensayo."); ir(req, resp, "/ensayos/nuevo"); return; }
                    cargarCronograma(req, e);
                    Set<Integer> marcadas = new HashSet<>();
                    for (FiguraSugerida f : dao.figurasEnsenadas(e.getIdEnsayo())) marcadas.add(f.getIdFigura());
                    req.setAttribute("marcadas", marcadas);
                    req.setAttribute("porNivel", figuras.activasPorNivel());
                    req.setAttribute("equipo", miembros.equipoEnsayos());
                    vista(req, resp, "ensayos/figuras-ensenadas");
                }
                case "/miembros" -> {
                    if (!Sesion.requiere(req, resp, "VER_CRONOGRAMA")) return;
                    String filtro = texto(req, "filtro"), nivel = texto(req, "nivel");
                    List<Miembro> lista = new ArrayList<>();
                    for (Miembro m : miembros.listar(filtro, texto(req, "q"))) {
                        if (nivel == null || ("SIN".equals(nivel) ? m.getNivel() == null : nivel.equals(m.getNivel()))) lista.add(m);
                    }
                    req.setAttribute("lista", lista);
                    vista(req, resp, "ensayos/miembros");
                }
                case "/miembro" -> {
                    if (!Sesion.requiere(req, resp, "VER_CRONOGRAMA")) return;
                    Integer id = entero(req, "id");
                    Miembro m = id == null ? null : miembros.obtener(id);
                    if (m == null) { Sesion.error(req, "No encontré a ese miembro."); ir(req, resp, "/ensayos/miembros"); return; }
                    // Figuras agrupadas por el nivel de la figura (AZUL = base → ROJO = avanzada)
                    Map<String, List<Evaluacion>> porNivel = new LinkedHashMap<>();
                    for (String n : List.of("AZUL", "VERDE", "AMARILLO", "ROJO", "SIN")) porNivel.put(n, new ArrayList<>());
                    Map<String, int[]> sabe = new LinkedHashMap<>();   // nivel → [sabe, total]
                    boolean soloFaltan = "1".equals(req.getParameter("faltan"));
                    for (Evaluacion ev : evaluaciones.deMiembro(id)) {
                        String n = ev.getColorFigura() == null ? "SIN" : ev.getColorFigura();
                        int[] c = sabe.computeIfAbsent(n, k -> new int[2]);
                        c[1]++;
                        if ("VERDE".equals(ev.getColor()) || "AZUL".equals(ev.getColor())) c[0]++;
                        if (!soloFaltan || !("VERDE".equals(ev.getColor()) || "AZUL".equals(ev.getColor()))) porNivel.get(n).add(ev);
                    }
                    porNivel.values().removeIf(List::isEmpty);
                    req.setAttribute("m", m);
                    req.setAttribute("porNivel", porNivel);
                    req.setAttribute("sabe", sabe);
                    req.setAttribute("soloFaltan", soloFaltan);
                    req.setAttribute("resumen", evaluaciones.resumenMiembro(id));
                    req.setAttribute("porcentaje", dao.porcentajeAsistencia(id));
                    req.setAttribute("asistencias", dao.misAsistencias(id, 10));
                    List<HistorialEvaluacion> h = evaluaciones.historialDe(id, null);
                    req.setAttribute("historial", h.size() > 15 ? h.subList(0, 15) : h);
                    vista(req, resp, "ensayos/miembro");
                }
                default -> resp.sendError(404);
            }
        } catch (SQLException e) { fallo(req, resp, e); }
    }

    private void cargarCronograma(HttpServletRequest req, Ensayo e) throws SQLException {
        List<String> equipo = new ArrayList<>(), sinNivel = new ArrayList<>();
        List<NivelCronograma> niveles = dao.resumenPorNivel(e.getIdEnsayo(), equipo, sinNivel);
        Set<String> abiertos = new LinkedHashSet<>();
        for (NivelCronograma n : niveles) if (n.isSeAbre()) abiertos.add(n.getNivel());
        req.setAttribute("ensayo", e);
        req.setAttribute("niveles", niveles);
        req.setAttribute("abiertos", abiertos);
        req.setAttribute("equipoAsiste", equipo);
        req.setAttribute("sinNivel", sinNivel);
        req.setAttribute("sugeridas", dao.figurasSugeridas(e.getIdEnsayo(), abiertos));
    }

    private void cargarDetalle(HttpServletRequest req, Ensayo e) throws SQLException {
        req.setAttribute("ensayo", e);
        req.setAttribute("filas", dao.asistenciaDe(e.getIdEnsayo()));
        List<FiguraSugerida> ens = dao.figurasEnsenadas(e.getIdEnsayo());
        Map<String, List<FiguraSugerida>> porNivel = new LinkedHashMap<>();
        for (FiguraSugerida f : ens) porNivel.computeIfAbsent(f.getNivel() == null ? "SIN" : f.getNivel(), k -> new ArrayList<>()).add(f);
        req.setAttribute("ensenadasPorNivel", porNivel);
        req.setAttribute("avances", dao.avances(e.getIdEnsayo()));
        List<LocalDate> fechas = new ArrayList<>();
        req.setAttribute("historialAsis", dao.historialAsistencia(e.getIdEnsayo(), fechas));
        List<String> fs = new ArrayList<>();
        for (LocalDate d : fechas) fs.add(Formato.ddmm(d));
        req.setAttribute("fechasHist", fs);
        req.setAttribute("hoy", Formato.ddmmyyyy(LocalDate.now()));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int yo = Sesion.idUsuario(req);
        try {
            switch (ruta(req)) {
                case "/nuevo", "/editar" -> {
                    if (!Sesion.requiere(req, resp, "GESTIONAR_ENSAYOS")) return;
                    Ensayo e = new Ensayo();
                    Integer id = entero(req, "idEnsayo");
                    e.setIdEnsayo(id == null ? 0 : id);
                    String tipo = texto(req, "tipo");
                    e.setTipo(List.of("GENERAL", "COMPETENCIA", "NIVELACION", "EXHIBICION").contains(tipo) ? tipo : "GENERAL");
                    e.setFecha(fecha(req, "fecha"));
                    e.setHoraInicio(hora(req, "horaInicio"));
                    e.setHoraFin(hora(req, "horaFin"));
                    e.setLugar(texto(req, "lugar") == null ? "Explanada del polideportivo" : texto(req, "lugar"));
                    e.setCierreConfirmacion(fechaHora(req, "cierreConfirmacion"));
                    e.setEstado("borrador".equals(req.getParameter("publicar")) ? "BORRADOR" : "PROGRAMADO");
                    if (e.getFecha() == null || e.getHoraInicio() == null || e.getHoraFin() == null || !e.getHoraFin().isAfter(e.getHoraInicio())) {
                        Sesion.error(req, "Revisa la fecha y el horario (la hora de fin debe ser después de la de inicio).");
                        ir(req, resp, id == null ? "/ensayos/nuevo" : "/ensayos/editar?id=" + id);
                        return;
                    }
                    List<String> ind = req.getParameterValues("indicacion") == null ? List.of() : Arrays.asList(req.getParameterValues("indicacion"));
                    List<Integer> dict = new ArrayList<>();
                    if (req.getParameterValues("dictan") != null) for (String d : req.getParameterValues("dictan")) { Integer x = Formato.entero(d); if (x != null) dict.add(x); }
                    int nuevoId = dao.guardar(e, ind, dict, yo);
                    Sesion.ok(req, "BORRADOR".equals(e.getEstado()) ? "Borrador guardado." : "Ensayo publicado: todos ya pueden confirmar su asistencia.");
                    ir(req, resp, "/ensayos/detalle?id=" + nuevoId);
                }
                case "/detalle" -> {
                    if (!Sesion.requiere(req, resp, "GESTIONAR_ENSAYOS")) return;
                    int id = entero(req, "id");
                    if ("eliminar".equals(accion(req, "notas"))) {
                        if (!Sesion.usuario(req).isAdmin()) { Sesion.error(req, "Solo un admin puede eliminar ensayos."); ir(req, resp, "/ensayos/detalle?id=" + id); return; }
                        Ensayo e = dao.obtener(id);
                        int regresados = dao.eliminar(id);
                        Sesion.ok(req, "Ensayo del " + (e == null ? "" : e.getFechaCorta()) + " eliminado" + (regresados == 1 ? ". Se regresó 1 color que había subido su autoevaluación." : regresados > 1 ? ". Se regresaron " + regresados + " colores que había subido su autoevaluación." : "."));
                        ir(req, resp, "/ensayos");
                        return;
                    }
                    switch (accion(req, "notas")) {
                        case "cerrar" -> { dao.cambiarEstado(id, "CERRADO"); Sesion.ok(req, "Ensayo cerrado. Si ya registraste asistencia y figuras enseñadas, los asistentes ya ven el aviso para autoevaluarse."); }
                        case "cancelar" -> { dao.cambiarEstado(id, "CANCELADO"); Sesion.ok(req, "Ensayo cancelado."); }
                        default -> { dao.guardarNotas(id, texto(req, "notas"), yo); Sesion.ok(req, "Notas guardadas."); }
                    }
                    ir(req, resp, "/ensayos/detalle?id=" + id);
                }
                case "/asistencia" -> {
                    if (!Sesion.requiere(req, resp, "TOMAR_ASISTENCIA")) return;
                    int id = entero(req, "idEnsayo");
                    Map<Integer, String> codigos = new HashMap<>();
                    for (String p : Collections.list(req.getParameterNames())) {
                        if (!p.startsWith("asis_")) continue;
                        Integer m = Formato.entero(p.substring(5));
                        String c = req.getParameter(p);
                        if (m != null && List.of("AP", "AN", "TJ", "TI", "FJ", "FI", "").contains(c)) codigos.put(m, c);
                    }
                    dao.registrarAsistencia(id, codigos, yo);
                    Sesion.ok(req, "Asistencia guardada (" + codigos.values().stream().filter(x -> !x.isEmpty()).count() + " personas).");
                    ir(req, resp, "/ensayos/asistencia?id=" + id);
                }
                case "/figuras" -> {
                    if (!Sesion.requiere(req, resp, "REGISTRAR_FIGURAS_ENSENADAS")) return;
                    int id = entero(req, "idEnsayo");
                    List<Integer> figs = new ArrayList<>();
                    if (req.getParameterValues("figura") != null) for (String f : req.getParameterValues("figura")) { Integer x = Formato.entero(f); if (x != null) figs.add(x); }
                    dao.guardarFigurasEnsenadas(id, figs, entero(req, "dicto"));
                    Sesion.ok(req, figs.size() + " figuras registradas. Los asistentes ya pueden hacer su autoevaluación del ensayo.");
                    ir(req, resp, "/ensayos/detalle?id=" + id);
                }
                default -> resp.sendError(404);
            }
        } catch (SQLException e) { fallo(req, resp, e); }
    }
}
