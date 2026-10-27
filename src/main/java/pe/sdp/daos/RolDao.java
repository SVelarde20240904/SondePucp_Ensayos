package pe.sdp.daos;

import pe.sdp.beans.Miembro;
import pe.sdp.beans.Permiso;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Roles y permisos (pantalla 19). Roles: 1 MIEMBRO, 2 EQUIPO_ENSAYOS, 3 ADMIN. */
public class RolDao extends BaseDao {

    public static final int MIEMBRO = 1, EQUIPO = 2, ADMIN = 3;

    public List<Permiso> matriz() throws SQLException {
        String sql = "SELECT p.*, " +
                     " EXISTS(SELECT 1 FROM rol_permiso r WHERE r.id_permiso=p.id_permiso AND r.id_rol=1) AS r1, " +
                     " EXISTS(SELECT 1 FROM rol_permiso r WHERE r.id_permiso=p.id_permiso AND r.id_rol=2) AS r2, " +
                     " EXISTS(SELECT 1 FROM rol_permiso r WHERE r.id_permiso=p.id_permiso AND r.id_rol=3) AS r3 " +
                     "FROM permiso p ORDER BY FIELD(p.seccion,'MI_ESPACIO','ENSAYOS','ADMINISTRACION'), p.id_permiso";
        try (Connection c = getConnection(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            List<Permiso> l = new ArrayList<>();
            while (rs.next()) {
                Permiso p = new Permiso();
                p.setIdPermiso(rs.getInt("id_permiso")); p.setCodigo(rs.getString("codigo")); p.setDescripcion(rs.getString("descripcion"));
                p.setSeccion(rs.getString("seccion")); p.setMiembro(rs.getBoolean("r1")); p.setEquipo(rs.getBoolean("r2")); p.setAdmin(rs.getBoolean("r3"));
                l.add(p);
            }
            return l;
        }
    }

    /**
     * Reemplaza los permisos de Equipo de ensayos y Admin. Los de Miembro son fijos.
     * Admin siempre conserva ASIGNAR_ROLES para no bloquearse.
     */
    public void guardarMatriz(Set<String> marcados) throws SQLException {
        try (Connection c = getConnection()) {
            c.setAutoCommit(false);
            try {
                try (Statement st = c.createStatement()) { st.executeUpdate("DELETE FROM rol_permiso WHERE id_rol IN (2,3)"); }
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO rol_permiso (id_rol, id_permiso) VALUES (?,?)")) {
                    for (String m : marcados) {                       // formato "rol_idPermiso"
                        String[] p = m.split("_");
                        int rol = Integer.parseInt(p[0]);
                        if (rol != EQUIPO && rol != ADMIN) continue;
                        ps.setInt(1, rol); ps.setInt(2, Integer.parseInt(p[1])); ps.addBatch();
                    }
                    ps.executeBatch();
                }
                try (Statement st = c.createStatement()) {
                    st.executeUpdate("INSERT IGNORE INTO rol_permiso (id_rol, id_permiso) SELECT 3, id_permiso FROM permiso WHERE codigo='ASIGNAR_ROLES'");
                    st.executeUpdate("INSERT IGNORE INTO rol_permiso (id_rol, id_permiso) SELECT r.id_rol, rp.id_permiso FROM rol_sistema r JOIN rol_permiso rp ON rp.id_rol=1 WHERE r.id_rol IN (2,3)");
                }
                c.commit();
            } catch (SQLException e) { c.rollback(); throw e; }
        }
    }

    /** Personas con rol Equipo de ensayos o Admin. */
    public List<Miembro> personasConRol() throws SQLException {
        String sql = "SELECT m.id_miembro, m.nombre, MAX(mr.id_rol=3) AS adm, MAX(mr.id_rol=2) AS eq FROM miembro m JOIN miembro_rol mr ON mr.id_miembro=m.id_miembro " +
                     "WHERE mr.id_rol IN (2,3) AND m.estado='ACTIVO' GROUP BY m.id_miembro, m.nombre ORDER BY adm DESC, m.nombre";
        try (Connection c = getConnection(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            List<Miembro> l = new ArrayList<>();
            while (rs.next()) {
                Miembro m = new Miembro();
                m.setIdMiembro(rs.getInt(1)); m.setNombre(rs.getString(2)); m.setAdmin(rs.getBoolean(3)); m.setEquipoEnsayos(rs.getBoolean(4));
                l.add(m);
            }
            return l;
        }
    }

    public void asignar(int idMiembro, int idRol, int idAdmin) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("INSERT IGNORE INTO miembro_rol (id_miembro, id_rol, asignado_por) VALUES (?,?,?)")) {
            ps.setInt(1, idMiembro); ps.setInt(2, idRol); ps.setInt(3, idAdmin); ps.executeUpdate();
        }
    }

    /** Lanza SQLException con el mensaje del trigger si se intenta dejar el sistema sin admin. */
    public void quitar(int idMiembro, int idRol) throws SQLException {
        if (idRol == MIEMBRO) return;
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("DELETE FROM miembro_rol WHERE id_miembro=? AND id_rol=?")) {
            ps.setInt(1, idMiembro); ps.setInt(2, idRol); ps.executeUpdate();
        }
    }
}
