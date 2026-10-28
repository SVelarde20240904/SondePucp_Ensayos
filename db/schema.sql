-- =====================================================================
--  Son de PUCP · Sistema de Ensayos — Esquema de base de datos
--  MySQL 8.0+ (probado también en MariaDB 10.11)
--  Reemplaza al Excel "Matriz de Figuras SDP v2"
-- =====================================================================
DROP DATABASE IF EXISTS sdp_ensayos;
CREATE DATABASE sdp_ensayos CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE sdp_ensayos;

-- ---------------------------------------------------------------------
-- 1. MIEMBROS, ROLES Y PERMISOS
-- ---------------------------------------------------------------------
-- Nota: la app agrega sola al arrancar (ArranqueDao.migrar) lo que vino después de este script:
--   figura.video_url · miembro.apodo · miembro.fecha_nacimiento · tablas team y miembro_team
--   roles 4 Moderador de ensayos y 5 Admin de ensayos (el 3 se llama Owner) y sus permisos.
CREATE TABLE miembro (
  id_miembro      INT AUTO_INCREMENT PRIMARY KEY,
  nombre          VARCHAR(60)  NOT NULL,              -- "Nombre_Matriz": nombre corto que se ve en el sistema
  nombre_completo VARCHAR(120) NOT NULL,
  codigo_pucp     CHAR(8)      NULL,
  correo          VARCHAR(100) NULL,                  -- usuario de login (correo PUCP)
  password_hash   VARCHAR(255) NULL,                  -- se llena cuando la persona activa su cuenta
  celular         VARCHAR(15)  NULL,
  facultad        VARCHAR(80)  NULL,
  rol_baile       ENUM('LEADER','FOLLOWER') NULL,     -- se registra una vez; solo lo cambia el admin
  frecuencia      ENUM('REGULAR','OCASIONAL','EVENTUAL') NULL,
  estado          ENUM('ACTIVO','INACTIVO') NOT NULL DEFAULT 'ACTIVO',  -- nunca se borra: se inactiva
  fecha_ingreso   DATE NULL,
  notas           VARCHAR(255) NULL,
  aviso_asistencia TINYINT(1) NOT NULL DEFAULT 1,     -- preferencias de "Mi perfil"
  aviso_mensual    TINYINT(1) NOT NULL DEFAULT 1,
  aviso_ensayo     TINYINT(1) NOT NULL DEFAULT 0,
  -- Alta y baja (solo el admin)
  debe_cambiar_password TINYINT(1) NOT NULL DEFAULT 1,  -- el admin da una clave temporal; se cambia al primer login
  creado_por      INT NULL,
  fecha_creacion  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  fecha_baja      DATETIME NULL,                      -- se llena al pasar a INACTIVO
  baja_por        INT NULL,
  motivo_baja     VARCHAR(200) NULL,
  CONSTRAINT fk_miembro_creado FOREIGN KEY (creado_por) REFERENCES miembro(id_miembro),
  CONSTRAINT fk_miembro_baja   FOREIGN KEY (baja_por)   REFERENCES miembro(id_miembro),
  CONSTRAINT ck_miembro_baja CHECK (estado = 'INACTIVO' OR fecha_baja IS NULL),
  CONSTRAINT uq_miembro_nombre UNIQUE (nombre),
  CONSTRAINT uq_miembro_codigo UNIQUE (codigo_pucp),
  CONSTRAINT uq_miembro_correo UNIQUE (correo)
) ENGINE=InnoDB;

-- Todos son MIEMBRO; además pueden ser EQUIPO_ENSAYOS y/o ADMIN
CREATE TABLE rol_sistema (
  id_rol  TINYINT PRIMARY KEY,
  codigo  VARCHAR(20) NOT NULL UNIQUE,
  nombre  VARCHAR(40) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE miembro_rol (
  id_miembro INT NOT NULL,
  id_rol     TINYINT NOT NULL,
  asignado_por INT NULL,
  fecha_asignacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id_miembro, id_rol),
  CONSTRAINT fk_mr_miembro FOREIGN KEY (id_miembro) REFERENCES miembro(id_miembro) ON DELETE CASCADE,
  CONSTRAINT fk_mr_rol     FOREIGN KEY (id_rol)     REFERENCES rol_sistema(id_rol),
  CONSTRAINT fk_mr_asigna  FOREIGN KEY (asignado_por) REFERENCES miembro(id_miembro)
) ENGINE=InnoDB;

CREATE TABLE permiso (
  id_permiso SMALLINT PRIMARY KEY,
  codigo     VARCHAR(40) NOT NULL UNIQUE,
  descripcion VARCHAR(80) NOT NULL,
  seccion    ENUM('MI_ESPACIO','ENSAYOS','ADMINISTRACION') NOT NULL
) ENGINE=InnoDB;

CREATE TABLE rol_permiso (
  id_rol     TINYINT  NOT NULL,
  id_permiso SMALLINT NOT NULL,
  PRIMARY KEY (id_rol, id_permiso),
  CONSTRAINT fk_rp_rol FOREIGN KEY (id_rol) REFERENCES rol_sistema(id_rol),
  CONSTRAINT fk_rp_perm FOREIGN KEY (id_permiso) REFERENCES permiso(id_permiso)
) ENGINE=InnoDB;

-- Antes: la "tablita verde" de MIEMBROS (J2:K4) y la celda K7
CREATE TABLE regla_conteo_color (
  clave  ENUM('REGULAR','OCASIONAL','EVENTUAL','EQUIPO_ENSAYOS') PRIMARY KEY,
  cuenta TINYINT(1) NOT NULL
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 2. FIGURAS Y AUTOEVALUACIONES
-- ---------------------------------------------------------------------
-- Baja de figuras:
--   * DESACTIVAR (activa = 0): la figura sale de la mano, de las autoevaluaciones y de los
--     cálculos, pero se conserva su historial. Se puede reactivar.
--   * ELIMINAR (DELETE): solo si nunca se evaluó ni se enseñó (p. ej. se creó por error).
--     Las FK (RESTRICT) impiden borrar una figura con datos; ver v_figura_admin.puede_eliminar.
CREATE TABLE figura (
  id_figura  INT AUTO_INCREMENT PRIMARY KEY,
  nombre     VARCHAR(60) NOT NULL UNIQUE,           -- un nombre repetido se reactiva, no se duplica
  orden      SMALLINT NULL,                         -- si llega NULL, el trigger la pone al final
  activa     TINYINT(1) NOT NULL DEFAULT 1,
  descripcion VARCHAR(1000) NULL,                   -- pasos o notas (biblioteca de figuras)
  video_url  VARCHAR(500) NULL,                     -- enlace a Drive/YouTube (Drive RESTRINGIDO al grupo de SDP)
  creado_por INT NULL,
  fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  fecha_baja DATETIME NULL,
  baja_por   INT NULL,
  CONSTRAINT fk_figura_creado FOREIGN KEY (creado_por) REFERENCES miembro(id_miembro),
  CONSTRAINT fk_figura_baja   FOREIGN KEY (baja_por)   REFERENCES miembro(id_miembro),
  CONSTRAINT ck_figura_baja CHECK (activa = 0 OR fecha_baja IS NULL)
) ENGINE=InnoDB;

-- La "MATRIZ": color ACTUAL de cada miembro en cada figura
CREATE TABLE evaluacion (
  id_miembro INT NOT NULL,
  id_figura  INT NOT NULL,
  color      ENUM('ROJO','AMARILLO','VERDE','AZUL') NOT NULL,
  fecha_actualizacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id_miembro, id_figura),
  CONSTRAINT fk_ev_miembro FOREIGN KEY (id_miembro) REFERENCES miembro(id_miembro),
  CONSTRAINT fk_ev_figura  FOREIGN KEY (id_figura)  REFERENCES figura(id_figura)
) ENGINE=InnoDB;

-- Ventanas de la autoevaluación mensual
CREATE TABLE periodo_autoevaluacion (
  id_periodo    INT AUTO_INCREMENT PRIMARY KEY,
  anio          SMALLINT NOT NULL,
  mes           TINYINT  NOT NULL,
  fecha_apertura DATETIME NOT NULL,
  fecha_cierre   DATETIME NOT NULL,
  CONSTRAINT uq_periodo UNIQUE (anio, mes),
  CONSTRAINT ck_periodo_mes CHECK (mes BETWEEN 1 AND 12)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 3. ENSAYOS
-- ---------------------------------------------------------------------
CREATE TABLE ensayo (
  id_ensayo   INT AUTO_INCREMENT PRIMARY KEY,
  tipo        ENUM('GENERAL','COMPETENCIA','NIVELACION','EXHIBICION') NOT NULL DEFAULT 'GENERAL',
  fecha       DATE NOT NULL,
  hora_inicio TIME NOT NULL,
  hora_fin    TIME NOT NULL,
  lugar       VARCHAR(80) NOT NULL DEFAULT 'Explanada del polideportivo',
  cierre_confirmacion DATETIME NULL,                 -- después de esto, quien no respondió queda "Sin respuesta"
  estado      ENUM('BORRADOR','PROGRAMADO','CERRADO','CANCELADO') NOT NULL DEFAULT 'BORRADOR',
  notas       TEXT NULL,
  notas_autor INT NULL,
  creado_por  INT NULL,
  fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_en_notas  FOREIGN KEY (notas_autor) REFERENCES miembro(id_miembro),
  CONSTRAINT fk_en_creado FOREIGN KEY (creado_por)  REFERENCES miembro(id_miembro),
  CONSTRAINT ck_en_horas CHECK (hora_fin > hora_inicio)
) ENGINE=InnoDB;
CREATE INDEX ix_ensayo_fecha ON ensayo(fecha);

CREATE TABLE ensayo_indicacion (
  id_indicacion INT AUTO_INCREMENT PRIMARY KEY,
  id_ensayo INT NOT NULL,
  texto     VARCHAR(120) NOT NULL,
  orden     TINYINT NOT NULL DEFAULT 1,
  CONSTRAINT fk_ind_ensayo FOREIGN KEY (id_ensayo) REFERENCES ensayo(id_ensayo) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Quiénes del equipo de ensayos dictan (sirven para completar parejas)
CREATE TABLE ensayo_dictante (
  id_ensayo  INT NOT NULL,
  id_miembro INT NOT NULL,
  PRIMARY KEY (id_ensayo, id_miembro),
  CONSTRAINT fk_dic_ensayo  FOREIGN KEY (id_ensayo)  REFERENCES ensayo(id_ensayo) ON DELETE CASCADE,
  CONSTRAINT fk_dic_miembro FOREIGN KEY (id_miembro) REFERENCES miembro(id_miembro)
) ENGINE=InnoDB;

-- Confirmación (antes: encuesta de WhatsApp) + asistencia real
CREATE TABLE asistencia (
  id_ensayo    INT NOT NULL,
  id_miembro   INT NOT NULL,
  confirmacion ENUM('SI','NO','SIN_RESPUESTA') NOT NULL DEFAULT 'SIN_RESPUESTA',
  motivo       VARCHAR(200) NULL,                 -- si avisa que no va
  fecha_confirmacion DATETIME NULL,
  estado_real  ENUM('AP','AN','TJ','TI','FJ','FI') NULL,  -- se llena en "Tomar asistencia"
  registrado_por INT NULL,
  PRIMARY KEY (id_ensayo, id_miembro),
  CONSTRAINT fk_as_ensayo  FOREIGN KEY (id_ensayo)  REFERENCES ensayo(id_ensayo) ON DELETE CASCADE,
  CONSTRAINT fk_as_miembro FOREIGN KEY (id_miembro) REFERENCES miembro(id_miembro),
  CONSTRAINT fk_as_reg     FOREIGN KEY (registrado_por) REFERENCES miembro(id_miembro)
) ENGINE=InnoDB;

-- Figuras que realmente se enseñaron (habilita la autoevaluación del ensayo)
CREATE TABLE ensayo_figura (
  id_ensayo INT NOT NULL,
  id_figura INT NOT NULL,
  nivel     ENUM('AZUL','VERDE','AMARILLO','ROJO') NULL,  -- color de la figura al momento de enseñarla
  dicto     INT NULL,
  PRIMARY KEY (id_ensayo, id_figura),
  CONSTRAINT fk_ef_ensayo FOREIGN KEY (id_ensayo) REFERENCES ensayo(id_ensayo) ON DELETE CASCADE,
  CONSTRAINT fk_ef_figura FOREIGN KEY (id_figura) REFERENCES figura(id_figura),
  CONSTRAINT fk_ef_dicto  FOREIGN KEY (dicto)     REFERENCES miembro(id_miembro)
) ENGINE=InnoDB;

-- Log de cada cambio de color (antes: hoja HISTORICO)
CREATE TABLE historial_evaluacion (
  id_historial   INT AUTO_INCREMENT PRIMARY KEY,
  id_miembro     INT NOT NULL,
  id_figura      INT NOT NULL,
  color_anterior ENUM('ROJO','AMARILLO','VERDE','AZUL') NULL,
  color_nuevo    ENUM('ROJO','AMARILLO','VERDE','AZUL') NOT NULL,
  fuente         ENUM('CARGA_INICIAL','MENSUAL','ENSAYO','CORRECCION') NOT NULL,
  aplicado       TINYINT(1) NOT NULL DEFAULT 1,   -- 0 = ignorado por la regla "solo sube"
  id_ensayo      INT NULL,
  id_periodo     INT NULL,
  registrado_por INT NULL,                         -- quien hizo la corrección (o la propia persona)
  motivo         VARCHAR(200) NULL,
  fecha          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_he_miembro FOREIGN KEY (id_miembro) REFERENCES miembro(id_miembro),
  CONSTRAINT fk_he_figura  FOREIGN KEY (id_figura)  REFERENCES figura(id_figura),
  CONSTRAINT fk_he_ensayo  FOREIGN KEY (id_ensayo)  REFERENCES ensayo(id_ensayo),
  CONSTRAINT fk_he_periodo FOREIGN KEY (id_periodo) REFERENCES periodo_autoevaluacion(id_periodo),
  CONSTRAINT fk_he_reg     FOREIGN KEY (registrado_por) REFERENCES miembro(id_miembro)
) ENGINE=InnoDB;
CREATE INDEX ix_he_miembro_fecha ON historial_evaluacion(id_miembro, fecha);
CREATE INDEX ix_he_fecha ON historial_evaluacion(fecha);

-- ---------------------------------------------------------------------
-- 4. RESPONSABILIDADES (sistema nuevo: un estado + "cumplido por")
-- ---------------------------------------------------------------------
CREATE TABLE tipo_encargo (
  id_tipo  SMALLINT AUTO_INCREMENT PRIMARY KEY,
  nombre   VARCHAR(60) NOT NULL UNIQUE,
  activo   TINYINT(1) NOT NULL DEFAULT 1      -- 0 = lo automatizó la web
) ENGINE=InnoDB;

CREATE TABLE encargo (
  id_encargo   INT AUTO_INCREMENT PRIMARY KEY,
  id_tipo      SMALLINT NOT NULL,
  id_ensayo    INT NULL,
  semana       DATE NOT NULL,                  -- sábado de la semana del ensayo
  responsable  INT NOT NULL,
  plazo        DATETIME NULL,
  cumplido_por INT NULL,
  fecha_cumplimiento DATETIME NULL,
  exonerado    TINYINT(1) NOT NULL DEFAULT 0,  -- la "x" (eximido) de la hoja antigua
  justificada  TINYINT(1) NOT NULL DEFAULT 0,
  motivo_justificacion VARCHAR(200) NULL,
  CONSTRAINT fk_enc_tipo   FOREIGN KEY (id_tipo)      REFERENCES tipo_encargo(id_tipo),
  CONSTRAINT fk_enc_ensayo FOREIGN KEY (id_ensayo)    REFERENCES ensayo(id_ensayo),
  CONSTRAINT fk_enc_resp   FOREIGN KEY (responsable)  REFERENCES miembro(id_miembro),
  CONSTRAINT fk_enc_cumple FOREIGN KEY (cumplido_por) REFERENCES miembro(id_miembro)
) ENGINE=InnoDB;

-- =====================================================================
-- 5. VISTAS DE CÁLCULO (reemplazan las fórmulas de _CALC)
-- =====================================================================

-- ¿Quién cuenta para el color de las figuras? (antes: columna Cuenta_Color)
CREATE OR REPLACE VIEW v_miembro_flags AS
SELECT m.id_miembro,
       m.nombre,
       m.rol_baile,
       m.frecuencia,
       m.estado,
       EXISTS (SELECT 1 FROM miembro_rol mr JOIN rol_sistema r ON r.id_rol = mr.id_rol
               WHERE mr.id_miembro = m.id_miembro AND r.codigo = 'EQUIPO_ENSAYOS') AS es_equipo_ensayos,
       EXISTS (SELECT 1 FROM miembro_rol mr JOIN rol_sistema r ON r.id_rol = mr.id_rol
               WHERE mr.id_miembro = m.id_miembro AND r.codigo = 'ADMIN') AS es_admin
FROM miembro m;

CREATE OR REPLACE VIEW v_miembro_cuenta_color AS
SELECT f.id_miembro
FROM v_miembro_flags f
JOIN regla_conteo_color rf ON rf.clave = f.frecuencia AND rf.cuenta = 1
JOIN regla_conteo_color re ON re.clave = 'EQUIPO_ENSAYOS'
WHERE f.estado = 'ACTIVO'
  AND (f.es_equipo_ensayos = 0 OR re.cuenta = 1);

-- Color de cada figura = INT(MEDIANA) de los que cuentan (ROJO=1 … AZUL=4)
CREATE OR REPLACE VIEW v_color_figura AS
SELECT fi.id_figura,
       fi.nombre,
       fi.orden,
       med.n_evaluaciones,
       med.mediana,
       ELT(med.mediana, 'ROJO', 'AMARILLO', 'VERDE', 'AZUL') AS color
FROM figura fi
LEFT JOIN (
  SELECT r.id_figura, FLOOR(AVG(r.val)) AS mediana, MAX(r.n) AS n_evaluaciones
  FROM (
    SELECT e.id_figura,
           (e.color + 0) AS val,  -- posición en el ENUM: ROJO=1 … AZUL=4
           ROW_NUMBER() OVER (PARTITION BY e.id_figura ORDER BY (e.color + 0)) AS rn,
           COUNT(*)     OVER (PARTITION BY e.id_figura) AS n
    FROM evaluacion e
    JOIN v_miembro_cuenta_color c ON c.id_miembro = e.id_miembro
  ) r
  WHERE r.rn IN (FLOOR((r.n + 1) / 2), FLOOR((r.n + 2) / 2))
  GROUP BY r.id_figura
) med ON med.id_figura = fi.id_figura
WHERE fi.activa = 1;

-- Nivel de cada miembro: el primer nivel (AZUL→VERDE→AMARILLO) donde le falta
-- alguna figura (su color es ROJO o AMARILLO). Si sabe todo lo anterior → ROJO.
-- Sin ninguna evaluación → NULL (sin nivel).
CREATE OR REPLACE VIEW v_nivel_miembro AS
SELECT m.id_miembro,
       m.nombre,
       CASE
         WHEN NOT EXISTS (SELECT 1 FROM evaluacion e WHERE e.id_miembro = m.id_miembro) THEN NULL
         ELSE ELT(COALESCE((
                SELECT MIN(5 - cf.mediana)       -- AZUL(4)→1, VERDE(3)→2, AMARILLO(2)→3
                FROM evaluacion e
                JOIN v_color_figura cf ON cf.id_figura = e.id_figura
                WHERE e.id_miembro = m.id_miembro
                  AND e.color IN ('ROJO', 'AMARILLO')
                  AND cf.mediana >= 2), 4),
              'AZUL', 'VERDE', 'AMARILLO', 'ROJO')
       END AS nivel
FROM miembro m;

-- Estado de cada encargo (se calcula, no se guarda)
CREATE OR REPLACE VIEW v_encargo_estado AS
SELECT e.*,
       CASE
         WHEN e.exonerado = 1 THEN 'EXONERADO'
         WHEN e.fecha_cumplimiento IS NULL
              AND COALESCE(e.plazo, TIMESTAMP(e.semana, '23:59:59')) >= NOW() THEN 'PENDIENTE'
         WHEN e.fecha_cumplimiento IS NULL THEN 'NO_CUMPLIDA'
         WHEN e.plazo IS NULL OR e.fecha_cumplimiento <= e.plazo THEN 'A_TIEMPO'
         ELSE 'FUERA_DE_PLAZO'
       END AS estado,
       (e.cumplido_por IS NOT NULL AND e.cumplido_por <> e.responsable) AS es_cobertura
FROM encargo e;

-- Resumen por ensayo (pantalla 08 y reporte)
CREATE OR REPLACE VIEW v_ensayo_resumen AS
SELECT en.id_ensayo, en.fecha, en.tipo, en.estado,
       SUM(a.confirmacion = 'SI')                         AS confirmados,
       SUM(a.confirmacion = 'SIN_RESPUESTA')              AS sin_respuesta,
       SUM(a.estado_real IN ('AP','AN','TJ','TI'))        AS asistieron,
       SUM(a.estado_real IN ('AP','AN'))                  AS puntuales,
       SUM(a.estado_real IN ('TJ','TI'))                  AS tardanzas,
       SUM(a.estado_real IN ('FJ','FI'))                  AS faltas,
       (SELECT COUNT(*) FROM ensayo_figura ef WHERE ef.id_ensayo = en.id_ensayo) AS figuras_ensenadas,
       (SELECT COUNT(*) FROM historial_evaluacion h
         WHERE h.id_ensayo = en.id_ensayo AND h.aplicado = 1)                   AS avances
FROM ensayo en
LEFT JOIN asistencia a ON a.id_ensayo = en.id_ensayo
GROUP BY en.id_ensayo, en.fecha, en.tipo, en.estado;

-- Panel de admin: datos de uso para decidir si una figura se puede ELIMINAR o solo DESACTIVAR
CREATE OR REPLACE VIEW v_figura_admin AS
SELECT fi.id_figura, fi.nombre, fi.orden, fi.activa, fi.fecha_creacion, fi.fecha_baja,
       cf.color,
       (SELECT COUNT(*) FROM evaluacion e WHERE e.id_figura = fi.id_figura)           AS n_evaluaciones,
       (SELECT COUNT(*) FROM historial_evaluacion h WHERE h.id_figura = fi.id_figura) AS n_historial,
       (SELECT COUNT(*) FROM ensayo_figura ef WHERE ef.id_figura = fi.id_figura)      AS n_ensenada,
       NOT EXISTS (SELECT 1 FROM evaluacion e WHERE e.id_figura = fi.id_figura)
   AND NOT EXISTS (SELECT 1 FROM historial_evaluacion h WHERE h.id_figura = fi.id_figura)
   AND NOT EXISTS (SELECT 1 FROM ensayo_figura ef WHERE ef.id_figura = fi.id_figura) AS puede_eliminar
FROM figura fi
LEFT JOIN v_color_figura cf ON cf.id_figura = fi.id_figura;

-- Panel de admin: lo mismo para miembros (solo se elimina un alta hecha por error, sin datos)
CREATE OR REPLACE VIEW v_miembro_admin AS
SELECT m.id_miembro, m.nombre, m.nombre_completo, m.correo, m.rol_baile, m.frecuencia, m.estado,
       f.es_equipo_ensayos, f.es_admin, n.nivel,
       (m.password_hash IS NOT NULL) AS cuenta_activada,
       NOT EXISTS (SELECT 1 FROM evaluacion e WHERE e.id_miembro = m.id_miembro)
   AND NOT EXISTS (SELECT 1 FROM historial_evaluacion h WHERE h.id_miembro = m.id_miembro)
   AND NOT EXISTS (SELECT 1 FROM asistencia a WHERE a.id_miembro = m.id_miembro)
   AND NOT EXISTS (SELECT 1 FROM encargo en WHERE en.responsable = m.id_miembro OR en.cumplido_por = m.id_miembro) AS puede_eliminar
FROM miembro m
JOIN v_miembro_flags f ON f.id_miembro = m.id_miembro
LEFT JOIN v_nivel_miembro n ON n.id_miembro = m.id_miembro;

-- =====================================================================
-- 6. TRIGGERS (reglas que no dependen de que el Java las recuerde)
-- =====================================================================
DELIMITER $$

-- Todo miembro nuevo recibe el rol MIEMBRO automáticamente
CREATE TRIGGER trg_miembro_ai AFTER INSERT ON miembro FOR EACH ROW
BEGIN
  INSERT IGNORE INTO miembro_rol (id_miembro, id_rol, asignado_por) VALUES (NEW.id_miembro, 1, NEW.creado_por);
END$$

-- Inactivar / reactivar miembro: guarda o limpia la fecha de baja
CREATE TRIGGER trg_miembro_bu BEFORE UPDATE ON miembro FOR EACH ROW
BEGIN
  IF NEW.estado = 'INACTIVO' AND OLD.estado = 'ACTIVO' AND NEW.fecha_baja IS NULL THEN
    SET NEW.fecha_baja = NOW();
  ELSEIF NEW.estado = 'ACTIVO' AND OLD.estado = 'INACTIVO' THEN
    SET NEW.fecha_baja = NULL, NEW.baja_por = NULL, NEW.motivo_baja = NULL;
  END IF;
END$$

-- Nunca quedarse sin admin, y el rol MIEMBRO no se quita
CREATE TRIGGER trg_miembro_rol_bd BEFORE DELETE ON miembro_rol FOR EACH ROW
BEGIN
  IF OLD.id_rol = 1 AND EXISTS (SELECT 1 FROM miembro WHERE id_miembro = OLD.id_miembro) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El rol MIEMBRO no se puede quitar';
  END IF;
  IF OLD.id_rol = 3 AND (SELECT COUNT(*) FROM miembro_rol WHERE id_rol = 3) <= 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Debe quedar al menos un admin';
  END IF;
END$$

-- Figura nueva sin orden -> va al final de la mano
CREATE TRIGGER trg_figura_bi BEFORE INSERT ON figura FOR EACH ROW
BEGIN
  IF NEW.orden IS NULL THEN
    SET NEW.orden = (SELECT COALESCE(MAX(orden), 0) + 1 FROM figura);
  END IF;
END$$

-- Desactivar / reactivar figura: guarda o limpia la fecha de baja
CREATE TRIGGER trg_figura_bu BEFORE UPDATE ON figura FOR EACH ROW
BEGIN
  IF NEW.activa = 0 AND OLD.activa = 1 AND NEW.fecha_baja IS NULL THEN
    SET NEW.fecha_baja = NOW();
  ELSEIF NEW.activa = 1 AND OLD.activa = 0 THEN
    SET NEW.fecha_baja = NULL, NEW.baja_por = NULL;
  END IF;
END$$

DELIMITER ;
