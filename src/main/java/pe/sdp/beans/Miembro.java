package pe.sdp.beans;

import pe.sdp.util.Formato;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Miembro de Son de PUCP (también es el usuario que inicia sesión). */
public class Miembro {

    private int idMiembro;
    private String nombre;
    private String nombreCompleto;
    private String codigoPucp;
    private String correo;
    private String passwordHash;
    private String celular;
    private String facultad;
    private String rolBaile;
    private String frecuencia;
    private String estado;
    private LocalDate fechaIngreso;
    private String notas;
    private boolean avisoAsistencia;
    private boolean avisoMensual;
    private boolean avisoEnsayo;
    private boolean debeCambiarPassword;
    private LocalDateTime fechaBaja;
    private String motivoBaja;
    private String nivel;
    private boolean equipoEnsayos;
    private boolean admin;
    private boolean cuentaActivada;
    private boolean puedeEliminar;
    private int asistidosMes;
    private int ensayosMes;
    private String apodo;
    private LocalDate fechaNacimiento;
    private java.util.Set<Integer> rolIds = new java.util.HashSet<>();
    private java.util.Set<Integer> teamIds = new java.util.HashSet<>();
    private java.util.List<String> teams = new java.util.ArrayList<>();

    public int getIdMiembro() { return idMiembro; }
    public void setIdMiembro(int idMiembro) { this.idMiembro = idMiembro; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }
    public String getCodigoPucp() { return codigoPucp; }
    public void setCodigoPucp(String codigoPucp) { this.codigoPucp = codigoPucp; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getCelular() { return celular; }
    public void setCelular(String celular) { this.celular = celular; }
    public String getFacultad() { return facultad; }
    public void setFacultad(String facultad) { this.facultad = facultad; }
    public String getRolBaile() { return rolBaile; }
    public void setRolBaile(String rolBaile) { this.rolBaile = rolBaile; }
    public String getFrecuencia() { return frecuencia; }
    public void setFrecuencia(String frecuencia) { this.frecuencia = frecuencia; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public LocalDate getFechaIngreso() { return fechaIngreso; }
    public void setFechaIngreso(LocalDate fechaIngreso) { this.fechaIngreso = fechaIngreso; }
    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }
    public boolean isAvisoAsistencia() { return avisoAsistencia; }
    public void setAvisoAsistencia(boolean avisoAsistencia) { this.avisoAsistencia = avisoAsistencia; }
    public boolean isAvisoMensual() { return avisoMensual; }
    public void setAvisoMensual(boolean avisoMensual) { this.avisoMensual = avisoMensual; }
    public boolean isAvisoEnsayo() { return avisoEnsayo; }
    public void setAvisoEnsayo(boolean avisoEnsayo) { this.avisoEnsayo = avisoEnsayo; }
    public boolean isDebeCambiarPassword() { return debeCambiarPassword; }
    public void setDebeCambiarPassword(boolean debeCambiarPassword) { this.debeCambiarPassword = debeCambiarPassword; }
    public LocalDateTime getFechaBaja() { return fechaBaja; }
    public void setFechaBaja(LocalDateTime fechaBaja) { this.fechaBaja = fechaBaja; }
    public String getMotivoBaja() { return motivoBaja; }
    public void setMotivoBaja(String motivoBaja) { this.motivoBaja = motivoBaja; }
    public String getNivel() { return nivel; }
    public void setNivel(String nivel) { this.nivel = nivel; }
    public boolean isEquipoEnsayos() { return equipoEnsayos; }
    public void setEquipoEnsayos(boolean equipoEnsayos) { this.equipoEnsayos = equipoEnsayos; }
    public boolean isAdmin() { return admin; }
    public void setAdmin(boolean admin) { this.admin = admin; }
    public boolean isCuentaActivada() { return cuentaActivada; }
    public void setCuentaActivada(boolean cuentaActivada) { this.cuentaActivada = cuentaActivada; }
    public boolean isPuedeEliminar() { return puedeEliminar; }
    public void setPuedeEliminar(boolean puedeEliminar) { this.puedeEliminar = puedeEliminar; }
    public int getAsistidosMes() { return asistidosMes; }
    public void setAsistidosMes(int asistidosMes) { this.asistidosMes = asistidosMes; }
    public int getEnsayosMes() { return ensayosMes; }
    public void setEnsayosMes(int ensayosMes) { this.ensayosMes = ensayosMes; }

    public String getIniciales() { return Formato.iniciales(nombre); }
    public String getRolBaileTexto() { return rolBaile == null ? "—" : ("LEADER".equals(rolBaile) ? "Leader" : "Follower"); }
    public String getRolCorto() { return rolBaile == null ? "—" : ("LEADER".equals(rolBaile) ? "L" : "F"); }
    public String getFrecuenciaTexto() { return Formato.capital(frecuencia); }
    public boolean isActivo() { return "ACTIVO".equals(estado); }
    public String getFechaIngresoTexto() { return Formato.ddmmyyyy(fechaIngreso); }
    public String getFechaIngresoIso() { return fechaIngreso == null ? "" : fechaIngreso.toString(); }
    public String getMiembroDesde() { return fechaIngreso == null ? "—" : Formato.capital(fechaIngreso.getMonth().getDisplayName(java.time.format.TextStyle.FULL, Formato.ES)) + " " + fechaIngreso.getYear(); }
    public String getAsistenciaMesTexto() { return ensayosMes == 0 ? "—" : asistidosMes + "/" + ensayosMes + " · " + Math.round(asistidosMes * 100.0 / ensayosMes) + "%"; }
    /** Cuenta para el color si es activo, su frecuencia cuenta y no es del equipo de ensayos (por defecto). */
    private boolean cuentaColor;
    public boolean isCuentaColor() { return cuentaColor; }
    public void setCuentaColor(boolean cuentaColor) { this.cuentaColor = cuentaColor; }


    public String getApodo() { return apodo; }
    public void setApodo(String apodo) { this.apodo = apodo; }
    public LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(LocalDate fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }
    public java.util.Set<Integer> getRolIds() { return rolIds; }
    public void setRolIds(java.util.Set<Integer> rolIds) { this.rolIds = rolIds; }
    public java.util.Set<Integer> getTeamIds() { return teamIds; }
    public void setTeamIds(java.util.Set<Integer> teamIds) { this.teamIds = teamIds; }
    /** Nombres de sus teams, en orden (Ensayos, Talleres, …). */
    public java.util.List<String> getTeams() { return teams; }
    public void setTeams(java.util.List<String> teams) { this.teams = teams; }

    public boolean isTieneApodo() { return apodo != null && !apodo.isBlank(); }
    /** "Enmanuel Vega (Ilo)" o solo el nombre si no tiene apodo. */
    public String getNombreConApodo() { return isTieneApodo() ? nombre + " (" + apodo + ")" : nombre; }
    /** Cómo saludarlo: su apodo, o su primer nombre. */
    public String getComoLlamarlo() { return isTieneApodo() ? apodo : (nombre == null ? "" : nombre.split(" ")[0]); }
    public String getFechaNacimientoIso() { return fechaNacimiento == null ? "" : fechaNacimiento.toString(); }
    /** Solo día y mes (sin el año, para no mostrar la edad): "14 de marzo". */
    public String getCumpleTexto() { return fechaNacimiento == null ? "—" : Formato.diaMes(fechaNacimiento); }
    public boolean isCumpleHoy() {
        if (fechaNacimiento == null) return false;
        LocalDate hoy = LocalDate.now(Formato.ZONA);
        return fechaNacimiento.getMonth() == hoy.getMonth() && fechaNacimiento.getDayOfMonth() == hoy.getDayOfMonth();
    }
    public boolean tieneRol(int idRol) { return rolIds.contains(idRol); }
}
