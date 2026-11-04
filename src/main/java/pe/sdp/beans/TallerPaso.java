package pe.sdp.beans;

/** Un paso en un taller (o en el catálogo): planificado en el cronograma o agregado; enseñado = ✅/❌. */
public class TallerPaso {

    private int idPaso;
    private String nombre;
    private String nivel;
    private int orden;
    private int seccion = 1;
    private boolean planificado = true;
    private Boolean ensenado;
    private boolean activo = true;
    private int usos;

    public TallerPaso() { }
    public TallerPaso(String nombre, int seccion) { this.nombre = nombre; this.seccion = seccion; }

    public int getIdPaso() { return idPaso; }
    public void setIdPaso(int idPaso) { this.idPaso = idPaso; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getNivel() { return nivel; }
    public void setNivel(String nivel) { this.nivel = nivel; }
    public int getOrden() { return orden; }
    public void setOrden(int orden) { this.orden = orden; }
    public int getSeccion() { return seccion; }
    public void setSeccion(int seccion) { this.seccion = seccion; }
    public boolean isPlanificado() { return planificado; }
    public void setPlanificado(boolean planificado) { this.planificado = planificado; }
    public Boolean getEnsenado() { return ensenado; }
    public void setEnsenado(Boolean ensenado) { this.ensenado = ensenado; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public int getUsos() { return usos; }
    public void setUsos(int usos) { this.usos = usos; }
    public String getMarca() { return ensenado == null ? "·" : (ensenado ? "✅" : "❌"); }
}
