package pe.sdp.util;

import org.mindrot.jbcrypt.BCrypt;

/** Hash de contraseñas con BCrypt (nunca se guarda la contraseña en texto plano). */
public final class Password {
    private Password() {}

    public static String hash(String plano) {
        return BCrypt.hashpw(plano, BCrypt.gensalt(10));
    }

    public static boolean verifica(String plano, String hash) {
        if (plano == null || hash == null || hash.isBlank()) return false;
        try {
            // jBCrypt 0.4 solo reconoce el prefijo $2a$ (los hashes $2b$/$2y$ son equivalentes)
            String h = hash.startsWith("$2b$") || hash.startsWith("$2y$") ? "$2a$" + hash.substring(4) : hash;
            return BCrypt.checkpw(plano, h);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
