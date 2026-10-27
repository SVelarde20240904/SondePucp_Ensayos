package pe.sdp.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import pe.sdp.beans.Miembro;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

/** Ayudas para leer el usuario en sesión, sus permisos y los mensajes flash. */
public final class Sesion {

    public static final String USUARIO = "usuario";
    public static final String PERMISOS = "permisos";

    private Sesion() {}

    public static Miembro usuario(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        return s == null ? null : (Miembro) s.getAttribute(USUARIO);
    }

    public static int idUsuario(HttpServletRequest req) {
        return usuario(req).getIdMiembro();
    }

    @SuppressWarnings("unchecked")
    public static Set<String> permisos(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        Object p = s == null ? null : s.getAttribute(PERMISOS);
        return p == null ? Collections.emptySet() : (Set<String>) p;
    }

    public static boolean tiene(HttpServletRequest req, String permiso) {
        return permisos(req).contains(permiso);
    }

    /** Si no tiene el permiso, lo manda a Inicio con un aviso y devuelve false. */
    public static boolean requiere(HttpServletRequest req, HttpServletResponse resp, String permiso) throws IOException {
        if (tiene(req, permiso)) return true;
        error(req, "No tienes permiso para entrar a esa sección.");
        resp.sendRedirect(req.getContextPath() + "/inicio");
        return false;
    }

    public static void ok(HttpServletRequest req, String msg) { req.getSession().setAttribute("flashOk", msg); }

    public static void error(HttpServletRequest req, String msg) { req.getSession().setAttribute("flashError", msg); }
}
