package pe.sdp.servlets;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pe.sdp.beans.Figura;
import pe.sdp.beans.Miembro;
import pe.sdp.daos.*;
import pe.sdp.util.Formato;
import pe.sdp.util.Password;
import pe.sdp.util.Sesion;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Sección ADMINISTRACIÓN:
 *  /admin/miembros[/nuevo|/editar]   alta, edición, inactivar, reactivar, eliminar (alta por error)
 *  /admin/figuras[/nueva|/editar]    alta, edición, desactivar, reactivar, eliminar (sin uso), reglas de conteo
 *  /admin/responsabilidades          asignar, marcar cumplido, justificar
 *  /admin/historico                  búsqueda + corrección de colores
 *  /admin/roles                      matriz de permisos y personas con rol
 */
@WebServlet(urlPatterns = {"/admin/*"})
public class AdminServlet extends BaseServlet {

    private final MiembroDao miembros = new MiembroDao();
    private final FiguraDao figuras = new FiguraDao();
    private final EvaluacionDao evaluaciones = new EvaluacionDao();
    private final EncargoDao encargos = new EncargoDao();
    private final RolDao roles = new RolDao();

    public static final List<String> FACULTADES = List.of("Ciencias e Ingeniería", "Ciencias Sociales", "Derecho", "Gestión y Alta Dirección",
            "Arte y Diseño", "Arquitectura y Urbanismo", "Letras y Ciencias Humanas", "Educación", "Psicología",
            "Ciencias y Artes de la Comunicación", "Estudios Generales Ciencias", "Estudios Generales Letras", "Egresado(a)");

    private String ruta(HttpServletRequest req) { return req.getPathInfo() == null ? "/" : req.getPathInfo(); }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            switch (ruta(req)) {
                case "/miembros" -> {
                    if (!Sesion.requiere(req, resp, "GESTIONAR_MIEMBROS")) return;
                    String filtro = texto(req, "filtro") == null ? "TODOS" : texto(req, "filtro");
                    req.setAttribute("filtro", filtro);
                    req.setAttribute("q", texto(req, "q"));
                    req.setAttribute("lista", miembros.listar(filtro, texto(req, "q")));
                    req.setAttribute("activos", miembros.contarActivos());
                    vista(req, resp, "admin/miembros");
                }
                case "/miembros/nuevo", "/miembros/editar" -> {
                    if (!Sesion.requiere(req, resp, "GESTIONAR_MIEMBROS")) return;
                    Miembro m;
                    if (entero(req, "id") != null) m = miembros.obtener(entero(req, "id"));
                    else { m = new Miembro(); m.setFrecuencia("REGULAR"); m.setFechaIngreso(LocalDate.now()); m.setEstado("ACTIVO"); }
                    req.setAttribute("m", m);
                    req.setAttribute("facultades", FACULTADES);
                    req.setAttribute("passwordTemporal", Formato.passwordTemporal());
                    vista(req, resp, "admin/miembro-form");
                }
                case "/figuras" -> {
                    if (!Sesion.requiere(req, resp, "GESTIONAR_FIGURAS")) return;
                    req.setAttribute("porNivel", figuras.activasPorNivel());
                    req.setAttribute("inactivas", figuras.listarInactivas());
                    req.setAttribute("reglas", figuras.reglas());
                    req.setAttribute("total", figuras.listarActivas().size());
                    vista(req, resp, "admin/figuras");
                }
                case "/figuras/nueva", "/figuras/editar" -> {
                    if (!Sesion.requiere(req, resp, "GESTIONAR_FIGURAS")) return;
                    Figura f = entero(req, "id") != null ? figuras.obtener(entero(req, "id")) : null;
                    req.setAttribute("f", f);
                    List<Figura> todas = figuras.listarActivas();
                    req.setAttribute("todas", todas);
                    if (f != null) {
                        Integer anterior = null;
                        for (Figura x : todas) { if (x.getIdFigura() == f.getIdFigura()) break; anterior = x.getIdFigura(); }
                        req.setAttribute("anterior", anterior);
                    }
                    vista(req, resp, "admin/figura-form");
                }
                case "/responsabilidades" -> {
                    if (!Sesion.requiere(req, resp, "ASIGNAR_RESPONSABILIDADES")) return;
                    LocalDate semana = fecha(req, "semana");
                    semana = Formato.sabadoDe(semana == null ? LocalDate.now() : semana);
                    req.setAttribute("semana", semana);
                    req.setAttribute("semanaTexto", Formato.ddmmyyyy(semana));
                    req.setAttribute("semanaAnterior", semana.minusWeeks(1));
                    req.setAttribute("semanaSiguiente", semana.plusWeeks(1));
                    req.setAttribute("lista", encargos.deSemana(semana));
                    req.setAttribute("tipos", encargos.tiposActivos());
                    req.setAttribute("activos", miembros.listarActivos());
                    req.setAttribute("resumenMes", encargos.resumenMes(semana));
                    req.setAttribute("mes", Formato.capital(semana.getMonth().getDisplayName(java.time.format.TextStyle.FULL, Formato.ES)));
                    vista(req, resp, "admin/responsabilidades");
                }
                case "/historico" -> {
                    if (!Sesion.requiere(req, resp, "GESTIONAR_HISTORICO")) return;
                    Integer m = entero(req, "fMiembro"), f = entero(req, "fFigura");
                    String fuente = texto(req, "fFuente");
                    LocalDate desde = fecha(req, "fDesde"), hasta = fecha(req, "fHasta");
                    int pagina = Math.max(1, Optional.ofNullable(entero(req, "p")).orElse(1));
                    int tam = 20;
                    int[] cuentas = evaluaciones.contar(m, f, fuente, desde, hasta);
                    req.setAttribute("lista", evaluaciones.buscar(m, f, fuente, desde, hasta, pagina, tam));
                    req.setAttribute("cuentas", cuentas);
                    req.setAttribute("pagina", pagina);
                    req.setAttribute("paginas", Math.max(1, (cuentas[0] + tam - 1) / tam));
                    req.setAttribute("desdeFila", cuentas[0] == 0 ? 0 : (pagina - 1) * tam + 1);
                    req.setAttribute("hastaFila", Math.min(pagina * tam, cuentas[0]));
                    req.setAttribute("miembrosLista", miembros.listar("TODOS", null));
                    req.setAttribute("figurasLista", figuras.listarActivas());
                    vista(req, resp, "admin/historico");
                }
                case "/roles" -> {
                    if (!Sesion.requiere(req, resp, "ASIGNAR_ROLES")) return;
                    req.setAttribute("permisos", roles.matriz());
                    req.setAttribute("personas", roles.personasConRol());
                    req.setAttribute("activos", miembros.listarActivos());
                    vista(req, resp, "admin/roles");
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
                case "/miembros/nuevo", "/miembros/editar" -> postMiembro(req, resp, yo);
                case "/figuras/nueva", "/figuras/editar" -> postFigura(req, resp, yo);
                case "/figuras" -> {
                    if (!Sesion.requiere(req, resp, "GESTIONAR_FIGURAS")) return;
                    Map<String, Boolean> r = new LinkedHashMap<>();
                    for (String k : List.of("REGULAR", "OCASIONAL", "EVENTUAL", "EQUIPO_ENSAYOS")) r.put(k, check(req, "cuenta_" + k));
                    figuras.guardarReglas(r);
                    Sesion.ok(req, "Reglas guardadas: los colores de las figuras y los niveles ya se recalcularon.");
                    ir(req, resp, "/admin/figuras");
                }
                case "/responsabilidades" -> {
                    if (!Sesion.requiere(req, resp, "ASIGNAR_RESPONSABILIDADES")) return;
                    LocalDate semana = fecha(req, "semana");
                    switch (accion(req, "")) {
                        case "asignar" -> {
                            Integer tipo = entero(req, "idTipo"), resp_ = entero(req, "responsable");
                            if (tipo == null || resp_ == null) Sesion.error(req, "Elige el encargo y el responsable.");
                            else { encargos.asignar(tipo, semana, resp_, fechaHora(req, "plazo")); Sesion.ok(req, "Encargo asignado."); }
                        }
                        case "cumplir" -> {
                            Integer quien = entero(req, "cumplidoPor");
                            encargos.cumplir(entero(req, "idEncargo"), quien == null ? yo : quien);
                            Sesion.ok(req, "Encargo marcado como cumplido.");
                        }
                        case "justificar" -> { encargos.justificar(entero(req, "idEncargo"), texto(req, "motivo")); Sesion.ok(req, "Justificación registrada."); }
                        case "eliminar" -> { encargos.eliminar(entero(req, "idEncargo")); Sesion.ok(req, "Encargo eliminado."); }
                        default -> { }
                    }
                    ir(req, resp, "/admin/responsabilidades?semana=" + semana);
                }
                case "/historico" -> {
                    if (!Sesion.requiere(req, resp, "GESTIONAR_HISTORICO")) return;
                    Integer m = entero(req, "cMiembro"), f = entero(req, "cFigura");
                    String color = texto(req, "cColor"), motivo = texto(req, "cMotivo");
                    if (m == null || f == null || !Formato.esColor(color) || motivo == null) Sesion.error(req, "Para corregir elige miembro, figura, color y escribe el motivo.");
                    else { evaluaciones.corregir(m, f, color, motivo, yo); Sesion.ok(req, "Corrección registrada en el histórico."); }
                    ir(req, resp, "/admin/historico");
                }
                case "/roles" -> {
                    if (!Sesion.requiere(req, resp, "ASIGNAR_ROLES")) return;
                    switch (accion(req, "permisos")) {
                        case "asignar" -> {
                            Integer m = entero(req, "idMiembro"), rol = entero(req, "idRol");
                            if (m != null && rol != null && (rol == RolDao.EQUIPO || rol == RolDao.ADMIN)) { roles.asignar(m, rol, yo); Sesion.ok(req, "Rol asignado."); }
                        }
                        case "quitar" -> {
                            try { roles.quitar(entero(req, "idMiembro"), entero(req, "idRol")); Sesion.ok(req, "Rol quitado. La persona sigue siendo Miembro."); }
                            catch (SQLException ex) { log("No se pudo quitar", ex); Sesion.error(req, "No se pudo quitar: " + mensajeBd(ex)); }
                        }
                        default -> {
                            Set<String> marcados = new HashSet<>();
                            for (String p : Collections.list(req.getParameterNames())) if (p.startsWith("perm_")) marcados.add(p.substring(5));
                            roles.guardarMatriz(marcados);
                            Sesion.ok(req, "Permisos guardados. A cada persona le llegan en máximo 2 minutos (sin cerrar sesión).");
                        }
                    }
                    ir(req, resp, "/admin/roles");
                }
                default -> resp.sendError(404);
            }
        } catch (SQLException e) { fallo(req, resp, e); }
    }

    private void postMiembro(HttpServletRequest req, HttpServletResponse resp, int yo) throws SQLException, IOException {
        if (!Sesion.requiere(req, resp, "GESTIONAR_MIEMBROS")) return;
        Integer id = entero(req, "idMiembro");
        String accion = accion(req, "guardar");
        String volver = id == null ? "/admin/miembros/nuevo" : "/admin/miembros/editar?id=" + id;
        switch (accion) {
            case "inactivar" -> {
                if (id == yo) { Sesion.error(req, "No puedes inactivarte a ti mismo."); ir(req, resp, volver); return; }
                miembros.inactivar(id, texto(req, "motivoBaja"), yo);
                Sesion.ok(req, "Miembro inactivado. Su historial se conserva.");
                ir(req, resp, "/admin/miembros");
                return;
            }
            case "reactivar" -> { miembros.reactivar(id); Sesion.ok(req, "Miembro reactivado."); ir(req, resp, volver); return; }
            case "eliminar" -> {
                if (miembros.eliminar(id)) { Sesion.ok(req, "Miembro eliminado."); ir(req, resp, "/admin/miembros"); }
                else { Sesion.error(req, "No se puede eliminar: tiene historial. Inactívalo."); ir(req, resp, volver); }
                return;
            }
            case "restablecer" -> {
                // Si escribieron el correo en el formulario, se guarda primero (sin correo no puede entrar)
                Miembro actual = miembros.obtener(id);
                String correo = texto(req, "correo");
                if (correo != null) correo = correo.trim().toLowerCase();
                if (correo != null && !correo.equalsIgnoreCase(String.valueOf(actual.getCorreo()))) {
                    if (!correo.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                        Sesion.error(req, "Ese correo no es válido. Corrígelo antes de generar la contraseña.");
                        ir(req, resp, volver); return;
                    }
                    if (miembros.existeCorreo(correo, id)) {
                        Sesion.error(req, "Ese correo ya lo usa otro miembro.");
                        ir(req, resp, volver); return;
                    }
                    miembros.actualizarCorreo(id, correo);
                } else if (actual.getCorreo() == null || actual.getCorreo().isBlank()) {
                    Sesion.error(req, "Primero escribe su correo PUCP: sin correo no puede iniciar sesión.");
                    ir(req, resp, volver); return;
                }
                String temp = Formato.passwordTemporal();
                miembros.registrarPassword(id, Password.hash(temp), true);
                var s = req.getSession();
                s.setAttribute("claveGenerada", temp);
                s.setAttribute("claveCorreo", correo != null ? correo : actual.getCorreo());
                s.setAttribute("claveNombre", actual.getNombre());
                ir(req, resp, volver + "#acceso");
                return;
            }
            default -> { }
        }
        // guardar (crear o editar)
        Miembro m = new Miembro();
        m.setIdMiembro(id == null ? 0 : id);
        m.setNombre(texto(req, "nombre"));
        m.setNombreCompleto(texto(req, "nombreCompleto"));
        m.setCodigoPucp(texto(req, "codigoPucp"));
        m.setFacultad(texto(req, "facultad"));
        m.setCorreo(texto(req, "correo"));
        m.setCelular(texto(req, "celular"));
        m.setRolBaile(List.of("LEADER", "FOLLOWER").contains(req.getParameter("rolBaile")) ? req.getParameter("rolBaile") : null);
        String fr = req.getParameter("frecuencia");
        m.setFrecuencia(List.of("REGULAR", "OCASIONAL", "EVENTUAL").contains(fr) ? fr : "REGULAR");
        m.setFechaIngreso(fecha(req, "fechaIngreso"));
        boolean equipo = check(req, "rolEquipo"), admin = check(req, "rolAdmin");

        String error = null;
        if (m.getNombre() == null || m.getNombreCompleto() == null) error = "El nombre y el nombre completo son obligatorios.";
        else if (m.getRolBaile() == null) error = "Elige el rol de baile (Leader o Follower).";
        else if (m.getCorreo() == null || !m.getCorreo().toLowerCase().endsWith("@pucp.edu.pe")) error = "El correo debe ser PUCP (@pucp.edu.pe).";
        else if (m.getCodigoPucp() != null && !m.getCodigoPucp().matches("\\d{8}")) error = "El código PUCP debe tener 8 dígitos.";
        else if (miembros.existeNombre(m.getNombre(), m.getIdMiembro())) error = "Ya existe un miembro con ese nombre en el sistema.";
        else if (miembros.existeCorreo(m.getCorreo(), m.getIdMiembro())) error = "Ese correo ya lo usa otro miembro.";
        if (error != null) { Sesion.error(req, error); ir(req, resp, volver); return; }

        try {
            if (id == null) {
                String temp = texto(req, "passwordTemporal") == null ? Formato.passwordTemporal() : texto(req, "passwordTemporal");
                int nuevo = miembros.crear(m, Password.hash(temp), equipo, admin, yo);
                Sesion.ok(req, m.getNombre() + " ya es parte del sistema. Contraseña temporal: " + temp);
                ir(req, resp, "/admin/miembros/editar?id=" + nuevo);
            } else {
                miembros.actualizar(m, equipo, admin, yo);
                Sesion.ok(req, "Cambios guardados.");
                ir(req, resp, volver);
            }
        } catch (SQLException ex) {
            log("No se pudo guardar", ex); Sesion.error(req, String.valueOf(ex.getMessage()).contains("admin") ? "Debe quedar al menos un admin." : "No se pudo guardar: " + mensajeBd(ex));
            ir(req, resp, volver);
        }
    }

    private void postFigura(HttpServletRequest req, HttpServletResponse resp, int yo) throws SQLException, IOException {
        if (!Sesion.requiere(req, resp, "GESTIONAR_FIGURAS")) return;
        Integer id = entero(req, "idFigura");
        String volver = id == null ? "/admin/figuras/nueva" : "/admin/figuras/editar?id=" + id;
        switch (accion(req, "guardar")) {
            case "desactivar" -> { figuras.desactivar(id, yo); Sesion.ok(req, "Figura desactivada. Ya no aparece en la mano ni en los cálculos."); ir(req, resp, "/admin/figuras"); return; }
            case "reactivar" -> { figuras.reactivar(id); Sesion.ok(req, "Figura reactivada."); ir(req, resp, "/admin/figuras"); return; }
            case "eliminar" -> {
                if (figuras.eliminar(id)) { Sesion.ok(req, "Figura eliminada."); ir(req, resp, "/admin/figuras"); }
                else { Sesion.error(req, "No se puede eliminar: ya fue evaluada o enseñada. Desactívala."); ir(req, resp, volver); }
                return;
            }
            default -> { }
        }
        String nombre = texto(req, "nombre");
        if (nombre == null) { Sesion.error(req, "Escribe el nombre de la figura."); ir(req, resp, volver); return; }
        if (figuras.existeNombre(nombre, id == null ? 0 : id)) { Sesion.error(req, "Ya hay una figura activa con ese nombre."); ir(req, resp, volver); return; }
        Integer despues = entero(req, "despuesDe");
        if (id == null) {
            int nuevo = figuras.crear(nombre, texto(req, "descripcion"), despues, yo);
            Sesion.ok(req, "Figura “" + nombre + "” agregada. Tendrá color cuando el grupo la evalúe.");
            ir(req, resp, "/admin/figuras/editar?id=" + nuevo);
        } else {
            figuras.actualizar(id, nombre, texto(req, "descripcion"), despues);
            Sesion.ok(req, "Cambios guardados.");
            ir(req, resp, volver);
        }
    }
}
