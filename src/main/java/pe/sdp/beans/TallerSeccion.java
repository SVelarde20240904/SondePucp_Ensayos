package pe.sdp.beans;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Una sección (llave) del cronograma de un nivel: un rango de horas con uno o más pasos adentro.
 * Ej.: "8:15 - 8:40" con Cero (15') y Kentucky (10'); o la música fija de 5'.
 * estilo: INICIO (presentación), FILTRO, NORMAL (pasos), MUSICA, FINAL (foto y feedback).
 */
public class TallerSeccion {

    public static final List<String> ESTILOS = List.of("INICIO", "FILTRO", "NORMAL", "MUSICA", "FINAL");
    public static final String MUSICA_TEXTO = "🎶💃 MÚSICA 🎶💃";

    private String hora;
    private String estilo = "NORMAL";
    private final List<String[]> items = new ArrayList<>();   // [texto, duración] (ej. ["Kentucky", "10'"])

    public TallerSeccion() { }
    public TallerSeccion(String hora, String estilo) { this.hora = hora; setEstilo(estilo); }

    public TallerSeccion item(String texto, String duracion) { items.add(new String[]{texto, duracion}); return this; }

    public String getHora() { return hora == null ? "" : hora; }
    public void setHora(String hora) { this.hora = hora; }
    public String getEstilo() { return estilo; }
    public void setEstilo(String estilo) { this.estilo = ESTILOS.contains(estilo) ? estilo : "NORMAL"; }
    public List<String[]> getItems() { return items; }
    public boolean isMusica() { return "MUSICA".equals(estilo); }
    public boolean isVacia() { return items.stream().allMatch(i -> i[0] == null || i[0].isBlank()); }

    /** Pasos "de verdad" de la sección (para el checklist post-taller y el catálogo): los FILTROS se separan por comas. */
    public List<String> pasos() {
        List<String> l = new ArrayList<>();
        if (!"NORMAL".equals(estilo) && !"FILTRO".equals(estilo)) return l;
        for (String[] i : items) {
            String t = i[0] == null ? "" : i[0].trim();
            if (t.isEmpty() || t.matches("(?iu)secuencia\\s*\\d*")) continue;
            Matcher m = Pattern.compile("\\(([^)]*)\\)").matcher(t);
            if ("FILTRO".equals(estilo) && m.find()) {
                for (String p : m.group(1).split("\\s*(,|/|\\sy\\s)\\s*")) if (!p.isBlank()) l.add(capital(p.trim()));
            } else l.add(t);
        }
        return l;
    }

    private static String capital(String s) { return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1); }

    // ---------------- texto del formulario ----------------
    // El editor (js) manda cada nivel como texto:
    //   ## 8:15 - 8:40 | NORMAL
    //   Cero | 15'
    //   Kentucky | 10'

    public static List<TallerSeccion> leer(String txt) {
        List<TallerSeccion> l = new ArrayList<>();
        if (txt == null) return l;
        TallerSeccion actual = null;
        for (String linea : txt.split("\\r?\\n")) {
            if (linea.isBlank()) continue;
            if (linea.startsWith("##")) {
                String[] p = linea.substring(2).split("\\|", -1);
                actual = new TallerSeccion(corta(p[0], 20), p.length > 1 ? p[1].trim() : "NORMAL");
                l.add(actual);
            } else {
                if (actual == null) { actual = new TallerSeccion("", "NORMAL"); l.add(actual); }
                String[] p = linea.split("\\|", -1);
                String texto = corta(p[0], 300);
                if (texto != null) actual.item(texto, p.length > 1 ? corta(p[1], 10) : null);
            }
        }
        for (TallerSeccion s : l) if (s.isMusica() && s.items.isEmpty()) s.item(MUSICA_TEXTO, "5'");
        if (l.size() > 30) return l.subList(0, 30);
        return l;
    }

    public static String escribir(List<TallerSeccion> secciones) {
        StringBuilder sb = new StringBuilder();
        for (TallerSeccion s : secciones) {
            sb.append("## ").append(limpio(s.getHora())).append(" | ").append(s.estilo).append('\n');
            for (String[] i : s.items) sb.append(limpio(i[0])).append(" | ").append(limpio(i[1])).append('\n');
        }
        return sb.toString();
    }

    private static String limpio(String s) { return s == null ? "" : s.replace("|", "/").replace("\n", " ").trim(); }

    private static String corta(String s, int max) {
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }

    /** Cronograma por defecto (si no hay un taller anterior que sirva de plantilla). */
    public static List<TallerSeccion> plantilla() {
        List<TallerSeccion> l = new ArrayList<>();
        l.add(new TallerSeccion("7:30 - 7:45", "INICIO").item("Presentación y calentamiento · Distribución de niveles", "15'"));
        l.add(new TallerSeccion("7:45", "FILTRO").item("FILTROS ()", "10'"));
        l.add(new TallerSeccion("7:55 - 8:10", "NORMAL"));
        l.add(new TallerSeccion("8:10", "MUSICA").item(MUSICA_TEXTO, "5'"));
        l.add(new TallerSeccion("8:15 - 8:40", "NORMAL"));
        l.add(new TallerSeccion("8:40", "MUSICA").item(MUSICA_TEXTO, "5'"));
        l.add(new TallerSeccion("8:45 - 8:55", "NORMAL"));
        l.add(new TallerSeccion("8:55", "MUSICA").item(MUSICA_TEXTO, "5'"));
        l.add(new TallerSeccion("9:00 - 9:05", "FINAL").item("📷 Toma de foto grupal 📷", "5'").item("Feedback con los asistentes e invitación al siguiente taller", null));
        return l;
    }

    /** Estilo de una fila antigua (antes no se guardaba): por lo que dice. */
    public static String adivinarEstilo(String actividad) {
        String a = actividad == null ? "" : actividad.toLowerCase();
        if (a.contains("🎶") || a.matches("(?su).*(^|[^\\p{L}])m[uú]sica([^\\p{L}]|$).*")) return "MUSICA";
        if (a.contains("filtro")) return "FILTRO";
        if (a.contains("foto") || a.contains("feedback con")) return "FINAL";
        if (a.contains("presentación") || a.contains("presentacion") || a.contains("calentamiento")) return "INICIO";
        return "NORMAL";
    }
}
