package pe.sdp.beans;

import pe.sdp.util.Formato;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Un ensayo (general, de competencia, de nivelación o de exhibición). */
public class Ensayo {

    private int idEnsayo;
    private String tipo;
    private LocalDate fecha;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private String lugar;
    private LocalDateTime cierreConfirmacion;
    private String estado;
    private String notas;
    private String notasAutor;
    private int confirmados;
    private int sinRespuesta;
    private int asistieron;
    private int puntuales;
    private int tardanzas;
    private int faltas;
    private int faltasJustificadas;
    private int esperados;
    private int figurasEnsenadas;
    private int avances;
    private int autoevaluaron;
    private String nivelesEnsenados;
    private String miConfirmacion;
    private String miMotivo;
    private java.util.List<String> indicaciones;
    private java.util.List<Integer> dictantes;
    private String dictantesTexto;

    public int getIdEnsayo() { return idEnsayo; }
    public void setIdEnsayo(int idEnsayo) { this.idEnsayo = idEnsayo; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }
    public LocalTime getHoraFin() { return horaFin; }
    public void setHoraFin(LocalTime horaFin) { this.horaFin = horaFin; }
    public String getLugar() { return lugar; }
    public void setLugar(String lugar) { this.lugar = lugar; }
    public LocalDateTime getCierreConfirmacion() { return cierreConfirmacion; }
    public void setCierreConfirmacion(LocalDateTime cierreConfirmacion) { this.cierreConfirmacion = cierreConfirmacion; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }
    public String getNotasAutor() { return notasAutor; }
    public void setNotasAutor(String notasAutor) { this.notasAutor = notasAutor; }
    public int getConfirmados() { return confirmados; }
    public void setConfirmados(int confirmados) { this.confirmados = confirmados; }
    public int getSinRespuesta() { return sinRespuesta; }
    public void setSinRespuesta(int sinRespuesta) { this.sinRespuesta = sinRespuesta; }
    public int getAsistieron() { return asistieron; }
    public void setAsistieron(int asistieron) { this.asistieron = asistieron; }
    public int getPuntuales() { return puntuales; }
    public void setPuntuales(int puntuales) { this.puntuales = puntuales; }
    public int getTardanzas() { return tardanzas; }
    public void setTardanzas(int tardanzas) { this.tardanzas = tardanzas; }
    public int getFaltas() { return faltas; }
    public void setFaltas(int faltas) { this.faltas = faltas; }
    public int getFaltasJustificadas() { return faltasJustificadas; }
    public void setFaltasJustificadas(int faltasJustificadas) { this.faltasJustificadas = faltasJustificadas; }
    public int getEsperados() { return esperados; }
    public void setEsperados(int esperados) { this.esperados = esperados; }
    public int getFigurasEnsenadas() { return figurasEnsenadas; }
    public void setFigurasEnsenadas(int figurasEnsenadas) { this.figurasEnsenadas = figurasEnsenadas; }
    public int getAvances() { return avances; }
    public void setAvances(int avances) { this.avances = avances; }
    public int getAutoevaluaron() { return autoevaluaron; }
    public void setAutoevaluaron(int autoevaluaron) { this.autoevaluaron = autoevaluaron; }
    public String getNivelesEnsenados() { return nivelesEnsenados; }
    public void setNivelesEnsenados(String nivelesEnsenados) { this.nivelesEnsenados = nivelesEnsenados; }
    public String getMiConfirmacion() { return miConfirmacion; }
    public void setMiConfirmacion(String miConfirmacion) { this.miConfirmacion = miConfirmacion; }
    public String getMiMotivo() { return miMotivo; }
    public void setMiMotivo(String miMotivo) { this.miMotivo = miMotivo; }
    public java.util.List<String> getIndicaciones() { return indicaciones; }
    public void setIndicaciones(java.util.List<String> indicaciones) { this.indicaciones = indicaciones; }
    public java.util.List<Integer> getDictantes() { return dictantes; }
    public void setDictantes(java.util.List<Integer> dictantes) { this.dictantes = dictantes; }
    public String getDictantesTexto() { return dictantesTexto; }
    public void setDictantesTexto(String dictantesTexto) { this.dictantesTexto = dictantesTexto; }

    public String getTipoTexto() {
        switch (tipo == null ? "" : tipo) {
            case "COMPETENCIA": return "Ensayo de competencia";
            case "NIVELACION": return "Ensayo de nivelación";
            case "EXHIBICION": return "Ensayo de exhibición";
            default: return "Ensayo general";
        }
    }
    public String getTituloFlyer() {
        switch (tipo == null ? "" : tipo) {
            case "COMPETENCIA": return "ENSAYOS DE COMPETENCIA";
            case "NIVELACION": return "ENSAYOS DE NIVELACIÓN";
            case "EXHIBICION": return "ENSAYOS DE EXHIBICIÓN";
            default: return "ENSAYOS GENERALES";
        }
    }
    public String getFechaCorta() { return Formato.ddmm(fecha); }
    public String getFechaIso() { return fecha == null ? "" : fecha.toString(); }
    public String getDia() { return Formato.diaCorto(fecha); }
    public String getDiaLargo() { return Formato.diaLargo(fecha); }
    public String getFechaLarga() { return Formato.fechaLarga(fecha); }
    public String getHorario() { return Formato.hora(horaInicio) + " – " + Formato.hora(horaFin); }
    public String getHoraInicioIso() { return horaInicio == null ? "" : horaInicio.toString(); }
    public String getHoraFinIso() { return horaFin == null ? "" : horaFin.toString(); }
    public String getCierreIso() { return cierreConfirmacion == null ? "" : cierreConfirmacion.toString().substring(0, 16); }
    /** Ya pasó la fecha (o se marcó como cerrado). */
    public boolean isPasado() { return "CERRADO".equals(estado) || (fecha != null && fecha.isBefore(java.time.LocalDate.now())); }
    public String getEstadoTexto() {
        if ("CANCELADO".equals(estado)) return "Cancelado";
        if ("BORRADOR".equals(estado)) return "Borrador";
        return isPasado() ? "Cerrado" : "Programado";
    }
    public String getEstadoTono() {
        if ("CANCELADO".equals(estado)) return "bad";
        if ("BORRADOR".equals(estado)) return "neutral";
        return isPasado() ? "ok" : "info";
    }
    public int getPorcentajeAsistencia() { return esperados == 0 ? 0 : (int) Math.round(asistieron * 100.0 / esperados); }

}
