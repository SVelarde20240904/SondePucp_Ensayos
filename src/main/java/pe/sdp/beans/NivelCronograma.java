package pe.sdp.beans;

import pe.sdp.util.Formato;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Fila del 'Resumen por nivel' del cronograma sugerido. */
public class NivelCronograma {

    private String nivel;
    private int leaders;
    private int followers;
    private String nombresLeaders;
    private String nombresFollowers;
    private boolean seAbre;
    private String pareja;

    public String getNivel() { return nivel; }
    public void setNivel(String nivel) { this.nivel = nivel; }
    public int getLeaders() { return leaders; }
    public void setLeaders(int leaders) { this.leaders = leaders; }
    public int getFollowers() { return followers; }
    public void setFollowers(int followers) { this.followers = followers; }
    public String getNombresLeaders() { return nombresLeaders; }
    public void setNombresLeaders(String nombresLeaders) { this.nombresLeaders = nombresLeaders; }
    public String getNombresFollowers() { return nombresFollowers; }
    public void setNombresFollowers(String nombresFollowers) { this.nombresFollowers = nombresFollowers; }
    public boolean isSeAbre() { return seAbre; }
    public void setSeAbre(boolean seAbre) { this.seAbre = seAbre; }
    public String getPareja() { return pareja; }
    public void setPareja(String pareja) { this.pareja = pareja; }

}
