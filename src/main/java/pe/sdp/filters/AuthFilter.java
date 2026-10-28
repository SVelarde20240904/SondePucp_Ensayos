package pe.sdp.filters;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import pe.sdp.beans.Miembro;
import pe.sdp.daos.MiembroDao;
import pe.sdp.util.Config;
import pe.sdp.util.Sesion;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Protege todo el sistema, en este orden:
 *  1. HTTPS obligatorio en producción (redirige http → https) y cabeceras de seguridad
 *  2. /salud responde "ok" (para el chequeo del hosting)
 *  3. CSRF: todo POST debe traer el token de la sesión (campo oculto _csrf)
 *  4. sin sesión → /login · con contraseña temporal → /cambiar-password
 *  5. cada 2 minutos revisa en la BD que la cuenta siga activa y recarga sus permisos
 * Los permisos de cada sección los revisa cada servlet con Sesion.requiere(...)
 */
@WebFilter("/*")
public class AuthFilter implements Filter {

    private static final long REVISAR_CADA_MS = 2 * 60_000L;
    private static final String ULTIMA_REVISION = "ultimaRevision";

    private final MiembroDao dao = new MiembroDao();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        String ruta = req.getRequestURI().substring(req.getContextPath().length());

        if (ruta.equals("/salud")) {                       // chequeo del hosting: sin redirecciones ni sesión
            resp.setContentType("text/plain");
            resp.getWriter().write("ok");
            return;
        }

        // 1. HTTPS + cabeceras
        if (Config.forzarHttps() && !req.isSecure()) {
            if ("GET".equals(req.getMethod()) || "HEAD".equals(req.getMethod())) {
                String q = req.getQueryString();
                resp.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
                resp.setHeader("Location", "https://" + req.getServerName() + req.getRequestURI() + (q == null ? "" : "?" + q));
            } else {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Usa https");
            }
            return;
        }
        cabeceras(req, resp);

        boolean estatica = ruta.startsWith("/css/") || ruta.startsWith("/js/") || ruta.startsWith("/img/") || ruta.equals("/favicon.ico");
        if (estatica) { chain.doFilter(request, response); return; }

        // 2. CSRF
        if ("POST".equals(req.getMethod()) && !Sesion.csrfValido(req)) {
            HttpSession s = req.getSession(true);
            Sesion.error(req, "La página había caducado. Vuelve a intentarlo.");
            Sesion.csrf(s);
            String ref = req.getHeader("Referer");   // vuelve a la página del formulario (si es de este mismo sitio)
            boolean mismoSitio = ref != null && ref.matches("https?://" + java.util.regex.Pattern.quote(req.getServerName()) + "(:\\d+)?" + java.util.regex.Pattern.quote(req.getContextPath()) + "/.*");
            resp.sendRedirect(Sesion.usuario(req) == null ? req.getContextPath() + "/login" : (mismoSitio ? ref : req.getContextPath() + "/inicio"));
            return;
        }

        // 3. Login
        if (ruta.equals("/login")) {
            Sesion.csrf(req.getSession(true));            // el formulario de login también lleva token
            chain.doFilter(request, response);
            return;
        }
        Miembro u = Sesion.usuario(req);
        if (u == null) { resp.sendRedirect(req.getContextPath() + "/login"); return; }

        // 4. ¿Sigue activa la cuenta? (si el admin la inactivó, se cierra la sesión)
        if (!revisarCuenta(req)) {
            req.getSession().invalidate();
            HttpSession nueva = req.getSession(true);
            Sesion.csrf(nueva);
            Sesion.error(req, "Tu sesión se cerró porque tu cuenta ya no está activa.");
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        u = Sesion.usuario(req);
        if (u.isDebeCambiarPassword() && !ruta.equals("/cambiar-password") && !ruta.equals("/logout")) {
            resp.sendRedirect(req.getContextPath() + "/cambiar-password"); return;
        }
        resp.setHeader("Cache-Control", "no-store");   // que "atrás" no muestre páginas tras cerrar sesión
        chain.doFilter(request, response);
    }

    /** Cada 2 minutos vuelve a leer al usuario y sus permisos. Devuelve false si ya no puede entrar. */
    private boolean revisarCuenta(HttpServletRequest req) {
        HttpSession s = req.getSession();
        Long ultima = (Long) s.getAttribute(ULTIMA_REVISION);
        long ahora = System.currentTimeMillis();
        if (ultima != null && ahora - ultima < REVISAR_CADA_MS) return true;
        try {
            Miembro bd = dao.obtener(Sesion.idUsuario(req));
            if (bd == null || !bd.isActivo() || bd.getPasswordHash() == null) return false;
            s.setAttribute(Sesion.USUARIO, bd);
            s.setAttribute(Sesion.PERMISOS, dao.permisos(bd.getIdMiembro()));
            s.setAttribute(ULTIMA_REVISION, ahora);
            return true;
        } catch (SQLException e) {
            return true;    // si la BD falla un momento no botamos a nadie; el servlet mostrará el error
        }
    }

    private static void cabeceras(HttpServletRequest req, HttpServletResponse resp) {
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "SAMEORIGIN");
        resp.setHeader("Referrer-Policy", "same-origin");
        resp.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
        resp.setHeader("Content-Security-Policy",
                "default-src 'self'; img-src 'self' data:; " +
                "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; font-src 'self' https://fonts.gstatic.com; " +
                "script-src 'self' 'unsafe-inline'; form-action 'self'; frame-ancestors 'self'; base-uri 'self'; object-src 'none'");
        if (req.isSecure()) resp.setHeader("Strict-Transport-Security", "max-age=31536000");
    }
}
