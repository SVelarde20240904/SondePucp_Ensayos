package pe.sdp.servlets;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pe.sdp.util.Formato;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Ayudas comunes para todos los servlets. */
public abstract class BaseServlet extends HttpServlet {

    /** Muestra /WEB-INF/jsp/{vista}.jsp */
    protected void vista(HttpServletRequest req, HttpServletResponse resp, String vista) throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF/jsp/" + vista + ".jsp").forward(req, resp);
    }

    protected void ir(HttpServletRequest req, HttpServletResponse resp, String ruta) throws IOException {
        resp.sendRedirect(req.getContextPath() + ruta);
    }

    protected String accion(HttpServletRequest req, String porDefecto) {
        String a = req.getParameter("action");
        return a == null || a.isBlank() ? porDefecto : a;
    }

    protected Integer entero(HttpServletRequest req, String nombre) { return Formato.entero(req.getParameter(nombre)); }

    protected String texto(HttpServletRequest req, String nombre) { return Formato.vacio(req.getParameter(nombre)); }

    protected LocalDate fecha(HttpServletRequest req, String nombre) {
        String v = texto(req, nombre);
        try { return v == null ? null : LocalDate.parse(v); } catch (Exception e) { return null; }
    }

    protected LocalTime hora(HttpServletRequest req, String nombre) {
        String v = texto(req, nombre);
        try { return v == null ? null : LocalTime.parse(v); } catch (Exception e) { return null; }
    }

    protected LocalDateTime fechaHora(HttpServletRequest req, String nombre) {
        String v = texto(req, nombre);
        try { return v == null ? null : LocalDateTime.parse(v); } catch (Exception e) { return null; }
    }

    protected boolean check(HttpServletRequest req, String nombre) { return req.getParameter(nombre) != null; }

    /** Errores de BD: se muestran como aviso en vez de una pantalla en blanco. */
    protected void fallo(HttpServletRequest req, HttpServletResponse resp, Exception e) throws ServletException {
        log("Error de base de datos", e);
        throw new ServletException("No se pudo completar la operación: " + e.getMessage(), e);
    }
}
