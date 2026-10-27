package pe.sdp.beans;

import pe.sdp.util.Formato;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Un cambio de color (antes: hoja HISTORICO). */
public class HistorialEvaluacion {

    private int idHistorial;
    private int idMiembro;
    private String miembro;
    private int idFigura;
    private String figura;
    private String colorAnterior;
    private String colorNuevo;
    private String fuente;
    private boolean aplicado;
    private Integer idEnsayo;
    private String registradoPor;
    private String motivo;
    private LocalDateTime fecha;

    public int getIdHistorial() { return idHistorial; }
    public void setIdHistorial(int idHistorial) { this.idHistorial = idHistorial; }
    public int getIdMiembro() { return idMiembro; }
    public void setIdMiembro(int idMiembro) { this.idMiembro = idMiembro; }
    public String getMiembro() { return miembro; }
    public void setMiembro(String miembro) { this.miembro = miembro; }
    public int getIdFigura() { return idFigura; }
    public void setIdFigura(int idFigura) { this.idFigura = idFigura; }
    public String getFigura() { return figura; }
    public void setFigura(String figura) { this.figura = figura; }
    public String getColorAnterior() { return colorAnterior; }
    public void setColorAnterior(String colorAnterior) { this.colorAnterior = colorAnterior; }
    public String getColorNuevo() { return colorNuevo; }
    public void setColorNuevo(String colorNuevo) { this.colorNuevo = colorNuevo; }
    public String getFuente() { return fuente; }
    public void setFuente(String fuente) { this.fuente = fuente; }
    public boolean isAplicado() { return aplicado; }
    public void setAplicado(boolean aplicado) { this.aplicado = aplicado; }
    public Integer getIdEnsayo() { return idEnsayo; }
    public void setIdEnsayo(Integer idEnsayo) { this.idEnsayo = idEnsayo; }
    public String getRegistradoPor() { return registradoPor; }
    public void setRegistradoPor(String registradoPor) { this.registradoPor = registradoPor; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public String getFechaTexto() { return fecha == null ? "" : Formato.ddmmHora(fecha); }
    public String getFechaLarga() { return fecha == null ? "" : Formato.ddmmyyyy(fecha.toLocalDate()); }
    public String getFuenteTexto() {
        switch (fuente == null ? "" : fuente) {
            case "ENSAYO": return "Del ensayo";
            case "CORRECCION": return "Corrección";
            case "CARGA_INICIAL": return "Carga inicial";
            default: return "Mensual";
        }
    }
    public String getFuenteTono() {
        switch (fuente == null ? "" : fuente) {
            case "ENSAYO": return "info";
            case "CORRECCION": return "warn";
            default: return "neutral";
        }
    }

}
