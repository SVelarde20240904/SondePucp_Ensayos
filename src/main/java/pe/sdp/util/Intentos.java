package pe.sdp.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Freno contra adivinar contraseñas: después de 5 intentos fallidos en 15 minutos
 * (por correo o por IP) el login queda bloqueado 15 minutos.
 * Se guarda en memoria: si el servidor se reinicia, los contadores vuelven a cero.
 */
public final class Intentos {

    private static final int MAX_FALLOS_CORREO = 5;
    private static final int MAX_FALLOS_IP = 20;
    private static final long VENTANA_MS = 15 * 60_000L;
    private static final long BLOQUEO_MS = 15 * 60_000L;

    private static final class Registro { int fallos; long desde; long bloqueadoHasta; }

    private static final Map<String, Registro> MAPA = new ConcurrentHashMap<>();

    private Intentos() {}

    /** Minutos que faltan de bloqueo (0 si puede intentar). */
    public static long minutosBloqueado(String correo, String ip) {
        long ahora = System.currentTimeMillis();
        long hasta = Math.max(hasta("c:" + clave(correo)), hasta("ip:" + ip));
        return hasta > ahora ? (hasta - ahora + 59_999) / 60_000 : 0;
    }

    public static void fallo(String correo, String ip) {
        sumar("c:" + clave(correo), MAX_FALLOS_CORREO);
        sumar("ip:" + ip, MAX_FALLOS_IP);
        if (MAPA.size() > 10_000) limpiar();
    }

    public static void exito(String correo) { MAPA.remove("c:" + clave(correo)); }

    private static String clave(String correo) { return correo == null ? "" : correo.trim().toLowerCase(); }

    private static long hasta(String k) { Registro r = MAPA.get(k); return r == null ? 0 : r.bloqueadoHasta; }

    private static void sumar(String k, int max) {
        long ahora = System.currentTimeMillis();
        MAPA.compute(k, (key, r) -> {
            if (r == null || ahora - r.desde > VENTANA_MS) { r = new Registro(); r.desde = ahora; }
            r.fallos++;
            if (r.fallos >= max) { r.bloqueadoHasta = ahora + BLOQUEO_MS; r.fallos = 0; r.desde = ahora; }
            return r;
        });
    }

    private static void limpiar() {
        long ahora = System.currentTimeMillis();
        MAPA.entrySet().removeIf(e -> ahora - e.getValue().desde > VENTANA_MS && e.getValue().bloqueadoHasta < ahora);
    }
}
