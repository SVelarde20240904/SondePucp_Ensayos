package pe.sdp.daos;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import pe.sdp.util.Config;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Conexión JDBC común, con POOL de conexiones (tomcat-jdbc, ya viene dentro de Tomcat):
 * en vez de abrir una conexión nueva a MySQL en cada consulta, se reusan las que ya están abiertas.
 * Los datos de conexión salen de pe.sdp.util.Config (variables de entorno o db.properties).
 */
public abstract class BaseDao {

    private static volatile DataSource pool;

    private static DataSource pool() {
        if (pool == null) {
            synchronized (BaseDao.class) {
                if (pool == null) {
                    PoolProperties p = new PoolProperties();
                    p.setDriverClassName(Config.dbDriver());
                    p.setUrl(Config.dbUrl());
                    p.setUsername(Config.dbUser());
                    p.setPassword(Config.dbPassword());
                    p.setInitialSize(2);
                    p.setMinIdle(2);
                    p.setMaxIdle(10);
                    p.setMaxActive(20);
                    p.setMaxWait(10_000);
                    p.setTestOnBorrow(true);            // si MySQL cortó la conexión, se descarta y se abre otra
                    p.setValidationQuery("SELECT 1");
                    p.setValidationInterval(30_000);
                    p.setTimeBetweenEvictionRunsMillis(60_000);
                    p.setRemoveAbandoned(true);         // conexiones olvidadas sin cerrar se recuperan
                    p.setRemoveAbandonedTimeout(120);
                    p.setLogAbandoned(true);
                    DataSource ds = new DataSource();
                    ds.setPoolProperties(p);
                    pool = ds;
                }
            }
        }
        return pool;
    }

    /** Lo llama el listener al apagar la aplicación. */
    public static void cerrarPool() {
        if (pool != null) { pool.close(true); pool = null; }
    }

    protected Connection getConnection() throws SQLException {
        return pool().getConnection();
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
