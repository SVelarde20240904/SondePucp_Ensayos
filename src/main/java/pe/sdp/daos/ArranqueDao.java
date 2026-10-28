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

    /**
     * Migraciones automáticas: si la BD es de una versión anterior, le agrega lo que falta.
     * Así basta con actualizar la app, sin correr scripts a mano. Devuelve qué se cambió.
     */
    public java.util.List<String> migrar() throws SQLException {
        java.util.List<String> hecho = new java.util.ArrayList<>();
        try (Connection c = getConnection()) {
            if (!existeColumna(c, "figura", "video_url")) {
                try (Statement st = c.createStatement()) {
                    st.executeUpdate("ALTER TABLE figura ADD COLUMN video_url VARCHAR(500) NULL AFTER descripcion, MODIFY descripcion VARCHAR(1000) NULL");
                }
                hecho.add("figura.video_url (biblioteca de figuras)");
            }
            if (!existeColumna(c, "miembro", "apodo")) {
                try (Statement st = c.createStatement()) {
                    st.executeUpdate("ALTER TABLE miembro ADD COLUMN apodo VARCHAR(30) NULL AFTER nombre_completo, " +
                                     "ADD COLUMN fecha_nacimiento DATE NULL AFTER facultad");
                }
                hecho.add("miembro.apodo y miembro.fecha_nacimiento");
            }
            if (!existeTabla(c, "miembro_team")) {
                try (Statement st = c.createStatement()) {
                    st.executeUpdate("CREATE TABLE IF NOT EXISTS team (id_team SMALLINT AUTO_INCREMENT PRIMARY KEY, " +
                                     "codigo VARCHAR(20) NOT NULL UNIQUE, nombre VARCHAR(40) NOT NULL, orden SMALLINT NOT NULL DEFAULT 0) ENGINE=InnoDB");
                    st.executeUpdate("CREATE TABLE miembro_team (id_miembro INT NOT NULL, id_team SMALLINT NOT NULL, asignado_por INT NULL, " +
                                     "fecha_asignacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (id_miembro, id_team), " +
                                     "CONSTRAINT fk_mt_miembro FOREIGN KEY (id_miembro) REFERENCES miembro(id_miembro) ON DELETE CASCADE, " +
                                     "CONSTRAINT fk_mt_team FOREIGN KEY (id_team) REFERENCES team(id_team), " +
                                     "CONSTRAINT fk_mt_asigna FOREIGN KEY (asignado_por) REFERENCES miembro(id_miembro)) ENGINE=InnoDB");
                    st.executeUpdate("INSERT IGNORE INTO team (codigo, nombre, orden) VALUES ('ENSAYOS','Ensayos',1),('TALLERES','Talleres',2)," +
                                     "('LOGISTICA','Logística',3),('FINANZAS','Finanzas',4),('RRPP','RRPP',5),('COMUNICA','Comunica',6)");
                    // quien ya tenía el rol Equipo de ensayos queda en el team Ensayos
                    st.executeUpdate("INSERT IGNORE INTO miembro_team (id_miembro, id_team) SELECT mr.id_miembro, t.id_team FROM miembro_rol mr " +
                                     "JOIN team t ON t.codigo='ENSAYOS' WHERE mr.id_rol=2");
                }
                hecho.add("teams (Ensayos, Talleres, Logística, Finanzas, RRPP, Comunica)");
            }
            if (!existeFila(c, "SELECT 1 FROM rol_sistema WHERE codigo='ADMIN_ENSAYOS'")) {
                c.setAutoCommit(false);
                try (Statement st = c.createStatement()) {
                    st.executeUpdate("INSERT IGNORE INTO rol_sistema (id_rol, codigo, nombre) VALUES (4,'MODERADOR_ENSAYOS','Moderador de ensayos'),(5,'ADMIN_ENSAYOS','Admin de ensayos')");
                    st.executeUpdate("UPDATE rol_sistema SET nombre='Owner' WHERE id_rol=3");
                    st.executeUpdate("INSERT IGNORE INTO permiso (id_permiso, codigo, descripcion, seccion) SELECT COALESCE(MAX(id_permiso),0)+1, " +
                                     "'ELIMINAR_ENSAYOS', 'Eliminar ensayos', 'ENSAYOS' FROM permiso");
                    st.executeUpdate("INSERT IGNORE INTO permiso (id_permiso, codigo, descripcion, seccion) SELECT COALESCE(MAX(id_permiso),0)+1, " +
                                     "'EDITAR_FIGURAS_MIEMBRO', 'Editar las figuras de un miembro desde su ficha', 'ENSAYOS' FROM permiso");
                    // Moderador y Admin de ensayos: todo lo del equipo + Figuras, Histórico, Responsabilidades, eliminar ensayos y editar figuras
                    st.executeUpdate("INSERT IGNORE INTO rol_permiso (id_rol, id_permiso) SELECT r.id_rol, rp.id_permiso FROM rol_permiso rp " +
                                     "JOIN (SELECT 4 AS id_rol UNION SELECT 5) r WHERE rp.id_rol=2");
                    st.executeUpdate("INSERT IGNORE INTO rol_permiso (id_rol, id_permiso) SELECT r.id_rol, p.id_permiso FROM permiso p " +
                                     "JOIN (SELECT 3 AS id_rol UNION SELECT 4 UNION SELECT 5) r WHERE p.codigo IN " +
                                     "('GESTIONAR_FIGURAS','GESTIONAR_HISTORICO','ASIGNAR_RESPONSABILIDADES','ELIMINAR_ENSAYOS','EDITAR_FIGURAS_MIEMBRO')");
                    // El equipo de ensayos ve SUS responsabilidades (Mi espacio); el panel de todas es del admin del team
                    st.executeUpdate("DELETE rp FROM rol_permiso rp JOIN permiso p ON p.id_permiso=rp.id_permiso WHERE rp.id_rol=2 AND p.codigo='ASIGNAR_RESPONSABILIDADES'");
                    // Moderador y Admin de ensayos también son "del equipo de ensayos" para la regla del color
                    st.executeUpdate("CREATE OR REPLACE VIEW v_miembro_flags AS SELECT m.id_miembro, m.nombre, m.rol_baile, m.frecuencia, m.estado, " +
                                     "EXISTS (SELECT 1 FROM miembro_rol mr JOIN rol_sistema r ON r.id_rol = mr.id_rol WHERE mr.id_miembro = m.id_miembro " +
                                     "AND r.codigo IN ('EQUIPO_ENSAYOS','MODERADOR_ENSAYOS','ADMIN_ENSAYOS')) AS es_equipo_ensayos, " +
                                     "EXISTS (SELECT 1 FROM miembro_rol mr JOIN rol_sistema r ON r.id_rol = mr.id_rol WHERE mr.id_miembro = m.id_miembro " +
                                     "AND r.codigo = 'ADMIN') AS es_admin FROM miembro m");
                    c.commit();
                } catch (SQLException e) { c.rollback(); throw e; }
                finally { c.setAutoCommit(true); }
                hecho.add("roles Owner, Admin de ensayos y Moderador de ensayos");
            }
        }
        return hecho;
    }

    private static boolean existeTabla(Connection c, String tabla) throws SQLException {
        return existeFila(c, "SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = '" + tabla + "'");
    }

    private static boolean existeFila(Connection c, String sql) throws SQLException {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) { return rs.next(); }
    }

    private static boolean existeColumna(Connection c, String tabla, String columna) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?")) {
            ps.setString(1, tabla); ps.setString(2, columna);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
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
