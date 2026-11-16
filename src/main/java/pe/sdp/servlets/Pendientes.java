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
