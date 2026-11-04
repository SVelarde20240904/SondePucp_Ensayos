package pe.sdp.beans;

import pe.sdp.util.Formato;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/** Un taller de SDP (jueves): cronograma por nivel, dictado planeado/final, pasos y feedback. */
public class Taller {

    /** Los 4 niveles, en orden. */
    public static final List<String> NIVELES = List.of("PRINCIPIANTE", "BASICO", "INTRO_RUEDA", "RUEDA_BASICA");
    private static final Map<String, String> NOMBRE = Map.of("PRINCIPIANTE", "Principiante", "BASICO", "Básico",
            "INTRO_RUEDA", "Intro a la rueda", "RUEDA_BASICA", "Rueda básica");
    private static final Map<String, String> CODIGO = Map.of("PRINCIPIANTE", "TP", "BASICO", "TB", "INTRO_RUEDA", "TI", "RUEDA_BASICA", "TR");

    public static String nombreNivel(String nivel) { return nivel == null ? "Sin nivel" : NOMBRE.getOrDefault(nivel, nivel); }
    /** Código de la hoja DICTADO DE NIVELES: TP, TB, TI, TR (talleristas), AP, MT. */
    public static String codigo(String funcion, String nivel) {
        if ("APOYO".equals(funcion)) return "AP";
        if ("MAESTRO".equals(funcion)) return "MT";
        if ("MUSICA".equals(funcion)) return "MU";
        return CODIGO.getOrDefault(nivel, "T");
    }

    private int idTaller;
    private String ciclo;
    private Integer numero;
    private LocalDate fecha;
    private LocalTime horaInicio = LocalTime.of(19, 30);
    private LocalTime horaFin = LocalTime.of(21, 5);
    private String lugar;
    private String estado = "PROGRAMADO";
    private String musicaTexto;
    private String notas;
    private LocalDateTime finalFecha;
    private String finalPor;
    // contadores para la lista
    private int esperados, respondieron, feedbacks, resumenesEnviados, receptores;
    // detalle
    private final Map<String, List<String[]>> bloques = new LinkedHashMap<>();       // nivel -> [hora, actividad, duracion]
    private final Map<String, List<TallerPaso>> pasos = new LinkedHashMap<>();       // nivel -> pasos
    private final List<TallerPersona> personas = new ArrayList<>();
    private final Map<String, String> checklistPor = new HashMap<>();                // nivel -> quién lo respondió

    public Taller() { for (String n : NIVELES) { bloques.put(n, new ArrayList<>()); pasos.put(n, new ArrayList<>()); } }

    public int getIdTaller() { return idTaller; }
    public void setIdTaller(int idTaller) { this.idTaller = idTaller; }
    public String getCiclo() { return ciclo; }
    public void setCiclo(String ciclo) { this.ciclo = ciclo; }
    public Integer getNumero() { return numero; }
    public void setNumero(Integer numero) { this.numero = numero; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }
    public LocalTime getHoraFin() { return horaFin; }
    public void setHoraFin(LocalTime horaFin) { this.horaFin = horaFin; }
    public String getLugar() { return lugar; }
    public void setLugar(String lugar) { this.lugar = lugar; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getMusicaTexto() { return musicaTexto; }
    public void setMusicaTexto(String musicaTexto) { this.musicaTexto = musicaTexto; }
    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }
    public LocalDateTime getFinalFecha() { return finalFecha; }
    public void setFinalFecha(LocalDateTime finalFecha) { this.finalFecha = finalFecha; }
    public String getFinalPor() { return finalPor; }
    public void setFinalPor(String finalPor) { this.finalPor = finalPor; }
    public int getEsperados() { return esperados; }
    public void setEsperados(int esperados) { this.esperados = esperados; }
    public int getRespondieron() { return respondieron; }
    public void setRespondieron(int respondieron) { this.respondieron = respondieron; }
    public int getFeedbacks() { return feedbacks; }
    public void setFeedbacks(int feedbacks) { this.feedbacks = feedbacks; }
    public int getResumenesEnviados() { return resumenesEnviados; }
    public void setResumenesEnviados(int resumenesEnviados) { this.resumenesEnviados = resumenesEnviados; }
    public int getReceptores() { return receptores; }
    public void setReceptores(int receptores) { this.receptores = receptores; }
    public Map<String, List<String[]>> getBloques() { return bloques; }
    public Map<String, List<TallerPaso>> getPasos() { return pasos; }
    public List<TallerPersona> getPersonas() { return personas; }
    public Map<String, String> getChecklistPor() { return checklistPor; }

    public String getTitulo() { return (numero == null ? "" : numero + "º ") + "Taller SDP"; }
    public String getFechaTexto() { return fecha == null ? "" : Formato.diaCorto(fecha) + " " + Formato.ddmmyyyy(fecha); }
    public String getFechaCorta() { return Formato.ddmm(fecha); }
    public String getFechaIso() { return fecha == null ? "" : fecha.toString(); }
    public String getHorario() { return Formato.hora(horaInicio) + " - " + Formato.hora(horaFin); }
    public String getHoraInicioIso() { return horaInicio == null ? "" : horaInicio.toString(); }
    public String getHoraFinIso() { return horaFin == null ? "" : horaFin.toString(); }
    /** Plazo del formulario: el día siguiente a las 11:59 pm. */
    public LocalDateTime getPlazoFormulario() { return fecha == null ? null : fecha.plusDays(1).atTime(23, 59); }
    public String getPlazoTexto() { return getPlazoFormulario() == null ? "" : Formato.diaCorto(fecha.plusDays(1)) + " " + Formato.ddmm(fecha.plusDays(1)) + " a las 11:59 pm"; }
    public boolean isPlazoVencido() { return getPlazoFormulario() != null && LocalDateTime.now(Formato.ZONA).isAfter(getPlazoFormulario()); }
    public boolean isPasado() { return fecha != null && !fecha.isAfter(LocalDate.now(Formato.ZONA)); }
    public boolean isFinalRegistrado() { return finalFecha != null; }
    public String getEstadoTexto() {
        return switch (estado == null ? "" : estado) {
            case "BORRADOR" -> "Borrador"; case "REALIZADO" -> "Realizado"; case "CANCELADO" -> "Cancelado"; default -> "Programado";
        };
    }
    public String getEstadoTono() {
        return switch (estado == null ? "" : estado) { case "REALIZADO" -> "ok"; case "CANCELADO" -> "bad"; case "BORRADOR" -> "neutral"; default -> "info"; };
    }

    /** Personas de un nivel con una función (para la vista y el flyer). */
    public List<TallerPersona> personasDe(String funcion, String nivel) {
        if (nivel != null && nivel.isEmpty()) nivel = null;   // desde la JSP, EL convierte null en ""
        List<TallerPersona> l = new ArrayList<>();
        for (TallerPersona p : personas) if (p.getFuncion().equals(funcion) && Objects.equals(p.getNivel(), nivel)) l.add(p);
        return l;
    }
    public List<TallerPersona> getPlaneadas() { List<TallerPersona> l = new ArrayList<>(); for (TallerPersona p : personas) if (p.isPlaneado()) l.add(p); return l; }
    public List<TallerPersona> getExtras() { List<TallerPersona> l = new ArrayList<>(); for (TallerPersona p : personas) if (!p.isPlaneado()) l.add(p); return l; }
    public List<String> getNiveles() { return NIVELES; }
}
