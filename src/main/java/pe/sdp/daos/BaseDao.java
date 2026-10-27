package pe.sdp.daos;

import java.io.InputStream;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Properties;

/** Conexión JDBC común. Lee los datos de src/main/resources/db.properties. */
public abstract class BaseDao {

    private static final Properties CONF = new Properties();

    static {
        try (InputStream in = BaseDao.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in == null) throw new IllegalStateException("No se encontró db.properties");
            CONF.load(in);
            Class.forName(CONF.getProperty("db.driver"));
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    protected Connection getConnection() throws SQLException {
        return DriverManager.getConnection(CONF.getProperty("db.url"),
                CONF.getProperty("db.user"), CONF.getProperty("db.password"));
    }

    // ---------- ayudas para leer columnas que pueden ser NULL ----------
    protected static LocalDate fecha(ResultSet rs, String col) throws SQLException {
        Date d = rs.getDate(col);
        return d == null ? null : d.toLocalDate();
    }

    protected static LocalDateTime fechaHora(ResultSet rs, String col) throws SQLException {
        Timestamp t = rs.getTimestamp(col);
        return t == null ? null : t.toLocalDateTime();
    }

    protected static LocalTime hora(ResultSet rs, String col) throws SQLException {
        Time t = rs.getTime(col);
        return t == null ? null : t.toLocalTime();
    }

    protected static Integer entero(ResultSet rs, String col) throws SQLException {
        int v = rs.getInt(col);
        return rs.wasNull() ? null : v;
    }

    protected static void setEntero(PreparedStatement ps, int i, Integer v) throws SQLException {
        if (v == null) ps.setNull(i, Types.INTEGER); else ps.setInt(i, v);
    }

    protected static void setTexto(PreparedStatement ps, int i, String v) throws SQLException {
        if (v == null || v.isBlank()) ps.setNull(i, Types.VARCHAR); else ps.setString(i, v.trim());
    }

    protected static void setFecha(PreparedStatement ps, int i, LocalDate v) throws SQLException {
        if (v == null) ps.setNull(i, Types.DATE); else ps.setDate(i, Date.valueOf(v));
    }

    protected static void setFechaHora(PreparedStatement ps, int i, LocalDateTime v) throws SQLException {
        if (v == null) ps.setNull(i, Types.TIMESTAMP); else ps.setTimestamp(i, Timestamp.valueOf(v));
    }
}
