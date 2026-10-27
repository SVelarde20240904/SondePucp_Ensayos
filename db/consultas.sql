-- =====================================================================
--  Consultas clave para los DAOs (Fase 4). Cambia @ensayo por el id.
-- =====================================================================
USE sdp_ensayos;
SET @ensayo = 1;

-- ---------------------------------------------------------------
-- A) CRONOGRAMA SUGERIDO · Resumen por nivel (pantalla 12)
--    Se abre con 1 leader + 1 follower del nivel, o con un solo rol
--    + alguien del equipo de ensayos del rol contrario que asista.
-- ---------------------------------------------------------------
WITH asisten AS (
  SELECT f.id_miembro, f.nombre, f.rol_baile, f.es_equipo_ensayos, n.nivel
  FROM asistencia a
  JOIN v_miembro_flags f ON f.id_miembro = a.id_miembro AND f.estado = 'ACTIVO'
  LEFT JOIN v_nivel_miembro n ON n.id_miembro = a.id_miembro
  WHERE a.id_ensayo = @ensayo AND a.confirmacion = 'SI'
),
equipo AS (
  SELECT SUM(rol_baile = 'LEADER') AS eq_l, SUM(rol_baile = 'FOLLOWER') AS eq_f
  FROM asisten WHERE es_equipo_ensayos = 1
),
niveles AS (
  SELECT 'AZUL' AS nivel, 1 AS ord UNION ALL SELECT 'VERDE', 2 UNION ALL SELECT 'AMARILLO', 3 UNION ALL SELECT 'ROJO', 4
)
SELECT nv.nivel,
       COUNT(CASE WHEN a.rol_baile = 'LEADER' THEN 1 END)   AS leaders,
       COUNT(CASE WHEN a.rol_baile = 'FOLLOWER' THEN 1 END) AS followers,
       GROUP_CONCAT(a.nombre ORDER BY a.nombre SEPARATOR ', ') AS nombres,
       CASE
         WHEN COUNT(a.id_miembro) = 0 THEN 'NO'
         WHEN COUNT(CASE WHEN a.rol_baile='LEADER' THEN 1 END) > 0 AND COUNT(CASE WHEN a.rol_baile='FOLLOWER' THEN 1 END) > 0 THEN 'SI'
         WHEN COUNT(CASE WHEN a.rol_baile='LEADER' THEN 1 END) > 0 AND MAX(e.eq_f) > 0 THEN 'SI'
         WHEN COUNT(CASE WHEN a.rol_baile='FOLLOWER' THEN 1 END) > 0 AND MAX(e.eq_l) > 0 THEN 'SI'
         ELSE 'NO' END AS se_abre,
       CASE
         WHEN COUNT(a.id_miembro) = 0 THEN 'Sin asistentes'
         WHEN COUNT(CASE WHEN a.rol_baile='LEADER' THEN 1 END) > 0 AND COUNT(CASE WHEN a.rol_baile='FOLLOWER' THEN 1 END) > 0 THEN 'Completa'
         WHEN COUNT(CASE WHEN a.rol_baile='LEADER' THEN 1 END) > 0 THEN IF(MAX(e.eq_f) > 0, 'Con ensayos (follower)', 'Falta follower de ensayos')
         ELSE IF(MAX(e.eq_l) > 0, 'Con ensayos (leader)', 'Falta leader de ensayos') END AS pareja
FROM niveles nv
CROSS JOIN equipo e
LEFT JOIN asisten a ON a.nivel = nv.nivel AND a.es_equipo_ensayos = 0
GROUP BY nv.nivel, nv.ord
ORDER BY nv.ord;

-- ---------------------------------------------------------------
-- B) CRONOGRAMA SUGERIDO · Figuras sugeridas (solo niveles abiertos)
--    En Java conviene filtrar con el resultado de (A); aquí se muestran
--    todas las figuras del nivel de cada asistente que le faltan.
-- ---------------------------------------------------------------
SELECT cf.nombre AS figura, cf.color AS nivel,
       GROUP_CONCAT(m.nombre ORDER BY m.nombre SEPARATOR ', ') AS les_falta_a,
       COUNT(*) AS n_asistentes
FROM asistencia a
JOIN v_miembro_flags m ON m.id_miembro = a.id_miembro AND m.estado = 'ACTIVO' AND m.es_equipo_ensayos = 0
JOIN v_nivel_miembro n ON n.id_miembro = a.id_miembro
JOIN v_color_figura cf ON cf.color = n.nivel
JOIN evaluacion e ON e.id_miembro = a.id_miembro AND e.id_figura = cf.id_figura AND e.color IN ('ROJO','AMARILLO')
WHERE a.id_ensayo = @ensayo AND a.confirmacion = 'SI'
GROUP BY cf.id_figura, cf.nombre, cf.color
ORDER BY n_asistentes DESC, cf.orden;

-- ---------------------------------------------------------------
-- C) AUTOEVALUACIÓN DEL ENSAYO · regla "solo sube" (en una transacción)
--    Parámetros: @m (miembro), @f (figura), @nuevo (color), @ensayo
-- ---------------------------------------------------------------
-- SET @m = 3, @f = 1, @nuevo = 'VERDE';
-- SELECT color INTO @actual FROM evaluacion WHERE id_miembro=@m AND id_figura=@f FOR UPDATE;
-- INSERT INTO historial_evaluacion (id_miembro,id_figura,color_anterior,color_nuevo,fuente,aplicado,id_ensayo,registrado_por)
--   VALUES (@m,@f,@actual,@nuevo,'ENSAYO', (@actual IS NULL OR FIELD(@nuevo,'ROJO','AMARILLO','VERDE','AZUL') > FIELD(@actual,'ROJO','AMARILLO','VERDE','AZUL')), @ensayo, @m);
-- INSERT INTO evaluacion (id_miembro,id_figura,color) VALUES (@m,@f,@nuevo)
--   ON DUPLICATE KEY UPDATE color = IF(FIELD(VALUES(color),'ROJO','AMARILLO','VERDE','AZUL') > FIELD(color,'ROJO','AMARILLO','VERDE','AZUL'), VALUES(color), color),
--                           fecha_actualizacion = IF(FIELD(VALUES(color),'ROJO','AMARILLO','VERDE','AZUL') > FIELD(color,'ROJO','AMARILLO','VERDE','AZUL'), NOW(), fecha_actualizacion);
-- (La mensual es igual pero SIEMPRE reemplaza: ON DUPLICATE KEY UPDATE color = VALUES(color))

-- ---------------------------------------------------------------
-- D) HISTORIAL DE ASISTENCIA (página 2 del reporte): últimos 6 ensayos
-- ---------------------------------------------------------------
SELECT m.nombre, en.fecha, a.confirmacion, a.estado_real
FROM asistencia a
JOIN miembro m ON m.id_miembro = a.id_miembro
JOIN (SELECT id_ensayo, fecha FROM ensayo WHERE fecha <= (SELECT fecha FROM ensayo WHERE id_ensayo = @ensayo)
      ORDER BY fecha DESC LIMIT 6) en ON en.id_ensayo = a.id_ensayo
ORDER BY m.nombre, en.fecha;

-- =====================================================================
--  OPERACIONES DE ADMIN (pantallas 15 y 16) — solo con permiso
--  GESTIONAR_MIEMBROS / GESTIONAR_FIGURAS (rol ADMIN)
-- =====================================================================
SET @admin = 55;   -- id del admin que hace el cambio (sale de la sesión)

-- E) Alta de miembro. El trigger le pone el rol MIEMBRO. El hash lo genera el Java (BCrypt).
-- INSERT INTO miembro (nombre, nombre_completo, codigo_pucp, correo, celular, facultad,
--                      rol_baile, frecuencia, fecha_ingreso, password_hash, debe_cambiar_password, creado_por)
-- VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURDATE(), ?, 1, @admin);
-- Roles extra (opcional):  INSERT INTO miembro_rol (id_miembro, id_rol, asignado_por) VALUES (?, 2, @admin);

-- F) Editar miembro (el admin puede cambiar rol de baile y frecuencia; el miembro no)
-- UPDATE miembro SET nombre=?, nombre_completo=?, codigo_pucp=?, correo=?, celular=?, facultad=?,
--                    rol_baile=?, frecuencia=? WHERE id_miembro=?;

-- G) Inactivar / reactivar miembro (el trigger pone o borra fecha_baja)
-- UPDATE miembro SET estado='INACTIVO', baja_por=@admin, motivo_baja=? WHERE id_miembro=?;
-- UPDATE miembro SET estado='ACTIVO' WHERE id_miembro=?;

-- H) Eliminar miembro: SOLO si fue un alta por error (sin asistencias, evaluaciones ni encargos)
-- DELETE FROM miembro WHERE id_miembro=? AND id_miembro IN (SELECT id_miembro FROM v_miembro_admin WHERE puede_eliminar=1);
-- (si filas afectadas = 0 -> mostrar "tiene historial: inactívalo")

-- I) Nueva figura (va al final si no se manda orden). Si el nombre ya existía y estaba inactiva, se reactiva.
-- INSERT INTO figura (nombre, descripcion, creado_por) VALUES (?, ?, @admin)
--   ON DUPLICATE KEY UPDATE activa = 1, descripcion = VALUES(descripcion);

-- J) Renombrar / reordenar figura
-- UPDATE figura SET nombre=?, descripcion=? WHERE id_figura=?;
-- UPDATE figura SET orden=? WHERE id_figura=?;      -- en Java: en una transacción para todo el arrastre

-- K) Desactivar / reactivar figura (sale de la mano, de las autoevaluaciones y de los cálculos)
-- UPDATE figura SET activa=0, baja_por=@admin WHERE id_figura=?;
-- UPDATE figura SET activa=1 WHERE id_figura=?;

-- L) Eliminar figura: SOLO si nunca se evaluó ni se enseñó
-- DELETE FROM figura WHERE id_figura=? AND id_figura IN (SELECT id_figura FROM v_figura_admin WHERE puede_eliminar=1);

-- M) Listados del panel de admin
SELECT id_figura, nombre, orden, activa, color, n_evaluaciones, n_ensenada, puede_eliminar FROM v_figura_admin ORDER BY activa DESC, orden;
SELECT id_miembro, nombre, rol_baile, frecuencia, estado, nivel, es_equipo_ensayos, es_admin, cuenta_activada FROM v_miembro_admin ORDER BY estado, nombre LIMIT 5;

-- N) Mano de figuras para autoevaluar: SOLO activas
-- SELECT id_figura, nombre FROM figura WHERE activa = 1 ORDER BY orden;
