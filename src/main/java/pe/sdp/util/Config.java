package pe.sdp.util;

import java.io.InputStream;
import java.util.Properties;

/**
 * Configuración del sistema.
 * Primero mira las VARIABLES DE ENTORNO (así se configura en el hosting, sin subir claves a GitHub)
 * y si no están, usa src/main/resources/db.properties (para desarrollo local).
 *
 * Variables de entorno:
 *   DB_URL, DB_USER, DB_PASSWORD           conexión completa
 *   MYSQLHOST, MYSQLPORT, MYSQLUSER, MYSQLPASSWORD   (las que crea Railway) + DB_NAME (por defecto sdp_ensayos)
 *   SDP_ENTORNO=produccion                 activa HTTPS obligatorio y bloquea las cuentas de prueba
 *   SDP_ADMIN_CORREO, SDP_ADMIN_CLAVE      admin inicial (solo se usan si ningún admin tiene contraseña)
 */
public final class Config {

    private static final Properties P = new Properties();

    static {
        try (InputStream in = Config.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in != null) P.load(in);
        } catch (Exception ignored) { }
    }

    private Config() {}

    public static String get(String env, String prop, String porDefecto) {
        String v = System.getenv(env);
        if (v != null && !v.isBlank()) return v.trim();
        v = prop == null ? null : P.getProperty(prop);
        return v == null || v.isBlank() ? porDefecto : v.trim();
    }

    public static boolean produccion() {
        return "produccion".equalsIgnoreCase(get("SDP_ENTORNO", "app.entorno", "desarrollo"));
    }

    /** HTTPS obligatorio: por defecto sí en producción. Se puede apagar con SDP_FORZAR_HTTPS=false. */
    public static boolean forzarHttps() {
        return Boolean.parseBoolean(get("SDP_FORZAR_HTTPS", "app.forzarHttps", String.valueOf(produccion())));
    }

    public static String dbDriver() {
        return get("DB_DRIVER", "db.driver", "com.mysql.cj.jdbc.Driver");
    }

    public static String dbUrl() {
        String url = get("DB_URL", null, null);
        if (url != null) return url;
        String host = System.getenv("MYSQLHOST");
        if (host != null && !host.isBlank()) {
            String puerto = get("MYSQLPORT", null, "3306");
            String bd = get("DB_NAME", null, "sdp_ensayos");
            return "jdbc:mysql://" + host + ":" + puerto + "/" + bd
                    + "?serverTimezone=America/Lima&useUnicode=true&characterEncoding=UTF-8";
        }
        return get("DB_URL", "db.url", null);
    }

    public static String dbUser() { return primero("DB_USER", "MYSQLUSER", "db.user"); }

    public static String dbPassword() { String v = primero("DB_PASSWORD", "MYSQLPASSWORD", "db.password"); return v == null ? "" : v; }

    /** Variable de entorno 1, luego variable de entorno 2, luego db.properties. */
    private static String primero(String env1, String env2, String prop) {
        String v = get(env1, null, null);
        if (v == null) v = get(env2, null, null);
        return v != null ? v : get("__ninguna__", prop, null);
    }
}
