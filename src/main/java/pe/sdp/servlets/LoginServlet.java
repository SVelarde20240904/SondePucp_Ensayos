package pe.sdp.servlets;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import pe.sdp.beans.Miembro;
import pe.sdp.daos.MiembroDao;
import pe.sdp.util.Password;
import pe.sdp.util.Sesion;

import java.io.IOException;
import java.sql.SQLException;

/** Login, logout y cambio de contraseña (obligatorio con la contraseña temporal). */
@WebServlet(urlPatterns = {"/login", "/logout", "/cambiar-password", ""})
public class LoginServlet extends BaseServlet {

    private final MiembroDao dao = new MiembroDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        switch (req.getServletPath()) {
            case "/logout" -> {
                HttpSession s = req.getSession(false);
                if (s != null) s.invalidate();
                ir(req, resp, "/login");
            }
            case "/cambiar-password" -> vista(req, resp, "cambiar-password");
            case "/login" -> {
                if (Sesion.usuario(req) != null) ir(req, resp, "/inicio"); else vista(req, resp, "login");
            }
            default -> ir(req, resp, "/inicio");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            if ("/cambiar-password".equals(req.getServletPath())) { cambiarPassword(req, resp); return; }
            String correo = texto(req, "correo"), pass = req.getParameter("password");
            Miembro m = dao.buscarPorCorreo(correo);
            if (m == null || !Password.verifica(pass, m.getPasswordHash())) {
                req.setAttribute("error", "Correo o contraseña incorrectos.");
                req.setAttribute("correo", correo);
                vista(req, resp, "login");
                return;
            }
            if (!m.isActivo()) {
                req.setAttribute("error", "Tu cuenta está inactiva. Habla con la Mesa Directiva.");
                vista(req, resp, "login");
                return;
            }
            HttpSession s = req.getSession(true);
            s.setAttribute(Sesion.USUARIO, m);
            s.setAttribute(Sesion.PERMISOS, dao.permisos(m.getIdMiembro()));
            s.setMaxInactiveInterval(60 * 60 * 4);
            ir(req, resp, m.isDebeCambiarPassword() ? "/cambiar-password" : "/inicio");
        } catch (SQLException e) { fallo(req, resp, e); }
    }

    private void cambiarPassword(HttpServletRequest req, HttpServletResponse resp) throws SQLException, ServletException, IOException {
        Miembro u = Sesion.usuario(req);
        String actual = req.getParameter("actual"), nueva = req.getParameter("nueva"), repite = req.getParameter("repite");
        Miembro bd = dao.obtener(u.getIdMiembro());
        String error = null;
        if (!Password.verifica(actual, bd.getPasswordHash())) error = "La contraseña actual no es correcta.";
        else if (nueva == null || nueva.length() < 8) error = "La nueva contraseña debe tener al menos 8 caracteres.";
        else if (!nueva.equals(repite)) error = "Las contraseñas nuevas no coinciden.";
        else if (nueva.equals(actual)) error = "La nueva contraseña debe ser distinta a la temporal.";
        if (error != null) { req.setAttribute("error", error); vista(req, resp, "cambiar-password"); return; }
        dao.registrarPassword(u.getIdMiembro(), Password.hash(nueva), false);
        u.setDebeCambiarPassword(false);
        Sesion.ok(req, "Listo, tu contraseña quedó actualizada.");
        ir(req, resp, "/inicio");
    }
}
