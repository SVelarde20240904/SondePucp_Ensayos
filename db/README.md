# Base de datos · `sdp_ensayos` (Fase 3)

MySQL 8.0+ (probado también en MariaDB 10.11). Reemplaza al Excel **Matriz de Figuras SDP v2**.

## Archivos
| Archivo | Qué es |
|---|---|
| `schema.sql` | Crea la BD, las 17 tablas, las 8 vistas y los 5 triggers |
| `seed.sql` | Datos reales migrados del Excel (se genera con el script de abajo) |
| `consultas.sql` | Consultas clave para los DAOs: cronograma sugerido, regla "solo sube", historial de asistencia |
| `pruebas/prueba_admin.sql` | Altas y bajas de miembros y figuras (no deja cambios: termina en ROLLBACK) |
| `pruebas/prueba_guia.sql` | El ejemplo de Ana, Beto, Carla, Diego y Elena de la guía, con los resultados esperados |
| `migracion/generar_seed.py` | Lee el Excel y genera `seed.sql` (`pip install openpyxl`) |
| `modelo_er.png` / `.svg` / `.mmd` | Diagrama E/R |

## Cómo cargarla
```bash
mysql -u root -p < schema.sql
mysql -u root -p < seed.sql
# para regenerar el seed con un Excel más nuevo:
python migracion/generar_seed.py Matriz_Figuras_SDP.xlsx > seed.sql
```

## Tablas
**Miembros y acceso**
- `miembro`: datos personales (los de la convocatoria), login (`correo` + `password_hash`), `rol_baile` (LEADER/FOLLOWER), `frecuencia`, `estado` (nunca se borra: se pone INACTIVO) y las preferencias de avisos.
- `rol_sistema`, `miembro_rol`: MIEMBRO / EQUIPO_ENSAYOS / ADMIN. Una persona puede tener varios roles.
- `permiso`, `rol_permiso`: la tabla de la pantalla 19.
- `regla_conteo_color`: qué frecuencias cuentan para el color (antes la tablita verde y la celda K7).

**Figuras**
- `figura`: la mano de figuras (`activa=0` para dar de baja).
- `evaluacion`: el color **actual** de cada miembro en cada figura (lo que era la MATRIZ).
- `historial_evaluacion`: cada cambio, con `color_anterior → color_nuevo`, su fuente (CARGA_INICIAL, MENSUAL, ENSAYO o CORRECCION), quién lo registró y si se aplicó (`aplicado=0` = ignorado por la regla "solo sube").
- `periodo_autoevaluacion`: las ventanas de la autoevaluación mensual.

**Ensayos**
- `ensayo`: tipo (GENERAL, COMPETENCIA, NIVELACION, EXHIBICION), fecha, horario, lugar, cierre de confirmación, estado y notas.
- `ensayo_indicacion`, `ensayo_dictante`: indicaciones del flyer y quiénes dictan.
- `asistencia`: `confirmacion` (SI/NO/SIN_RESPUESTA, que reemplaza la encuesta) + `estado_real` (AP, AN, TJ, TI, FJ, FI).
- `ensayo_figura`: figuras enseñadas (habilitan la autoevaluación del ensayo).

**Responsabilidades**
- `tipo_encargo` (`activo=0` en los que automatizó la web) y `encargo` con `responsable`, `plazo`, `cumplido_por`, `fecha_cumplimiento`, `justificada` y `exonerado`.

## Vistas (reemplazan las fórmulas de `_CALC`)
| Vista | Calcula |
|---|---|
| `v_miembro_flags` | Si es del equipo de ensayos / admin |
| `v_miembro_cuenta_color` | Quién entra en la mediana: activo + frecuencia que cuenta + (no es del equipo o el equipo cuenta) |
| `v_color_figura` | `INT(MEDIANA)` con ROJO=1 … AZUL=4, igual que el Excel |
| `v_nivel_miembro` | Primer nivel (AZUL → VERDE → AMARILLO) donde le falta una figura (ROJO o AMARILLO); si sabe todo lo anterior → ROJO; sin evaluaciones → NULL |
| `v_encargo_estado` | PENDIENTE / A_TIEMPO / FUERA_DE_PLAZO / NO_CUMPLIDA / EXONERADO + `es_cobertura` |
| `v_ensayo_resumen` | Confirmados, sin respuesta, asistieron, puntuales, tardanzas, faltas, figuras y avances por ensayo |

## Altas y bajas (solo admin)
| Acción | Cómo lo resuelve la BD |
|---|---|
| **Nuevo miembro** | `INSERT` en `miembro` (con `creado_por`). El trigger `trg_miembro_ai` le da el rol MIEMBRO. Entra con una contraseña temporal (`debe_cambiar_password = 1`). Queda sin nivel hasta su primera autoevaluación. |
| **Inactivar / reactivar miembro** | `estado = 'INACTIVO'`. El trigger guarda o limpia `fecha_baja`. Deja de contar para el color y de salir en las listas, pero conserva su historial. |
| **Eliminar miembro** | Solo un alta hecha por error: `v_miembro_admin.puede_eliminar = 1`. Si tiene datos, las FK lo impiden. |
| **Nueva figura** | `INSERT` sin orden: el trigger `trg_figura_bi` la pone al final. Queda sin color hasta que la evalúen, así que no cambia ningún nivel. Si el nombre ya existía, `ON DUPLICATE KEY UPDATE activa = 1` la reactiva. |
| **Desactivar / reactivar figura** | `activa = 0`: sale de la mano, de las autoevaluaciones y de las vistas de cálculo, y conserva su historial. |
| **Eliminar figura** | Solo si nunca se evaluó ni se enseñó (`v_figura_admin.puede_eliminar = 1`). Si no, las FK lo impiden. |
| **Quitar roles** | El trigger `trg_miembro_rol_bd` impide quitar el rol MIEMBRO y quedarse sin ningún admin. |

Las sentencias para cada caso están en `consultas.sql` (secciones E a N) y `pruebas/prueba_admin.sql` las prueba todas dentro de un ROLLBACK.

## Validación
- Con los datos migrados, `v_color_figura` y `v_nivel_miembro` dan **exactamente** los mismos colores (37 figuras) y niveles (58 miembros) que calcula el Excel.
- `pruebas/prueba_guia.sql` reproduce el flujo feliz de la guía, incluido el paso 5 (Ana y Beto saltan a AMARILLO, Diego sigue en VERDE).
- El resumen por nivel de `consultas.sql` para el ensayo 1 da lo mismo que la hoja CRONOGRAMA SUGERIDO: solo se abre AMARILLO, con Esteban y una follower de ensayos.

## Qué se migró
58 miembros · 11 del equipo de ensayos · 1 admin (Sergio) · 37 figuras · 218 evaluaciones y su histórico (autoevaluación de octubre) · 13 ensayos (abril–junio) con 490 registros de asistencia · 33 encargos históricos.

## Decisiones y supuestos (revísalos)
1. **La "x" de ASISTENCIA** se cargó como `SIN_RESPUESTA` (no marcó la encuesta), como me dijiste. La leyenda del Excel dice "Marcó que no venía al ensayo". Si es eso, corrígelo con:
   `UPDATE asistencia SET confirmacion='NO' WHERE confirmacion='SIN_RESPUESTA' AND id_ensayo BETWEEN 1 AND 13;`
2. Se agregó el código **AN** (asistencia no planeada: marcó que no, pero vino), que estaba en el Excel. Para la migración, AP/TJ/TI/FJ/FI se cargaron como `confirmacion='SI'` y AN como `'NO'`.
3. Los ensayos históricos quedaron como tipo GENERAL: los sábados 2:00–4:00 PM y los otros días 8:00–10:00 PM. El del 06/06 no tiene asistencias registradas en el Excel.
4. En los encargos históricos, el plazo es desconocido (NULL). Si se cumplió cuenta como A_TIEMPO; si no hay fecha de cumplimiento cuenta como NO_CUMPLIDA. "Cubrió X" se cargó como `cumplido_por = X`. Cuando había dos responsables ("Deyvid + Mafer"), se tomó el primero.
5. Una figura **sin evaluar** no cuenta como "le falta" (igual que el Excel). Solo ROJO o AMARILLO cuentan como que le falta.
6. Los correos, códigos, celulares y contraseñas quedan vacíos. Se llenan cuando cada miembro activa su cuenta o los completa en "Mi perfil".
