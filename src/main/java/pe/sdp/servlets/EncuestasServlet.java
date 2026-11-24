package pe.sdp.servlets;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pe.sdp.daos.EncuestaDao;
import pe.sdp.daos.MiembroDao;
import pe.sdp.util.Formato;
import pe.sdp.util.Sesion;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 *  /encuestas            las que me tocan (abiertas y cerradas)
 *  /encuestas/ver?id=    votar y ver resultados
 *  /encuestas/nueva      crear (permiso CREAR_ENCUESTAS)
 */
@WebServlet(urlPatterns = {"/encuestas", "/encuestas/*"})
public class EncuestasServlet extends BaseServlet {

    private static final String PERM = "CREAR_ENCUESTAS";
    private final EncuestaDao dao = new EncuestaDao();

    private String ruta(HttpServletRequest req) { return req.getPathInfo() == null ? "/" : req.getPathInfo(); }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int yo = Sesion.idUsuario(req);
        try {
            switch (ruta(req)) {
                case "/" -> {
                    req.setAttribute("abiertas", dao.paraMi(yo, true));
                    req.setAttribute("cerradas", dao.paraMi(yo, false));
                    vista(req, resp, "encuestas/lista");
                }
                case "/ver" -> {
                    Integer id = entero(req, "id");
                    Map<String, Object> e = id == null ? null : dao.ver(id, yo);
                    if (e == null) { Sesion.error(req, "Esa encuesta no existe o no es para ti."); ir(req, resp, "/encuestas"); return; }
                    boolean duenio = Objects.equals(e.get("idCreador"), yo) || Sesion.tiene(req, "ASIGNAR_ROLES");
                    boolean verResultados = !Boolean.TRUE.equals(e.get("abierta")) || Boolean.TRUE.equals(e.get("yaVote"))
                            || Boolean.TRUE.equals(e.get("verAntes")) || duenio;
                    req.setAttribute("e", e);
                    req.setAttribute("duenio", duenio);
                    req.setAttribute("verResultados", verResultados);
                    req.setAttribute("editarVoto", "1".equals(req.getParameter("cambiar")));
                    vista(req, resp, "encuestas/ver");
                }
                case "/nueva" -> {
                    if (!Sesion.requiere(req, resp, PERM)) return;
                    req.setAttribute("teams", new MiembroDao().teams());
                    LocalDate d = LocalDate.now(Formato.ZONA).plusDays(3);
                    req.setAttribute("cierreFecha", d.toString());
                    vista(req, resp, "encuestas/nueva");
                }
                default -> resp.sendError(404);
            }
        } catch (SQLException e) { fallo(req, resp, e); }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int yo = Sesion.idUsuario(req);
        try {
            switch (ruta(req)) {
                case "/nueva" -> crear(req, resp, yo);
                case "/ver" -> {
                    Integer id = entero(req, "id");
                    Map<String, Object> e = id == null ? null : dao.ver(id, yo);
                    if (e == null) { Sesion.error(req, "Esa encuesta no existe o no es para ti."); ir(req, resp, "/encuestas"); return; }
                    String a = accion(req, "votar");
                    boolean duenio = Objects.equals(e.get("idCreador"), yo) || Sesion.tiene(req, "ASIGNAR_ROLES");
                    switch (a) {
                        case "votar" -> {
                            List<Integer> ops = new ArrayList<>();
                            String[] vs = req.getParameterValues("opcion");
                            if (vs != null) for (String v : vs) { Integer x = Formato.entero(v); if (x != null && !ops.contains(x)) ops.add(x); }
                            if (!Boolean.TRUE.equals(e.get("multiple")) && ops.size() > 1) ops = ops.subList(0, 1);
                            if (ops.isEmpty()) Sesion.error(req, "Marca una opción.");
                            else {
                                int n = dao.votar(id, ops, yo);
                                if (n < 0) Sesion.error(req, "La encuesta ya cerró.");
                                else if (n == 0) Sesion.error(req, "Esa opción no es de esta encuesta.");
                                else Sesion.ok(req, "¡Voto registrado! Puedes cambiarlo hasta que cierre.");
                            }
                        }
                        case "cerrar" -> { if (duenio) { dao.cerrarYa(id); Sesion.ok(req, "Encuesta cerrada."); } }
                        case "eliminar" -> {
                            if (duenio) { dao.eliminar(id); Sesion.ok(req, "Encuesta eliminada."); ir(req, resp, "/encuestas"); return; }
                        }
                        default -> { }
                    }
                    ir(req, resp, "/encuestas/ver?id=" + id);
                }
                default -> resp.sendError(404);
            }
        } catch (SQLException e) { fallo(req, resp, e); }
    }

    private void crear(HttpServletRequest req, HttpServletResponse resp, int yo) throws SQLException, IOException {
        if (!Sesion.requiere(req, resp, PERM)) return;
        String pregunta = texto(req, "pregunta");
        String tipo = texto(req, "tipo");
        if (tipo == null || !EncuestaDao.TIPOS.contains(tipo)) tipo = "UNA";
        List<String> ops = new ArrayList<>();
        if ("SI_NO".equals(tipo)) ops = List.of("Sí", "No");
        else {
            String t = req.getParameter("opciones");
            if (t != null) for (String l : t.split("\\r?\\n")) {
                String o = l.trim();
                if (!o.isEmpty() && o.length() <= 120 && !ops.contains(o)) ops.add(o);
                if (ops.size() == 20) break;
            }
        }
        LocalDate f = fecha(req, "cierreFecha");
        LocalTime h = hora(req, "cierreHora");
        LocalDateTime cierre = f == null ? null : f.atTime(h == null ? LocalTime.of(23, 59) : h);
        Integer team = entero(req, "team");
        String error = null;
        if (pregunta == null || pregunta.length() > 200) error = "Escribe la pregunta (máx. 200 letras).";
        else if (ops.size() < 2) error = "Pon al menos 2 opciones, una por línea.";
        else if (cierre == null || !cierre.isAfter(LocalDateTime.now(Formato.ZONA))) error = "La fecha de cierre tiene que ser en el futuro.";
        else if (cierre.isAfter(LocalDateTime.now(Formato.ZONA).plusDays(60))) error = "Máximo 60 días abierta.";
        else if (team != null && new MiembroDao().teams().stream().noneMatch(x -> x.getIdTeam() == team)) error = "Ese team no existe.";
        if (error != null) { Sesion.error(req, error); ir(req, resp, "/encuestas/nueva"); return; }
        String desc = texto(req, "descripcion");
        if (desc != null && desc.length() > 1000) desc = desc.substring(0, 1000);
        int id = dao.crear(pregunta, desc, tipo, check(req, "anonima"), check(req, "verAntes"), team, cierre, ops, yo);
        Sesion.ok(req, "Encuesta publicada. Les aparece en Pendientes a todos los que les toca.");
        ir(req, resp, "/encuestas/ver?id=" + id);
    }
}
