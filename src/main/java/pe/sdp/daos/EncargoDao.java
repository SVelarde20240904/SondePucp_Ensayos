package pe.sdp.daos;

import pe.sdp.beans.Encargo;
import pe.sdp.beans.ResumenPersona;
import pe.sdp.beans.TipoEncargo;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Responsabilidades semanales: el estado lo calcula la vista v_encargo_estado. */
public class EncargoDao extends BaseDao {

    public List<Encargo> deSemana(LocalDate sabado) throws SQLException {
        String sql = "SELECT v.*, t.nombre AS tipo, r.nombre AS resp, cp.nombre AS cumple FROM v_encargo_estado v " +
                     "JOIN tipo_encargo t ON t.id_tipo=v.id_tipo JOIN miembro r ON r.id_miembro=v.responsable " +
                     "LEFT JOIN miembro cp ON cp.id_miembro=v.cumplido_por WHERE v.semana=? ORDER BY v.plazo IS NULL, v.plazo, t.nombre";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            setFecha(ps, 1, sabado);
            return mapearLista(ps);
        }
    }

    /** Las responsabilidades de una persona (a su cargo o que cubrió), de la más reciente a la más antigua. */
    public List<Encargo> deMiembro(int idMiembro, int limite) throws SQLException {
        String sql = "SELECT v.*, t.nombre AS tipo, r.nombre AS resp, cp.nombre AS cumple FROM v_encargo_estado v " +
                     "JOIN tipo_encargo t ON t.id_tipo=v.id_tipo JOIN miembro r ON r.id_miembro=v.responsable " +
                     "LEFT JOIN miembro cp ON cp.id_miembro=v.cumplido_por WHERE v.responsable=? OR v.cumplido_por=? " +
                     "ORDER BY v.semana DESC, v.plazo IS NULL, v.plazo, t.nombre LIMIT ?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idMiembro); ps.setInt(2, idMiembro); ps.setInt(3, limite);
            return mapearLista(ps);
        }
    }

    /** Cuántas responsabilidades pendientes (aún en plazo) tiene a su cargo. */
    public int pendientesDe(int idMiembro) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(
                "SELECT COUNT(*) FROM v_encargo_estado WHERE responsable=? AND estado='PENDIENTE'")) {
            ps.setInt(1, idMiembro);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getInt(1); }
        }
    }

    private List<Encargo> mapearLista(PreparedStatement ps) throws SQLException {
        {
            List<Encargo> l = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Encargo e = new Encargo();
                    e.setIdEncargo(rs.getInt("id_encargo")); e.setIdTipo(rs.getInt("id_tipo")); e.setTipo(rs.getString("tipo"));
                    e.setSemana(fecha(rs, "semana")); e.setIdResponsable(rs.getInt("responsable")); e.setResponsable(rs.getString("resp"));
                    e.setPlazo(fechaHora(rs, "plazo")); e.setIdCumplidoPor(entero(rs, "cumplido_por")); e.setCumplidoPor(rs.getString("cumple"));
                    e.setFechaCumplimiento(fechaHora(rs, "fecha_cumplimiento")); e.setJustificada(rs.getBoolean("justificada"));
                    e.setMotivoJustificacion(rs.getString("motivo_justificacion")); e.setEstado(rs.getString("estado")); e.setCobertura(rs.getBoolean("es_cobertura"));
                    l.add(e);
                }
            }
            return l;
        }
    }

    public List<TipoEncargo> tiposActivos() throws SQLException {
        try (Connection c = getConnection(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM tipo_encargo WHERE activo=1 ORDER BY nombre")) {
            List<TipoEncargo> l = new ArrayList<>();
            while (rs.next()) { TipoEncargo t = new TipoEncargo(); t.setIdTipo(rs.getInt("id_tipo")); t.setNombre(rs.getString("nombre")); t.setActivo(true); l.add(t); }
            return l;
        }
    }

    public void asignar(int idTipo, LocalDate semana, int responsable, LocalDateTime plazo) throws SQLException {
        String sql = "INSERT INTO encargo (id_tipo, id_ensayo, semana, responsable, plazo) VALUES (?, (SELECT id_ensayo FROM ensayo WHERE fecha=? LIMIT 1), ?, ?, ?)";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idTipo); setFecha(ps, 2, semana); setFecha(ps, 3, semana); ps.setInt(4, responsable); setFechaHora(ps, 5, plazo);
            ps.executeUpdate();
        }
    }

    /** Marca como cumplido ahora por 'quien' (si no es el responsable, queda como cobertura). */
    public void cumplir(int idEncargo, int quien) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("UPDATE encargo SET cumplido_por=?, fecha_cumplimiento=NOW() WHERE id_encargo=?")) {
            ps.setInt(1, quien); ps.setInt(2, idEncargo); ps.executeUpdate();
        }
    }

    public void justificar(int idEncargo, String motivo) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("UPDATE encargo SET justificada=1, motivo_justificacion=? WHERE id_encargo=?")) {
            setTexto(ps, 1, motivo); ps.setInt(2, idEncargo); ps.executeUpdate();
        }
    }

    public void eliminar(int idEncargo) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("DELETE FROM encargo WHERE id_encargo=?")) {
            ps.setInt(1, idEncargo); ps.executeUpdate();
        }
    }

    /** Por persona en un mes: a = cumplidos a tiempo, b = total asignados, c = coberturas. */
    public List<ResumenPersona> resumenMes(LocalDate cualquierDiaDelMes) throws SQLException {
        String sql = "SELECT m.id_miembro, m.nombre, " +
                     " SUM(v.responsable=m.id_miembro AND v.estado='A_TIEMPO' AND (v.cumplido_por IS NULL OR v.cumplido_por=m.id_miembro)) AS a, " +
                     " SUM(v.responsable=m.id_miembro) AS b, SUM(v.cumplido_por=m.id_miembro AND v.responsable<>m.id_miembro) AS c " +
                     "FROM v_encargo_estado v JOIN miembro m ON m.id_miembro IN (v.responsable, v.cumplido_por) " +
                     "WHERE v.semana BETWEEN ? AND LAST_DAY(?) GROUP BY m.id_miembro, m.nombre ORDER BY b DESC, m.nombre";
        LocalDate ini = cualquierDiaDelMes.withDayOfMonth(1);
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            setFecha(ps, 1, ini); setFecha(ps, 2, ini);
            List<ResumenPersona> l = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ResumenPersona r = new ResumenPersona();
                    r.setIdMiembro(rs.getInt(1)); r.setNombre(rs.getString(2)); r.setA(rs.getInt(3)); r.setB(rs.getInt(4)); r.setC(rs.getInt(5));
                    l.add(r);
                }
            }
            return l;
        }
    }
}
