package pe.sdp.daos;

import pe.sdp.beans.Miembro;
import pe.sdp.beans.Permiso;
import pe.sdp.beans.Rol;

import java.sql.*;
import java.util.*;

/**
 * Roles y permisos (pantalla 19).
 * Roles: 1 Miembro (todos), 2 Equipo de ensayos, 4 Moderador de ensayos, 5 Admin de ensayos, 3 Owner.
 * Los permisos de cada rol (menos Miembro) se marcan en la matriz; los roles de una persona se suman.
 */
public class RolDao extends BaseDao {

    public static final int MIEMBRO = 1, EQUIPO = 2, OWNER = 3, MODERADOR_ENSAYOS = 4, ADMIN_ENSAYOS = 5;
    private static final String ORDEN = "FIELD(r.id_rol,1,2,4,5,3)";

    /** Todos los roles menos Miembro, de menor a mayor. */
    public List<Rol> rolesExtra() throws SQLException {
        try (Connection c = getConnection(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT r.id_rol, r.codigo, r.nombre FROM rol_sistema r WHERE r.id_rol<>1 ORDER BY " + ORDEN)) {
            List<Rol> l = new ArrayList<>();
            while (rs.next()) l.add(new Rol(rs.getInt(1), rs.getString(2), rs.getString(3)));
            return l;
        }
    }

    public List<Permiso> matriz() throws SQLException {
        String sql = "SELECT p.*, (SELECT GROUP_CONCAT(rp.id_rol) FROM rol_permiso rp WHERE rp.id_permiso=p.id_permiso) AS roles " +
                     "FROM permiso p ORDER BY FIELD(p.seccion,'MI_ESPACIO','ENSAYOS','ADMINISTRACION'), p.id_permiso";
        try (Connection c = getConnection(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            List<Permiso> l = new ArrayList<>();
            while (rs.next()) {
                Permiso p = new Permiso();
                p.setIdPermiso(rs.getInt("id_permiso")); p.setCodigo(rs.getString("codigo")); p.setDescripcion(rs.getString("descripcion"));
                p.setSeccion(rs.getString("seccion"));
                String r = rs.getString("roles");
                if (r != null) for (String x : r.split(",")) p.getRoles().add(Integer.parseInt(x));
                l.add(p);
            }
            return l;
        }
    }

    /**
     * Reemplaza los permisos de todos los roles menos Miembro (que son fijos).
     * Todos conservan los de Mi espacio y el Owner siempre conserva "Asignar roles" para no bloquearse.
     */
    public void guardarMatriz(Set<String> marcados) throws SQLException {
        Set<Integer> editables = new HashSet<>();
        for (Rol r : rolesExtra()) editables.add(r.getIdRol());
        try (Connection c = getConnection()) {
            c.setAutoCommit(false);
            try {
                try (Statement st = c.createStatement()) { st.executeUpdate("DELETE FROM rol_permiso WHERE id_rol<>1"); }
                try (PreparedStatement ps = c.prepareStatement("INSERT IGNORE INTO rol_permiso (id_rol, id_permiso) VALUES (?,?)")) {
                    for (String m : marcados) {                       // formato "rol_idPermiso"
                        String[] p = m.split("_");
                        if (p.length != 2) continue;
                        int rol = Integer.parseInt(p[0]);
                        if (!editables.contains(rol)) continue;
                        ps.setInt(1, rol); ps.setInt(2, Integer.parseInt(p[1])); ps.addBatch();
                    }
                    ps.executeBatch();
                }
                try (Statement st = c.createStatement()) {
                    st.executeUpdate("INSERT IGNORE INTO rol_permiso (id_rol, id_permiso) SELECT 3, id_permiso FROM permiso WHERE codigo='ASIGNAR_ROLES'");
                    st.executeUpdate("INSERT IGNORE INTO rol_permiso (id_rol, id_permiso) SELECT r.id_rol, rp.id_permiso FROM rol_sistema r JOIN rol_permiso rp ON rp.id_rol=1 WHERE r.id_rol<>1");
                }
                c.commit();
            } catch (SQLException e) { c.rollback(); throw e; }
        }
    }

    /** Personas activas con algún rol además de Miembro (Owner primero). */
    public List<Miembro> personasConRol() throws SQLException {
        String sql = "SELECT m.id_miembro, m.nombre, m.apodo, GROUP_CONCAT(mr.id_rol) AS roles, MAX(mr.id_rol=3) AS owner FROM miembro m " +
                     "JOIN miembro_rol mr ON mr.id_miembro=m.id_miembro " +
                     "WHERE mr.id_rol<>1 AND m.estado='ACTIVO' GROUP BY m.id_miembro, m.nombre, m.apodo ORDER BY owner DESC, m.nombre";
        try (Connection c = getConnection(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            List<Miembro> l = new ArrayList<>();
            while (rs.next()) {
                Miembro m = new Miembro();
                m.setIdMiembro(rs.getInt(1)); m.setNombre(rs.getString(2)); m.setApodo(rs.getString(3));
                for (String x : rs.getString(4).split(",")) m.getRolIds().add(Integer.parseInt(x));
                m.setAdmin(m.tieneRol(OWNER)); m.setEquipoEnsayos(m.tieneRol(EQUIPO));
                l.add(m);
            }
            return l;
        }
    }

    public boolean esRolExtra(Integer idRol) throws SQLException {
        if (idRol == null) return false;
        for (Rol r : rolesExtra()) if (r.getIdRol() == idRol) return true;
        return false;
    }

    public void asignar(int idMiembro, int idRol, int idAdmin) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("INSERT IGNORE INTO miembro_rol (id_miembro, id_rol, asignado_por) VALUES (?,?,?)")) {
            ps.setInt(1, idMiembro); ps.setInt(2, idRol); ps.setInt(3, idAdmin); ps.executeUpdate();
        }
    }

    /** Lanza SQLException con el mensaje del trigger si se intenta dejar el sistema sin Owner. */
    public void quitar(int idMiembro, int idRol) throws SQLException {
        if (idRol == MIEMBRO) return;
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("DELETE FROM miembro_rol WHERE id_miembro=? AND id_rol=?")) {
            ps.setInt(1, idMiembro); ps.setInt(2, idRol); ps.executeUpdate();
        }
    }
}
