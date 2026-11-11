package pe.sdp.beans;

import pe.sdp.util.Formato;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;

/**
 * Cuota mensual de un miembro.
 * Regla: S/ 6 hasta el último día del mes · S/ 7.50 desde el día 1 del mes siguiente · S/ 9 desde el día 8 (tope).
 * Pago anual: S/ 66 por los 12 meses, solo en enero.
 * Una cuota puede tener un monto fijo (deudas que vienen del Excel) y abonos parciales.
 */
public class Cuota {

    public static final BigDecimal MONTO = new BigDecimal("6"), TARDE = new BigDecimal("7.50"), RECARGO = new BigDecimal("9"),
            ANUAL = new BigDecimal("66"), ANUAL_MES = new BigDecimal("5.50");
    /** Desde aquí la web crea las cuotas sola (lo anterior viene del Excel). */
    public static final YearMonth INICIO = YearMonth.of(2026, 11);

    private int idCuota;
    private int idMiembro;
    private String nombre;
    private String celular;
    private Integer idCobrador;
    private String cobrador;
    private boolean miembroActivo = true;
    private int anio;
    private int mes;
    private String estado;          // PENDIENTE · PAGADA · EXONERADA
    private BigDecimal montoFijo;
    private BigDecimal abonado = BigDecimal.ZERO;
    private BigDecimal montoPagado;
    private Integer idPago;
    private String pagoEstado;      // POR_CONFIRMAR · CONFIRMADO · RECHAZADO
    private LocalDateTime pagoFecha;
    private String pagoMedio;
    private boolean pagoAnual;
    private String nota;

    /** Cuánto cuesta la cuota de ese mes si se paga en la fecha ref. */
    public static BigDecimal montoAl(YearMonth ym, LocalDate ref) {
        if (!ref.isAfter(ym.atEndOfMonth())) return MONTO;
        if (ref.isBefore(ym.plusMonths(1).atDay(8))) return TARDE;
        return RECARGO;
    }

    /** El pago anual solo se abre en enero. */
    public static boolean anualAbierto(LocalDate hoy) { return hoy.getMonth() == Month.JANUARY; }

    public static String mesTexto(YearMonth ym) {
        return Formato.capital(ym.getMonth().getDisplayName(TextStyle.FULL, Formato.ES)) + " " + ym.getYear();
    }

    public static String mesCorto(YearMonth ym) {
        return Formato.capital(ym.getMonth().getDisplayName(TextStyle.SHORT, Formato.ES).replace(".", "")) + " " + String.valueOf(ym.getYear()).substring(2);
    }

    public YearMonth getYm() { return YearMonth.of(anio, mes); }
    public String getMesTexto() { return mesTexto(getYm()); }
    public String getMesCorto() { return mesCorto(getYm()); }
    public String getClave() { return getYm().toString(); }
    public LocalDate getVence() { return getYm().atEndOfMonth(); }
    public String getVenceTexto() { return Formato.diaMes(getVence()); }

    public boolean isPagada() { return "PAGADA".equals(estado); }
    public boolean isExonerada() { return "EXONERADA".equals(estado); }
    public boolean isPendiente() { return "PENDIENTE".equals(estado); }
    public boolean isEnRevision() { return isPendiente() && "POR_CONFIRMAR".equals(pagoEstado); }
    public boolean isVencida() { return isPendiente() && hoy().isAfter(getVence()); }
    public boolean isParcial() { return isPendiente() && abonado != null && abonado.signum() > 0; }

    /** Lo que vale la cuota completa si se paga en esa fecha (monto fijo si lo tiene). */
    public BigDecimal montoCompleto(LocalDate ref) { return montoFijo != null ? montoFijo : montoAl(getYm(), ref); }

    /** Lo que falta pagar en esa fecha. */
    public BigDecimal falta(LocalDate ref) {
        BigDecimal f = montoCompleto(ref).subtract(abonado == null ? BigDecimal.ZERO : abonado);
        return f.signum() < 0 ? BigDecimal.ZERO : f;
    }

    /** Pendiente: lo que falta hoy (o a la fecha de la captura). Pagada: lo que se pagó. */
    public BigDecimal getMontoHoy() {
        if (isExonerada()) return BigDecimal.ZERO;
        if (isPagada()) return montoPagado == null ? BigDecimal.ZERO : montoPagado;
        if (isEnRevision() && pagoFecha != null) return falta(pagoFecha.toLocalDate());
        return falta(hoy());
    }
    public String getMontoTexto() { return Formato.soles(getMontoHoy()); }
    public String getAbonadoTexto() { return Formato.soles(abonado); }

    /** Para el color: PAGADA · EXONERADA · REVISION · AL_DIA (6) · TARDE (7.50) · RECARGO (9 o deuda fija). */
    public String getSituacion() {
        if (isPagada()) return "PAGADA";
        if (isExonerada()) return "EXONERADA";
        if (isEnRevision()) return "REVISION";
        if (montoFijo != null) return isVencida() ? "RECARGO" : "AL_DIA";
        BigDecimal m = montoAl(getYm(), hoy());
        return m.compareTo(MONTO) == 0 ? "AL_DIA" : (m.compareTo(TARDE) == 0 ? "TARDE" : "RECARGO");
    }

    public String getSituacionTexto() {
        if (isParcial() && !isEnRevision()) return "Abonó S/ " + getAbonadoTexto();
        return switch (getSituacion()) {
            case "PAGADA" -> "Pagada";
            case "EXONERADA" -> "Exonerada";
            case "REVISION" -> "En revisión";
            case "AL_DIA" -> "Pendiente";
            case "TARDE" -> "Vencida";
            default -> "Deuda";
        };
    }

    public String getTono() {
        return switch (getSituacion()) {
            case "PAGADA" -> "ok";
            case "REVISION" -> "info";
            case "TARDE" -> "warn";
            case "RECARGO" -> "bad";
            default -> "neutral";
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
    public Integer getIdCobrador() { return idCobrador; }
    public void setIdCobrador(Integer idCobrador) { this.idCobrador = idCobrador; }
    public String getCobrador() { return cobrador; }
    public void setCobrador(String cobrador) { this.cobrador = cobrador; }
    public boolean isMiembroActivo() { return miembroActivo; }
    public void setMiembroActivo(boolean miembroActivo) { this.miembroActivo = miembroActivo; }
    public int getAnio() { return anio; }
    public void setAnio(int anio) { this.anio = anio; }
    public int getMes() { return mes; }
    public void setMes(int mes) { this.mes = mes; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public BigDecimal getMontoFijo() { return montoFijo; }
    public void setMontoFijo(BigDecimal montoFijo) { this.montoFijo = montoFijo; }
    public BigDecimal getAbonado() { return abonado; }
    public void setAbonado(BigDecimal abonado) { this.abonado = abonado == null ? BigDecimal.ZERO : abonado; }
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
    public boolean isPagoAnual() { return pagoAnual; }
    public void setPagoAnual(boolean pagoAnual) { this.pagoAnual = pagoAnual; }
    public String getNota() { return nota; }
    public void setNota(String nota) { this.nota = nota; }
}
