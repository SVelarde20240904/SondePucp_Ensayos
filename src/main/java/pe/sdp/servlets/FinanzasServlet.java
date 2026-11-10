package pe.sdp.servlets;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import pe.sdp.beans.Cuota;
import pe.sdp.beans.Miembro;
import pe.sdp.beans.PagoCuota;
import pe.sdp.daos.CuotaDao;
import pe.sdp.daos.MiembroDao;
import pe.sdp.util.Formato;
import pe.sdp.util.Sesion;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

/**
 * Módulo Finanzas · cuotas.
 *  /mi-cuota                 cada miembro: sus cuotas, su deuda y subir la captura del Yape
 *  /finanzas                 resumen: capturas por confirmar, deudores y recordatorios
 *  /finanzas/matriz          miembros × meses
 *  /finanzas/mes?m=2026-11   un mes: marcar varios como pagados de una vez
 *  /finanzas/miembro?id=     cuenta de un miembro: pagar, exonerar, anular, desde qué mes paga
 *  /finanzas/voucher?id=     la captura (solo Finanzas o el dueño)
 */
@WebServlet(urlPatterns = {"/mi-cuota", "/finanzas", "/finanzas/*"})
@MultipartConfig(maxFileSize = 8 * 1024 * 1024, maxRequestSize = 9 * 1024 * 1024)
public class FinanzasServlet extends BaseServlet {

    private static final String PERM = "GESTIONAR_CUOTAS";
    // las capturas se procesan en memoria (el usuario de Tomcat no siempre puede escribir en /tmp)
    static { ImageIO.setUseCache(false); }
    private final CuotaDao dao = new CuotaDao();
    private final MiembroDao miembros = new MiembroDao();

    private String ruta(HttpServletRequest req) {
        if ("/mi-cuota".equals(req.getServletPath())) return "/mi-cuota";
        return req.getPathInfo() == null ? "/" : req.getPathInfo();
    }

    private static LocalDate hoy() { return LocalDate.now(Formato.ZONA); }

    /** Lo que muestra la tarjeta de Inicio: la cuota de este mes, la deuda y si hay algo en revisión. */
    public static void resumenCuota(HttpServletRequest req, int yo) throws SQLException {
        CuotaDao d = new CuotaDao();
        d.generar(false);
        List<Cuota> mias = d.deMiembro(yo);
        Cuota actual = null; int deuda = 0, mesesDeuda = 0, enRevision = 0;
        YearMonth ym = YearMonth.from(hoy());
        for (Cuota c : mias) {
            if (c.getYm().equals(ym)) actual = c;
            if (c.isEnRevision()) enRevision++;
            else if (c.isVencida()) { deuda += c.getMontoHoy(); mesesDeuda++; }
        }
        req.setAttribute("cuotaActual", actual);
        req.setAttribute("cuotaDeuda", deuda);
        req.setAttribute("cuotaMesesDeuda", mesesDeuda);
        req.setAttribute("cuotaEnRevision", enRevision);
        req.setAttribute("hayCuotas", !mias.isEmpty());
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int yo = Sesion.idUsuario(req);
        try {
            String r = ruta(req);
            if ("/mi-cuota".equals(r)) { miCuota(req, resp, yo); return; }
            if ("/voucher".equals(r)) { voucher(req, resp, yo); return; }
            if (!Sesion.requiere(req, resp, PERM)) return;
            dao.generar(false);
            switch (r) {
                case "/" -> {
                    YearMonth ym = YearMonth.from(hoy());
                    List<Cuota> todas = dao.todas();
                    // deudores: cuotas vencidas sin captura en revisión, agrupadas por persona
                    Map<Integer, List<Cuota>> deudas = new LinkedHashMap<>();
                    int pagaronMes = 0, totalMes = 0, deudaTotal = 0;
                    for (Cuota c : todas) {
                        if (c.getYm().equals(ym) && !c.isExonerada()) { totalMes++; if (c.isPagada()) pagaronMes++; }
                        if (c.isVencida() && !c.isEnRevision()) { deudas.computeIfAbsent(c.getIdMiembro(), k -> new ArrayList<>()).add(c); deudaTotal += c.getMontoHoy(); }
                    }
                    List<Map<String, Object>> deudores = new ArrayList<>();
                    for (List<Cuota> l : deudas.values()) deudores.add(deudor(l));
                    deudores.sort((a, b) -> Integer.compare((int) b.get("total"), (int) a.get("total")));
                    req.setAttribute("porConfirmar", dao.porConfirmar());
                    req.setAttribute("deudores", deudores);
                    req.setAttribute("deudaTotal", deudaTotal);
                    req.setAttribute("mesTexto", Cuota.mesTexto(ym));
                    req.setAttribute("mesClave", ym.toString());
                    req.setAttribute("pagaronMes", pagaronMes);
                    req.setAttribute("totalMes", totalMes);
                    req.setAttribute("recaudadoMes", dao.recaudadoEn(ym));
                    req.setAttribute("recordatorioGeneral", recordatorioGeneral(ym));
                    vista(req, resp, "finanzas/resumen");
                }
                case "/matriz" -> {
                    List<YearMonth> meses = dao.meses();
                    Map<Integer, Map<String, Cuota>> filas = new LinkedHashMap<>();
                    Map<Integer, String> nombres = new LinkedHashMap<>();
                    Map<Integer, Integer> deudaDe = new HashMap<>();
                    for (Cuota c : dao.todas()) {
                        filas.computeIfAbsent(c.getIdMiembro(), k -> new HashMap<>()).put(c.getClave(), c);
                        nombres.put(c.getIdMiembro(), c.getNombre() + (c.isMiembroActivo() ? "" : " (inactivo)"));
                        if (c.isVencida() && !c.isEnRevision()) deudaDe.merge(c.getIdMiembro(), c.getMontoHoy(), Integer::sum);
                    }
                    List<String> claves = new ArrayList<>(); List<String> titulos = new ArrayList<>();
                    for (YearMonth ym : meses) { claves.add(ym.toString()); titulos.add(Cuota.mesCorto(ym)); }
                    req.setAttribute("claves", claves);
                    req.setAttribute("titulos", titulos);
                    req.setAttribute("filas", filas);
                    req.setAttribute("nombres", nombres);
                    req.setAttribute("deudaDe", deudaDe);
                    vista(req, resp, "finanzas/matriz");
                }
                case "/mes" -> {
                    YearMonth ym = mesPedido(req);
                    List<YearMonth> meses = dao.meses();
                    req.setAttribute("ym", ym.toString());
                    req.setAttribute("mesTexto", Cuota.mesTexto(ym));
                    req.setAttribute("meses", meses.stream().map(YearMonth::toString).toList());
                    req.setAttribute("mesesTexto", meses.stream().map(Cuota::mesTexto).toList());
                    req.setAttribute("cuotas", dao.delMes(ym));
                    req.setAttribute("hoy", hoy().toString());
                    vista(req, resp, "finanzas/mes");
                }
                case "/miembro" -> {
                    Integer id = entero(req, "id");
                    Miembro m = id == null ? null : miembros.obtener(id);
                    if (m == null) { Sesion.error(req, "No encontré a ese miembro."); ir(req, resp, "/finanzas"); return; }
                    List<Cuota> cuotas = dao.deMiembro(id);
                    req.setAttribute("m", m);
                    req.setAttribute("cuotas", cuotas);
                    req.setAttribute("deuda", deudor(cuotas.stream().filter(c -> c.isVencida() && !c.isEnRevision()).toList()));
                    req.setAttribute("desde", dao.desdeDe(id).toString());
                    req.setAttribute("desdeTexto", Cuota.mesTexto(dao.desdeDe(id)));
                    req.setAttribute("desdeManual", dao.desdeManual(id));
                    req.setAttribute("hoy", hoy().toString());
                    vista(req, resp, "finanzas/miembro");
                }
                default -> resp.sendError(404);
            }
        } catch (SQLException e) { fallo(req, resp, e); }
    }

    private YearMonth mesPedido(HttpServletRequest req) {
        try { String m = req.getParameter("m"); if (m != null) return YearMonth.parse(m); } catch (Exception ignored) { }
        return YearMonth.from(hoy());
    }

    private void miCuota(HttpServletRequest req, HttpServletResponse resp, int yo) throws SQLException, ServletException, IOException {
        dao.generar(false);
        List<Cuota> cuotas = dao.deMiembro(yo);
        List<Cuota> porPagar = new ArrayList<>();
        int deuda = 0;
        for (Cuota c : cuotas) if (c.isPendiente() && !c.isEnRevision()) { porPagar.add(c); deuda += c.getMontoHoy(); }
        Collections.reverse(porPagar);   // la más vieja primero
        req.setAttribute("cuotas", cuotas);
        req.setAttribute("porPagar", porPagar);
        req.setAttribute("totalPorPagar", deuda);
        req.setAttribute("rechazos", dao.rechazosRecientes(yo));
        req.setAttribute("desdeTexto", Cuota.mesTexto(dao.desdeDe(yo)));
        req.setAttribute("montos", new int[]{Cuota.MONTO, Cuota.RECARGO, Cuota.TOPE});
        vista(req, resp, "finanzas/mi-cuota");
    }

    private void voucher(HttpServletRequest req, HttpServletResponse resp, int yo) throws SQLException, IOException {
        Integer id = entero(req, "id");
        Object[] v = id == null ? null : dao.voucher(id);
        if (v == null || v[1] == null || ((int) v[0] != yo && !Sesion.tiene(req, PERM))) { resp.sendError(404); return; }
        byte[] img = (byte[]) v[1];
        resp.setContentType("image/jpeg");
        resp.setHeader("Cache-Control", "private, max-age=3600");
        resp.setContentLength(img.length);
        resp.getOutputStream().write(img);
    }

    /** {id, nombre, celular, meses, total, mensaje, whatsapp} de alguien que debe. */
    private static Map<String, Object> deudor(List<Cuota> l) {
        Map<String, Object> d = new HashMap<>();
        if (l.isEmpty()) { d.put("total", 0); d.put("meses", ""); return d; }
        Cuota primera = l.get(0);
        int total = 0; StringBuilder meses = new StringBuilder();
        List<Cuota> orden = new ArrayList<>(l); orden.sort(Comparator.comparing(Cuota::getYm));
        for (Cuota c : orden) { total += c.getMontoHoy(); meses.append(meses.length() == 0 ? "" : ", ").append(c.getMesTexto().toLowerCase()).append(" (S/ ").append(c.getMontoHoy()).append(")"); }
        String nombre = primera.getNombre().split(" ")[0];
        String msg = "Hola " + nombre + "! Te escribo de Finanzas de SDP 💃🏻 Tienes pendiente la cuota de " + meses + ": en total S/ " + total + ". "
                + "Puedes yapear y subir tu captura en la web, en \"Mi cuota\". ¡Gracias!";
        d.put("id", primera.getIdMiembro());
        d.put("nombre", primera.getNombre());
        d.put("meses", meses.toString());
        d.put("nMeses", orden.size());
        d.put("total", total);
        d.put("mensaje", msg);
        String cel = primera.getCelular() == null ? "" : primera.getCelular().replaceAll("\\D", "");
        if (cel.length() == 9) cel = "51" + cel;
        d.put("whatsapp", cel.length() >= 11 ? "https://wa.me/" + cel + "?text=" + URLEncoder.encode(msg, StandardCharsets.UTF_8).replace("+", "%20") : null);
        return d;
    }

    private static String recordatorioGeneral(YearMonth ym) {
        return "📢 Recordatorio de Finanzas: la cuota de " + Cuota.mesTexto(ym).toLowerCase() + " es de S/ " + Cuota.MONTO
                + " hasta el " + Formato.diaMes(ym.atEndOfMonth()) + ". Desde el 1 de " + Cuota.mesTexto(ym.plusMonths(1)).toLowerCase().split(" ")[0]
                + " sube a S/ " + Cuota.RECARGO + " y desde el 15 a S/ " + Cuota.TOPE + ". "
                + "Yapea y sube tu captura en la web, en \"Mi cuota\". ¡Gracias! 💃🏻";
    }

    // ===================== POST =====================

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int yo = Sesion.idUsuario(req);
        try {
            String r = ruta(req);
            if ("/mi-cuota".equals(r)) { postMiCuota(req, resp, yo); return; }
            if (!Sesion.requiere(req, resp, PERM)) return;
            switch (r) {
                case "/" -> {
                    Integer idPago = entero(req, "idPago");
                    if (idPago != null && "confirmar".equals(accion(req, ""))) { dao.confirmar(idPago, yo); Sesion.ok(req, "Pago confirmado."); }
                    else if (idPago != null && "rechazar".equals(accion(req, ""))) {
                        String motivo = texto(req, "motivo");
                        if (motivo == null) { Sesion.error(req, "Escribe por qué rechazas la captura: el miembro lo va a ver."); }
                        else { dao.rechazar(idPago, motivo.length() > 200 ? motivo.substring(0, 200) : motivo, yo); Sesion.ok(req, "Captura rechazada. El miembro verá el motivo en \"Mi cuota\"."); }
                    }
                    ir(req, resp, "/finanzas");
                }
                case "/mes" -> {
                    YearMonth ym = mesPedido(req);
                    Map<Integer, List<Integer>> porMiembro = new LinkedHashMap<>();
                    String[] ids = req.getParameterValues("cuota");
                    if (ids != null) for (String s : ids) {
                        Integer idCuota = Formato.entero(s); if (idCuota == null) continue;
                        Cuota c = dao.obtener(idCuota);
                        if (c != null && c.isPendiente()) porMiembro.computeIfAbsent(c.getIdMiembro(), k -> new ArrayList<>()).add(idCuota);
                    }
                    LocalDate f = fechaPago(req);
                    int n = 0;
                    for (Map.Entry<Integer, List<Integer>> e : porMiembro.entrySet()) n += dao.registrarManual(e.getKey(), e.getValue(), f, medio(req), yo);
                    if (n == 0) Sesion.error(req, "No marcaste a nadie.");
                    else Sesion.ok(req, n == 1 ? "Se registró 1 pago." : "Se registraron " + n + " pagos.");
                    ir(req, resp, "/finanzas/mes?m=" + ym);
                }
                case "/miembro" -> {
                    Integer id = entero(req, "id");
                    if (id == null) { ir(req, resp, "/finanzas"); return; }
                    String volver = "/finanzas/miembro?id=" + id;
                    switch (accion(req, "")) {
                        case "pagar" -> {
                            List<Integer> ids = new ArrayList<>();
                            String[] vs = req.getParameterValues("cuota");
                            if (vs != null) for (String s : vs) { Integer x = Formato.entero(s); if (x != null) ids.add(x); }
                            int n = ids.isEmpty() ? 0 : dao.registrarManual(id, ids, fechaPago(req), medio(req), yo);
                            if (n == 0) Sesion.error(req, "Marca qué meses pagó."); else Sesion.ok(req, "Pago registrado (" + n + (n == 1 ? " mes)." : " meses)."));
                        }
                        case "exonerar" -> {
                            if (!Sesion.requiere(req, resp, "EXONERAR_CUOTAS")) return;
                            String nota = texto(req, "nota");
                            if (nota == null) Sesion.error(req, "Escribe el motivo de la exoneración.");
                            else { dao.exonerar(entero(req, "idCuota"), nota.length() > 200 ? nota.substring(0, 200) : nota, yo); Sesion.ok(req, "Cuota exonerada."); }
                        }
                        case "anular" -> {
                            if (!Sesion.requiere(req, resp, "EXONERAR_CUOTAS")) return;
                            dao.anular(entero(req, "idCuota"), yo); Sesion.ok(req, "La cuota volvió a pendiente.");
                        }
                        case "desde" -> {
                            String m = texto(req, "desde");
                            LocalDate d = null;
                            try { if (m != null) d = YearMonth.parse(m).atDay(1); } catch (Exception ignored) { }
                            dao.cambiarDesde(id, d);
                            Sesion.ok(req, d == null ? "Vuelve a la regla: paga desde el segundo mes después de entrar." : "Ahora paga desde " + Cuota.mesTexto(YearMonth.from(d)).toLowerCase() + ".");
                        }
                        default -> { }
                    }
                    ir(req, resp, volver);
                }
                default -> resp.sendError(404);
            }
        } catch (SQLException e) { fallo(req, resp, e); }
    }

    private LocalDate fechaPago(HttpServletRequest req) {
        LocalDate f = fecha(req, "fecha");
        return f == null || f.isAfter(hoy()) ? hoy() : f;
    }

    private static String medio(HttpServletRequest req) {
        String m = req.getParameter("medio");
        return List.of("YAPE", "EFECTIVO", "OTRO").contains(m) ? m : "YAPE";
    }

    private void postMiCuota(HttpServletRequest req, HttpServletResponse resp, int yo) throws SQLException, IOException, ServletException {
        List<Integer> ids = new ArrayList<>();
        String[] vs = req.getParameterValues("cuota");
        if (vs != null) for (String s : vs) { Integer x = Formato.entero(s); if (x != null) ids.add(x); }
        if (ids.isEmpty()) { Sesion.error(req, "Marca qué mes(es) estás pagando."); ir(req, resp, "/mi-cuota"); return; }
        Part foto;
        try { foto = req.getPart("voucher"); } catch (IllegalStateException e) { Sesion.error(req, "La imagen es muy pesada (máximo 8 MB)."); ir(req, resp, "/mi-cuota"); return; }
        if (foto == null || foto.getSize() == 0) { Sesion.error(req, "Sube la captura de tu Yape."); ir(req, resp, "/mi-cuota"); return; }
        byte[] img;
        try (InputStream in = foto.getInputStream()) { img = comprimir(in.readAllBytes()); }
        if (img == null) { Sesion.error(req, "No pude leer la imagen. Sube una captura en JPG o PNG."); ir(req, resp, "/mi-cuota"); return; }
        String op = texto(req, "operacion");
        if (op != null && op.length() > 40) op = op.substring(0, 40);
        int n = dao.reportar(yo, ids, img, op);
        if (n == 0) Sesion.error(req, "Esas cuotas ya estaban pagadas o en revisión.");
        else Sesion.ok(req, "¡Listo! Tu captura quedó en revisión. Cuando Finanzas la confirme, tu cuota sale como pagada.");
        ir(req, resp, "/mi-cuota");
    }

    /** Lee la imagen (JPG/PNG/GIF/BMP), la achica a máx. 1400 px y la guarda como JPG liviano. null si no es una imagen. */
    static byte[] comprimir(byte[] original) {
        try {
            BufferedImage src = ImageIO.read(new ByteArrayInputStream(original));
            if (src == null) return null;
            double k = Math.min(1.0, 1400.0 / Math.max(src.getWidth(), src.getHeight()));
            int w = Math.max(1, (int) Math.round(src.getWidth() * k)), h = Math.max(1, (int) Math.round(src.getHeight() * k));
            BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = dst.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setColor(java.awt.Color.WHITE); g.fillRect(0, 0, w, h);
            g.drawImage(src, 0, 0, w, h, null); g.dispose();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageWriter wr = ImageIO.getImageWritersByFormatName("jpg").next();
            ImageWriteParam p = wr.getDefaultWriteParam();
            p.setCompressionMode(ImageWriteParam.MODE_EXPLICIT); p.setCompressionQuality(0.82f);
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(out)) { wr.setOutput(ios); wr.write(null, new IIOImage(dst, null, null), p); }
            wr.dispose();
            return out.toByteArray();
        } catch (Exception | LinkageError e) {
            java.util.logging.Logger.getLogger("SDP").warning("SDP · no se pudo leer la captura: " + e);
            return null;
        }
    }
}
