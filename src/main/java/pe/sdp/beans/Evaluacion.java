package pe.sdp.beans;

import pe.sdp.util.Formato;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Color actual de un miembro en una figura (una celda de la antigua MATRIZ). */
public class Evaluacion {

    private int idMiembro;
    private int idFigura;
    private String figura;
    private String color;
    private String colorFigura;
    private LocalDateTime fechaActualizacion;
    private String fuente;

    public int getIdMiembro() { return idMiembro; }
    public void setIdMiembro(int idMiembro) { this.idMiembro = idMiembro; }
    public int getIdFigura() { return idFigura; }
    public void setIdFigura(int idFigura) { this.idFigura = idFigura; }
    public String getFigura() { return figura; }
    public void setFigura(String figura) { this.figura = figura; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getColorFigura() { return colorFigura; }
    public void setColorFigura(String colorFigura) { this.colorFigura = colorFigura; }
    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(LocalDateTime fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }
    public String getFuente() { return fuente; }
    public void setFuente(String fuente) { this.fuente = fuente; }

    /** Le falta = su color es ROJO o AMARILLO. */
    public boolean isLeFalta() { return "ROJO".equals(color) || "AMARILLO".equals(color); }
    public String getActualizacionTexto() { return fechaActualizacion == null ? "—" : Formato.ddmmyyyy(fechaActualizacion.toLocalDate()) + (fuente == null ? "" : " · " + Formato.capital(fuente)); }

}
