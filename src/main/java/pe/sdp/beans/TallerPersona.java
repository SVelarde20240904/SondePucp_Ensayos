package pe.sdp.beans;

/** Una persona en un taller: tallerista, apoyo, maestro de taller o música. Planeada o agregada al registrar el final. */
public class TallerPersona {

    private int id;
    private int idTaller;
    private int idMiembro;
    private String nombre;
    private String funcion;
    private String nivel;
    private boolean planeado = true;
    private String asistencia;
    private Integer reemplazaA;
    private String reemplazaNombre;

    public TallerPersona() { }
    public TallerPersona(int idMiembro, String funcion, String nivel) { this.idMiembro = idMiembro; this.funcion = funcion; this.nivel = nivel; }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getIdTaller() { return idTaller; }
    public void setIdTaller(int idTaller) { this.idTaller = idTaller; }
    public int getIdMiembro() { return idMiembro; }
    public void setIdMiembro(int idMiembro) { this.idMiembro = idMiembro; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getFuncion() { return funcion; }
    public void setFuncion(String funcion) { this.funcion = funcion; }
    public String getNivel() { return nivel; }
    public void setNivel(String nivel) { this.nivel = nivel; }
    public boolean isPlaneado() { return planeado; }
    public void setPlaneado(boolean planeado) { this.planeado = planeado; }
    public String getAsistencia() { return asistencia; }
    public void setAsistencia(String asistencia) { this.asistencia = asistencia; }
    public Integer getReemplazaA() { return reemplazaA; }
    public void setReemplazaA(Integer reemplazaA) { this.reemplazaA = reemplazaA; }
    public String getReemplazaNombre() { return reemplazaNombre; }
    public void setReemplazaNombre(String reemplazaNombre) { this.reemplazaNombre = reemplazaNombre; }

    /** Participó de verdad (no se marcó "no se presentó"). */
    public boolean isEfectivo() { return !"NO_SE_PRESENTO".equals(asistencia); }
    public String getCodigo() { return Taller.codigo(funcion, nivel); }
    public String getNivelTexto() { return Taller.nombreNivel(nivel); }
    public String getFuncionTexto() {
        return switch (funcion == null ? "" : funcion) {
            case "TALLERISTA" -> "Tallerista"; case "APOYO" -> "Apoyo"; case "MAESTRO" -> "Maestro(a) de taller"; case "MUSICA" -> "Música social"; default -> funcion;
        };
    }
    public String getAsistenciaTexto() {
        return switch (asistencia == null ? "" : asistencia) {
            case "A_TIEMPO" -> "A tiempo"; case "TARDE" -> "Llegó tarde"; case "NO_SE_PRESENTO" -> "No se presentó"; default -> "Sin registrar";
        };
    }
    public String getAsistenciaTono() {
        return switch (asistencia == null ? "" : asistencia) { case "A_TIEMPO" -> "ok"; case "TARDE" -> "warn"; case "NO_SE_PRESENTO" -> "bad"; default -> "neutral"; };
    }
}
