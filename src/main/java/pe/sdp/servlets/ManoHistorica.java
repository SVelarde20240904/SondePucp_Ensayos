package pe.sdp.servlets;

import pe.sdp.beans.Cuota;
import pe.sdp.beans.Ensayo;
import pe.sdp.daos.EnsayoDao;
import pe.sdp.daos.EvaluacionDao;
import pe.sdp.util.Formato;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;

/**
 * Reconstruye "la mano" (el color de cada miembro en cada figura) en fechas pasadas, repitiendo el histórico.
 * Cortes: después de cada ensayo, a fin de cada mes y hoy.
 */
public final class ManoHistorica {

    /** Un corte en el tiempo: clave (para el select), etiqueta y momento. */
    public record Corte(String clave, String etiqueta, String corta, LocalDateTime hasta) { }

    private ManoHistorica() { }

    public static List<Corte> cortes(List<Object[]> cambios) throws SQLException {
        TreeMap<LocalDateTime, Corte> m = new TreeMap<>();
        LocalDateTime ahora = LocalDateTime.now(Formato.ZONA);
        if (cambios.isEmpty()) { m.put(ahora, new Corte("hoy", "Hoy", "Hoy", ahora)); return new ArrayList<>(m.values()); }
        LocalDate inicio = ((LocalDateTime) cambios.get(0)[3]).toLocalDate();
        for (YearMonth ym = YearMonth.from(inicio); ym.isBefore(YearMonth.from(ahora)); ym = ym.plusMonths(1)) {
            LocalDateTime fin = ym.atEndOfMonth().atTime(23, 59, 59);
            m.put(fin, new Corte("m" + ym, "Fin de " + Cuota.mesTexto(ym).toLowerCase(), Cuota.mesCorto(ym), fin));
        }
        for (Ensayo e : new EnsayoDao().listar(null)) {
            if ("CANCELADO".equals(e.getEstado()) || "BORRADOR".equals(e.getEstado()) || e.getFecha().isBefore(inicio)) continue;
            LocalDateTime fin = e.getFecha().plusDays(6).atTime(23, 59, 59);   // la semana del ensayo (autoevaluaciones incluidas)
            if (!fin.isBefore(ahora)) continue;
            m.put(fin, new Corte("e" + e.getIdEnsayo(), "Después del ensayo del " + Formato.ddmmyyyy(e.getFecha()), Formato.ddmm(e.getFecha()), fin));
        }
        m.put(ahora, new Corte("hoy", "Hoy", "Hoy", ahora));
        return new ArrayList<>(m.values());
    }

    /** La mano en cada corte (clave del corte -> {miembro,figura} -> color). El corte "hoy" sale de la tabla de evaluaciones. */
    public static Map<String, Map<Long, String>> manos(List<Object[]> cambios, List<Corte> cortes, Map<Long, String> hoy) {
        Map<String, Map<Long, String>> res = new LinkedHashMap<>();
        Map<Long, String> estado = new HashMap<>();
        int i = 0;
        for (Corte c : cortes) {
            if ("hoy".equals(c.clave()) && hoy != null) { res.put(c.clave(), hoy); continue; }
            while (i < cambios.size() && !((LocalDateTime) cambios.get(i)[3]).isAfter(c.hasta())) {
                Object[] x = cambios.get(i++);
                estado.put(EvaluacionDao.clave((Integer) x[0], (Integer) x[1]), (String) x[2]);
            }
            res.put(c.clave(), new HashMap<>(estado));
        }
        return res;
    }

    public static boolean domina(String color) { return "AZUL".equals(color) || "VERDE".equals(color); }

    /** Figuras que domina un miembro en una mano (solo figuras dadas). */
    public static int dominadas(Map<Long, String> mano, int miembro, Collection<Integer> figuras) {
        int n = 0;
        for (Integer f : figuras) if (domina(mano.get(EvaluacionDao.clave(miembro, f)))) n++;
        return n;
    }
}
