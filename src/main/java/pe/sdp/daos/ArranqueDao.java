package pe.sdp.daos;

import java.sql.*;

/** Consultas que se corren una sola vez al arrancar la aplicación. */
public class ArranqueDao extends BaseDao {

    /** Comprueba que la BD responda (para el log de arranque y /salud). */
    public boolean bdResponde() {
        try (Connection c = getConnection(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT 1")) {
            return rs.next();
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Admin inicial: si NINGÚN admin tiene contraseña todavía (BD recién cargada con los datos del Excel),
     * al primer admin le pone el correo y una contraseña temporal (tendrá que cambiarla al entrar).
     * Si ya hay un admin con contraseña no hace nada, así que dejar las variables puestas no es peligroso.
     * @return el nombre del admin activado, o null si no hizo nada
     */
    public String activarAdminInicial(String correo, String hash) throws SQLException {
        String buscar = "SELECT m.id_miembro, m.nombre, " +
                " (SELECT COUNT(*) FROM miembro m2 JOIN miembro_rol r2 ON r2.id_miembro=m2.id_miembro " +
                "   JOIN rol_sistema s2 ON s2.id_rol=r2.id_rol WHERE s2.codigo='ADMIN' AND m2.password_hash IS NOT NULL) AS con_clave " +
                "FROM miembro m JOIN miembro_rol r ON r.id_miembro=m.id_miembro JOIN rol_sistema s ON s.id_rol=r.id_rol " +
                "WHERE s.codigo='ADMIN' AND m.estado='ACTIVO' ORDER BY m.id_miembro LIMIT 1";
        try (Connection c = getConnection(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(buscar)) {
            if (!rs.next() || rs.getInt("con_clave") > 0) return null;
            int id = rs.getInt("id_miembro");
            String nombre = rs.getString("nombre");
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE miembro SET correo=?, password_hash=?, debe_cambiar_password=1 WHERE id_miembro=?")) {
                ps.setString(1, correo.trim().toLowerCase());
                ps.setString(2, hash);
                ps.setInt(3, id);
                ps.executeUpdate();
            }
            return nombre;
        }
    }

    /**
     * Migraciones automáticas: si la BD es de una versión anterior, le agrega lo que falta.
     * Así basta con actualizar la app, sin correr scripts a mano. Devuelve qué se cambió.
     */
    public java.util.List<String> migrar() throws SQLException {
        java.util.List<String> hecho = new java.util.ArrayList<>();
        try (Connection c = getConnection()) {
            if (!existeColumna(c, "figura", "video_url")) {
                try (Statement st = c.createStatement()) {
                    st.executeUpdate("ALTER TABLE figura ADD COLUMN video_url VARCHAR(500) NULL AFTER descripcion, MODIFY descripcion VARCHAR(1000) NULL");
                }
                hecho.add("figura.video_url (biblioteca de figuras)");
            }
            if (!existeColumna(c, "miembro", "apodo")) {
                try (Statement st = c.createStatement()) {
                    st.executeUpdate("ALTER TABLE miembro ADD COLUMN apodo VARCHAR(30) NULL AFTER nombre_completo, " +
                                     "ADD COLUMN fecha_nacimiento DATE NULL AFTER facultad");
                }
                hecho.add("miembro.apodo y miembro.fecha_nacimiento");
            }
            if (!existeTabla(c, "miembro_team")) {
                try (Statement st = c.createStatement()) {
                    st.executeUpdate("CREATE TABLE IF NOT EXISTS team (id_team SMALLINT AUTO_INCREMENT PRIMARY KEY, " +
                                     "codigo VARCHAR(20) NOT NULL UNIQUE, nombre VARCHAR(40) NOT NULL, orden SMALLINT NOT NULL DEFAULT 0) ENGINE=InnoDB");
                    st.executeUpdate("CREATE TABLE miembro_team (id_miembro INT NOT NULL, id_team SMALLINT NOT NULL, asignado_por INT NULL, " +
                                     "fecha_asignacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (id_miembro, id_team), " +
                                     "CONSTRAINT fk_mt_miembro FOREIGN KEY (id_miembro) REFERENCES miembro(id_miembro) ON DELETE CASCADE, " +
                                     "CONSTRAINT fk_mt_team FOREIGN KEY (id_team) REFERENCES team(id_team), " +
                                     "CONSTRAINT fk_mt_asigna FOREIGN KEY (asignado_por) REFERENCES miembro(id_miembro)) ENGINE=InnoDB");
                    st.executeUpdate("INSERT IGNORE INTO team (codigo, nombre, orden) VALUES ('ENSAYOS','Ensayos',1),('TALLERES','Talleres',2)," +
                                     "('LOGISTICA','Logística',3),('FINANZAS','Finanzas',4),('RRPP','RRPP',5),('COMUNICA','Comunica',6)");
                    // quien ya tenía el rol Equipo de ensayos queda en el team Ensayos
                    st.executeUpdate("INSERT IGNORE INTO miembro_team (id_miembro, id_team) SELECT mr.id_miembro, t.id_team FROM miembro_rol mr " +
                                     "JOIN team t ON t.codigo='ENSAYOS' WHERE mr.id_rol=2");
                }
                hecho.add("teams (Ensayos, Talleres, Logística, Finanzas, RRPP, Comunica)");
            }
            if (!existeFila(c, "SELECT 1 FROM rol_sistema WHERE codigo='ADMIN_ENSAYOS'")) {
                c.setAutoCommit(false);
                try (Statement st = c.createStatement()) {
                    st.executeUpdate("INSERT IGNORE INTO rol_sistema (id_rol, codigo, nombre) VALUES (4,'MODERADOR_ENSAYOS','Moderador de ensayos'),(5,'ADMIN_ENSAYOS','Admin de ensayos')");
                    st.executeUpdate("UPDATE rol_sistema SET nombre='Owner' WHERE id_rol=3");
                    st.executeUpdate("INSERT IGNORE INTO permiso (id_permiso, codigo, descripcion, seccion) SELECT COALESCE(MAX(id_permiso),0)+1, " +
                                     "'ELIMINAR_ENSAYOS', 'Eliminar ensayos', 'ENSAYOS' FROM permiso");
                    st.executeUpdate("INSERT IGNORE INTO permiso (id_permiso, codigo, descripcion, seccion) SELECT COALESCE(MAX(id_permiso),0)+1, " +
                                     "'EDITAR_FIGURAS_MIEMBRO', 'Editar las figuras de un miembro desde su ficha', 'ENSAYOS' FROM permiso");
                    // Moderador y Admin de ensayos: todo lo del equipo + Figuras, Histórico, Responsabilidades, eliminar ensayos y editar figuras
                    st.executeUpdate("INSERT IGNORE INTO rol_permiso (id_rol, id_permiso) SELECT r.id_rol, rp.id_permiso FROM rol_permiso rp " +
                                     "JOIN (SELECT 4 AS id_rol UNION SELECT 5) r WHERE rp.id_rol=2");
                    st.executeUpdate("INSERT IGNORE INTO rol_permiso (id_rol, id_permiso) SELECT r.id_rol, p.id_permiso FROM permiso p " +
                                     "JOIN (SELECT 3 AS id_rol UNION SELECT 4 UNION SELECT 5) r WHERE p.codigo IN " +
                                     "('GESTIONAR_FIGURAS','GESTIONAR_HISTORICO','ASIGNAR_RESPONSABILIDADES','ELIMINAR_ENSAYOS','EDITAR_FIGURAS_MIEMBRO')");
                    // El equipo de ensayos ve SUS responsabilidades (Mi espacio); el panel de todas es del admin del team
                    st.executeUpdate("DELETE rp FROM rol_permiso rp JOIN permiso p ON p.id_permiso=rp.id_permiso WHERE rp.id_rol=2 AND p.codigo='ASIGNAR_RESPONSABILIDADES'");
                    // Moderador y Admin de ensayos también son "del equipo de ensayos" para la regla del color
                    st.executeUpdate("CREATE OR REPLACE VIEW v_miembro_flags AS SELECT m.id_miembro, m.nombre, m.rol_baile, m.frecuencia, m.estado, " +
                                     "EXISTS (SELECT 1 FROM miembro_rol mr JOIN rol_sistema r ON r.id_rol = mr.id_rol WHERE mr.id_miembro = m.id_miembro " +
                                     "AND r.codigo IN ('EQUIPO_ENSAYOS','MODERADOR_ENSAYOS','ADMIN_ENSAYOS')) AS es_equipo_ensayos, " +
                                     "EXISTS (SELECT 1 FROM miembro_rol mr JOIN rol_sistema r ON r.id_rol = mr.id_rol WHERE mr.id_miembro = m.id_miembro " +
                                     "AND r.codigo = 'ADMIN') AS es_admin FROM miembro m");
                    c.commit();
                } catch (SQLException e) { c.rollback(); throw e; }
                finally { c.setAutoCommit(true); }
                hecho.add("roles Owner, Admin de ensayos y Moderador de ensayos");
            }
            if (!existeTabla(c, "taller_resumen")) {
                migrarTalleres(c);
                hecho.add("módulo Talleres (roles, permisos, talleres, pasos, dictado y feedback)");
            }
            if (!existeTabla(c, "cuota")) {
                migrarFinanzas(c);
                hecho.add("módulo Finanzas · cuotas (roles, permisos, cuotas y pagos)");
            }
            // registro de claves temporales: quién la generó y cuándo (la persona lo ve al entrar)
            if (!existeColumna(c, "miembro", "clave_restablecida_por")) {
                try (Statement st = c.createStatement()) {
                    st.executeUpdate("ALTER TABLE miembro ADD COLUMN clave_restablecida_por INT NULL, ADD COLUMN clave_restablecida_en DATETIME NULL");
                }
                hecho.add("registro de claves temporales (quién y cuándo)");
            }
            if (!existeColumna(c, "cuota", "abonado")) {
                try (Statement st = c.createStatement()) {
                    // pagos parciales y montos fijos (deudas que vienen del Excel), pago anual y encargado de cobro
                    st.executeUpdate("ALTER TABLE cuota ADD COLUMN monto_fijo DECIMAL(7,2) NULL AFTER estado, ADD COLUMN abonado DECIMAL(7,2) NOT NULL DEFAULT 0 AFTER monto_fijo, " +
                                     "MODIFY monto_pagado DECIMAL(7,2) NULL");
                    st.executeUpdate("ALTER TABLE pago_cuota ADD COLUMN monto DECIMAL(7,2) NULL AFTER medio, ADD COLUMN anual TINYINT(1) NOT NULL DEFAULT 0 AFTER monto");
                    if (!existeColumna(c, "miembro", "id_cobrador"))
                        st.executeUpdate("ALTER TABLE miembro ADD COLUMN id_cobrador INT NULL AFTER cuota_desde, " +
                                         "ADD CONSTRAINT fk_miembro_cobrador FOREIGN KEY (id_cobrador) REFERENCES miembro(id_miembro) ON DELETE SET NULL");
                }
                hecho.add("cuotas: pagos parciales, pago anual y encargado de cobro");
            }
            if (!existeTabla(c, "config")) {
                try (Statement st = c.createStatement()) {
                    st.executeUpdate("CREATE TABLE config (clave VARCHAR(40) PRIMARY KEY, valor VARCHAR(500) NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
                    // la barra de emojis que ya estaba es la de team Ensayos; las otras secciones se llenan desde Roles y permisos
                    st.executeUpdate("INSERT INTO config (clave, valor) VALUES ('emojis.ENSAYOS','⚾😑💃🏻🦉🐥🌈🌕🐧👀🍄🍗')");
                }
                hecho.add("config (emojis del pie por sección)");
            }
            if (!existeTabla(c, "taller_confirmacion")) {
                try (Statement st = c.createStatement()) {
                    // cada miembro confirma si va al taller del jueves (como con los ensayos)
                    st.executeUpdate("CREATE TABLE taller_confirmacion (id_taller INT NOT NULL, id_miembro INT NOT NULL, respuesta ENUM('SI','NO') NOT NULL, " +
                                     "motivo VARCHAR(200) NULL, fecha DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (id_taller, id_miembro), " +
                                     "CONSTRAINT fk_tk_taller FOREIGN KEY (id_taller) REFERENCES taller(id_taller) ON DELETE CASCADE, " +
                                     "CONSTRAINT fk_tk_miembro FOREIGN KEY (id_miembro) REFERENCES miembro(id_miembro)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
                }
                hecho.add("confirmar asistencia a talleres");
            }
            if (!existeColumna(c, "ensayo", "por_niveles")) {
                try (Statement st = c.createStatement()) {
                    String COL = "ENUM('AZUL','VERDE','AMARILLO','ROJO')";
                    // ensayos donde todos trabajaron juntos (sin separarse por niveles)
                    st.executeUpdate("ALTER TABLE ensayo ADD COLUMN por_niveles TINYINT(1) NOT NULL DEFAULT 1 AFTER estado");
                    // alguien que estuvo en más de un nivel ese día (ej. lo sumaron al siguiente): autoevalúa las figuras de ambos
                    st.executeUpdate("CREATE TABLE ensayo_nivel_extra (id_ensayo INT NOT NULL, id_miembro INT NOT NULL, nivel " + COL + " NOT NULL, " +
                                     "PRIMARY KEY (id_ensayo, id_miembro, nivel), " +
                                     "CONSTRAINT fk_ene_ensayo FOREIGN KEY (id_ensayo) REFERENCES ensayo(id_ensayo) ON DELETE CASCADE, " +
                                     "CONSTRAINT fk_ene_miembro FOREIGN KEY (id_miembro) REFERENCES miembro(id_miembro)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
                    // quién enseñó cada nivel: varios profes por nivel y un profe en varios niveles (TODOS = ensayo sin niveles)
                    st.executeUpdate("CREATE TABLE ensayo_dicto (id_ensayo INT NOT NULL, nivel ENUM('AZUL','VERDE','AMARILLO','ROJO','TODOS') NOT NULL, id_miembro INT NOT NULL, " +
                                     "PRIMARY KEY (id_ensayo, nivel, id_miembro), " +
                                     "CONSTRAINT fk_edi_ensayo FOREIGN KEY (id_ensayo) REFERENCES ensayo(id_ensayo) ON DELETE CASCADE, " +
                                     "CONSTRAINT fk_edi_miembro FOREIGN KEY (id_miembro) REFERENCES miembro(id_miembro)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
                    st.executeUpdate("INSERT IGNORE INTO ensayo_dicto (id_ensayo, nivel, id_miembro) SELECT DISTINCT id_ensayo, nivel, dicto FROM ensayo_figura " +
                                     "WHERE dicto IS NOT NULL AND nivel IS NOT NULL");
                }
                hecho.add("ensayos: sin niveles, asistentes en varios niveles y varios profes por nivel");
                // "avances" = solo lo que SUBIÓ (desde ahora en el ensayo también se puede bajar)
                try (Statement st = c.createStatement()) {
                    st.executeUpdate("CREATE OR REPLACE VIEW v_ensayo_resumen AS SELECT en.id_ensayo, en.fecha, en.tipo, en.estado, " +
                            "SUM(a.confirmacion = 'SI') AS confirmados, SUM(a.confirmacion = 'SIN_RESPUESTA') AS sin_respuesta, " +
                            "SUM(a.estado_real IN ('AP','AN','TJ','TI')) AS asistieron, SUM(a.estado_real IN ('AP','AN')) AS puntuales, " +
                            "SUM(a.estado_real IN ('TJ','TI')) AS tardanzas, SUM(a.estado_real IN ('FJ','FI')) AS faltas, " +
                            "(SELECT COUNT(*) FROM ensayo_figura ef WHERE ef.id_ensayo = en.id_ensayo) AS figuras_ensenadas, " +
                            "(SELECT COUNT(*) FROM historial_evaluacion h WHERE h.id_ensayo = en.id_ensayo AND h.aplicado = 1 " +
                            " AND FIELD(h.color_nuevo,'ROJO','AMARILLO','VERDE','AZUL') > FIELD(COALESCE(h.color_anterior,''),'ROJO','AMARILLO','VERDE','AZUL')) AS avances " +
                            "FROM ensayo en LEFT JOIN asistencia a ON a.id_ensayo = en.id_ensayo GROUP BY en.id_ensayo, en.fecha, en.tipo, en.estado");
                }
            }
            if (!existeColumna(c, "taller_bloque", "grupo")) {
                try (Statement st = c.createStatement()) {
                    // cronograma por secciones: varias filas (pasos) comparten la misma llave de horas (grupo);
                    // estilo para el flyer; versión PLAN (lo planeado) o DICTADO (lo que se dictó al final)
                    st.executeUpdate("ALTER TABLE taller_bloque ADD COLUMN grupo SMALLINT NOT NULL DEFAULT 0 AFTER orden, " +
                                     "ADD COLUMN estilo ENUM('INICIO','FILTRO','NORMAL','MUSICA','FINAL') NOT NULL DEFAULT 'NORMAL' AFTER duracion, " +
                                     "ADD COLUMN version ENUM('PLAN','DICTADO') NOT NULL DEFAULT 'PLAN' AFTER nivel, MODIFY actividad VARCHAR(300) NOT NULL DEFAULT ''");
                }
                // las filas sin hora siguen a la llave de arriba (así estaban en los flyers)
                try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT id_bloque, id_taller, nivel, hora, actividad FROM taller_bloque ORDER BY id_taller, nivel, orden");
                     PreparedStatement up = c.prepareStatement("UPDATE taller_bloque SET grupo=?, estilo=? WHERE id_bloque=?")) {
                    int grupo = 0; String clave = "";
                    while (rs.next()) {
                        String k = rs.getInt(2) + rs.getString(3);
                        if (!k.equals(clave)) { clave = k; grupo = 0; }
                        String hora = rs.getString(4);
                        if (grupo == 0 || (hora != null && !hora.isBlank())) grupo++;
                        up.setInt(1, grupo); up.setString(2, pe.sdp.beans.TallerSeccion.adivinarEstilo(rs.getString(5))); up.setInt(3, rs.getInt(1));
                        up.addBatch();
                    }
                    up.executeBatch();
                }
                try (Statement st = c.createStatement()) {
                    st.executeUpdate("INSERT IGNORE INTO config (clave, valor) VALUES ('flyer.lema','¡11 años repartiendo cubanía!')");
                }
                hecho.add("talleres: cronograma por secciones (plan y lo que se dictó)");
            }
            if (!existeTabla(c, "caja_movimiento")) {
                try (Statement st = c.createStatement()) {
                    // caja: ingresos y gastos (las cuotas pagadas por la web entran solas, no se guardan aquí)
                    st.executeUpdate("CREATE TABLE caja_movimiento (id INT AUTO_INCREMENT PRIMARY KEY, fecha DATE NOT NULL, tipo ENUM('INGRESO','GASTO') NOT NULL, " +
                                     "categoria VARCHAR(40) NOT NULL, concepto VARCHAR(200) NOT NULL, monto DECIMAL(9,2) NOT NULL, proyecto VARCHAR(80) NULL, " +
                                     "registrado_por INT NULL, fecha_registro DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, KEY ix_caja_fecha (fecha), " +
                                     "CONSTRAINT ck_caja_monto CHECK (monto > 0), " +
                                     "CONSTRAINT fk_caja_reg FOREIGN KEY (registrado_por) REFERENCES miembro(id_miembro) ON DELETE SET NULL) " +
                                     "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
                    st.executeUpdate("INSERT IGNORE INTO config (clave, valor) VALUES ('caja.saldo_inicial','2279.05'),('caja.desde','2026-01-01')");
                    // registrar y borrar movimientos: moderador y admin de finanzas (y el Owner); el equipo solo ve
                    st.executeUpdate("INSERT IGNORE INTO permiso (id_permiso, codigo, descripcion, seccion) SELECT COALESCE(MAX(id_permiso),0)+1, " +
                                     "'GESTIONAR_CAJA', 'Registrar ingresos y gastos de la caja', 'FINANZAS' FROM permiso");
                    st.executeUpdate("INSERT IGNORE INTO rol_permiso (id_rol, id_permiso) SELECT r.id_rol, p.id_permiso FROM permiso p " +
                                     "JOIN (SELECT 3 AS id_rol UNION SELECT 10 UNION SELECT 11) r WHERE p.codigo='GESTIONAR_CAJA'");
                }
                hecho.add("caja de Finanzas (saldo, ingresos y gastos)");
            }
            if (!existeFila(c, "SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'historial_evaluacion' " +
                               "AND COLUMN_NAME = 'fuente' AND COLUMN_TYPE LIKE '%MATRIZ_EXCEL%'")) {
                try (Statement st = c.createStatement()) {
                    // historia de la mano de antes de la web (las matrices del Excel, ensayo por ensayo): solo para ver la evolución
                    st.executeUpdate("ALTER TABLE historial_evaluacion MODIFY fuente ENUM('CARGA_INICIAL','MENSUAL','ENSAYO','CORRECCION','MATRIZ_EXCEL') NOT NULL");
                }
                hecho.add("histórico: matrices del Excel (evolución de antes de la web)");
            }
            // va después de config y de los teams (usa ambos)
            if (!existeTabla(c, "com_proyecto")) {
                migrarComunica(c);
                hecho.add("módulo Comunica (roles, permisos, saludos de cumpleaños, proyectos e ideas, plantillas de flyers)");
            }
            if (!existeTabla(c, "encuesta")) {
                migrarRrpp(c);
                hecho.add("módulo RRPP (roles, externos del jueves, visitas formales, directorio) y encuestas");
            }
            // Nadie puede figurar dos veces con la misma función en un taller (pasaba al sumarse como "adicional" siendo del plan).
            // Se queda la fila del plan (o la más antigua) y hereda la asistencia/nivel de la repetida si le faltaba.
            try (Statement st = c.createStatement()) {
                st.executeUpdate("UPDATE taller_persona k JOIN taller_persona d ON d.id_taller=k.id_taller AND d.id_miembro=k.id_miembro AND d.funcion=k.funcion " +
                                 "AND (k.planeado > d.planeado OR (k.planeado = d.planeado AND k.id < d.id)) " +
                                 "SET k.asistencia = IF(k.asistencia IS NULL OR k.asistencia='NO_SE_PRESENTO', COALESCE(d.asistencia, k.asistencia), k.asistencia), " +
                                 "k.nivel = COALESCE(k.nivel, d.nivel)");
                int n = st.executeUpdate("DELETE d FROM taller_persona d JOIN taller_persona k ON d.id_taller=k.id_taller AND d.id_miembro=k.id_miembro AND d.funcion=k.funcion " +
                                         "AND (k.planeado > d.planeado OR (k.planeado = d.planeado AND k.id < d.id))");
                if (n > 0) hecho.add(n + " tallerista(s) repetidos en el dictado de talleres");
            }
        }
        return hecho;
    }

    /** Módulo Talleres: roles 6-8, permisos, responsabilidades por team y tablas propias. */
    private static void migrarTalleres(Connection c) throws SQLException {
        String NIV = "ENUM('PRINCIPIANTE','BASICO','INTRO_RUEDA','RUEDA_BASICA')";
        try (Statement st = c.createStatement()) {
            // --- roles y permisos ---
            st.executeUpdate("INSERT IGNORE INTO rol_sistema (id_rol, codigo, nombre) VALUES (6,'EQUIPO_TALLERES','Equipo de talleres')," +
                             "(7,'MODERADOR_TALLERES','Moderador de talleres'),(8,'ADMIN_TALLERES','Admin de talleres')");
            st.executeUpdate("ALTER TABLE permiso MODIFY seccion ENUM('MI_ESPACIO','ENSAYOS','TALLERES','ADMINISTRACION') NOT NULL");
            st.executeUpdate("INSERT IGNORE INTO permiso (id_permiso, codigo, descripcion, seccion) SELECT COALESCE(MAX(id_permiso),0)+1, " +
                             "'GESTIONAR_TALLERES', 'Armar talleres, registrar el dictado y ver el feedback', 'TALLERES' FROM permiso");
            st.executeUpdate("INSERT IGNORE INTO permiso (id_permiso, codigo, descripcion, seccion) SELECT COALESCE(MAX(id_permiso),0)+1, " +
                             "'ASIGNAR_RESPONSABILIDADES_TALLERES', 'Asignar responsabilidades de talleres', 'TALLERES' FROM permiso");
            // todos los roles de talleres tienen lo de Mi espacio
            st.executeUpdate("INSERT IGNORE INTO rol_permiso (id_rol, id_permiso) SELECT r.id_rol, rp.id_permiso FROM rol_permiso rp " +
                             "JOIN (SELECT 6 AS id_rol UNION SELECT 7 UNION SELECT 8) r WHERE rp.id_rol=1");
            st.executeUpdate("INSERT IGNORE INTO rol_permiso (id_rol, id_permiso) SELECT r.id_rol, p.id_permiso FROM permiso p " +
                             "JOIN (SELECT 3 AS id_rol UNION SELECT 6 UNION SELECT 7 UNION SELECT 8) r WHERE p.codigo='GESTIONAR_TALLERES'");
            st.executeUpdate("INSERT IGNORE INTO rol_permiso (id_rol, id_permiso) SELECT r.id_rol, p.id_permiso FROM permiso p " +
                             "JOIN (SELECT 3 AS id_rol UNION SELECT 7 UNION SELECT 8) r WHERE p.codigo='ASIGNAR_RESPONSABILIDADES_TALLERES'");
            // quien está en el team Talleres queda con el rol Equipo de talleres
            st.executeUpdate("INSERT IGNORE INTO miembro_rol (id_miembro, id_rol) SELECT mt.id_miembro, 6 FROM miembro_team mt " +
                             "JOIN team t ON t.id_team=mt.id_team WHERE t.codigo='TALLERES'");

            // --- responsabilidades por team (NULL = Ensayos, como hasta ahora) ---
            if (!existeColumna(c, "tipo_encargo", "id_team")) {
                st.executeUpdate("ALTER TABLE tipo_encargo ADD COLUMN id_team SMALLINT NULL, ADD CONSTRAINT fk_te_team FOREIGN KEY (id_team) REFERENCES team(id_team)");
                st.executeUpdate("ALTER TABLE tipo_encargo DROP INDEX nombre, ADD CONSTRAINT uq_tipo_team UNIQUE (nombre, id_team)");
            }
            st.executeUpdate("INSERT IGNORE INTO tipo_encargo (nombre, activo, id_team) SELECT x.n, 1, t.id_team FROM team t JOIN (" +
                             "SELECT 'Armar el cronograma del taller' n UNION SELECT 'Hacer el flyer' UNION SELECT 'Registrar el dictado final' " +
                             "UNION SELECT 'Resumir y enviar el feedback' UNION SELECT 'Música social') x WHERE t.codigo='TALLERES'");

            // --- talleres ---
            st.executeUpdate("CREATE TABLE IF NOT EXISTS taller (id_taller INT AUTO_INCREMENT PRIMARY KEY, ciclo VARCHAR(7) NOT NULL, numero SMALLINT NULL, " +
                             "fecha DATE NOT NULL, hora_inicio TIME NOT NULL, hora_fin TIME NOT NULL, lugar VARCHAR(120) NULL, " +
                             "estado ENUM('BORRADOR','PROGRAMADO','REALIZADO','CANCELADO') NOT NULL DEFAULT 'PROGRAMADO', " +
                             "musica_texto VARCHAR(120) NULL, notas VARCHAR(1000) NULL, final_por INT NULL, final_fecha DATETIME NULL, " +
                             "creado_por INT NULL, fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                             "CONSTRAINT uq_taller_fecha UNIQUE (fecha), " +
                             "CONSTRAINT fk_ta_final FOREIGN KEY (final_por) REFERENCES miembro(id_miembro), " +
                             "CONSTRAINT fk_ta_creado FOREIGN KEY (creado_por) REFERENCES miembro(id_miembro)) ENGINE=InnoDB");
            // bloques del cronograma (lo que sale en el flyer), por nivel
            st.executeUpdate("CREATE TABLE IF NOT EXISTS taller_bloque (id_bloque INT AUTO_INCREMENT PRIMARY KEY, id_taller INT NOT NULL, nivel " + NIV + " NOT NULL, " +
                             "orden SMALLINT NOT NULL, hora VARCHAR(20) NULL, actividad VARCHAR(300) NOT NULL, duracion VARCHAR(10) NULL, " +
                             "CONSTRAINT fk_tb_taller FOREIGN KEY (id_taller) REFERENCES taller(id_taller) ON DELETE CASCADE) ENGINE=InnoDB");
            // catálogo de pasos por nivel (se arma con los cronogramas pasados y se le agregan nuevos)
            st.executeUpdate("CREATE TABLE IF NOT EXISTS paso_taller (id_paso INT AUTO_INCREMENT PRIMARY KEY, nombre VARCHAR(80) NOT NULL, nivel " + NIV + " NOT NULL, " +
                             "activo TINYINT(1) NOT NULL DEFAULT 1, CONSTRAINT uq_paso UNIQUE (nombre, nivel)) ENGINE=InnoDB");
            // pasos de cada taller: planificados (del cronograma) y agregados; ensenado = checklist ✅/❌ del formulario
            st.executeUpdate("CREATE TABLE IF NOT EXISTS taller_paso (id_taller INT NOT NULL, nivel " + NIV + " NOT NULL, id_paso INT NOT NULL, " +
                             "orden SMALLINT NOT NULL DEFAULT 0, seccion TINYINT NOT NULL DEFAULT 1, planificado TINYINT(1) NOT NULL DEFAULT 1, " +
                             "ensenado TINYINT(1) NULL, PRIMARY KEY (id_taller, nivel, id_paso), " +
                             "CONSTRAINT fk_tp_taller FOREIGN KEY (id_taller) REFERENCES taller(id_taller) ON DELETE CASCADE, " +
                             "CONSTRAINT fk_tp_paso FOREIGN KEY (id_paso) REFERENCES paso_taller(id_paso)) ENGINE=InnoDB");
            // quién respondió el checklist de cada nivel (basta con uno de los dos talleristas)
            st.executeUpdate("CREATE TABLE IF NOT EXISTS taller_checklist (id_taller INT NOT NULL, nivel " + NIV + " NOT NULL, respondido_por INT NOT NULL, " +
                             "fecha DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (id_taller, nivel), " +
                             "CONSTRAINT fk_tc_taller FOREIGN KEY (id_taller) REFERENCES taller(id_taller) ON DELETE CASCADE, " +
                             "CONSTRAINT fk_tc_miembro FOREIGN KEY (respondido_por) REFERENCES miembro(id_miembro)) ENGINE=InnoDB");
            // dictado: planeado (cronograma) y final (asistencia real, reemplazos)
            st.executeUpdate("CREATE TABLE IF NOT EXISTS taller_persona (id INT AUTO_INCREMENT PRIMARY KEY, id_taller INT NOT NULL, id_miembro INT NOT NULL, " +
                             "funcion ENUM('TALLERISTA','APOYO','MAESTRO','MUSICA') NOT NULL, nivel " + NIV + " NULL, " +
                             "planeado TINYINT(1) NOT NULL DEFAULT 1, asistencia ENUM('A_TIEMPO','TARDE','NO_SE_PRESENTO') NULL, reemplaza_a INT NULL, " +
                             "CONSTRAINT fk_tpe_taller FOREIGN KEY (id_taller) REFERENCES taller(id_taller) ON DELETE CASCADE, " +
                             "CONSTRAINT fk_tpe_miembro FOREIGN KEY (id_miembro) REFERENCES miembro(id_miembro), " +
                             "CONSTRAINT fk_tpe_reemp FOREIGN KEY (reemplaza_a) REFERENCES miembro(id_miembro)) ENGINE=InnoDB");
            // quién ya mandó su formulario
            st.executeUpdate("CREATE TABLE IF NOT EXISTS taller_respuesta (id_taller INT NOT NULL, id_miembro INT NOT NULL, fecha DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                             "PRIMARY KEY (id_taller, id_miembro), CONSTRAINT fk_tr_taller FOREIGN KEY (id_taller) REFERENCES taller(id_taller) ON DELETE CASCADE, " +
                             "CONSTRAINT fk_tr_miembro FOREIGN KEY (id_miembro) REFERENCES miembro(id_miembro)) ENGINE=InnoDB");
            // feedback crudo (con nombre, lo ve el team) · TALLER = sugerencia para el taller (para_miembro NULL)
            st.executeUpdate("CREATE TABLE IF NOT EXISTS taller_feedback (id INT AUTO_INCREMENT PRIMARY KEY, id_taller INT NOT NULL, de_miembro INT NOT NULL, " +
                             "para_miembro INT NULL, tipo ENUM('COMPANERO','APOYO','TALLER') NOT NULL, texto VARCHAR(2000) NOT NULL, " +
                             "fecha DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                             "CONSTRAINT fk_tf_taller FOREIGN KEY (id_taller) REFERENCES taller(id_taller) ON DELETE CASCADE, " +
                             "CONSTRAINT fk_tf_de FOREIGN KEY (de_miembro) REFERENCES miembro(id_miembro), " +
                             "CONSTRAINT fk_tf_para FOREIGN KEY (para_miembro) REFERENCES miembro(id_miembro)) ENGINE=InnoDB");
            // resumen anónimo que le llega a cada tallerista
            st.executeUpdate("CREATE TABLE IF NOT EXISTS taller_resumen (id_taller INT NOT NULL, para_miembro INT NOT NULL, texto VARCHAR(2000) NOT NULL, " +
                             "escrito_por INT NOT NULL, fecha_actualizacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, enviado TINYINT(1) NOT NULL DEFAULT 0, " +
                             "fecha_envio DATETIME NULL, PRIMARY KEY (id_taller, para_miembro), " +
                             "CONSTRAINT fk_trs_taller FOREIGN KEY (id_taller) REFERENCES taller(id_taller) ON DELETE CASCADE, " +
                             "CONSTRAINT fk_trs_para FOREIGN KEY (para_miembro) REFERENCES miembro(id_miembro), " +
                             "CONSTRAINT fk_trs_esc FOREIGN KEY (escrito_por) REFERENCES miembro(id_miembro)) ENGINE=InnoDB");
        }
    }

    /** Módulo Finanzas (cuotas): roles 9-11, permisos, cuotas mensuales y pagos con captura de Yape. */
    private static void migrarFinanzas(Connection c) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.executeUpdate("INSERT IGNORE INTO rol_sistema (id_rol, codigo, nombre) VALUES (9,'EQUIPO_FINANZAS','Equipo de finanzas')," +
                             "(10,'MODERADOR_FINANZAS','Moderador de finanzas'),(11,'ADMIN_FINANZAS','Admin de finanzas')");
            st.executeUpdate("ALTER TABLE permiso MODIFY seccion ENUM('MI_ESPACIO','ENSAYOS','TALLERES','FINANZAS','ADMINISTRACION') NOT NULL");
            st.executeUpdate("INSERT IGNORE INTO permiso (id_permiso, codigo, descripcion, seccion) SELECT COALESCE(MAX(id_permiso),0)+1, " +
                             "'GESTIONAR_CUOTAS', 'Ver las cuotas de todos, confirmar pagos y mandar recordatorios', 'FINANZAS' FROM permiso");
            st.executeUpdate("INSERT IGNORE INTO permiso (id_permiso, codigo, descripcion, seccion) SELECT COALESCE(MAX(id_permiso),0)+1, " +
                             "'EXONERAR_CUOTAS', 'Exonerar cuotas y anular pagos', 'FINANZAS' FROM permiso");
            st.executeUpdate("INSERT IGNORE INTO rol_permiso (id_rol, id_permiso) SELECT r.id_rol, rp.id_permiso FROM rol_permiso rp " +
                             "JOIN (SELECT 9 AS id_rol UNION SELECT 10 UNION SELECT 11) r WHERE rp.id_rol=1");
            st.executeUpdate("INSERT IGNORE INTO rol_permiso (id_rol, id_permiso) SELECT r.id_rol, p.id_permiso FROM permiso p " +
                             "JOIN (SELECT 3 AS id_rol UNION SELECT 9 UNION SELECT 10 UNION SELECT 11) r WHERE p.codigo='GESTIONAR_CUOTAS'");
            st.executeUpdate("INSERT IGNORE INTO rol_permiso (id_rol, id_permiso) SELECT r.id_rol, p.id_permiso FROM permiso p " +
                             "JOIN (SELECT 3 AS id_rol UNION SELECT 10 UNION SELECT 11) r WHERE p.codigo='EXONERAR_CUOTAS'");
            // quien está en el team Finanzas queda con el rol Equipo de finanzas
            st.executeUpdate("INSERT IGNORE INTO miembro_rol (id_miembro, id_rol) SELECT mt.id_miembro, 9 FROM miembro_team mt " +
                             "JOIN team t ON t.id_team=mt.id_team WHERE t.codigo='FINANZAS'");
            // desde qué mes paga cuota (NULL = la regla: 2 meses después de su ingreso)
            if (!existeColumna(c, "miembro", "cuota_desde"))
                st.executeUpdate("ALTER TABLE miembro ADD COLUMN cuota_desde DATE NULL AFTER fecha_ingreso");
            // un pago = una captura de Yape (o un registro manual de Finanzas) que cubre uno o más meses
            st.executeUpdate("CREATE TABLE IF NOT EXISTS pago_cuota (id_pago INT AUTO_INCREMENT PRIMARY KEY, id_miembro INT NOT NULL, " +
                             "fecha DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, operacion VARCHAR(40) NULL, medio ENUM('YAPE','EFECTIVO','OTRO') NOT NULL DEFAULT 'YAPE', " +
                             "voucher MEDIUMBLOB NULL, estado ENUM('POR_CONFIRMAR','CONFIRMADO','RECHAZADO') NOT NULL DEFAULT 'POR_CONFIRMAR', " +
                             "registrado_por INT NOT NULL, revisado_por INT NULL, fecha_revision DATETIME NULL, motivo_rechazo VARCHAR(200) NULL, " +
                             "CONSTRAINT fk_pc_miembro FOREIGN KEY (id_miembro) REFERENCES miembro(id_miembro), " +
                             "CONSTRAINT fk_pc_reg FOREIGN KEY (registrado_por) REFERENCES miembro(id_miembro), " +
                             "CONSTRAINT fk_pc_rev FOREIGN KEY (revisado_por) REFERENCES miembro(id_miembro)) ENGINE=InnoDB");
            // una fila por miembro y mes; el monto se calcula con la fecha del pago (6 / 9 / 15)
            st.executeUpdate("CREATE TABLE IF NOT EXISTS cuota (id_cuota INT AUTO_INCREMENT PRIMARY KEY, id_miembro INT NOT NULL, anio SMALLINT NOT NULL, mes TINYINT NOT NULL, " +
                             "estado ENUM('PENDIENTE','PAGADA','EXONERADA') NOT NULL DEFAULT 'PENDIENTE', monto_pagado DECIMAL(6,2) NULL, id_pago INT NULL, " +
                             "nota VARCHAR(200) NULL, actualizado_por INT NULL, fecha_actualizacion DATETIME NULL, " +
                             "CONSTRAINT uq_cuota UNIQUE (id_miembro, anio, mes), " +
                             "CONSTRAINT fk_cu_miembro FOREIGN KEY (id_miembro) REFERENCES miembro(id_miembro), " +
                             "CONSTRAINT fk_cu_pago FOREIGN KEY (id_pago) REFERENCES pago_cuota(id_pago) ON DELETE SET NULL, " +
                             "CONSTRAINT fk_cu_act FOREIGN KEY (actualizado_por) REFERENCES miembro(id_miembro)) ENGINE=InnoDB");
        }
    }

    /** Módulo Comunica: roles 12-14, permisos, saludos de cumpleaños, proyectos/ideas y plantillas de flyers. */
    private static void migrarComunica(Connection c) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.executeUpdate("INSERT IGNORE INTO rol_sistema (id_rol, codigo, nombre) VALUES (12,'EQUIPO_COMUNICA','Equipo de comunica')," +
                             "(13,'MODERADOR_COMUNICA','Moderador de comunica'),(14,'ADMIN_COMUNICA','Admin de comunica')");
            st.executeUpdate("ALTER TABLE permiso MODIFY seccion ENUM('MI_ESPACIO','ENSAYOS','TALLERES','FINANZAS','COMUNICA','ADMINISTRACION') NOT NULL");
            st.executeUpdate("INSERT IGNORE INTO permiso (id_permiso, codigo, descripcion, seccion) SELECT COALESCE(MAX(id_permiso),0)+1, " +
                             "'GESTIONAR_COMUNICA', 'Entrar a Comunica: saludos de cumpleaños, proyectos, ideas y plantillas', 'COMUNICA' FROM permiso");
            st.executeUpdate("INSERT IGNORE INTO permiso (id_permiso, codigo, descripcion, seccion) SELECT COALESCE(MAX(id_permiso),0)+1, " +
                             "'MODERAR_COMUNICA', 'Reasignar saludos, editar la plantilla del saludo y borrar proyectos, ideas o plantillas', 'COMUNICA' FROM permiso");
            // los roles de Comunica tienen lo básico de cualquier miembro (Mi espacio)
            st.executeUpdate("INSERT IGNORE INTO rol_permiso (id_rol, id_permiso) SELECT r.id_rol, rp.id_permiso FROM rol_permiso rp " +
                             "JOIN (SELECT 12 AS id_rol UNION SELECT 13 UNION SELECT 14) r WHERE rp.id_rol=1");
            st.executeUpdate("INSERT IGNORE INTO rol_permiso (id_rol, id_permiso) SELECT r.id_rol, p.id_permiso FROM permiso p " +
                             "JOIN (SELECT 3 AS id_rol UNION SELECT 12 UNION SELECT 13 UNION SELECT 14) r WHERE p.codigo='GESTIONAR_COMUNICA'");
            st.executeUpdate("INSERT IGNORE INTO rol_permiso (id_rol, id_permiso) SELECT r.id_rol, p.id_permiso FROM permiso p " +
                             "JOIN (SELECT 3 AS id_rol UNION SELECT 13 UNION SELECT 14) r WHERE p.codigo='MODERAR_COMUNICA'");
            // quien está en el team Comunica queda con el rol Equipo de comunica
            st.executeUpdate("INSERT IGNORE INTO miembro_rol (id_miembro, id_rol) SELECT mt.id_miembro, 12 FROM miembro_team mt " +
                             "JOIN team t ON t.id_team=mt.id_team WHERE t.codigo='COMUNICA'");
            String UTF = " ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
            // un saludo por cumpleañero y año: quién lo redacta y en qué va
            st.executeUpdate("CREATE TABLE IF NOT EXISTS com_saludo (id INT AUTO_INCREMENT PRIMARY KEY, id_miembro INT NOT NULL, anio SMALLINT NOT NULL, " +
                             "fecha DATE NOT NULL, id_responsable INT NULL, " +
                             "estado ENUM('PENDIENTE','REDACTADO','VIDEO_LISTO','PUBLICADO','OMITIDO') NOT NULL DEFAULT 'PENDIENTE', " +
                             "texto TEXT NULL, link_video VARCHAR(500) NULL, publicado_en VARCHAR(60) NULL, " +
                             "actualizado_por INT NULL, fecha_actualizacion DATETIME NULL, " +
                             "CONSTRAINT uq_saludo UNIQUE (id_miembro, anio), KEY ix_saludo_fecha (fecha), " +
                             "CONSTRAINT fk_sal_miembro FOREIGN KEY (id_miembro) REFERENCES miembro(id_miembro) ON DELETE CASCADE, " +
                             "CONSTRAINT fk_sal_resp FOREIGN KEY (id_responsable) REFERENCES miembro(id_miembro) ON DELETE SET NULL, " +
                             "CONSTRAINT fk_sal_act FOREIGN KEY (actualizado_por) REFERENCES miembro(id_miembro) ON DELETE SET NULL)" + UTF);
            // proyectos de contenido; las ideas son proyectos en estado IDEA (cualquier miembro puede proponer)
            st.executeUpdate("CREATE TABLE IF NOT EXISTS com_proyecto (id INT AUTO_INCREMENT PRIMARY KEY, titulo VARCHAR(120) NOT NULL, " +
                             "tipo ENUM('TIKTOK','REEL','POST','HISTORIA','DESTACADA','FLYER','MEME','TUTORIAL','COMUNICADO','OTRO') NOT NULL DEFAULT 'OTRO', " +
                             "red ENUM('INSTAGRAM','TIKTOK','TELEGRAM','VARIAS') NOT NULL DEFAULT 'INSTAGRAM', " +
                             "estado ENUM('IDEA','EN_DESARROLLO','PUBLICADO','ARCHIVADO') NOT NULL DEFAULT 'IDEA', " +
                             "descripcion TEXT NULL, etiquetas VARCHAR(200) NULL, id_responsable INT NULL, fecha_objetivo DATE NULL, " +
                             "link_referencia VARCHAR(500) NULL, link_drive VARCHAR(500) NULL, link_publicacion VARCHAR(500) NULL, " +
                             "propuesto_por INT NULL, fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, fecha_publicacion DATE NULL, " +
                             "actualizado_por INT NULL, fecha_actualizacion DATETIME NULL, KEY ix_proy_estado (estado), " +
                             "CONSTRAINT fk_proy_resp FOREIGN KEY (id_responsable) REFERENCES miembro(id_miembro) ON DELETE SET NULL, " +
                             "CONSTRAINT fk_proy_prop FOREIGN KEY (propuesto_por) REFERENCES miembro(id_miembro) ON DELETE SET NULL, " +
                             "CONSTRAINT fk_proy_act FOREIGN KEY (actualizado_por) REFERENCES miembro(id_miembro) ON DELETE SET NULL)" + UTF);
            st.executeUpdate("CREATE TABLE IF NOT EXISTS com_tarea (id INT AUTO_INCREMENT PRIMARY KEY, id_proyecto INT NOT NULL, texto VARCHAR(200) NOT NULL, " +
                             "hecha TINYINT(1) NOT NULL DEFAULT 0, " +
                             "CONSTRAINT fk_tarea_proy FOREIGN KEY (id_proyecto) REFERENCES com_proyecto(id) ON DELETE CASCADE)" + UTF);
            st.executeUpdate("CREATE TABLE IF NOT EXISTS com_voto (id_proyecto INT NOT NULL, id_miembro INT NOT NULL, PRIMARY KEY (id_proyecto, id_miembro), " +
                             "CONSTRAINT fk_voto_proy FOREIGN KEY (id_proyecto) REFERENCES com_proyecto(id) ON DELETE CASCADE, " +
                             "CONSTRAINT fk_voto_miembro FOREIGN KEY (id_miembro) REFERENCES miembro(id_miembro) ON DELETE CASCADE)" + UTF);
            // plantillas de flyers: vista previa (imagen liviana) + link de Canva para adaptarla rápido
            st.executeUpdate("CREATE TABLE IF NOT EXISTS com_plantilla (id INT AUTO_INCREMENT PRIMARY KEY, nombre VARCHAR(100) NOT NULL, " +
                             "tipo VARCHAR(40) NOT NULL, ciclo VARCHAR(10) NULL, link_canva VARCHAR(500) NULL, notas VARCHAR(300) NULL, " +
                             "imagen MEDIUMBLOB NULL, activa TINYINT(1) NOT NULL DEFAULT 1, subido_por INT NULL, " +
                             "fecha DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                             "CONSTRAINT fk_plant_sub FOREIGN KEY (subido_por) REFERENCES miembro(id_miembro) ON DELETE SET NULL)" + UTF);
            st.executeUpdate("INSERT IGNORE INTO config (clave, valor) VALUES ('comunica.plantilla_saludo', " +
                             "'¡Feliz cumpleaños, {apodo}! 🎉 Todo Son de PUCP te desea un día lleno de son, casino y mucha cubanía. ¡Que lo bailes con todo! 💃🕺')");
        }
    }

    /** Módulo RRPP (roles 15-17): externos del taller del jueves, visitas formales y directorio. Encuestas para los admins de todos los teams. */
    private static void migrarRrpp(Connection c) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.executeUpdate("INSERT IGNORE INTO rol_sistema (id_rol, codigo, nombre) VALUES (15,'EQUIPO_RRPP','Equipo de RRPP')," +
                             "(16,'MODERADOR_RRPP','Moderador de RRPP'),(17,'ADMIN_RRPP','Admin de RRPP')");
            st.executeUpdate("ALTER TABLE permiso MODIFY seccion ENUM('MI_ESPACIO','ENSAYOS','TALLERES','FINANZAS','COMUNICA','RRPP','ADMINISTRACION') NOT NULL");
            st.executeUpdate("INSERT IGNORE INTO permiso (id_permiso, codigo, descripcion, seccion) SELECT COALESCE(MAX(id_permiso),0)+1, " +
                             "'GESTIONAR_RRPP', 'Entrar a RRPP: externos del jueves, visitas formales y directorio de contactos', 'RRPP' FROM permiso");
            st.executeUpdate("INSERT IGNORE INTO permiso (id_permiso, codigo, descripcion, seccion) SELECT COALESCE(MAX(id_permiso),0)+1, " +
                             "'MODERAR_RRPP', 'Configurar el ciclo de externos, cambiar el encargado y borrar contactos o visitas', 'RRPP' FROM permiso");
            st.executeUpdate("INSERT IGNORE INTO permiso (id_permiso, codigo, descripcion, seccion) SELECT COALESCE(MAX(id_permiso),0)+1, " +
                             "'CREAR_ENCUESTAS', 'Crear encuestas para todo el grupo o para un team', 'ADMINISTRACION' FROM permiso");
            st.executeUpdate("INSERT IGNORE INTO rol_permiso (id_rol, id_permiso) SELECT r.id_rol, rp.id_permiso FROM rol_permiso rp " +
                             "JOIN (SELECT 15 AS id_rol UNION SELECT 16 UNION SELECT 17) r WHERE rp.id_rol=1");
            st.executeUpdate("INSERT IGNORE INTO rol_permiso (id_rol, id_permiso) SELECT r.id_rol, p.id_permiso FROM permiso p " +
                             "JOIN (SELECT 3 AS id_rol UNION SELECT 15 UNION SELECT 16 UNION SELECT 17) r WHERE p.codigo='GESTIONAR_RRPP'");
            st.executeUpdate("INSERT IGNORE INTO rol_permiso (id_rol, id_permiso) SELECT r.id_rol, p.id_permiso FROM permiso p " +
                             "JOIN (SELECT 3 AS id_rol UNION SELECT 16 UNION SELECT 17) r WHERE p.codigo='MODERAR_RRPP'");
            // encuestas: Owner y los moderadores/admins de todos los teams
            st.executeUpdate("INSERT IGNORE INTO rol_permiso (id_rol, id_permiso) SELECT r.id_rol, p.id_permiso FROM permiso p JOIN (SELECT 3 AS id_rol " +
                             "UNION SELECT 4 UNION SELECT 5 UNION SELECT 7 UNION SELECT 8 UNION SELECT 10 UNION SELECT 11 UNION SELECT 13 UNION SELECT 14 " +
                             "UNION SELECT 16 UNION SELECT 17) r WHERE p.codigo='CREAR_ENCUESTAS'");
            st.executeUpdate("INSERT IGNORE INTO miembro_rol (id_miembro, id_rol) SELECT mt.id_miembro, 15 FROM miembro_team mt " +
                             "JOIN team t ON t.id_team=mt.id_team WHERE t.codigo='RRPP'");
            String UTF = " ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";

            // ---------- encuestas ----------
            st.executeUpdate("CREATE TABLE IF NOT EXISTS encuesta (id INT AUTO_INCREMENT PRIMARY KEY, pregunta VARCHAR(200) NOT NULL, descripcion VARCHAR(1000) NULL, " +
                             "tipo ENUM('SI_NO','UNA','VARIAS','FECHAS') NOT NULL DEFAULT 'UNA', anonima TINYINT(1) NOT NULL DEFAULT 0, " +
                             "ver_antes TINYINT(1) NOT NULL DEFAULT 0, id_team SMALLINT NULL, cierre DATETIME NOT NULL, " +
                             "creada_por INT NULL, fecha DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, KEY ix_enc_cierre (cierre), " +
                             "CONSTRAINT fk_enc_team FOREIGN KEY (id_team) REFERENCES team(id_team), " +
                             "CONSTRAINT fk_enc_crea FOREIGN KEY (creada_por) REFERENCES miembro(id_miembro) ON DELETE SET NULL)" + UTF);
            st.executeUpdate("CREATE TABLE IF NOT EXISTS encuesta_opcion (id INT AUTO_INCREMENT PRIMARY KEY, id_encuesta INT NOT NULL, texto VARCHAR(120) NOT NULL, " +
                             "orden SMALLINT NOT NULL DEFAULT 0, CONSTRAINT fk_eo_enc FOREIGN KEY (id_encuesta) REFERENCES encuesta(id) ON DELETE CASCADE)" + UTF);
            st.executeUpdate("CREATE TABLE IF NOT EXISTS encuesta_voto (id_opcion INT NOT NULL, id_miembro INT NOT NULL, id_encuesta INT NOT NULL, " +
                             "fecha DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (id_opcion, id_miembro), KEY ix_ev_enc (id_encuesta, id_miembro), " +
                             "CONSTRAINT fk_ev_op FOREIGN KEY (id_opcion) REFERENCES encuesta_opcion(id) ON DELETE CASCADE, " +
                             "CONSTRAINT fk_ev_enc FOREIGN KEY (id_encuesta) REFERENCES encuesta(id) ON DELETE CASCADE, " +
                             "CONSTRAINT fk_ev_m FOREIGN KEY (id_miembro) REFERENCES miembro(id_miembro) ON DELETE CASCADE)" + UTF);

            // ---------- directorio de contactos (privado: solo RRPP) ----------
            st.executeUpdate("CREATE TABLE IF NOT EXISTS rrpp_contacto (id INT AUTO_INCREMENT PRIMARY KEY, " +
                             "categoria ENUM('RUEDA','ACADEMIA','CENTRO_FEDERADO','H_T','AUTORIDAD','OTRO') NOT NULL, nombre VARCHAR(120) NOT NULL, " +
                             "persona VARCHAR(120) NULL, cargo VARCHAR(80) NULL, telefono VARCHAR(30) NULL, correo VARCHAR(120) NULL, instagram VARCHAR(60) NULL, " +
                             "ubicacion VARCHAR(200) NULL, link_mapa VARCHAR(500) NULL, horario VARCHAR(200) NULL, ciclo VARCHAR(10) NULL, " +
                             "estado ENUM('SIN_CONTACTAR','CONTACTADO','RESPONDIO','ALIADO') NOT NULL DEFAULT 'SIN_CONTACTAR', notas VARCHAR(1000) NULL, " +
                             "ultimo_contacto DATE NULL, id_responsable INT NULL, creado_por INT NULL, fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                             "KEY ix_cont_cat (categoria), " +
                             "CONSTRAINT fk_cont_resp FOREIGN KEY (id_responsable) REFERENCES miembro(id_miembro) ON DELETE SET NULL, " +
                             "CONSTRAINT fk_cont_crea FOREIGN KEY (creado_por) REFERENCES miembro(id_miembro) ON DELETE SET NULL)" + UTF);
            st.executeUpdate("CREATE TABLE IF NOT EXISTS rrpp_interaccion (id INT AUTO_INCREMENT PRIMARY KEY, id_contacto INT NOT NULL, fecha DATE NOT NULL, " +
                             "nota VARCHAR(500) NOT NULL, registrado_por INT NULL, " +
                             "CONSTRAINT fk_int_cont FOREIGN KEY (id_contacto) REFERENCES rrpp_contacto(id) ON DELETE CASCADE, " +
                             "CONSTRAINT fk_int_reg FOREIGN KEY (registrado_por) REFERENCES miembro(id_miembro) ON DELETE SET NULL)" + UTF);

            // ---------- visitas formales (vamos todos) ----------
            st.executeUpdate("CREATE TABLE IF NOT EXISTS rrpp_visita (id INT AUTO_INCREMENT PRIMARY KEY, titulo VARCHAR(120) NOT NULL, id_contacto INT NULL, " +
                             "lugar VARCHAR(200) NULL, link_mapa VARCHAR(500) NULL, fecha DATE NOT NULL, hora TIME NULL, punto_encuentro VARCHAR(200) NULL, " +
                             "hora_encuentro TIME NULL, como_llegar VARCHAR(500) NULL, costo VARCHAR(80) NULL, dress_code VARCHAR(120) NULL, notas VARCHAR(1000) NULL, " +
                             "estado ENUM('BORRADOR','CONVOCADA','REALIZADA','CANCELADA') NOT NULL DEFAULT 'BORRADOR', " +
                             "checklist SET('AVISAR','PUBLICAR','RECORDAR','FOTO','AGRADECER') NOT NULL DEFAULT '', " +
                             "id_coordinador INT NULL, link_foto VARCHAR(500) NULL, id_encuesta INT NULL, id_proyecto INT NULL, " +
                             "creado_por INT NULL, fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, KEY ix_vis_fecha (fecha), " +
                             "CONSTRAINT fk_vis_cont FOREIGN KEY (id_contacto) REFERENCES rrpp_contacto(id) ON DELETE SET NULL, " +
                             "CONSTRAINT fk_vis_coord FOREIGN KEY (id_coordinador) REFERENCES miembro(id_miembro) ON DELETE SET NULL, " +
                             "CONSTRAINT fk_vis_enc FOREIGN KEY (id_encuesta) REFERENCES encuesta(id) ON DELETE SET NULL, " +
                             "CONSTRAINT fk_vis_proy FOREIGN KEY (id_proyecto) REFERENCES com_proyecto(id) ON DELETE SET NULL, " +
                             "CONSTRAINT fk_vis_crea FOREIGN KEY (creado_por) REFERENCES miembro(id_miembro) ON DELETE SET NULL)" + UTF);
            st.executeUpdate("CREATE TABLE IF NOT EXISTS rrpp_visita_resp (id_visita INT NOT NULL, id_miembro INT NOT NULL, respuesta ENUM('VOY','NO_PUEDO') NOT NULL, " +
                             "fecha DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (id_visita, id_miembro), " +
                             "CONSTRAINT fk_vr_vis FOREIGN KEY (id_visita) REFERENCES rrpp_visita(id) ON DELETE CASCADE, " +
                             "CONSTRAINT fk_vr_m FOREIGN KEY (id_miembro) REFERENCES miembro(id_miembro) ON DELETE CASCADE)" + UTF);

            // ---------- externos del taller del jueves: una fila por semana ----------
            st.executeUpdate("CREATE TABLE IF NOT EXISTS rrpp_externos (id INT AUTO_INCREMENT PRIMARY KEY, jueves DATE NOT NULL UNIQUE, link_form VARCHAR(500) NULL, " +
                             "id_encargado INT NULL, publicado_por INT NULL, publicado_en DATETIME NULL, enviado_por INT NULL, enviado_en DATETIME NULL, " +
                             "sin_taller TINYINT(1) NOT NULL DEFAULT 0, nota VARCHAR(200) NULL, " +
                             "CONSTRAINT fk_ext_enc FOREIGN KEY (id_encargado) REFERENCES miembro(id_miembro) ON DELETE SET NULL, " +
                             "CONSTRAINT fk_ext_pub FOREIGN KEY (publicado_por) REFERENCES miembro(id_miembro) ON DELETE SET NULL, " +
                             "CONSTRAINT fk_ext_env FOREIGN KEY (enviado_por) REFERENCES miembro(id_miembro) ON DELETE SET NULL)" + UTF);
            // ciclo por defecto (se cambia en RRPP → Externos): publicar el sábado, cerrar el lunes 23:59, enviar a la DAES el martes
            st.executeUpdate("INSERT IGNORE INTO config (clave, valor) VALUES ('rrpp.ext.dia_publicar','6'),('rrpp.ext.dia_cierre','1')," +
                             "('rrpp.ext.hora_cierre','23:59'),('rrpp.ext.dia_envio','2'),('rrpp.ext.encargado',''),('rrpp.meta_visitas','1')");
        }
    }

    private static boolean existeTabla(Connection c, String tabla) throws SQLException {
        return existeFila(c, "SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = '" + tabla + "'");
    }

    private static boolean existeFila(Connection c, String sql) throws SQLException {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) { return rs.next(); }
    }

    private static boolean existeColumna(Connection c, String tabla, String columna) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?")) {
            ps.setString(1, tabla); ps.setString(2, columna);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    /** Cuántas cuentas de prueba (@sdp.test) hay activas: en producción no debería haber ninguna. */
    public int cuentasDePrueba() throws SQLException {
        try (Connection c = getConnection(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM miembro WHERE correo LIKE '%@sdp.test' AND password_hash IS NOT NULL")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }
}
