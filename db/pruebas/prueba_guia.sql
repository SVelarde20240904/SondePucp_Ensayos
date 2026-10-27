-- =====================================================================
--  Prueba con el ejemplo de la "Guía de flujos" (sección 3, flujo feliz)
--  Ejecutar sobre una BD VACÍA: primero schema.sql, luego este archivo
--  (NO después de seed.sql, porque los datos reales cambian las medianas)
-- =====================================================================
USE sdp_ensayos;
SET NAMES utf8mb4;
INSERT INTO rol_sistema VALUES (1,'MIEMBRO','Miembro'),(2,'EQUIPO_ENSAYOS','Equipo de ensayos'),(3,'ADMIN','Admin');
INSERT INTO regla_conteo_color VALUES ('REGULAR',1),('OCASIONAL',1),('EVENTUAL',0),('EQUIPO_ENSAYOS',0);
INSERT INTO miembro (id_miembro, nombre, nombre_completo, rol_baile, frecuencia) VALUES
 (1,'Ana','Ana','FOLLOWER','REGULAR'),(2,'Beto','Beto','LEADER','REGULAR'),(3,'Carla','Carla','FOLLOWER','REGULAR'),
 (4,'Diego','Diego','LEADER','REGULAR'),(5,'Elena','Elena','FOLLOWER','REGULAR'),(6,'Sergio','Sergio','LEADER','REGULAR');
INSERT INTO miembro_rol VALUES (6,2,NULL,NOW());   -- Sergio es del equipo de ensayos: no cuenta para el color
INSERT INTO figura (id_figura, nombre, orden) VALUES (1,'Figura A',1),(2,'Figura B',2),(3,'Figura C',3),(4,'Figura D',4),(5,'Figura E',5),(6,'Figura F',6);
INSERT INTO evaluacion (id_miembro, id_figura, color) VALUES
 (1,1,'ROJO'),(1,2,'AZUL'),(1,3,'VERDE'),(1,4,'VERDE'),(1,5,'ROJO'),(1,6,'ROJO'),
 (2,1,'AMARILLO'),(2,2,'AZUL'),(2,3,'VERDE'),(2,4,'VERDE'),(2,5,'ROJO'),(2,6,'ROJO'),
 (3,1,'AZUL'),(3,2,'VERDE'),(3,3,'AMARILLO'),(3,4,'AZUL'),(3,5,'AMARILLO'),(3,6,'ROJO'),
 (4,1,'AZUL'),(4,2,'AZUL'),(4,3,'VERDE'),(4,4,'ROJO'),(4,5,'AMARILLO'),(4,6,'ROJO'),
 (5,1,'AZUL'),(5,2,'AZUL'),(5,3,'AZUL'),(5,4,'VERDE'),(5,5,'AMARILLO'),(5,6,'ROJO'),
 (6,1,'ROJO'),(6,2,'ROJO'),(6,3,'ROJO'),(6,4,'ROJO'),(6,5,'ROJO'),(6,6,'ROJO');  -- no debe mover las medianas

-- Esperado: A=AZUL, B=AZUL, C=VERDE, D=VERDE, E=AMARILLO, F=ROJO
SELECT nombre, color,
       CASE nombre WHEN 'Figura A' THEN 'AZUL' WHEN 'Figura B' THEN 'AZUL' WHEN 'Figura C' THEN 'VERDE'
                   WHEN 'Figura D' THEN 'VERDE' WHEN 'Figura E' THEN 'AMARILLO' ELSE 'ROJO' END AS esperado
FROM v_color_figura ORDER BY orden;

-- Esperado: Ana=AZUL, Beto=AZUL, Carla=VERDE, Diego=VERDE, Elena=AMARILLO
SELECT nombre, nivel,
       CASE nombre WHEN 'Ana' THEN 'AZUL' WHEN 'Beto' THEN 'AZUL' WHEN 'Carla' THEN 'VERDE'
                   WHEN 'Diego' THEN 'VERDE' WHEN 'Elena' THEN 'AMARILLO' ELSE '(equipo)' END AS esperado
FROM v_nivel_miembro WHERE nombre <> 'Sergio';

-- Paso 5 de la guía: Ana y Beto aprenden A (VERDE) -> deben saltar a AMARILLO; Carla aprende C -> AMARILLO
UPDATE evaluacion SET color='VERDE' WHERE (id_miembro IN (1,2) AND id_figura=1) OR (id_miembro=3 AND id_figura=3);
UPDATE evaluacion SET color='AMARILLO' WHERE id_miembro=4 AND id_figura=4;   -- Diego sigue en VERDE
SELECT nombre, nivel,
       CASE nombre WHEN 'Ana' THEN 'AMARILLO' WHEN 'Beto' THEN 'AMARILLO' WHEN 'Carla' THEN 'AMARILLO'
                   WHEN 'Diego' THEN 'VERDE' WHEN 'Elena' THEN 'AMARILLO' ELSE '(equipo)' END AS esperado_despues
FROM v_nivel_miembro WHERE nombre <> 'Sergio';
