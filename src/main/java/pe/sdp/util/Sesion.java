package pe.sdp.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import pe.sdp.beans.Miembro;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
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

    // ---------- Cambios de roles/permisos ----------
    private static volatile long ultimoCambio = 0;

    /** Avisar que cambiaron roles, permisos o el estado de alguien: todas las sesiones se revisan en su siguiente clic. */
    public static void permisosCambiaron() { ultimoCambio = System.currentTimeMillis(); }

    public static long ultimoCambio() { return ultimoCambio; }

    // ---------- CSRF ----------
    /** Nombre del campo oculto que llevan todos los formularios POST y de la variable de sesión. */
    public static final String CSRF = "csrf";
    private static final SecureRandom RANDOM = new SecureRandom();

    /** Devuelve el token CSRF de la sesión (lo crea si no existe). */
    public static String csrf(HttpSession s) {
        Object t = s.getAttribute(CSRF);
        if (t == null) { t = nuevoToken(); s.setAttribute(CSRF, t); }
        return (String) t;
    }

    public static void renovarCsrf(HttpSession s) { s.setAttribute(CSRF, nuevoToken()); }

    /** ¿El formulario trae el mismo token que la sesión? (comparación en tiempo constante) */
    public static boolean csrfValido(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        Object esperado = s == null ? null : s.getAttribute(CSRF);
        String recibido = req.getParameter("_csrf");
        return esperado != null && recibido != null
                && MessageDigest.isEqual(((String) esperado).getBytes(StandardCharsets.UTF_8), recibido.getBytes(StandardCharsets.UTF_8));
    }

    private static String nuevoToken() {
        byte[] b = new byte[32];
        RANDOM.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    public static void ok(HttpServletRequest req, String msg) { req.getSession().setAttribute("flashOk", msg); }

    public static void error(HttpServletRequest req, String msg) { req.getSession().setAttribute("flashError", msg); }
}
