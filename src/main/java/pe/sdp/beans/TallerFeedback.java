package pe.sdp.beans;

import pe.sdp.util.Formato;
import java.time.LocalDateTime;

/** Feedback del formulario post-taller (crudo, con nombre) o resumen anónimo que se le envía a alguien. */
public class TallerFeedback {

    private int id;
    private int idTaller;
    private int deMiembro;
    private String de;
    private Integer paraMiembro;
    private String para;
    private String tipo;          // COMPANERO, APOYO, TALLER · o RESUMEN
    private String texto;
    private LocalDateTime fecha;
    private boolean enviado;
    private String tallerTexto;   // para "Mi feedback"

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getIdTaller() { return idTaller; }
    public void setIdTaller(int idTaller) { this.idTaller = idTaller; }
    public int getDeMiembro() { return deMiembro; }
    public void setDeMiembro(int deMiembro) { this.deMiembro = deMiembro; }
    public String getDe() { return de; }
    public void setDe(String de) { this.de = de; }
    public Integer getParaMiembro() { return paraMiembro; }
    public void setParaMiembro(Integer paraMiembro) { this.paraMiembro = paraMiembro; }
    public String getPara() { return para; }
    public void setPara(String para) { this.para = para; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public boolean isEnviado() { return enviado; }
    public void setEnviado(boolean enviado) { this.enviado = enviado; }
    public String getTallerTexto() { return tallerTexto; }
    public void setTallerTexto(String tallerTexto) { this.tallerTexto = tallerTexto; }

    public String getFechaTexto() { return fecha == null ? "" : Formato.ddmmHora(fecha); }
    public String getTipoTexto() {
        return switch (tipo == null ? "" : tipo) { case "COMPANERO" -> "Compañero de dictado"; case "APOYO" -> "Apoyo"; case "TALLER" -> "Sugerencia"; default -> tipo; };
    }
}
