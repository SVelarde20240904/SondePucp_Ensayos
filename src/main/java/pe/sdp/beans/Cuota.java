package pe.sdp.beans;

import pe.sdp.util.Formato;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.TextStyle;

/**
 * Cuota mensual de un miembro.
 * Regla: S/ 6 hasta el último día del mes · S/ 9 desde el día 1 del mes siguiente · S/ 15 desde el día 15 del mes siguiente (tope).
 * El monto de una cuota pagada se calcula con la fecha en que se pagó (la de la captura, no la de la confirmación).
 */
public class Cuota {

    public static final int MONTO = 6, RECARGO = 9, TOPE = 15;
    /** Primer mes que maneja la web (antes de esto las cuotas se llevaban en Excel). */
    public static final YearMonth INICIO = YearMonth.of(2026, 11);

    private int idCuota;
    private int idMiembro;
    private String nombre;
    private String celular;
    private boolean miembroActivo = true;
    private int anio;
    private int mes;
    private String estado;          // PENDIENTE · PAGADA · EXONERADA
    private BigDecimal montoPagado;
    private Integer idPago;
    private String pagoEstado;      // POR_CONFIRMAR · CONFIRMADO · RECHAZADO (del pago amarrado)
    private LocalDateTime pagoFecha;
    private String pagoMedio;
    private String nota;

    /** Cuánto cuesta la cuota de ese mes si se paga en la fecha ref. */
    public static int montoAl(YearMonth ym, LocalDate ref) {
        if (!ref.isAfter(ym.atEndOfMonth())) return MONTO;
        if (ref.isBefore(ym.plusMonths(1).atDay(15))) return RECARGO;
        return TOPE;
    }

    public static String mesTexto(YearMonth ym) {
        return Formato.capital(ym.getMonth().getDisplayName(TextStyle.FULL, Formato.ES)) + " " + ym.getYear();
    }

    public static String mesCorto(YearMonth ym) {
        return Formato.capital(ym.getMonth().getDisplayName(TextStyle.SHORT, Formato.ES).replace(".", "")) + " " + String.valueOf(ym.getYear()).substring(2);
    }

    public YearMonth getYm() { return YearMonth.of(anio, mes); }
    public String getMesTexto() { return mesTexto(getYm()); }
    public String getMesCorto() { return mesCorto(getYm()); }
    public String getClave() { return anio + "-" + (mes < 10 ? "0" : "") + mes; }
    public LocalDate getVence() { return getYm().atEndOfMonth(); }
    public String getVenceTexto() { return Formato.diaMes(getVence()); }

    public boolean isPagada() { return "PAGADA".equals(estado); }
    public boolean isExonerada() { return "EXONERADA".equals(estado); }
    public boolean isPendiente() { return "PENDIENTE".equals(estado); }
    /** Pendiente pero con una captura esperando que Finanzas la confirme. */
    public boolean isEnRevision() { return isPendiente() && "POR_CONFIRMAR".equals(pagoEstado); }
    /** Pendiente y ya pasó el último día del mes. */
    public boolean isVencida() { return isPendiente() && hoy().isAfter(getVence()); }

    /** Lo que cuesta hoy (o lo que se pagó). */
    public int getMontoHoy() {
        if (isExonerada()) return 0;
        if (isPagada()) return montoPagado == null ? MONTO : montoPagado.intValue();
        if (isEnRevision() && pagoFecha != null) return montoAl(getYm(), pagoFecha.toLocalDate());
        return montoAl(getYm(), hoy());
    }

    /** Para el color: PAGADA · EXONERADA · REVISION · AL_DIA · RECARGO (9) · TOPE (15). */
    public String getSituacion() {
        if (isPagada()) return "PAGADA";
        if (isExonerada()) return "EXONERADA";
        if (isEnRevision()) return "REVISION";
        int m = getMontoHoy();
        return m == MONTO ? "AL_DIA" : (m == RECARGO ? "RECARGO" : "TOPE");
    }

    public String getSituacionTexto() {
        return switch (getSituacion()) {
            case "PAGADA" -> "Pagada";
            case "EXONERADA" -> "Exonerada";
            case "REVISION" -> "En revisión";
            case "AL_DIA" -> "Pendiente";
            case "RECARGO" -> "Vencida";
            default -> "Deuda";
        };
    }

    public String getTono() {
        return switch (getSituacion()) {
            case "PAGADA" -> "ok";
            case "EXONERADA" -> "neutral";
            case "REVISION" -> "info";
            case "AL_DIA" -> "neutral";
            case "RECARGO" -> "warn";
            default -> "bad";
        };
    }

    public String getPagoFechaTexto() { return pagoFecha == null ? "" : Formato.ddmmyyyy(pagoFecha.toLocalDate()); }

    private static LocalDate hoy() { return LocalDate.now(Formato.ZONA); }

    public int getIdCuota() { return idCuota; }
    public void setIdCuota(int idCuota) { this.idCuota = idCuota; }
    public int getIdMiembro() { return idMiembro; }
    public void setIdMiembro(int idMiembro) { this.idMiembro = idMiembro; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getCelular() { return celular; }
    public void setCelular(String celular) { this.celular = celular; }
    public boolean isMiembroActivo() { return miembroActivo; }
    public void setMiembroActivo(boolean miembroActivo) { this.miembroActivo = miembroActivo; }
    public int getAnio() { return anio; }
    public void setAnio(int anio) { this.anio = anio; }
    public int getMes() { return mes; }
    public void setMes(int mes) { this.mes = mes; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public BigDecimal getMontoPagado() { return montoPagado; }
    public void setMontoPagado(BigDecimal montoPagado) { this.montoPagado = montoPagado; }
    public Integer getIdPago() { return idPago; }
    public void setIdPago(Integer idPago) { this.idPago = idPago; }
    public String getPagoEstado() { return pagoEstado; }
    public void setPagoEstado(String pagoEstado) { this.pagoEstado = pagoEstado; }
    public LocalDateTime getPagoFecha() { return pagoFecha; }
    public void setPagoFecha(LocalDateTime pagoFecha) { this.pagoFecha = pagoFecha; }
    public String getPagoMedio() { return pagoMedio; }
    public void setPagoMedio(String pagoMedio) { this.pagoMedio = pagoMedio; }
    public String getNota() { return nota; }
    public void setNota(String nota) { this.nota = nota; }
}
