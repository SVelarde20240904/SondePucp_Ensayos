package pe.sdp.beans;

import pe.sdp.util.Formato;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Confirmación y asistencia real de un miembro a un ensayo. */
public class Asistencia {

    private int idEnsayo;
    private int idMiembro;
    private String miembro;
    private String rolBaile;
    private String nivel;
    private String confirmacion;
    private String motivo;
    private String estadoReal;
    private boolean equipoEnsayos;
    private LocalDate fecha;

    public int getIdEnsayo() { return idEnsayo; }
    public void setIdEnsayo(int idEnsayo) { this.idEnsayo = idEnsayo; }
    public int getIdMiembro() { return idMiembro; }
    public void setIdMiembro(int idMiembro) { this.idMiembro = idMiembro; }
    public String getMiembro() { return miembro; }
    public void setMiembro(String miembro) { this.miembro = miembro; }
    public String getRolBaile() { return rolBaile; }
    public void setRolBaile(String rolBaile) { this.rolBaile = rolBaile; }
    public String getNivel() { return nivel; }
    public void setNivel(String nivel) { this.nivel = nivel; }
    public String getConfirmacion() { return confirmacion; }
    public void setConfirmacion(String confirmacion) { this.confirmacion = confirmacion; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public String getEstadoReal() { return estadoReal; }
    public void setEstadoReal(String estadoReal) { this.estadoReal = estadoReal; }
    public boolean isEquipoEnsayos() { return equipoEnsayos; }
    public void setEquipoEnsayos(boolean equipoEnsayos) { this.equipoEnsayos = equipoEnsayos; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public String getRolTexto() { return rolBaile == null ? "—" : ("LEADER".equals(rolBaile) ? "Leader" : "Follower"); }
    public boolean isAsistio() { return estadoReal != null && (estadoReal.equals("AP") || estadoReal.equals("AN") || estadoReal.equals("TJ") || estadoReal.equals("TI")); }
    public String getFechaCorta() { return Formato.ddmm(fecha); }

}
