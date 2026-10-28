package pe.sdp.beans;

/** Team de SDP (Ensayos, Talleres, Logística, Finanzas, RRPP, Comunica). Una persona puede estar en varios. */
public class Team {

    private int idTeam;
    private String codigo;
    private String nombre;

    public Team() { }
    public Team(int idTeam, String codigo, String nombre) { this.idTeam = idTeam; this.codigo = codigo; this.nombre = nombre; }

    public int getIdTeam() { return idTeam; }
    public void setIdTeam(int idTeam) { this.idTeam = idTeam; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
}
