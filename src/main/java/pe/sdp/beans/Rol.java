package pe.sdp.beans;

/** Rol del sistema: 1 Miembro, 2 Equipo de ensayos, 3 Owner, 4 Moderador de ensayos, 5 Admin de ensayos. */
public class Rol {

    private int idRol;
    private String codigo;
    private String nombre;

    public Rol() { }
    public Rol(int idRol, String codigo, String nombre) { this.idRol = idRol; this.codigo = codigo; this.nombre = nombre; }

    public int getIdRol() { return idRol; }
    public void setIdRol(int idRol) { this.idRol = idRol; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    /** Color de la etiqueta: Owner dorado, roles de ensayos en azul. */
    public String getTono() { return "ADMIN".equals(codigo) ? "warn" : "info"; }
    /** Texto corto para las etiquetas. */
    public String getCorto() {
        return switch (codigo == null ? "" : codigo) {
            case "ADMIN" -> "Owner";
            case "ADMIN_ENSAYOS" -> "Admin ensayos";
            case "MODERADOR_ENSAYOS" -> "Mod. ensayos";
            case "EQUIPO_ENSAYOS" -> "Ensayos";
            default -> nombre;
        };
    }
}
