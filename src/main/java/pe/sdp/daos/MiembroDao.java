package pe.sdp.daos;

import pe.sdp.beans.Miembro;
import pe.sdp.beans.Team;

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
        "   WHERE a.id_miembro=m.id_miembro AND a.estado_real IN ('AP','AN','TJ','TI') AND e.fecha BETWEEN DATE_FORMAT(CURDATE(),'%Y-%m-01') AND CURDATE()) AS asistidos_mes, " +
        " (SELECT GROUP_CONCAT(mr.id_rol) FROM miembro_rol mr WHERE mr.id_miembro=m.id_miembro) AS rol_ids, " +
        " (SELECT GROUP_CONCAT(t.id_team ORDER BY t.orden) FROM miembro_team mt JOIN team t ON t.id_team=mt.id_team WHERE mt.id_miembro=m.id_miembro) AS team_ids, " +
        " (SELECT GROUP_CONCAT(t.nombre ORDER BY t.orden SEPARATOR '|') FROM miembro_team mt JOIN team t ON t.id_team=mt.id_team WHERE mt.id_miembro=m.id_miembro) AS team_nombres " +
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
        m.setApodo(rs.getString("apodo"));
        m.setFechaNacimiento(fecha(rs, "fecha_nacimiento"));
        m.setRolIds(ids(rs.getString("rol_ids")));
        m.setTeamIds(ids(rs.getString("team_ids")));
        String tn = rs.getString("team_nombres");
        m.setTeams(tn == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(tn.split("\\|"))));
        return m;
    }

    private static Set<Integer> ids(String csv) {
        Set<Integer> s = new LinkedHashSet<>();
        if (csv != null) for (String x : csv.split(",")) if (!x.isBlank()) s.add(Integer.parseInt(x.trim()));
        return s;
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

    /** filtro: TODOS, LEADER, FOLLOWER, EQUIPO, INACTIVOS · busqueda: texto libre (nombre, nombre completo o apodo) */
    public List<Miembro> listar(String filtro, String busqueda) throws SQLException { return listar(filtro, busqueda, null); }

    /** Igual, y si idTeam no es null solo los de ese team. */
    public List<Miembro> listar(String filtro, String busqueda, Integer idTeam) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT_ADMIN).append("WHERE 1=1 ");
        if ("INACTIVOS".equals(filtro)) sql.append("AND m.estado='INACTIVO' ");
        else {
            sql.append("AND m.estado='ACTIVO' ");
            if ("LEADER".equals(filtro) || "FOLLOWER".equals(filtro)) sql.append("AND m.rol_baile=? ");
            if ("EQUIPO".equals(filtro)) sql.append("AND v.es_equipo_ensayos=1 ");
        }
        if (idTeam != null) sql.append("AND EXISTS (SELECT 1 FROM miembro_team mt WHERE mt.id_miembro=m.id_miembro AND mt.id_team=?) ");
        if (busqueda != null && !busqueda.isBlank()) sql.append("AND (m.nombre LIKE ? OR m.nombre_completo LIKE ? OR m.apodo LIKE ?) ");
        sql.append("ORDER BY m.nombre");
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql.toString())) {
            int i = 1;
            if ("LEADER".equals(filtro) || "FOLLOWER".equals(filtro)) ps.setString(i++, filtro);
            if (idTeam != null) ps.setInt(i++, idTeam);
            if (busqueda != null && !busqueda.isBlank()) for (int k = 0; k < 3; k++) ps.setString(i++, "%" + busqueda.trim() + "%");
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
                     "aviso_asistencia=?, aviso_mensual=?, aviso_ensayo=?, apodo=?, fecha_nacimiento=? WHERE id_miembro=?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, m.getNombreCompleto());
            setTexto(ps, 2, m.getCodigoPucp());
            setTexto(ps, 3, m.getFacultad());
            setTexto(ps, 4, m.getCorreo() == null ? null : m.getCorreo().toLowerCase());
            setTexto(ps, 5, m.getCelular());
            ps.setBoolean(6, m.isAvisoAsistencia());
            ps.setBoolean(7, m.isAvisoMensual());
            ps.setBoolean(8, m.isAvisoEnsayo());
            setTexto(ps, 9, m.getApodo());
            setFecha(ps, 10, m.getFechaNacimiento());
            ps.setInt(11, m.getIdMiembro());
            ps.executeUpdate();
        }
    }

    // ===================== ADMINISTRACIÓN =====================

    /**
     * Alta de miembro. El trigger le da el rol MIEMBRO; aquí se agregan los roles extra y sus teams.
     * roles / teams en null = no se tocan.
     */
    public int crear(Miembro m, String passwordHash, Set<Integer> roles, Set<Integer> teams, int idAdmin) throws SQLException {
        String sql = "INSERT INTO miembro (nombre, nombre_completo, codigo_pucp, correo, celular, facultad, rol_baile, frecuencia, " +
                     "fecha_ingreso, apodo, fecha_nacimiento, password_hash, debe_cambiar_password, creado_por) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,1,?)";
        try (Connection c = getConnection()) {
            c.setAutoCommit(false);
            try {
                int id;
                try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    llenarDatos(ps, m);
                    ps.setString(12, passwordHash);
                    ps.setInt(13, idAdmin);
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) { rs.next(); id = rs.getInt(1); }
                }
                guardarRoles(c, id, roles, idAdmin);
                guardarTeams(c, id, teams, idAdmin);
                c.commit();
                return id;
            } catch (SQLException e) { c.rollback(); throw e; }
        }
    }

    public void actualizar(Miembro m, Set<Integer> roles, Set<Integer> teams, int idAdmin) throws SQLException {
        String sql = "UPDATE miembro SET nombre=?, nombre_completo=?, codigo_pucp=?, correo=?, celular=?, facultad=?, rol_baile=?, " +
                     "frecuencia=?, fecha_ingreso=?, apodo=?, fecha_nacimiento=? WHERE id_miembro=?";
        try (Connection c = getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    llenarDatos(ps, m);
                    ps.setInt(12, m.getIdMiembro());
                    ps.executeUpdate();
                }
                guardarRoles(c, m.getIdMiembro(), roles, idAdmin);
                guardarTeams(c, m.getIdMiembro(), teams, idAdmin);
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
        setTexto(ps, 10, m.getApodo());
        setFecha(ps, 11, m.getFechaNacimiento());
    }

    /** Roles que se pueden poner o quitar (el 1, Miembro, lo tienen todos). El trigger impide quedarse sin Owner. */
    public static final List<Integer> ROLES_EXTRA = List.of(2, 4, 5, 3);

    private void guardarRoles(Connection c, int id, Set<Integer> roles, int idAdmin) throws SQLException {
        if (roles == null) return;
        // primero se agregan y luego se quitan: así un Owner que se edita a sí mismo no choca con el trigger
        for (int rol : ROLES_EXTRA) if (roles.contains(rol)) {
            try (PreparedStatement ps = c.prepareStatement("INSERT IGNORE INTO miembro_rol (id_miembro, id_rol, asignado_por) VALUES (?,?,?)")) {
                ps.setInt(1, id); ps.setInt(2, rol); ps.setInt(3, idAdmin); ps.executeUpdate();
            }
        }
        for (int rol : ROLES_EXTRA) if (!roles.contains(rol)) {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM miembro_rol WHERE id_miembro=? AND id_rol=?")) {
                ps.setInt(1, id); ps.setInt(2, rol); ps.executeUpdate();
            }
        }
    }

    private void guardarTeams(Connection c, int id, Set<Integer> teams, int idAdmin) throws SQLException {
        if (teams == null) return;
        try (PreparedStatement del = c.prepareStatement("DELETE FROM miembro_team WHERE id_miembro=?")) { del.setInt(1, id); del.executeUpdate(); }
        try (PreparedStatement ps = c.prepareStatement("INSERT IGNORE INTO miembro_team (id_miembro, id_team, asignado_por) VALUES (?,?,?)")) {
            for (int t : teams) { ps.setInt(1, id); ps.setInt(2, t); ps.setInt(3, idAdmin); ps.addBatch(); }
            ps.executeBatch();
        }
    }

    /** Los teams de SDP, en orden. */
    public List<Team> teams() throws SQLException {
        try (Connection c = getConnection(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT id_team, codigo, nombre FROM team ORDER BY orden, nombre")) {
            List<Team> l = new ArrayList<>();
            while (rs.next()) l.add(new Team(rs.getInt(1), rs.getString(2), rs.getString(3)));
            return l;
        }
    }

    /** Cumpleaños de los próximos "dias" días (incluye hoy), ordenados por cercanía. Solo miembros activos. */
    public List<Miembro> cumpleanosProximos(int dias) throws SQLException {
        java.time.LocalDate hoy = java.time.LocalDate.now(pe.sdp.util.Formato.ZONA);
        List<Miembro> l = new ArrayList<>();
        Map<Integer, Long> falta = new HashMap<>();
        try (Connection c = getConnection(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT id_miembro, nombre, apodo, fecha_nacimiento FROM miembro WHERE estado='ACTIVO' AND fecha_nacimiento IS NOT NULL")) {
            while (rs.next()) {
                java.time.LocalDate n = rs.getDate(4).toLocalDate();
                java.time.LocalDate prox;
                try { prox = n.withYear(hoy.getYear()); } catch (java.time.DateTimeException e) { prox = java.time.LocalDate.of(hoy.getYear(), 3, 1); } // 29/02
                if (prox.isBefore(hoy)) { try { prox = n.withYear(hoy.getYear() + 1); } catch (java.time.DateTimeException e) { prox = java.time.LocalDate.of(hoy.getYear() + 1, 3, 1); } }
                long d = java.time.temporal.ChronoUnit.DAYS.between(hoy, prox);
                if (d < dias) {
                    Miembro m = new Miembro();
                    m.setIdMiembro(rs.getInt(1)); m.setNombre(rs.getString(2)); m.setApodo(rs.getString(3)); m.setFechaNacimiento(n);
                    l.add(m); falta.put(m.getIdMiembro(), d);
                }
            }
        }
        l.sort(Comparator.comparing(m -> falta.get(m.getIdMiembro())));
        return l;
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
