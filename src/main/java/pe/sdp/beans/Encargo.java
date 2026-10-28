package pe.sdp.beans;

import pe.sdp.util.Formato;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Una responsabilidad semanal (sistema nuevo: un estado + 'cumplido por'). */
public class Encargo {

    private int idEncargo;
    private int idTipo;
    private String tipo;
    private LocalDate semana;
    private int idResponsable;
    private String responsable;
    private LocalDateTime plazo;
    private Integer idCumplidoPor;
    private String cumplidoPor;
    private LocalDateTime fechaCumplimiento;
    private boolean justificada;
    private String motivoJustificacion;
    private String estado;
    private boolean cobertura;

    public int getIdEncargo() { return idEncargo; }
    public void setIdEncargo(int idEncargo) { this.idEncargo = idEncargo; }
    public int getIdTipo() { return idTipo; }
    public void setIdTipo(int idTipo) { this.idTipo = idTipo; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public LocalDate getSemana() { return semana; }
    public void setSemana(LocalDate semana) { this.semana = semana; }
    public int getIdResponsable() { return idResponsable; }
    public void setIdResponsable(int idResponsable) { this.idResponsable = idResponsable; }
    public String getResponsable() { return responsable; }
    public void setResponsable(String responsable) { this.responsable = responsable; }
    public LocalDateTime getPlazo() { return plazo; }
    public void setPlazo(LocalDateTime plazo) { this.plazo = plazo; }
    public Integer getIdCumplidoPor() { return idCumplidoPor; }
    public void setIdCumplidoPor(Integer idCumplidoPor) { this.idCumplidoPor = idCumplidoPor; }
    public String getCumplidoPor() { return cumplidoPor; }
    public void setCumplidoPor(String cumplidoPor) { this.cumplidoPor = cumplidoPor; }
    public LocalDateTime getFechaCumplimiento() { return fechaCumplimiento; }
    public void setFechaCumplimiento(LocalDateTime fechaCumplimiento) { this.fechaCumplimiento = fechaCumplimiento; }
    public boolean isJustificada() { return justificada; }
    public void setJustificada(boolean justificada) { this.justificada = justificada; }
    public String getMotivoJustificacion() { return motivoJustificacion; }
    public void setMotivoJustificacion(String motivoJustificacion) { this.motivoJustificacion = motivoJustificacion; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public boolean isCobertura() { return cobertura; }
    public void setCobertura(boolean cobertura) { this.cobertura = cobertura; }

    public String getPlazoTexto() { return plazo == null ? "—" : Formato.diaCorto(plazo.toLocalDate()) + " " + Formato.ddmmHora(plazo); }
    public String getCumplimientoTexto() { return fechaCumplimiento == null ? "" : Formato.diaCorto(fechaCumplimiento.toLocalDate()) + " " + Formato.ddmmHora(fechaCumplimiento); }
    public String getEstadoTexto() { return Formato.capital(estado == null ? null : estado.replace('_', ' ')); }
    public String getSemanaTexto() { return Formato.ddmmyyyy(semana); }
    public String getEstadoTono() {
        switch (estado == null ? "" : estado) {
            case "A_TIEMPO": return "ok";
            case "FUERA_DE_PLAZO": return "warn";
            case "NO_CUMPLIDA": return "bad";
            default: return "neutral";
        }
    }

}
