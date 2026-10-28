package pe.sdp.util;

import java.security.SecureRandom;
import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

/** Formatos de fecha, colores y utilidades varias para las vistas. */
public final class Formato {

    public static final Locale ES = new Locale("es", "PE");
    public static final String[] COLORES = {"ROJO", "AMARILLO", "VERDE", "AZUL"};
    private static final DateTimeFormatter DDMM = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter DDMMYYYY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DDMM_HHMM = DateTimeFormatter.ofPattern("dd/MM HH:mm");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("h:mm a", Locale.US);

    private Formato() {}

    /** Posición del color: ROJO=1 … AZUL=4 (0 si es null). */
    public static int valor(String color) {
        if (color == null) return 0;
        for (int i = 0; i < COLORES.length; i++) if (COLORES[i].equals(color)) return i + 1;
        return 0;
    }

    public static boolean esColor(String c) { return valor(c) > 0; }

    public static String ddmm(LocalDate d) { return d == null ? "" : d.format(DDMM); }
    public static String ddmmyyyy(LocalDate d) { return d == null ? "" : d.format(DDMMYYYY); }
    public static String ddmmHora(LocalDateTime d) { return d == null ? "" : d.format(DDMM_HHMM); }
    public static String hora(LocalTime t) { return t == null ? "" : t.format(HORA).replace("AM", "AM").replace("PM", "PM"); }

    public static String diaCorto(LocalDate d) {
        if (d == null) return "";
        String s = d.getDayOfWeek().getDisplayName(TextStyle.SHORT, ES).replace(".", "");
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    public static String diaLargo(LocalDate d) {
        return d == null ? "" : d.getDayOfWeek().getDisplayName(TextStyle.FULL, ES);
    }

    public static String fechaLarga(LocalDate d) {
        if (d == null) return "";
        return diaLargo(d) + " " + d.getDayOfMonth() + " de " + d.getMonth().getDisplayName(TextStyle.FULL, ES);
    }

    /** Sábado de la semana (para agrupar responsabilidades). */
    public static LocalDate sabadoDe(LocalDate d) {
        int diff = DayOfWeek.SATURDAY.getValue() - d.getDayOfWeek().getValue();
        return d.plusDays(diff >= 0 ? diff : diff + 7);
    }

    public static String capital(String s) {
        if (s == null || s.isEmpty()) return "";
        String t = s.toLowerCase(ES).replace('_', ' ');
        return t.substring(0, 1).toUpperCase(ES) + t.substring(1);
    }

    public static String slug(String s) {
        String n = Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return n.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }

    public static String iniciales(String nombre) {
        if (nombre == null || nombre.isBlank()) return "?";
        String[] p = nombre.trim().split("\\s+");
        return (p[0].substring(0, 1) + (p.length > 1 ? p[1].substring(0, 1) : "")).toUpperCase();
    }

    public static Integer entero(String s) {
        try { return s == null || s.isBlank() ? null : Integer.valueOf(s.trim()); }
        catch (NumberFormatException e) { return null; }
    }

    public static String vacio(String s) { return s == null || s.isBlank() ? null : s.trim(); }

    private static final SecureRandom RND = new SecureRandom();

    /** Contraseña temporal fácil de dictar: SDP-1234 */
    /**
     * Convierte un enlace de Google Drive o YouTube en la URL para mostrarlo dentro de la página.
     * Drive: https://drive.google.com/file/d/ID/view → .../file/d/ID/preview
     * YouTube: youtu.be/ID, youtube.com/watch?v=ID, /shorts/ID → youtube-nocookie.com/embed/ID
     */
    public static String videoEmbed(String url) {
        if (url == null || !url.startsWith("https://")) return null;
        java.util.regex.Matcher m;
        m = java.util.regex.Pattern.compile("^https://drive\\.google\\.com/(?:file/d/|open\\?id=)([A-Za-z0-9_-]{10,})").matcher(url);
        if (m.find()) return "https://drive.google.com/file/d/" + m.group(1) + "/preview";
        m = java.util.regex.Pattern.compile("^https://(?:www\\.|m\\.)?(?:youtube\\.com/(?:watch\\?(?:.*&)?v=|shorts/|embed/)|youtu\\.be/)([A-Za-z0-9_-]{6,})").matcher(url);
        if (m.find()) return "https://www.youtube-nocookie.com/embed/" + m.group(1);
        return null;
    }

    /** ¿Es un enlace aceptable para el video? (https y de un sitio conocido) */
    public static boolean videoValido(String url) {
        return url != null && url.startsWith("https://") && url.length() <= 500 && !url.contains(" ") && !url.contains("\"");
    }

    /** Clave temporal tipo SDP-K7M4-Q9XR: 8 caracteres al azar sin letras que se confundan (0/O, 1/I/L). */
    private static final String ALFABETO = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    public static String passwordTemporal() {
        StringBuilder sb = new StringBuilder("SDP-");
        for (int i = 0; i < 8; i++) { if (i == 4) sb.append('-'); sb.append(ALFABETO.charAt(RND.nextInt(ALFABETO.length()))); }
        return sb.toString();
    }
}
