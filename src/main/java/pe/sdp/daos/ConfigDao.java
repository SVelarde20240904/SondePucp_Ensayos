package pe.sdp.daos;

import java.sql.*;
import java.util.*;

/** Ajustes sueltos de la web (clave -> valor), por ejemplo los emojis del pie de cada sección. */
public class ConfigDao extends BaseDao {

    /** Secciones con su barra de emojis: INICIO = los admins, y un team por sección. */
    public static final List<String> SECCIONES_PIE = List.of("INICIO", "ENSAYOS", "TALLERES", "FINANZAS");

    public Map<String, String> emojis() throws SQLException {
        Map<String, String> m = new LinkedHashMap<>();
        for (String s : SECCIONES_PIE) m.put(s, "");
        try (Connection c = getConnection(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT clave, valor FROM config WHERE clave LIKE 'emojis.%'")) {
            while (rs.next()) m.put(rs.getString(1).substring(7), rs.getString(2) == null ? "" : rs.getString(2));
        }
        return m;
    }

    public String valor(String clave, String porDefecto) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("SELECT valor FROM config WHERE clave=?")) {
            ps.setString(1, clave);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() && rs.getString(1) != null ? rs.getString(1) : porDefecto; }
        }
    }

    public void guardar(String clave, String valor) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(
                "INSERT INTO config (clave, valor) VALUES (?,?) ON DUPLICATE KEY UPDATE valor=VALUES(valor)")) {
            ps.setString(1, clave); ps.setString(2, valor); ps.executeUpdate();
        }
    }
}
