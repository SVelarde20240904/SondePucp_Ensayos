package pe.sdp.beans;

import java.util.HashSet;
import java.util.Set;

/** Permiso del sistema y qué roles lo tienen (pantalla Roles y permisos). */
public class Permiso {

    private int idPermiso;
    private String codigo;
    private String descripcion;
    private String seccion;
    private final Set<Integer> roles = new HashSet<>();

    public int getIdPermiso() { return idPermiso; }
    public void setIdPermiso(int idPermiso) { this.idPermiso = idPermiso; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getSeccion() { return seccion; }
    public void setSeccion(String seccion) { this.seccion = seccion; }
    /** Ids de los roles que tienen este permiso. */
    public Set<Integer> getRoles() { return roles; }
    public boolean tiene(int idRol) { return roles.contains(idRol); }
}
