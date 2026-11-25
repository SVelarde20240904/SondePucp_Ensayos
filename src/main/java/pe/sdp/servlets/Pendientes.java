package pe.sdp.servlets;

import pe.sdp.beans.Cuota;
import pe.sdp.beans.Ensayo;
import pe.sdp.beans.Taller;
import pe.sdp.daos.*;
import pe.sdp.util.Formato;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;

/**
 * Todo lo que el miembro tiene por hacer, en una sola lista (pestaña Pendientes y resumen en Inicio):
 * confirmar ensayos y talleres, autoevaluaciones (ensayo y mensual), formulario post-taller, cuota y responsabilidades.
 * Cada item: icono, titulo, detalle, url, boton, tono (bad = urgente, warn = pendiente, info = cuando puedas).
 */
public final class Pendientes {

    private Pendientes() { }

    public static List<Map<String, String>> de(int yo) throws SQLException {
        List<Map<String, String>> l = new ArrayList<>();
        EnsayoDao ensayos = new EnsayoDao();
        EvaluacionDao evaluaciones = new EvaluacionDao();
        TallerDao talleres = new TallerDao();

        Ensayo pe = ensayos.autoevaluacionPendiente(yo);
        if (pe != null)
            l.add(item("⭐", "Autoevaluación del ensayo del " + pe.getFechaCorta(), "Marca las figuras que viste ese día · toma un minuto",
                    "/autoevaluacion?ensayo=" + pe.getIdEnsayo(), "Autoevaluarme", "warn"));

        int periodo = evaluaciones.periodoActual();
        LocalDateTime[] v = evaluaciones.ventanaPeriodo(periodo);
        LocalDateTime ahora = LocalDateTime.now(Formato.ZONA);
        if (!ahora.isBefore(v[0]) && !ahora.isAfter(v[1]) && !evaluaciones.respondioPeriodo(yo, periodo))
            l.add(item("📋", "Autoevaluación mensual", "Cierra el " + Formato.fechaLarga(v[1].toLocalDate()) + " · toma ~3 min",
                    "/autoevaluacion?tipo=mensual", "Autoevaluarme", "warn"));

        for (Taller t : talleres.formulariosPendientes(yo))
            l.add(item("💬", "Feedback del " + t.getTitulo() + " (" + t.getFechaCorta() + ")", "Pasos ✅/❌ y feedback para tu compañero(a) · plazo: " + t.getPlazoTexto(),
                    "/talleres/formulario?id=" + t.getIdTaller(), "Llenarlo", "warn"));

        // cuota: deuda vencida, o la del mes si todavía no la paga
        CuotaDao cuotas = new CuotaDao();
        cuotas.generar(false);
        BigDecimal deuda = BigDecimal.ZERO; int meses = 0; Cuota actual = null;
        YearMonth ym = YearMonth.now(Formato.ZONA);
        for (Cuota c : cuotas.deMiembro(yo)) {
            if (c.isEnRevision()) continue;
            if (c.getYm().equals(ym) && c.isPendiente()) actual = c;
            else if (c.isVencida()) { deuda = deuda.add(c.getMontoHoy()); meses++; }
        }
        if (deuda.signum() > 0)
            l.add(item("💸", "Cuota atrasada: S/ " + Formato.soles(deuda), meses + (meses == 1 ? " mes" : " meses") + " · cada mes atrasado sube a S/ 7.50 y luego a S/ 9",
                    "/mi-cuota", "Pagar", "bad"));
        else if (actual != null)
            l.add(item("💰", "Cuota de " + actual.getMesTexto().toLowerCase() + ": S/ " + actual.getMontoTexto(), "Hasta el " + actual.getVenceTexto() + " es S/ 6",
                    "/mi-cuota", "Pagar", "info"));

        if (Cuota.anualAbierto(ym.atDay(1)) && cuotas.anualPosible(yo, ym.getYear()))
            l.add(item("🎉", "Paga todo " + ym.getYear() + " por S/ 66", "Solo en enero · te ahorras un mes", "/mi-cuota", "Ver", "info"));

        // confirmar asistencia (ensayos y talleres) que aún no respondo
        for (Ensayo e : ensayos.proximos(yo, 3))
            if ("SIN_RESPUESTA".equals(e.getMiConfirmacion()) && (e.getCierreConfirmacion() == null || ahora.isBefore(e.getCierreConfirmacion())))
                l.add(item("🗓", "¿Vas al ensayo del " + e.getFechaCorta() + "?", e.getTipoTexto() + " · " + e.getHorario() + " · " + e.getLugar(),
                        "/confirmar-asistencia", "Responder", "info"));
        for (Taller t : talleres.proximosParaMi(yo, 2))
            if ("SIN_RESPUESTA".equals(t.getMiConfirmacion()))
                l.add(item("🎓", "¿Vas al " + t.getTitulo() + " del " + t.getFechaCorta() + "?", (t.getMiPapel() == null ? "" : t.getMiPapel() + " · ") + t.getHorario(),
                        "/confirmar-asistencia", "Responder", "info"));

        // Externos del taller del jueves: a todos mientras el Form está abierto; al encargado (y a mod/admin de RRPP si se atrasa), publicar y enviar
        RrppDao rrpp = new RrppDao();
        Map<String, Object> sem = rrpp.semana(RrppDao.juevesActual());
        if (Boolean.TRUE.equals(sem.get("abierto")))
            l.add(0, item("🎟️", "¿Traes a alguien al taller del jueves " + sem.get("juevesCorto") + "?", "Regístralo en el Form antes de que RRPP envíe la lista a la DAES (a más tardar el " + sem.get("envioCorto") + ")",
                    "/externos", "Registrar", "info"));
        boolean encargado = Objects.equals(sem.get("idEncargado"), yo);
        boolean modRrpp = !encargado && (Boolean.TRUE.equals(sem.get("linkAtrasado")) || Boolean.TRUE.equals(sem.get("envioAtrasado"))) && rrpp.esModerador(yo);
        if (encargado || modRrpp) {
            if (Boolean.TRUE.equals(sem.get("faltaLink")))
                l.add(0, item("📣", "Publica el link de externos del jueves " + sem.get("juevesCorto"),
                        (Boolean.TRUE.equals(sem.get("linkAtrasado")) ? "¡Atrasado! Tocaba el " : "Toca hoy · ") + sem.get("publicarTexto") + (modRrpp ? " · encargado: " + sem.get("encargado") : ""),
                        "/rrpp/externos", "Publicar", Boolean.TRUE.equals(sem.get("linkAtrasado")) ? "bad" : "warn"));
            if (Boolean.TRUE.equals(sem.get("faltaEnvio")))
                l.add(0, item("📨", "Envía la lista de externos a la DAES", (Boolean.TRUE.equals(sem.get("envioAtrasado")) ? "¡No se envió! Era hasta el " : "Hoy, sin falta · al marcarla como enviada se cierra el Form · ") + sem.get("envioTexto")
                        + (modRrpp ? " · encargado: " + sem.get("encargado") : ""), "/rrpp/externos", "Ya la envié", Boolean.TRUE.equals(sem.get("envioAtrasado")) ? "bad" : "warn"));
        }
        // Visitas formales: responder la convocatoria, y recordatorio si voy y es hoy o mañana
        for (Map<String, Object> vv : rrpp.visitasPendientes(yo)) {
            boolean voy = "VOY".equals(vv.get("miRespuesta"));
            l.add(item("🚌", (voy ? vv.get("cuando") + ": visita " : "¿Vas a la visita? ") + vv.get("titulo"),
                    vv.get("fechaTexto") + (vv.get("puntoEncuentro") == null ? "" : " · encuentro: " + vv.get("puntoEncuentro") + (vv.get("horaEncuentroTexto").toString().isEmpty() ? "" : " " + vv.get("horaEncuentroTexto"))),
                    "/visitas#v" + vv.get("id"), voy ? "Ver" : "Responder", voy ? "warn" : "info"));
        }
        // Encuestas abiertas donde no voté
        for (Map<String, Object> e : new EncuestaDao().sinVotar(yo))
            l.add(item("📊", "Encuesta: " + e.get("pregunta"), "Cierra el " + e.get("cierreTexto"), "/encuestas/ver?id=" + e.get("id"), "Votar", "info"));

        // Comunica: saludos de cumpleaños que me tocan (atrasados o de los próximos días) y proyectos por vencer
        ComunicaDao comunica = new ComunicaDao();
        comunica.generarSiHaceFalta();
        for (Map<String, Object> s : comunica.misSaludosPendientes(yo, 3))
            l.add(item("🎂", "Saludo de cumpleaños: " + s.get("comoLlamarlo") + " (" + s.get("fechaTexto") + ")",
                    s.get("cuando") + " · " + s.get("estadoTexto"), "/comunica/saludo?id=" + s.get("id"), "Abrir",
                    Boolean.TRUE.equals(s.get("atrasado")) || Boolean.TRUE.equals(s.get("urgente")) ? "warn" : "info"));
        for (Map<String, Object> p : comunica.misProyectosPorVencer(yo, 2))
            l.add(item(String.valueOf(p.get("tipoIcono")), String.valueOf(p.get("titulo")),
                    (Boolean.TRUE.equals(p.get("vencido")) ? "Se pasó la fecha: " : "Para el ") + p.get("fechaObjetivoTexto") + " · " + p.get("tipoTexto") + " en " + p.get("redTexto"),
                    "/comunica/proyecto?id=" + p.get("id"), "Abrir", Boolean.TRUE.equals(p.get("vencido")) ? "warn" : "info"));

        int resp = new EncargoDao().pendientesDe(yo);
        if (resp > 0)
            l.add(item("✅", resp + (resp == 1 ? " responsabilidad pendiente" : " responsabilidades pendientes"), "Lo que te toca esta semana",
                    "/mis-responsabilidades", "Ver", "info"));
        return l;
    }

    private static Map<String, String> item(String icono, String titulo, String detalle, String url, String boton, String tono) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("icono", icono); m.put("titulo", titulo); m.put("detalle", detalle); m.put("url", url); m.put("boton", boton); m.put("tono", tono);
        return m;
    }
}
