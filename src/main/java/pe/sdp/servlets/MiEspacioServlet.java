package pe.sdp.servlets;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pe.sdp.beans.Ensayo;
import pe.sdp.beans.Evaluacion;
import pe.sdp.beans.FiguraSugerida;
import pe.sdp.beans.Miembro;
import pe.sdp.daos.EnsayoDao;
import pe.sdp.daos.EvaluacionDao;
import pe.sdp.daos.MiembroDao;
import pe.sdp.util.Formato;
import pe.sdp.util.Sesion;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/** Mi espacio: Inicio, Confirmar asistencia, Autoevaluaciones, Mi historial y Mi perfil. */
@WebServlet(urlPatterns = {"/inicio", "/confirmar-asistencia", "/mi-asistencia", "/autoevaluacion", "/mi-historial", "/perfil", "/mis-responsabilidades",
                           "/pendientes", "/mi-progreso"})
public class MiEspacioServlet extends BaseServlet {

    private final MiembroDao miembros = new MiembroDao();
    private final EvaluacionDao evaluaciones = new EvaluacionDao();
    private final EnsayoDao ensayos = new EnsayoDao();
    private final pe.sdp.daos.FiguraDao figuras = new pe.sdp.daos.FiguraDao();
    private final pe.sdp.daos.EncargoDao encargos = new pe.sdp.daos.EncargoDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int yo = Sesion.idUsuario(req);
        try {
            switch (req.getServletPath()) {
                case "/inicio" -> inicio(req, resp, yo);
                case "/confirmar-asistencia", "/mi-asistencia" -> miAsistencia(req, resp, yo);
                case "/pendientes" -> {
                    req.setAttribute("pendientes", Pendientes.de(yo));
                    vista(req, resp, "miembro/pendientes");
                }
                case "/mi-progreso" -> {
                    Miembro m = miembros.obtener(yo);
                    List<Evaluacion> faltan = evaluaciones.queLeFaltan(yo);
                    req.setAttribute("yo", m);
                    req.setAttribute("faltan", faltan);
                    req.setAttribute("faltanSuNivel", faltan.stream().filter(e -> Objects.equals(e.getColorFigura(), m.getNivel())).count());
                    req.setAttribute("siguienteNivel", siguienteNivel(m.getNivel()));
                    req.setAttribute("nFiguras", evaluaciones.deMiembro(yo).size());
                    Map<String, Integer> cuenta = new LinkedHashMap<>();
                    for (String col : List.of("AZUL", "VERDE", "AMARILLO", "ROJO")) cuenta.put(col, 0);
                    for (Evaluacion ev : evaluaciones.deMiembro(yo)) if (ev.getColor() != null) cuenta.merge(ev.getColor(), 1, Integer::sum);
                    req.setAttribute("cuentaColores", cuenta);
                    // mi evolución: figuras que domino después de cada ensayo y a fin de cada mes
                    List<Object[]> cambios = evaluaciones.cambiosAplicados(yo);
                    List<ManoHistorica.Corte> cortes = ManoHistorica.cortes(cambios);
                    Map<Long, String> hoy = new HashMap<>();
                    List<Integer> figs = new ArrayList<>();
                    for (Evaluacion ev : evaluaciones.deMiembro(yo)) { figs.add(ev.getIdFigura()); if (ev.getColor() != null) hoy.put(EvaluacionDao.clave(yo, ev.getIdFigura()), ev.getColor()); }
                    Map<String, Map<Long, String>> manos = ManoHistorica.manos(cambios, cortes, hoy);
                    StringBuilder js = new StringBuilder("{\"cortes\":["), vals = new StringBuilder("],\"valores\":[");
                    for (int i = 0; i < cortes.size(); i++) {
                        js.append(i == 0 ? "" : ",").append("\"").append(cortes.get(i).corta()).append("\"");
                        vals.append(i == 0 ? "" : ",").append(ManoHistorica.dominadas(manos.get(cortes.get(i).clave()), yo, figs));
                    }
                    req.setAttribute("datosEvolucion", js.append(vals).append("]}").toString());
                    req.setAttribute("nCortes", cortes.size());
                    vista(req, resp, "miembro/mi-progreso");
                }
                case "/autoevaluacion" -> autoevaluacion(req, resp, yo);
                // "Mi historial" ahora es el historial de feedback de talleres (el de colores de figuras no se usaba)
                case "/mi-historial" -> resp.sendRedirect(req.getContextPath() + "/mi-feedback");
                case "/mis-responsabilidades" -> {
                    req.setAttribute("lista", encargos.deMiembro(yo, 60));
                    vista(req, resp, "miembro/mis-responsabilidades");
                }
                case "/perfil" -> {
                    req.setAttribute("yo", miembros.obtener(yo));
                    req.setAttribute("facultades", AdminServlet.FACULTADES);
                    req.setAttribute("maxNacimiento", LocalDate.now().minusYears(14));
                    vista(req, resp, "miembro/mi-perfil");
                }
            }
        } catch (SQLException e) { fallo(req, resp, e); }
    }

    private void inicio(HttpServletRequest req, HttpServletResponse resp, int yo) throws SQLException, ServletException, IOException {
        Miembro m = miembros.obtener(yo);
        List<Ensayo> prox = ensayos.proximos(yo, 1);
        List<pe.sdp.beans.Taller> proxT = new pe.sdp.daos.TallerDao().proximosParaMi(yo, 1);
        req.setAttribute("yo", m);
        req.setAttribute("siguienteNivel", siguienteNivel(m.getNivel()));
        if (m.getNivel() != null)
            req.setAttribute("faltanSuNivel", evaluaciones.queLeFaltan(yo).stream().filter(e -> Objects.equals(e.getColorFigura(), m.getNivel())).count());
        req.setAttribute("proximo", prox.isEmpty() ? null : prox.get(0));
        req.setAttribute("proximoTaller", proxT.isEmpty() ? null : proxT.get(0));
        req.setAttribute("pendientes", Pendientes.de(yo));
        req.setAttribute("porcentaje", ensayos.porcentajeAsistencia(yo));
        req.setAttribute("cumples", miembros.cumpleanosProximos(7));
        vista(req, resp, "miembro/inicio");
    }

    /** Mi asistencia: confirmar lo que viene (ensayos y talleres) + cómo me ha ido en el ciclo. */
    private void miAsistencia(HttpServletRequest req, HttpServletResponse resp, int yo) throws SQLException, ServletException, IOException {
        pe.sdp.daos.TallerDao talleres = new pe.sdp.daos.TallerDao();
        LocalDate desde = EnsayoDao.inicioCiclo(LocalDate.now(Formato.ZONA));
        List<pe.sdp.beans.Asistencia> hist = ensayos.historialMio(yo, desde);
        if (hist.isEmpty()) { desde = LocalDate.now(Formato.ZONA).minusMonths(6); hist = ensayos.historialMio(yo, desde); }   // ciclo recién empezado: últimos 6 meses
        int asistio = 0, tarde = 0, fj = 0, fi = 0, registrados = 0, dijoSi = 0, cumplio = 0, sinResp = 0;
        for (pe.sdp.beans.Asistencia a : hist) {
            String e = a.getEstadoReal();
            if ("SIN_RESPUESTA".equals(a.getConfirmacion())) sinResp++;
            if (e == null) continue;
            registrados++;
            switch (e) { case "AP", "AN" -> asistio++; case "TJ", "TI" -> { asistio++; tarde++; } case "FJ" -> fj++; default -> fi++; }
            if ("SI".equals(a.getConfirmacion())) { dijoSi++; if (a.isAsistio()) cumplio++; }
        }
        req.setAttribute("proximos", ensayos.proximos(yo, 3));
        req.setAttribute("proximosTalleres", talleres.proximosParaMi(yo, 2));
        req.setAttribute("historial", hist);
        req.setAttribute("misTalleres", talleres.misTalleres(yo, 12));
        req.setAttribute("porcentaje", registrados == 0 ? null : Math.round(100.0 * asistio / registrados));
        req.setAttribute("nAsistio", asistio); req.setAttribute("nTarde", tarde); req.setAttribute("nFJ", fj); req.setAttribute("nFI", fi);
        req.setAttribute("nRegistrados", registrados); req.setAttribute("nSinRespuesta", sinResp);
        req.setAttribute("cumplimiento", dijoSi == 0 ? null : Math.round(100.0 * cumplio / dijoSi));
        req.setAttribute("desdeTexto", Formato.fechaLarga(desde));
        vista(req, resp, "miembro/confirmar-asistencia");
    }

    private static boolean mensualAbierta(LocalDateTime[] v) {
        LocalDateTime ahora = LocalDateTime.now();
        return !ahora.isBefore(v[0]) && !ahora.isAfter(v[1]);
    }

    private static String siguienteNivel(String nivel) {
        if (nivel == null) return null;
        return switch (nivel) { case "AZUL" -> "VERDE"; case "VERDE" -> "AMARILLO"; case "AMARILLO" -> "ROJO"; default -> null; };
    }

    private void autoevaluacion(HttpServletRequest req, HttpServletResponse resp, int yo) throws SQLException, ServletException, IOException {
        req.setAttribute("conVideo", figuras.idsConVideo());   // "▶ ver" junto a las figuras que tienen video
        Integer idEnsayo = entero(req, "ensayo");
        if (idEnsayo == null) {
            Ensayo pendiente = ensayos.autoevaluacionPendiente(yo);
            if (pendiente != null && !"mensual".equals(req.getParameter("tipo"))) idEnsayo = pendiente.getIdEnsayo();
        }
        if (idEnsayo != null) {
            Ensayo e = ensayos.obtener(idEnsayo);
            List<FiguraSugerida> figs = ensayos.figurasParaAutoevaluar(idEnsayo, yo);
            Map<Integer, String> actual = new HashMap<>();
            for (Evaluacion ev : evaluaciones.deMiembro(yo)) actual.put(ev.getIdFigura(), ev.getColor());
            req.setAttribute("ensayo", e);
            req.setAttribute("figuras", figs);
            req.setAttribute("actual", actual);
            vista(req, resp, "miembro/autoevaluacion-ensayo");
            return;
        }
        int periodo = evaluaciones.periodoActual();
        List<Evaluacion> lista = evaluaciones.deMiembro(yo);
        long respondidas = lista.stream().filter(x -> x.getColor() != null).count();
        int mitad = (lista.size() + 1) / 2;
        req.setAttribute("columnaA", lista.subList(0, mitad));
        req.setAttribute("columnaB", lista.subList(mitad, lista.size()));
        req.setAttribute("total", lista.size());
        req.setAttribute("respondidas", respondidas);
        req.setAttribute("mes", Formato.capital(LocalDateTime.now().getMonth().getDisplayName(java.time.format.TextStyle.FULL, Formato.ES)));
        req.setAttribute("abierta", mensualAbierta(evaluaciones.ventanaPeriodo(periodo)) || miembros.obtener(yo).getNivel() == null);
        vista(req, resp, "miembro/autoevaluacion-mensual");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int yo = Sesion.idUsuario(req);
        try {
            switch (req.getServletPath()) {
                case "/confirmar-asistencia", "/mi-asistencia" -> {
                    if (!Sesion.requiere(req, resp, "CONFIRMAR_ASISTENCIA")) return;
                    String volver = "inicio".equals(req.getParameter("volver")) ? "/inicio" : "/confirmar-asistencia";
                    Integer idTaller = entero(req, "idTaller");
                    if (idTaller != null) {
                        pe.sdp.daos.TallerDao td = new pe.sdp.daos.TallerDao();
                        pe.sdp.beans.Taller t = td.obtener(idTaller);
                        String r = "NO".equals(req.getParameter("respuesta")) ? "NO" : "SI";
                        if (t == null || !"PROGRAMADO".equals(t.getEstado()) || t.getFecha().isBefore(LocalDate.now(Formato.ZONA))) Sesion.error(req, "Ese taller ya no recibe confirmaciones.");
                        else {
                            td.confirmar(idTaller, yo, r, texto(req, "motivo"));
                            Sesion.ok(req, "SI".equals(r) ? "¡Bacán! Quedaste confirmado para el taller del " + t.getFechaCorta() + "." : "Listo, avisaste que no vas al taller del " + t.getFechaCorta() + ".");
                        }
                        ir(req, resp, volver); return;
                    }
                    Integer id = entero(req, "idEnsayo");
                    String r = "NO".equals(req.getParameter("respuesta")) ? "NO" : "SI";
                    Ensayo e = id == null ? null : ensayos.obtener(id);
                    if (e == null || e.isPasado()) { Sesion.error(req, "Ese ensayo ya no recibe confirmaciones."); }
                    else if (e.getCierreConfirmacion() != null && LocalDateTime.now().isAfter(e.getCierreConfirmacion())) {
                        Sesion.error(req, "La confirmación de ese ensayo ya cerró. Avísale al equipo de ensayos.");
                    } else {
                        ensayos.confirmar(id, yo, r, texto(req, "motivo"));
                        Sesion.ok(req, "SI".equals(r) ? "¡Bacán! Quedaste confirmado para el " + e.getFechaCorta() + "." : "Listo, avisaste que no vas el " + e.getFechaCorta() + ".");
                    }
                    ir(req, resp, volver);
                }
                case "/autoevaluacion" -> {
                    if (!Sesion.requiere(req, resp, "AUTOEVALUARSE")) return;
                    Map<Integer, String> colores = new HashMap<>();
                    for (String p : Collections.list(req.getParameterNames())) {
                        if (p.startsWith("color_")) { Integer f = Formato.entero(p.substring(6)); if (f != null) colores.put(f, req.getParameter(p)); }
                    }
                    Integer idEnsayo = entero(req, "idEnsayo");
                    if (colores.isEmpty()) { Sesion.error(req, "No marcaste ninguna figura."); }
                    else if (idEnsayo != null) {
                        // en el ensayo uno se puede recalificar en las dos direcciones (si creía saberla y no)
                        Map<Integer, String> antes = new HashMap<>();
                        for (Evaluacion ev : evaluaciones.deMiembro(yo)) antes.put(ev.getIdFigura(), ev.getColor());
                        int sube = 0, baja = 0;
                        for (Map.Entry<Integer, String> c : colores.entrySet()) {
                            int d = Formato.valor(c.getValue()) - Formato.valor(antes.get(c.getKey()));
                            if (d > 0) sube++; else if (d < 0 && antes.get(c.getKey()) != null) baja++;
                        }
                        evaluaciones.guardarEnsayo(yo, colores, idEnsayo);
                        Sesion.ok(req, sube == 0 && baja == 0 ? "Guardado. Ninguna figura cambió de color."
                                : (sube > 0 ? "¡Bien! Mejoraste en " + sube + (sube == 1 ? " figura" : " figuras") : "")
                                + (sube > 0 && baja > 0 ? " y te " : (baja > 0 ? "Te " : ""))
                                + (baja > 0 ? "recalificaste en " + baja + (baja == 1 ? " figura" : " figuras") + " (está bien darse cuenta practicando)" : "") + ".");
                    } else {
                        evaluaciones.guardarMensual(yo, colores, evaluaciones.periodoActual());
                        Sesion.ok(req, "Autoevaluación mensual guardada (" + colores.size() + " figuras).");
                    }
                    ir(req, resp, "/inicio");
                }
                case "/perfil" -> {
                    Miembro m = miembros.obtener(yo);
                    String correo = texto(req, "correo");
                    if (correo == null || !correo.toLowerCase().endsWith("@pucp.edu.pe")) { Sesion.error(req, "Usa tu correo PUCP (@pucp.edu.pe)."); ir(req, resp, "/perfil"); return; }
                    if (miembros.existeCorreo(correo, yo)) { Sesion.error(req, "Ese correo ya lo usa otro miembro."); ir(req, resp, "/perfil"); return; }
                    m.setNombreCompleto(texto(req, "nombreCompleto") == null ? m.getNombreCompleto() : texto(req, "nombreCompleto"));
                    m.setCodigoPucp(texto(req, "codigoPucp"));
                    m.setFacultad(texto(req, "facultad"));
                    m.setCorreo(correo);
                    m.setCelular(texto(req, "celular"));
                    String apodo = texto(req, "apodo");
                    if (apodo != null && apodo.length() > 30) { Sesion.error(req, "El apodo puede tener hasta 30 letras."); ir(req, resp, "/perfil"); return; }
                    if (!AdminServlet.fechaNacimientoValida(fecha(req, "fechaNacimiento"))) { Sesion.error(req, "Revisa tu fecha de nacimiento."); ir(req, resp, "/perfil"); return; }
                    m.setApodo(apodo);
                    m.setFechaNacimiento(fecha(req, "fechaNacimiento"));
                    m.setAvisoAsistencia(check(req, "avisoAsistencia"));
                    m.setAvisoMensual(check(req, "avisoMensual"));
                    m.setAvisoEnsayo(check(req, "avisoEnsayo"));
                    miembros.actualizarPerfil(m);
                    Miembro sesion = Sesion.usuario(req);
                    sesion.setNombreCompleto(m.getNombreCompleto()); sesion.setCorreo(m.getCorreo());
                    sesion.setApodo(m.getApodo()); sesion.setFechaNacimiento(m.getFechaNacimiento());
                    Sesion.ok(req, "Tus datos quedaron guardados.");
                    ir(req, resp, "/perfil");
                }
                default -> ir(req, resp, "/inicio");
            }
        } catch (SQLException e) { fallo(req, resp, e); }
    }
}
