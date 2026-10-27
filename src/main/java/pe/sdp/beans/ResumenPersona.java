package pe.sdp.beans;

import pe.sdp.util.Formato;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Fila genérica 'persona + números' (responsabilidades del mes, historial de asistencia...). */
public class ResumenPersona {

    private int idMiembro;
    private String nombre;
    private String rol;
    private String nivel;
    private int a;
    private int b;
    private int c;
    private java.util.List<String> codigos;
    private int porcentaje;

    public int getIdMiembro() { return idMiembro; }
    public void setIdMiembro(int idMiembro) { this.idMiembro = idMiembro; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }
    public String getNivel() { return nivel; }
    public void setNivel(String nivel) { this.nivel = nivel; }
    public int getA() { return a; }
    public void setA(int a) { this.a = a; }
    public int getB() { return b; }
    public void setB(int b) { this.b = b; }
    public int getC() { return c; }
    public void setC(int c) { this.c = c; }
    public java.util.List<String> getCodigos() { return codigos; }
    public void setCodigos(java.util.List<String> codigos) { this.codigos = codigos; }
    public int getPorcentaje() { return porcentaje; }
    public void setPorcentaje(int porcentaje) { this.porcentaje = porcentaje; }

}
