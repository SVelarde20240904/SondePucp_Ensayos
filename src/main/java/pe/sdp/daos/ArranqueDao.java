package pe.sdp.daos;

import java.sql.*;

/** Consultas que se corren una sola vez al arrancar la aplicación. */
public class ArranqueDao extends BaseDao {

    /** Comprueba que la BD responda (para el log de arranque y /salud). */
    public boolean bdResponde() {
        try (Connection c = getConnection(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT 1")) {
            return rs.next();
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Admin inicial: si NINGÚN admin tiene contraseña todavía (BD recién cargada con los datos del Excel),
     * al primer admin le pone el correo y una contraseña temporal (tendrá que cambiarla al entrar).
     * Si ya hay un admin con contraseña no hace nada, así que dejar las variables puestas no es peligroso.
     * @return el nombre del admin activado, o null si no hizo nada
     */
    public String activarAdminInicial(String correo, String hash) throws SQLException {
        String buscar = "SELECT m.id_miembro, m.nombre, " +
                " (SELECT COUNT(*) FROM miembro m2 JOIN miembro_rol r2 ON r2.id_miembro=m2.id_miembro " +
                "   JOIN rol_sistema s2 ON s2.id_rol=r2.id_rol WHERE s2.codigo='ADMIN' AND m2.password_hash IS NOT NULL) AS con_clave " +
                "FROM miembro m JOIN miembro_rol r ON r.id_miembro=m.id_miembro JOIN rol_sistema s ON s.id_rol=r.id_rol " +
                "WHERE s.codigo='ADMIN' AND m.estado='ACTIVO' ORDER BY m.id_miembro LIMIT 1";
        try (Connection c = getConnection(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(buscar)) {
            if (!rs.next() || rs.getInt("con_clave") > 0) return null;
            int id = rs.getInt("id_miembro");
            String nombre = rs.getString("nombre");
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE miembro SET correo=?, password_hash=?, debe_cambiar_password=1 WHERE id_miembro=?")) {
                ps.setString(1, correo.trim().toLowerCase());
                ps.setString(2, hash);
                ps.setInt(3, id);
                ps.executeUpdate();
            }
            return nombre;
        }
    }

    /** Cuántas cuentas de prueba (@sdp.test) hay activas: en producción no debería haber ninguna. */
    public int cuentasDePrueba() throws SQLException {
        try (Connection c = getConnection(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM miembro WHERE correo LIKE '%@sdp.test' AND password_hash IS NOT NULL")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }
}
