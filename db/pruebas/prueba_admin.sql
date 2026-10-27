-- =====================================================================
--  Prueba de altas y bajas (ejecutar DESPUÉS de schema.sql + seed.sql)
--  Todo va en una transacción y se deshace al final (ROLLBACK).
-- =====================================================================
USE sdp_ensayos;
SET NAMES utf8mb4;
START TRANSACTION;

-- 1. Alta de miembro -> el trigger le da el rol MIEMBRO; sin evaluaciones = sin nivel
INSERT INTO miembro (nombre, nombre_completo, correo, rol_baile, frecuencia, fecha_ingreso, creado_por)
VALUES ('Hugo Ramos', 'Hugo Ramos Díaz', 'a2025xxxx@pucp.edu.pe', 'LEADER', 'REGULAR', CURDATE(), 55);
SELECT 'alta miembro' AS prueba, nombre, estado, nivel, puede_eliminar,
       (SELECT GROUP_CONCAT(r.codigo) FROM miembro_rol mr JOIN rol_sistema r USING (id_rol) WHERE mr.id_miembro = v.id_miembro) AS roles
FROM v_miembro_admin v WHERE nombre = 'Hugo Ramos';                       -- esperado: ACTIVO, NULL, 1, MIEMBRO

-- 2. Alta de figura sin orden -> va al final, sin color, no cambia ningún nivel
INSERT INTO figura (nombre, creado_por) VALUES ('Paseo de la Hélice', 55);
SELECT 'alta figura' AS prueba, nombre, orden, color, puede_eliminar FROM v_figura_admin WHERE nombre = 'Paseo de la Hélice';  -- 38, NULL, 1

-- 3. Eliminar la figura nueva (sin uso) -> se borra
DELETE FROM figura WHERE nombre = 'Paseo de la Hélice' AND id_figura IN (SELECT id_figura FROM v_figura_admin WHERE puede_eliminar = 1);
SELECT 'eliminar figura sin uso' AS prueba, ROW_COUNT() AS filas_borradas;   -- 1

-- 4. Intentar eliminar una figura con evaluaciones -> el filtro no borra nada (y la FK lo impediría igual)
DELETE FROM figura WHERE nombre = 'Coyote' AND id_figura IN (SELECT id_figura FROM v_figura_admin WHERE puede_eliminar = 1);
SELECT 'eliminar figura usada' AS prueba, ROW_COUNT() AS filas_borradas;     -- 0 -> la app sugiere desactivarla

-- 5. Desactivar Coyote -> sale del cálculo (36 figuras) y queda con fecha de baja; reactivar la limpia
UPDATE figura SET activa = 0, baja_por = 55 WHERE nombre = 'Coyote';
SELECT 'desactivar figura' AS prueba, (SELECT COUNT(*) FROM v_color_figura) AS figuras_en_calculo,
       (SELECT fecha_baja IS NOT NULL FROM figura WHERE nombre = 'Coyote') AS tiene_fecha_baja;   -- 36, 1
UPDATE figura SET activa = 1 WHERE nombre = 'Coyote';
SELECT 'reactivar figura' AS prueba, (SELECT COUNT(*) FROM v_color_figura) AS figuras_en_calculo; -- 37

-- 6. Re-crear una figura con un nombre que ya existe -> se reactiva en vez de duplicarse
UPDATE figura SET activa = 0 WHERE nombre = 'Pulpo';
INSERT INTO figura (nombre, creado_por) VALUES ('Pulpo', 55) ON DUPLICATE KEY UPDATE activa = 1;
SELECT 'nombre repetido' AS prueba, COUNT(*) AS filas_pulpo, MAX(activa) AS activa FROM figura WHERE nombre = 'Pulpo'; -- 1, 1

-- 7. Inactivar miembro -> fecha de baja y deja de contar para el color
UPDATE miembro SET estado = 'INACTIVO', baja_por = 55, motivo_baja = 'Se retiró' WHERE nombre = 'Hugo Ramos';
SELECT 'inactivar miembro' AS prueba, estado, fecha_baja IS NOT NULL AS tiene_fecha_baja FROM miembro WHERE nombre = 'Hugo Ramos';

-- Casos que deben dar ERROR (probar uno por uno, fuera de este script):
--   DELETE FROM miembro_rol WHERE id_rol = 3;            -> "Debe quedar al menos un admin"
--   DELETE FROM figura WHERE nombre = 'Coyote';          -> error 1451 (FK): tiene evaluaciones
--   DELETE FROM miembro WHERE nombre = 'Esteban Gutiérrez'; -> error 1451 (FK): tiene asistencias

ROLLBACK;
