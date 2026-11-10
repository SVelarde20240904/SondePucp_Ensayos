package pe.sdp.beans;

import pe.sdp.util.Formato;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Un pago: una captura de Yape que manda el miembro, o un registro manual de Finanzas. Cubre uno o más meses. */
public class PagoCuota {
    private int idPago;
    private int idMiembro;
    private String nombre;
    private LocalDateTime fecha;
    private String operacion;
    private String medio;
    private String estado;
    private boolean voucher;
    private String motivoRechazo;
    private String revisadoPor;
    private final List<Cuota> cuotas = new ArrayList<>();

    /** Total según la fecha del pago (cada mes con su recargo). */
    public int getTotal() {
        int t = 0;
        for (Cuota c : cuotas) t += Cuota.montoAl(c.getYm(), fecha.toLocalDate());
        return t;
    }
    public String getMeses() {
        StringBuilder sb = new StringBuilder();
        for (Cuota c : cuotas) sb.append(sb.length() == 0 ? "" : ", ").append(c.getMesTexto());
        return sb.toString();
    }
    public String getFechaTexto() { return fecha == null ? "" : Formato.ddmmHora(fecha); }

    public int getIdPago() { return idPago; }
    public void setIdPago(int idPago) { this.idPago = idPago; }
    public int getIdMiembro() { return idMiembro; }
    public void setIdMiembro(int idMiembro) { this.idMiembro = idMiembro; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public String getOperacion() { return operacion; }
    public void setOperacion(String operacion) { this.operacion = operacion; }
    public String getMedio() { return medio; }
    public void setMedio(String medio) { this.medio = medio; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public boolean isVoucher() { return voucher; }
    public void setVoucher(boolean voucher) { this.voucher = voucher; }
    public String getMotivoRechazo() { return motivoRechazo; }
    public void setMotivoRechazo(String motivoRechazo) { this.motivoRechazo = motivoRechazo; }
    public String getRevisadoPor() { return revisadoPor; }
    public void setRevisadoPor(String revisadoPor) { this.revisadoPor = revisadoPor; }
    public List<Cuota> getCuotas() { return cuotas; }
}
