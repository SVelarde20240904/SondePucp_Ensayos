package pe.sdp.daos;

import pe.sdp.beans.Cuota;
import pe.sdp.beans.PagoCuota;
import pe.sdp.util.Formato;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;

/** Cuotas mensuales y pagos (módulo Finanzas). */
public class CuotaDao extends BaseDao {

    private static volatile long ultimaGeneracion = 0;

    /** Desde qué mes paga alguien: lo que puso Finanzas, o 2 meses después de su ingreso (entra en octubre → paga desde diciembre). */
    private static final String DESDE = "CAST(GREATEST('" + Cuota.INICIO.atDay(1) + "', COALESCE(m.cuota_desde, " +
            "IF(m.fecha_ingreso IS NULL, '" + Cuota.INICIO.atDay(1) + "', DATE_FORMAT(DATE_ADD(m.fecha_ingreso, INTERVAL 2 MONTH), '%Y-%m-01')))) AS DATE)";

    private static final String SELECT = "SELECT c.*, m.nombre, m.celular, m.estado AS m_estado, p.estado AS p_estado, p.fecha AS p_fecha, p.medio AS p_medio " +
            "FROM cuota c JOIN miembro m ON m.id_miembro = c.id_miembro LEFT JOIN pago_cuota p ON p.id_pago = c.id_pago ";

    private static LocalDate hoy() { return LocalDate.now(Formato.ZONA); }

    /** Crea las cuotas que falten (cada mes, para cada miembro activo que ya debe pagar). Es idempotente. */
    public void generar(boolean forzar) throws SQLException {
        long ahora = System.currentTimeMillis();
        if (!forzar && ahora - ultimaGeneracion < 10 * 60_000) return;
        YearMonth actual = YearMonth.from(hoy());
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement("INSERT IGNORE INTO cuota (id_miembro, anio, mes) SELECT m.id_miembro, ?, ? FROM miembro m " +
                     "WHERE m.estado = 'ACTIVO' AND " + DESDE + " <= ?")) {
            for (YearMonth ym = Cuota.INICIO; !ym.isAfter(actual); ym = ym.plusMonths(1)) {
                ps.setInt(1, ym.getYear()); ps.setInt(2, ym.getMonthValue()); ps.setDate(3, java.sql.Date.valueOf(ym.atDay(1)));
                ps.addBatch();
            }
            ps.executeBatch();
        }
        ultimaGeneracion = ahora;
    }

    private static Cuota leer(ResultSet rs) throws SQLException {
        Cuota q = new Cuota();
        q.setIdCuota(rs.getInt("id_cuota")); q.setIdMiembro(rs.getInt("id_miembro"));
        q.setNombre(rs.getString("nombre")); q.setCelular(rs.getString("celular"));
        q.setMiembroActivo("ACTIVO".equals(rs.getString("m_estado")));
        q.setAnio(rs.getInt("anio")); q.setMes(rs.getInt("mes")); q.setEstado(rs.getString("estado"));
        q.setMontoPagado(rs.getBigDecimal("monto_pagado")); q.setIdPago(entero(rs, "id_pago"));
        q.setPagoEstado(rs.getString("p_estado")); q.setPagoFecha(fechaHora(rs, "p_fecha")); q.setPagoMedio(rs.getString("p_medio"));
        q.setNota(rs.getString("nota"));
        return q;
    }

    private List<Cuota> lista(String where, Object... args) throws SQLException {
        List<Cuota> l = new ArrayList<>();
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(SELECT + where)) {
            for (int i = 0; i < args.length; i++) ps.setObject(i + 1, args[i]);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) l.add(leer(rs)); }
        }
        return l;
    }

    /** Las cuotas de un miembro, de la más nueva a la más vieja. */
    public List<Cuota> deMiembro(int id) throws SQLException {
        return lista("WHERE c.id_miembro = ? ORDER BY c.anio DESC, c.mes DESC", id);
    }

    public Cuota obtener(int idCuota) throws SQLException {
        List<Cuota> l = lista("WHERE c.id_cuota = ?", idCuota);
        return l.isEmpty() ? null : l.get(0);
    }

    /** Todas (para la matriz y los deudores). */
    public List<Cuota> todas() throws SQLException {
        return lista("ORDER BY m.nombre, c.anio, c.mes");
    }

    public List<Cuota> delMes(YearMonth ym) throws SQLException {
        return lista("WHERE c.anio = ? AND c.mes = ? ORDER BY m.nombre", ym.getYear(), ym.getMonthValue());
    }

    /** Meses que ya existen (para las columnas de la matriz). */
    public List<YearMonth> meses() throws SQLException {
        List<YearMonth> l = new ArrayList<>();
        try (Connection c = getConnection(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT DISTINCT anio, mes FROM cuota ORDER BY anio, mes")) {
            while (rs.next()) l.add(YearMonth.of(rs.getInt(1), rs.getInt(2)));
        }
        if (l.isEmpty()) l.add(YearMonth.from(hoy()));
        return l;
    }

    // ===================== PAGOS =====================

    /** El miembro manda su captura para uno o más meses pendientes. Devuelve cuántas cuotas cubre. */
    public int reportar(int idMiembro, List<Integer> idCuotas, byte[] voucher, String operacion) throws SQLException {
        try (Connection c = getConnection()) {
            c.setAutoCommit(false);
            try {
                int idPago;
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO pago_cuota (id_miembro, operacion, medio, voucher, estado, registrado_por) VALUES (?,?,'YAPE',?,'POR_CONFIRMAR',?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, idMiembro); setTexto(ps, 2, operacion); ps.setBytes(3, voucher); ps.setInt(4, idMiembro);
                    ps.executeUpdate();
                    try (ResultSet k = ps.getGeneratedKeys()) { k.next(); idPago = k.getInt(1); }
                }
                int n = amarrar(c, idPago, idMiembro, idCuotas);
                if (n == 0) { c.rollback(); return 0; }
                c.commit();
                return n;
            } catch (SQLException e) { c.rollback(); throw e; }
        }
    }

    /** Amarra las cuotas (solo las pendientes del miembro que no están ya en revisión) al pago. */
    private static int amarrar(Connection c, int idPago, int idMiembro, List<Integer> idCuotas) throws SQLException {
        int n = 0;
        try (PreparedStatement ps = c.prepareStatement("UPDATE cuota SET id_pago = ? WHERE id_cuota = ? AND id_miembro = ? AND estado = 'PENDIENTE' " +
                "AND (id_pago IS NULL OR NOT EXISTS (SELECT 1 FROM pago_cuota p WHERE p.id_pago = cuota.id_pago AND p.estado = 'POR_CONFIRMAR'))")) {
            for (Integer id : idCuotas) { ps.setInt(1, idPago); ps.setInt(2, id); ps.setInt(3, idMiembro); n += ps.executeUpdate(); }
        }
        return n;
    }

    /** Finanzas confirma la captura: las cuotas quedan pagadas con el monto que tocaba en la fecha de la captura. */
    public void confirmar(int idPago, int yo) throws SQLException {
        try (Connection c = getConnection()) {
            c.setAutoCommit(false);
            try {
                LocalDateTime fecha;
                try (PreparedStatement ps = c.prepareStatement("SELECT fecha FROM pago_cuota WHERE id_pago = ? AND estado = 'POR_CONFIRMAR' FOR UPDATE")) {
                    ps.setInt(1, idPago);
                    try (ResultSet rs = ps.executeQuery()) { if (!rs.next()) { c.rollback(); return; } fecha = rs.getTimestamp(1).toLocalDateTime(); }
                }
                pagarCuotasDe(c, idPago, fecha.toLocalDate(), yo);
                try (PreparedStatement ps = c.prepareStatement("UPDATE pago_cuota SET estado = 'CONFIRMADO', revisado_por = ?, fecha_revision = NOW() WHERE id_pago = ?")) {
                    ps.setInt(1, yo); ps.setInt(2, idPago); ps.executeUpdate();
                }
                c.commit();
            } catch (SQLException e) { c.rollback(); throw e; }
        }
    }

    private static void pagarCuotasDe(Connection c, int idPago, LocalDate fecha, int yo) throws SQLException {
        List<int[]> cuotas = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement("SELECT id_cuota, anio, mes FROM cuota WHERE id_pago = ? AND estado = 'PENDIENTE'")) {
            ps.setInt(1, idPago);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) cuotas.add(new int[]{rs.getInt(1), rs.getInt(2), rs.getInt(3)}); }
        }
        try (PreparedStatement ps = c.prepareStatement("UPDATE cuota SET estado = 'PAGADA', monto_pagado = ?, actualizado_por = ?, fecha_actualizacion = NOW() WHERE id_cuota = ?")) {
            for (int[] q : cuotas) {
                ps.setBigDecimal(1, BigDecimal.valueOf(Cuota.montoAl(YearMonth.of(q[1], q[2]), fecha)));
                ps.setInt(2, yo); ps.setInt(3, q[0]); ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /** La captura no sirve: las cuotas vuelven a pendientes y el miembro ve el motivo. */
    public void rechazar(int idPago, String motivo, int yo) throws SQLException {
        try (Connection c = getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement("UPDATE pago_cuota SET estado = 'RECHAZADO', motivo_rechazo = ?, revisado_por = ?, fecha_revision = NOW() " +
                        "WHERE id_pago = ? AND estado = 'POR_CONFIRMAR'")) {
                    setTexto(ps, 1, motivo); ps.setInt(2, yo); ps.setInt(3, idPago);
                    if (ps.executeUpdate() == 0) { c.rollback(); return; }
                }
                try (PreparedStatement ps = c.prepareStatement("UPDATE cuota SET id_pago = NULL WHERE id_pago = ? AND estado = 'PENDIENTE'")) {
                    ps.setInt(1, idPago); ps.executeUpdate();
                }
                c.commit();
            } catch (SQLException e) { c.rollback(); throw e; }
        }
    }

    /** Finanzas registra un pago sin captura (lo vio en el Yape, en efectivo…) para uno o más meses de un miembro. */
    public int registrarManual(int idMiembro, List<Integer> idCuotas, LocalDate fecha, String medio, int yo) throws SQLException {
        try (Connection c = getConnection()) {
            c.setAutoCommit(false);
            try {
                int idPago;
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO pago_cuota (id_miembro, fecha, medio, estado, registrado_por, revisado_por, fecha_revision) " +
                        "VALUES (?,?,?,'CONFIRMADO',?,?,NOW())", Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, idMiembro); ps.setTimestamp(2, Timestamp.valueOf(fecha.atTime(12, 0))); ps.setString(3, medio);
                    ps.setInt(4, yo); ps.setInt(5, yo); ps.executeUpdate();
                    try (ResultSet k = ps.getGeneratedKeys()) { k.next(); idPago = k.getInt(1); }
                }
                // si alguna estaba en revisión con otra captura, esa captura queda superada
                int n = 0;
                try (PreparedStatement ps = c.prepareStatement("UPDATE cuota SET id_pago = ? WHERE id_cuota = ? AND id_miembro = ? AND estado = 'PENDIENTE'")) {
                    for (Integer id : idCuotas) { ps.setInt(1, idPago); ps.setInt(2, id); ps.setInt(3, idMiembro); n += ps.executeUpdate(); }
                }
                if (n == 0) { c.rollback(); return 0; }
                pagarCuotasDe(c, idPago, fecha, yo);
                c.commit();
                return n;
            } catch (SQLException e) { c.rollback(); throw e; }
        }
    }

    public void exonerar(int idCuota, String nota, int yo) throws SQLException {
        actualizar("UPDATE cuota SET estado = 'EXONERADA', monto_pagado = NULL, id_pago = NULL, nota = ?, actualizado_por = ?, fecha_actualizacion = NOW() " +
                "WHERE id_cuota = ? AND estado = 'PENDIENTE'", nota, yo, idCuota);
    }

    /** Deshace un pago o una exoneración: la cuota vuelve a pendiente. */
    public void anular(int idCuota, int yo) throws SQLException {
        actualizar("UPDATE cuota SET estado = 'PENDIENTE', monto_pagado = NULL, id_pago = NULL, nota = NULL, actualizado_por = ?, fecha_actualizacion = NOW() " +
                "WHERE id_cuota = ? AND estado <> 'PENDIENTE'", yo, idCuota);
    }

    private void actualizar(String sql, Object... args) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) ps.setObject(i + 1, args[i]);
            ps.executeUpdate();
        }
    }

    /** Cambia desde qué mes paga alguien (null = la regla). Borra las cuotas pendientes sin pago que queden antes. */
    public void cambiarDesde(int idMiembro, LocalDate desde) throws SQLException {
        try (Connection c = getConnection()) {
            try (PreparedStatement ps = c.prepareStatement("UPDATE miembro SET cuota_desde = ? WHERE id_miembro = ?")) {
                setFecha(ps, 1, desde); ps.setInt(2, idMiembro); ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement("DELETE c FROM cuota c JOIN miembro m ON m.id_miembro = c.id_miembro " +
                    "WHERE c.id_miembro = ? AND c.estado = 'PENDIENTE' AND c.id_pago IS NULL AND STR_TO_DATE(CONCAT(c.anio,'-',c.mes,'-01'), '%Y-%c-%d') < " + DESDE)) {
                ps.setInt(1, idMiembro); ps.executeUpdate();
            }
        }
        generar(true);
    }

    /** Primer mes que paga (con la regla o lo que puso Finanzas). */
    public YearMonth desdeDe(int idMiembro) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("SELECT " + DESDE + ", m.cuota_desde FROM miembro m WHERE m.id_miembro = ?")) {
            ps.setInt(1, idMiembro);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? YearMonth.from(rs.getDate(1).toLocalDate()) : Cuota.INICIO; }
        }
    }

    public boolean desdeManual(int idMiembro) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("SELECT cuota_desde FROM miembro WHERE id_miembro = ?")) {
            ps.setInt(1, idMiembro);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() && rs.getDate(1) != null; }
        }
    }

    // ===================== LECTURAS DE PAGOS =====================

    private static final String SELECT_PAGO = "SELECT p.id_pago, p.id_miembro, p.fecha, p.operacion, p.medio, p.estado, p.motivo_rechazo, " +
            "p.voucher IS NOT NULL AS tiene_voucher, m.nombre, r.nombre AS revisor FROM pago_cuota p JOIN miembro m ON m.id_miembro = p.id_miembro " +
            "LEFT JOIN miembro r ON r.id_miembro = p.revisado_por ";

    private List<PagoCuota> pagos(String where, Object... args) throws SQLException {
        List<PagoCuota> l = new ArrayList<>();
        Map<Integer, PagoCuota> porId = new HashMap<>();
        try (Connection c = getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(SELECT_PAGO + where)) {
                for (int i = 0; i < args.length; i++) ps.setObject(i + 1, args[i]);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        PagoCuota p = new PagoCuota();
                        p.setIdPago(rs.getInt("id_pago")); p.setIdMiembro(rs.getInt("id_miembro")); p.setFecha(fechaHora(rs, "fecha"));
                        p.setOperacion(rs.getString("operacion")); p.setMedio(rs.getString("medio")); p.setEstado(rs.getString("estado"));
                        p.setMotivoRechazo(rs.getString("motivo_rechazo")); p.setVoucher(rs.getBoolean("tiene_voucher"));
                        p.setNombre(rs.getString("nombre")); p.setRevisadoPor(rs.getString("revisor"));
                        l.add(p); porId.put(p.getIdPago(), p);
                    }
                }
            }
        }
        if (porId.isEmpty()) return l;
        // meses de cada pago: los que tiene amarrados; si fue rechazado, los que dijo la captura ya no están → se guardan en la nota
        for (Cuota q : lista("WHERE c.id_pago IN (" + String.join(",", Collections.nCopies(porId.size(), "?")) + ") ORDER BY c.anio, c.mes", porId.keySet().toArray()))
            porId.get(q.getIdPago()).getCuotas().add(q);
        return l;
    }

    public List<PagoCuota> porConfirmar() throws SQLException {
        return pagos("WHERE p.estado = 'POR_CONFIRMAR' ORDER BY p.fecha");
    }

    /** Capturas rechazadas del miembro en los últimos 30 días (para que sepa por qué). */
    public List<PagoCuota> rechazosRecientes(int idMiembro) throws SQLException {
        return pagos("WHERE p.id_miembro = ? AND p.estado = 'RECHAZADO' AND p.fecha_revision >= NOW() - INTERVAL 30 DAY " +
                "AND NOT EXISTS (SELECT 1 FROM pago_cuota p2 WHERE p2.id_miembro = p.id_miembro AND p2.fecha > p.fecha_revision) ORDER BY p.fecha_revision DESC", idMiembro);
    }

    /** Dueño del pago y la imagen (null si no hay). */
    public Object[] voucher(int idPago) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("SELECT id_miembro, voucher FROM pago_cuota WHERE id_pago = ?")) {
            ps.setInt(1, idPago);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? new Object[]{rs.getInt(1), rs.getBytes(2)} : null; }
        }
    }

    /** Lo recaudado (cuotas pagadas) con fecha de pago en ese mes. */
    public int recaudadoEn(YearMonth ym) throws SQLException {
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement("SELECT COALESCE(SUM(c.monto_pagado),0) FROM cuota c JOIN pago_cuota p ON p.id_pago = c.id_pago " +
                "WHERE c.estado = 'PAGADA' AND p.fecha >= ? AND p.fecha < ?")) {
            ps.setTimestamp(1, Timestamp.valueOf(ym.atDay(1).atStartOfDay())); ps.setTimestamp(2, Timestamp.valueOf(ym.plusMonths(1).atDay(1).atStartOfDay()));
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getBigDecimal(1).intValue() : 0; }
        }
    }
}
