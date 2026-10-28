package pe.sdp.servlets;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pe.sdp.beans.Evaluacion;
import pe.sdp.beans.Figura;
import pe.sdp.daos.EvaluacionDao;
import pe.sdp.daos.FiguraDao;
import pe.sdp.util.Sesion;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/**
 * Biblioteca de figuras (para todos los miembros):
 *  /biblioteca               todas las figuras de la mano, por nivel, con buscador
 *  /biblioteca/figura?id=    una figura: nivel, posición en la mano, descripción y video
 * Los videos son enlaces a Drive/YouTube. En Drive deben estar RESTRINGIDOS al Grupo de Google de SDP:
 * así, aunque el link se filtre, solo lo ve quien está en el grupo.
 */
@WebServlet(urlPatterns = {"/biblioteca", "/biblioteca/*"})
public class BibliotecaServlet extends BaseServlet {

    private final FiguraDao figuras = new FiguraDao();
    private final EvaluacionDao evaluaciones = new EvaluacionDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int yo = Sesion.idUsuario(req);
        try {
            Map<Integer, String> miColor = new HashMap<>();
            for (Evaluacion e : evaluaciones.deMiembro(yo)) if (e.getColor() != null) miColor.put(e.getIdFigura(), e.getColor());
            req.setAttribute("miColor", miColor);

            String ruta = req.getPathInfo() == null ? "/" : req.getPathInfo();
            if ("/figura".equals(ruta)) {
                Integer id = entero(req, "id");
                Figura f = id == null ? null : figuras.obtener(id);
                if (f == null || !f.isActiva()) { Sesion.error(req, "Esa figura no está en la mano."); ir(req, resp, "/biblioteca"); return; }
                // posición en la mano: número, anterior y siguiente
                List<Figura> mano = figuras.listarActivas();
                int i = 0;
                for (int k = 0; k < mano.size(); k++) if (mano.get(k).getIdFigura() == f.getIdFigura()) i = k;
                req.setAttribute("f", f);
                req.setAttribute("posicion", i + 1);
                req.setAttribute("totalMano", mano.size());
                req.setAttribute("anterior", i > 0 ? mano.get(i - 1) : null);
                req.setAttribute("siguiente", i < mano.size() - 1 ? mano.get(i + 1) : null);
                vista(req, resp, "miembro/biblioteca-figura");
                return;
            }

            String q = texto(req, "q");
            boolean soloVideo = "1".equals(req.getParameter("video"));
            Map<String, List<Figura>> porNivel = figuras.activasPorNivel();
            int conVideo = 0, total = 0;
            for (List<Figura> l : porNivel.values()) {
                for (Figura f : l) { total++; if (f.isTieneVideo()) conVideo++; }
                l.removeIf(f -> (q != null && !f.getNombre().toLowerCase().contains(q.toLowerCase())) || (soloVideo && !f.isTieneVideo()));
            }
            porNivel.values().removeIf(List::isEmpty);
            req.setAttribute("porNivel", porNivel);
            req.setAttribute("conVideo", conVideo);
            req.setAttribute("total", total);
            vista(req, resp, "miembro/biblioteca");
        } catch (SQLException e) { fallo(req, resp, e); }
    }
}
