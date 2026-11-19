package pe.sdp.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Freno contra adivinar contraseñas (ventana de 15 minutos, bloqueo de 15 minutos):
 *  - 5 fallos con un correo DESDE UNA MISMA IP bloquean ese correo solo en esa IP
 *    (así nadie puede dejarte sin entrar tecleando mal tu correo desde su celular)
 *  - 20 fallos desde una IP (con cualquier correo) bloquean esa IP
 *  - 50 fallos contra un mismo correo desde cualquier lado bloquean ese correo (alguien probando desde muchas IPs)
 * Se guarda en memoria: si el servidor se reinicia, los contadores vuelven a cero.
 */
public final class Intentos {

    private static final int MAX_FALLOS_CORREO_IP = 5;
    private static final int MAX_FALLOS_IP = 20;
    private static final int MAX_FALLOS_CORREO = 50;
    private static final long VENTANA_MS = 15 * 60_000L;
    private static final long BLOQUEO_MS = 15 * 60_000L;

    private static final class Registro { int fallos; long desde; long bloqueadoHasta; }

    private static final Map<String, Registro> MAPA = new ConcurrentHashMap<>();

    private Intentos() {}

    /** Minutos que faltan de bloqueo (0 si puede intentar). */
    public static long minutosBloqueado(String correo, String ip) {
        long ahora = System.currentTimeMillis();
        long hasta = Math.max(Math.max(hasta("ci:" + clave(correo) + "|" + ip), hasta("ip:" + ip)), hasta("c:" + clave(correo)));
        return hasta > ahora ? (hasta - ahora + 59_999) / 60_000 : 0;
    }

    public static void fallo(String correo, String ip) {
        sumar("ci:" + clave(correo) + "|" + ip, MAX_FALLOS_CORREO_IP);
        sumar("ip:" + ip, MAX_FALLOS_IP);
        sumar("c:" + clave(correo), MAX_FALLOS_CORREO);
        if (MAPA.size() > 10_000) limpiar();
    }

    public static void exito(String correo, String ip) { MAPA.remove("ci:" + clave(correo) + "|" + ip); }

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
