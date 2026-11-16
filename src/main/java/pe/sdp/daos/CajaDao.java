package pe.sdp.daos;

import pe.sdp.beans.Cuota;
import pe.sdp.util.Formato;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

/**
 * Caja de Finanzas: saldo inicial + movimientos (ingresos y gastos registrados a mano o importados del Excel)
 * + las cuotas pagadas por la web (pagos confirmados desde que la web cobra, Cuota.INICIO), que entran solas.
 */
public class CajaDao extends BaseDao {

    public static final List<String> CATEGORIAS_GASTO = List.of("Talleres", "Eventos", "Equipos", "Casillero", "Comida", "Movilidad", "Materiales y merch", "Otros");
    public static final List<String> CATEGORIAS_INGRESO = List.of("Cuotas", "Eventos", "Donaciones", "Ventas", "Otros");
    /** Desde aquí las cuotas entran solas (pagos confirmados en la web); antes, como movimientos del Excel. */
    public static final LocalDate CUOTAS_WEB_DESDE = Cuota.INICIO.atDay(1);

    /** Un movimiento: {id, fecha, tipo, categoria, concepto, monto, proyecto, registradoPor, automatico} */
    public List<Map<String, Object>> movimientos(Integer anio) throws SQLException {
        List<Map<String, Object>> l = new ArrayList<>();
        String sql = "SELECT k.*, m.nombre FROM caja_movimiento k LEFT JOIN miembro m ON m.id_miembro=k.registrado_por " +
                     (anio == null ? "" : "WHERE YEAR(k.fecha)=? ") + "ORDER BY k.fecha DESC, k.id DESC";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (anio != null) ps.setInt(1, anio);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", rs.getInt("id"));
                    LocalDate f = fecha(rs, "fecha");
                    m.put("fecha", f); m.put("fechaTexto", Formato.ddmmyyyy(f));
                    m.put("tipo", rs.getString("tipo")); m.put("categoria", rs.getString("categoria"));
                    m.put("concepto", rs.getString("concepto")); m.put("monto", rs.getBigDecimal("monto"));
                    m.put("montoTexto", Formato.soles(rs.getBigDecimal("monto")));
                    m.put("proyecto", rs.getString("proyecto")); m.put("registradoPor", rs.getString("nombre"));
                    m.put("automatico", false);
                    l.add(m);
                }
            }
        }
        return l;
    }

    /** Cuotas cobradas por la web, por mes (pagos confirmados con fecha desde CUOTAS_WEB_DESDE). */
    public Map<YearMonth, BigDecimal> cuotasWebPorMes() throws SQLException {
        Map<YearMonth, BigDecimal> m = new TreeMap<>();
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(
                "SELECT YEAR(fecha), MONTH(fecha), SUM(monto) FROM pago_cuota WHERE estado='CONFIRMADO' AND monto IS NOT NULL AND fecha >= ? GROUP BY 1, 2")) {
            setFecha(ps, 1, CUOTAS_WEB_DESDE);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) m.put(YearMonth.of(rs.getInt(1), rs.getInt(2)), rs.getBigDecimal(3)); }
        }
        return m;
    }

    public int agregar(LocalDate fecha, String tipo, String categoria, String concepto, BigDecimal monto, String proyecto, int yo) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(
                "INSERT INTO caja_movimiento (fecha, tipo, categoria, concepto, monto, proyecto, registrado_por) VALUES (?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
            setFecha(ps, 1, fecha); ps.setString(2, tipo); ps.setString(3, categoria); ps.setString(4, concepto);
            ps.setBigDecimal(5, monto); setTexto(ps, 6, proyecto); ps.setInt(7, yo);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) { rs.next(); return rs.getInt(1); }
        }
    }

    public void eliminar(int id) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("DELETE FROM caja_movimiento WHERE id=?")) {
            ps.setInt(1, id); ps.executeUpdate();
        }
    }

    /** Proyectos ya usados (para sugerirlos al registrar). */
    public List<String> proyectos() throws SQLException {
        List<String> l = new ArrayList<>();
        try (Connection c = getConnection(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT proyecto FROM caja_movimiento WHERE proyecto IS NOT NULL GROUP BY proyecto ORDER BY MAX(fecha) DESC")) {
            while (rs.next()) l.add(rs.getString(1));
        }
        return l;
    }

    /** Por mes (cuotas de la matriz): {mes, al día (pagadas + exoneradas), total}. Solo meses ya vencidos o el actual. */
    public List<Object[]> alDiaPorMes() throws SQLException {
        List<Object[]> l = new ArrayList<>();
        try (Connection c = getConnection(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT anio, mes, SUM(estado IN ('PAGADA','EXONERADA')), COUNT(*) FROM cuota GROUP BY anio, mes ORDER BY anio, mes")) {
            YearMonth hoy = YearMonth.now(Formato.ZONA);
            while (rs.next()) {
                YearMonth ym = YearMonth.of(rs.getInt(1), rs.getInt(2));
                if (ym.isAfter(hoy)) continue;
                l.add(new Object[]{ym, rs.getInt(3), rs.getInt(4)});
            }
        }
        return l;
    }
}
