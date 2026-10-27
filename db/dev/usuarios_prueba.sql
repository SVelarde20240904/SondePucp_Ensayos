-- =====================================================================
--  SOLO PARA DESARROLLO: cuentas de prueba (NO usar en producción)
--  Contraseña de todas: sdp12345
--  Ejecutar después de schema.sql + seed.sql
-- =====================================================================
USE sdp_ensayos;
SET NAMES utf8mb4;
UPDATE miembro SET correo='sergio.velarde@sdp.test', password_hash='$2a$10$qlJZsT3uVxdw3bpy/m4mg.oAAiybWy31aYQ7GpFjxc9ZLkx5YbcJK', debe_cambiar_password=0 WHERE nombre='Sergio Velarde';
UPDATE miembro SET correo='antony.palacios@sdp.test', password_hash='$2a$10$qlJZsT3uVxdw3bpy/m4mg.oAAiybWy31aYQ7GpFjxc9ZLkx5YbcJK', debe_cambiar_password=0 WHERE nombre='Antony Palacios';
UPDATE miembro SET correo='camila.reymundo@sdp.test', password_hash='$2a$10$qlJZsT3uVxdw3bpy/m4mg.oAAiybWy31aYQ7GpFjxc9ZLkx5YbcJK', debe_cambiar_password=0 WHERE nombre='Camila Reymundo';
UPDATE miembro SET correo='esteban.gutierrez@sdp.test', password_hash='$2a$10$qlJZsT3uVxdw3bpy/m4mg.oAAiybWy31aYQ7GpFjxc9ZLkx5YbcJK', debe_cambiar_password=0 WHERE nombre='Esteban Gutiérrez';
UPDATE miembro SET correo='pierina.perales@sdp.test', password_hash='$2a$10$qlJZsT3uVxdw3bpy/m4mg.oAAiybWy31aYQ7GpFjxc9ZLkx5YbcJK', debe_cambiar_password=0 WHERE nombre='Pierina Perales';
UPDATE miembro SET correo='claudia.alonso@sdp.test', password_hash='$2a$10$qlJZsT3uVxdw3bpy/m4mg.oAAiybWy31aYQ7GpFjxc9ZLkx5YbcJK', debe_cambiar_password=0 WHERE nombre='Claudia Alonso';

-- Un ensayo próximo publicado (sábado que viene) para probar confirmaciones y cronograma
INSERT INTO ensayo (tipo, fecha, hora_inicio, hora_fin, lugar, cierre_confirmacion, estado, creado_por)
VALUES ('GENERAL', CURDATE() + INTERVAL ((5 - WEEKDAY(CURDATE()) + 7) % 7) DAY, '14:00', '16:00', 'Explanada del polideportivo',
        TIMESTAMP(CURDATE() + INTERVAL ((5 - WEEKDAY(CURDATE()) + 7) % 7) DAY - INTERVAL 1 DAY, '23:59'), 'PROGRAMADO',
        (SELECT id_miembro FROM miembro WHERE nombre='Sergio Velarde'));
SET @e = LAST_INSERT_ID();
INSERT INTO ensayo_indicacion (id_ensayo, texto, orden) VALUES (@e,'No olvidar llegar temprano',1),(@e,'No olvidar traer su agüita',2);
INSERT INTO ensayo_dictante (id_ensayo, id_miembro) SELECT @e, id_miembro FROM miembro WHERE nombre IN ('Sergio Velarde','Antony Palacios');
INSERT INTO asistencia (id_ensayo, id_miembro) SELECT @e, id_miembro FROM miembro WHERE estado='ACTIVO';
UPDATE asistencia a JOIN miembro m ON m.id_miembro=a.id_miembro SET a.confirmacion='SI', a.fecha_confirmacion=NOW()
  WHERE a.id_ensayo=@e AND m.nombre IN ('Esteban Gutiérrez','Sergio Velarde','Antony Palacios','Gabriela Arrieta','Claudia Alonso');
