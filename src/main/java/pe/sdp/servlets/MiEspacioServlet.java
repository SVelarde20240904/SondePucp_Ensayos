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
import java.time.LocalDateTime;
import java.util.*;

/** Mi espacio: Inicio, Confirmar asistencia, Autoevaluaciones, Mi historial y Mi perfil. */
@WebServlet(urlPatterns = {"/inicio", "/confirmar-asistencia", "/autoevaluacion", "/mi-historial", "/perfil"})
public class MiEspacioServlet extends BaseServlet {

    private final MiembroDao miembros = new MiembroDao();
    private final EvaluacionDao evaluaciones = new EvaluacionDao();
    private final EnsayoDao ensayos = new EnsayoDao();
    private final pe.sdp.daos.FiguraDao figuras = new pe.sdp.daos.FiguraDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int yo = Sesion.idUsuario(req);
        try {
            switch (req.getServletPath()) {
                case "/inicio" -> inicio(req, resp, yo);
                case "/confirmar-asistencia" -> {
                    req.setAttribute("proximos", ensayos.proximos(yo, 3));
                    req.setAttribute("misAsistencias", ensayos.misAsistencias(yo, 6));
                    req.setAttribute("porcentaje", ensayos.porcentajeAsistencia(yo));
                    vista(req, resp, "miembro/confirmar-asistencia");
                }
                case "/autoevaluacion" -> autoevaluacion(req, resp, yo);
                case "/mi-historial" -> {
                    String fuente = texto(req, "fuente");
                    req.setAttribute("fuente", fuente);
                    req.setAttribute("historial", evaluaciones.historialDe(yo, fuente));
                    req.setAttribute("resumen", evaluaciones.resumenMiembro(yo));
                    req.setAttribute("porcentaje", ensayos.porcentajeAsistencia(yo));
                    req.setAttribute("yo", miembros.obtener(yo));
                    vista(req, resp, "miembro/mi-historial");
                }
                case "/perfil" -> {
                    req.setAttribute("yo", miembros.obtener(yo));
                    req.setAttribute("facultades", AdminServlet.FACULTADES);
                    vista(req, resp, "miembro/mi-perfil");
                }
            }
        } catch (SQLException e) { fallo(req, resp, e); }
    }

    private void inicio(HttpServletRequest req, HttpServletResponse resp, int yo) throws SQLException, ServletException, IOException {
        Miembro m = miembros.obtener(yo);
        List<Evaluacion> faltan = evaluaciones.queLeFaltan(yo);
        String siguiente = siguienteNivel(m.getNivel());
        long faltanSuNivel = faltan.stream().filter(e -> Objects.equals(e.getColorFigura(), m.getNivel())).count();
        int periodo = evaluaciones.periodoActual();
        LocalDateTime[] v = evaluaciones.ventanaPeriodo(periodo);
        List<Ensayo> prox = ensayos.proximos(yo, 1);
        req.setAttribute("yo", m);
        req.setAttribute("faltan", faltan);
        req.setAttribute("faltanSuNivel", faltanSuNivel);
        req.setAttribute("siguienteNivel", siguiente);
        req.setAttribute("proximo", prox.isEmpty() ? null : prox.get(0));
        req.setAttribute("mensualRespondida", evaluaciones.respondioPeriodo(yo, periodo));
        req.setAttribute("mensualAbierta", mensualAbierta(v) || m.getNivel() == null);
        req.setAttribute("cierreMensual", Formato.fechaLarga(v[1].toLocalDate()));
        req.setAttribute("nFiguras", evaluaciones.deMiembro(yo).size());
        req.setAttribute("pendienteEnsayo", ensayos.autoevaluacionPendiente(yo));
        vista(req, resp, "miembro/inicio");
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
                case "/confirmar-asistencia" -> {
                    if (!Sesion.requiere(req, resp, "CONFIRMAR_ASISTENCIA")) return;
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
                    ir(req, resp, "/confirmar-asistencia");
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
                        int n = evaluaciones.guardarEnsayo(yo, colores, idEnsayo);
                        Sesion.ok(req, n == 0 ? "Guardado. Ninguna figura subió de color (la del ensayo solo sube)." : "¡Bien! Mejoraste en " + n + (n == 1 ? " figura." : " figuras."));
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
                    m.setAvisoAsistencia(check(req, "avisoAsistencia"));
                    m.setAvisoMensual(check(req, "avisoMensual"));
                    m.setAvisoEnsayo(check(req, "avisoEnsayo"));
                    miembros.actualizarPerfil(m);
                    Miembro sesion = Sesion.usuario(req);
                    sesion.setNombreCompleto(m.getNombreCompleto()); sesion.setCorreo(m.getCorreo());
                    Sesion.ok(req, "Tus datos quedaron guardados.");
                    ir(req, resp, "/perfil");
                }
                default -> ir(req, resp, "/inicio");
            }
        } catch (SQLException e) { fallo(req, resp, e); }
    }
}
