package pe.sdp.beans;

import pe.sdp.util.Formato;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Fila de 'Figuras sugeridas' del cronograma. */
public class FiguraSugerida {

    private int idFigura;
    private String figura;
    private String nivel;
    private String lesFaltaA;
    private int nAsistentes;
    private boolean ensenada;
    private String dicto;

    public int getIdFigura() { return idFigura; }
    public void setIdFigura(int idFigura) { this.idFigura = idFigura; }
    public String getFigura() { return figura; }
    public void setFigura(String figura) { this.figura = figura; }
    public String getNivel() { return nivel; }
    public void setNivel(String nivel) { this.nivel = nivel; }
    public String getLesFaltaA() { return lesFaltaA; }
    public void setLesFaltaA(String lesFaltaA) { this.lesFaltaA = lesFaltaA; }
    public int getNAsistentes() { return nAsistentes; }
    public void setNAsistentes(int nAsistentes) { this.nAsistentes = nAsistentes; }
    public boolean isEnsenada() { return ensenada; }
    public void setEnsenada(boolean ensenada) { this.ensenada = ensenada; }
    public String getDicto() { return dicto; }
    public void setDicto(String dicto) { this.dicto = dicto; }

}
