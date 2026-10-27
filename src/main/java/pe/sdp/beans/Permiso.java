package pe.sdp.beans;

import pe.sdp.util.Formato;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Permiso del sistema y qué roles lo tienen (pantalla Roles y permisos). */
public class Permiso {

    private int idPermiso;
    private String codigo;
    private String descripcion;
    private String seccion;
    private boolean miembro;
    private boolean equipo;
    private boolean admin;

    public int getIdPermiso() { return idPermiso; }
    public void setIdPermiso(int idPermiso) { this.idPermiso = idPermiso; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getSeccion() { return seccion; }
    public void setSeccion(String seccion) { this.seccion = seccion; }
    public boolean isMiembro() { return miembro; }
    public void setMiembro(boolean miembro) { this.miembro = miembro; }
    public boolean isEquipo() { return equipo; }
    public void setEquipo(boolean equipo) { this.equipo = equipo; }
    public boolean isAdmin() { return admin; }
    public void setAdmin(boolean admin) { this.admin = admin; }

}
