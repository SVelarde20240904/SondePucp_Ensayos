package pe.sdp.daos;

import pe.sdp.beans.Miembro;

import java.sql.*;
import java.util.*;

/** Miembros: login, perfil y administración (alta, edición, baja). */
public class MiembroDao extends BaseDao {

    private static final String SELECT_ADMIN =
        "SELECT m.*, v.nivel, v.es_equipo_ensayos, v.es_admin, v.cuenta_activada, v.puede_eliminar, " +
        " (m.id_miembro IN (SELECT id_miembro FROM v_miembro_cuenta_color)) AS cuenta_color, " +
        " (SELECT COUNT(*) FROM asistencia a JOIN ensayo e ON e.id_ensayo=a.id_ensayo " +
        "   WHERE a.id_miembro=m.id_miembro AND e.estado<>'CANCELADO' AND e.fecha BETWEEN DATE_FORMAT(CURDATE(),'%Y-%m-01') AND CURDATE()) AS ensayos_mes, " +
        " (SELECT COUNT(*) FROM asistencia a JOIN ensayo e ON e.id_ensayo=a.id_ensayo " +
        "   WHERE a.id_miembro=m.id_miembro AND a.estado_real IN ('AP','AN','TJ','TI') AND e.fecha BETWEEN DATE_FORMAT(CURDATE(),'%Y-%m-01') AND CURDATE()) AS asistidos_mes " +
        "FROM miembro m JOIN v_miembro_admin v ON v.id_miembro = m.id_miembro ";

    private Miembro mapear(ResultSet rs) throws SQLException {
        Miembro m = new Miembro();
        m.setIdMiembro(rs.getInt("id_miembro"));
        m.setNombre(rs.getString("nombre"));
        m.setNombreCompleto(rs.getString("nombre_completo"));
        m.setCodigoPucp(rs.getString("codigo_pucp"));
        m.setCorreo(rs.getString("correo"));
        m.setPasswordHash(rs.getString("password_hash"));
        m.setCelular(rs.getString("celular"));
        m.setFacultad(rs.getString("facultad"));
        m.setRolBaile(rs.getString("rol_baile"));
        m.setFrecuencia(rs.getString("frecuencia"));
        m.setEstado(rs.getString("estado"));
        m.setFechaIngreso(fecha(rs, "fecha_ingreso"));
        m.setNotas(rs.getString("notas"));
        m.setAvisoAsistencia(rs.getBoolean("aviso_asistencia"));
        m.setAvisoMensual(rs.getBoolean("aviso_mensual"));
        m.setAvisoEnsayo(rs.getBoolean("aviso_ensayo"));
        m.setDebeCambiarPassword(rs.getBoolean("debe_cambiar_password"));
        m.setFechaBaja(fechaHora(rs, "fecha_baja"));
        m.setMotivoBaja(rs.getString("motivo_baja"));
        m.setNivel(rs.getString("nivel"));
        m.setEquipoEnsayos(rs.getBoolean("es_equipo_ensayos"));
        m.setAdmin(rs.getBoolean("es_admin"));
        m.setCuentaActivada(rs.getBoolean("cuenta_activada"));
        m.setPuedeEliminar(rs.getBoolean("puede_eliminar"));
        m.setCuentaColor(rs.getBoolean("cuenta_color"));
        m.setEnsayosMes(rs.getInt("ensayos_mes"));
        m.setAsistidosMes(rs.getInt("asistidos_mes"));
        return m;
    }

    public Miembro buscarPorCorreo(String correo) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(SELECT_ADMIN + "WHERE m.correo = ?")) {
            ps.setString(1, correo == null ? "" : correo.trim().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? mapear(rs) : null; }
        }
    }

    public Miembro obtener(int id) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(SELECT_ADMIN + "WHERE m.id_miembro = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? mapear(rs) : null; }
        }
    }

    /** filtro: TODOS, LEADER, FOLLOWER, EQUIPO, INACTIVOS · busqueda: texto libre */
    public List<Miembro> listar(String filtro, String busqueda) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT_ADMIN).append("WHERE 1=1 ");
        if ("INACTIVOS".equals(filtro)) sql.append("AND m.estado='INACTIVO' ");
        else {
            sql.append("AND m.estado='ACTIVO' ");
            if ("LEADER".equals(filtro) || "FOLLOWER".equals(filtro)) sql.append("AND m.rol_baile=? ");
            if ("EQUIPO".equals(filtro)) sql.append("AND v.es_equipo_ensayos=1 ");
        }
        if (busqueda != null && !busqueda.isBlank()) sql.append("AND (m.nombre LIKE ? OR m.nombre_completo LIKE ?) ");
        sql.append("ORDER BY m.nombre");
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql.toString())) {
            int i = 1;
            if ("LEADER".equals(filtro) || "FOLLOWER".equals(filtro)) ps.setString(i++, filtro);
            if (busqueda != null && !busqueda.isBlank()) { ps.setString(i++, "%" + busqueda.trim() + "%"); ps.setString(i, "%" + busqueda.trim() + "%"); }
            List<Miembro> lista = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) lista.add(mapear(rs)); }
            return lista;
        }
    }

    public List<Miembro> listarActivos() throws SQLException { return listar("TODOS", null); }

    public int contarActivos() throws SQLException {
        try (Connection c = getConnection(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM miembro WHERE estado='ACTIVO'")) {
            rs.next(); return rs.getInt(1);
        }
    }

    /** Códigos de permiso del usuario (unión de todos sus roles). */
    public Set<String> permisos(int idMiembro) throws SQLException {
        String sql = "SELECT DISTINCT p.codigo FROM miembro_rol mr JOIN rol_permiso rp ON rp.id_rol = mr.id_rol " +
                     "JOIN permiso p ON p.id_permiso = rp.id_permiso WHERE mr.id_miembro = ?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idMiembro);
            Set<String> s = new HashSet<>();
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) s.add(rs.getString(1)); }
            return s;
        }
    }

    public void registrarPassword(int idMiembro, String hash, boolean debeCambiar) throws SQLException {
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE miembro SET password_hash=?, debe_cambiar_password=? WHERE id_miembro=?")) {
            ps.setString(1, hash); ps.setBoolean(2, debeCambiar); ps.setInt(3, idMiembro);
            ps.executeUpdate();
        }
    }

    /** El propio miembro edita su perfil: NO puede tocar rol de baile, frecuencia ni estado. */
    public void actualizarPerfil(Miembro m) throws SQLException {
        String sql = "UPDATE miembro SET nombre_completo=?, codigo_pucp=?, facultad=?, correo=?, celular=?, " +
                     "aviso_asistencia=?, aviso_mensual=?, aviso_ensayo=? WHERE id_miembro=?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, m.getNombreCompleto());
            setTexto(ps, 2, m.getCodigoPucp());
            setTexto(ps, 3, m.getFacultad());
            setTexto(ps, 4, m.getCorreo() == null ? null : m.getCorreo().toLowerCase());
            setTexto(ps, 5, m.getCelular());
            ps.setBoolean(6, m.isAvisoAsistencia());
            ps.setBoolean(7, m.isAvisoMensual());
            ps.setBoolean(8, m.isAvisoEnsayo());
            ps.setInt(9, m.getIdMiembro());
            ps.executeUpdate();
        }
    }

    // ===================== ADMINISTRACIÓN =====================

    /** Alta de miembro. El trigger le da el rol MIEMBRO; aquí se agregan los roles extra. */
    public int crear(Miembro m, String passwordHash, boolean equipo, boolean admin, int idAdmin) throws SQLException {
        String sql = "INSERT INTO miembro (nombre, nombre_completo, codigo_pucp, correo, celular, facultad, rol_baile, frecuencia, " +
                     "fecha_ingreso, password_hash, debe_cambiar_password, creado_por) VALUES (?,?,?,?,?,?,?,?,?,?,1,?)";
        try (Connection c = getConnection()) {
            c.setAutoCommit(false);
            try {
                int id;
                try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    llenarDatos(ps, m);
                    ps.setString(10, passwordHash);
                    ps.setInt(11, idAdmin);
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) { rs.next(); id = rs.getInt(1); }
                }
                guardarRoles(c, id, equipo, admin, idAdmin);
                c.commit();
                return id;
            } catch (SQLException e) { c.rollback(); throw e; }
        }
    }

    public void actualizar(Miembro m, boolean equipo, boolean admin, int idAdmin) throws SQLException {
        String sql = "UPDATE miembro SET nombre=?, nombre_completo=?, codigo_pucp=?, correo=?, celular=?, facultad=?, rol_baile=?, " +
                     "frecuencia=?, fecha_ingreso=? WHERE id_miembro=?";
        try (Connection c = getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    llenarDatos(ps, m);
                    ps.setInt(10, m.getIdMiembro());
                    ps.executeUpdate();
                }
                guardarRoles(c, m.getIdMiembro(), equipo, admin, idAdmin);
                c.commit();
            } catch (SQLException e) { c.rollback(); throw e; }
        }
    }

    private void llenarDatos(PreparedStatement ps, Miembro m) throws SQLException {
        ps.setString(1, m.getNombre().trim());
        ps.setString(2, m.getNombreCompleto().trim());
        setTexto(ps, 3, m.getCodigoPucp());
        setTexto(ps, 4, m.getCorreo() == null ? null : m.getCorreo().toLowerCase());
        setTexto(ps, 5, m.getCelular());
        setTexto(ps, 6, m.getFacultad());
        setTexto(ps, 7, m.getRolBaile());
        setTexto(ps, 8, m.getFrecuencia());
        setFecha(ps, 9, m.getFechaIngreso());
    }

    /** Pone o quita los roles EQUIPO_ENSAYOS (2) y ADMIN (3). El trigger impide quedarse sin admin. */
    private void guardarRoles(Connection c, int id, boolean equipo, boolean admin, int idAdmin) throws SQLException {
        int[][] roles = {{2, equipo ? 1 : 0}, {3, admin ? 1 : 0}};
        for (int[] r : roles) {
            if (r[1] == 1) {
                try (PreparedStatement ps = c.prepareStatement("INSERT IGNORE INTO miembro_rol (id_miembro, id_rol, asignado_por) VALUES (?,?,?)")) {
                    ps.setInt(1, id); ps.setInt(2, r[0]); ps.setInt(3, idAdmin); ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = c.prepareStatement("DELETE FROM miembro_rol WHERE id_miembro=? AND id_rol=?")) {
                    ps.setInt(1, id); ps.setInt(2, r[0]); ps.executeUpdate();
                }
            }
        }
    }

    public void inactivar(int id, String motivo, int idAdmin) throws SQLException {
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE miembro SET estado='INACTIVO', baja_por=?, motivo_baja=? WHERE id_miembro=?")) {
            ps.setInt(1, idAdmin); setTexto(ps, 2, motivo); ps.setInt(3, id);
            ps.executeUpdate();
        }
    }

    public void reactivar(int id) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("UPDATE miembro SET estado='ACTIVO' WHERE id_miembro=?")) {
            ps.setInt(1, id); ps.executeUpdate();
        }
    }

    /** Solo borra un alta hecha por error (sin datos). Devuelve false si no se pudo. */
    public boolean eliminar(int id) throws SQLException {
        String sql = "DELETE FROM miembro WHERE id_miembro=? AND id_miembro IN (SELECT id_miembro FROM (SELECT id_miembro FROM v_miembro_admin WHERE puede_eliminar=1) x)";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() == 1;
        }
    }

    public void actualizarCorreo(int idMiembro, String correo) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("UPDATE miembro SET correo=? WHERE id_miembro=?")) {
            ps.setString(1, correo); ps.setInt(2, idMiembro);
            ps.executeUpdate();
        }
    }

    public boolean existeNombre(String nombre, int exceptoId) throws SQLException {
        return existe("nombre", nombre, exceptoId);
    }

    public boolean existeCorreo(String correo, int exceptoId) throws SQLException {
        return correo != null && !correo.isBlank() && existe("correo", correo.toLowerCase(), exceptoId);
    }

    private boolean existe(String col, String valor, int exceptoId) throws SQLException {
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT 1 FROM miembro WHERE " + col + "=? AND id_miembro<>?")) {
            ps.setString(1, valor.trim()); ps.setInt(2, exceptoId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    /** Miembros del equipo de ensayos activos (para elegir quién dicta). */
    public List<Miembro> equipoEnsayos() throws SQLException { return listar("EQUIPO", null); }
}
