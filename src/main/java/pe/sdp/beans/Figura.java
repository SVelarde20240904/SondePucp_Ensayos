package pe.sdp.beans;

import pe.sdp.util.Formato;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Figura de la mano de figuras. */
public class Figura {

    private int idFigura;
    private String nombre;
    private int orden;
    private boolean activa;
    private String descripcion;
    private LocalDateTime fechaBaja;
    private String bajaPor;
    private String color;
    private int nEvaluaciones;
    private int nEnsenada;
    private boolean puedeEliminar;
    private int porcentajeSaben;
    private String videoUrl;

    public int getIdFigura() { return idFigura; }
    public void setIdFigura(int idFigura) { this.idFigura = idFigura; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public int getOrden() { return orden; }
    public void setOrden(int orden) { this.orden = orden; }
    public boolean isActiva() { return activa; }
    public void setActiva(boolean activa) { this.activa = activa; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public LocalDateTime getFechaBaja() { return fechaBaja; }
    public void setFechaBaja(LocalDateTime fechaBaja) { this.fechaBaja = fechaBaja; }
    public String getBajaPor() { return bajaPor; }
    public void setBajaPor(String bajaPor) { this.bajaPor = bajaPor; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public int getNEvaluaciones() { return nEvaluaciones; }
    public void setNEvaluaciones(int nEvaluaciones) { this.nEvaluaciones = nEvaluaciones; }
    public int getNEnsenada() { return nEnsenada; }
    public void setNEnsenada(int nEnsenada) { this.nEnsenada = nEnsenada; }
    public boolean isPuedeEliminar() { return puedeEliminar; }
    public void setPuedeEliminar(boolean puedeEliminar) { this.puedeEliminar = puedeEliminar; }
    public int getPorcentajeSaben() { return porcentajeSaben; }
    public void setPorcentajeSaben(int porcentajeSaben) { this.porcentajeSaben = porcentajeSaben; }

    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
    public boolean isTieneVideo() { return videoUrl != null && !videoUrl.isBlank(); }
    /** URL para mostrar el video dentro de la página (Drive o YouTube); null si no se puede embeber. */
    public String getVideoEmbed() { return Formato.videoEmbed(videoUrl); }
    public String getVideoOrigen() {
        if (!isTieneVideo()) return null;
        return videoUrl.contains("drive.google.com") ? "Google Drive" : (videoUrl.contains("youtu") ? "YouTube" : "el enlace");
    }

    public String getFechaBajaTexto() { return fechaBaja == null ? "" : Formato.ddmmyyyy(fechaBaja.toLocalDate()); }

}
