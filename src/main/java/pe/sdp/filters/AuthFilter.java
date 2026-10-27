package pe.sdp.filters;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pe.sdp.beans.Miembro;
import pe.sdp.util.Sesion;

import java.io.IOException;

/**
 * Protege todo el sistema:
 *  - sin sesión → /login
 *  - con contraseña temporal → /cambiar-password
 * Los permisos de cada sección los revisa cada servlet con Sesion.requiere(...)
 */
@WebFilter("/*")
public class AuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");

        String ruta = req.getRequestURI().substring(req.getContextPath().length());
        boolean publica = ruta.equals("/login") || ruta.startsWith("/css/") || ruta.startsWith("/img/") || ruta.equals("/favicon.ico");
        if (publica) { chain.doFilter(request, response); return; }

        Miembro u = Sesion.usuario(req);
        if (u == null) { resp.sendRedirect(req.getContextPath() + "/login"); return; }
        if (u.isDebeCambiarPassword() && !ruta.equals("/cambiar-password") && !ruta.equals("/logout")) {
            resp.sendRedirect(req.getContextPath() + "/cambiar-password"); return;
        }
        resp.setHeader("Cache-Control", "no-store");   // que "atrás" no muestre páginas tras cerrar sesión
        chain.doFilter(request, response);
    }
}
