# Pendientes y feedback

Lista viva de mejoras. Lo que se termina pasa a **Hecho** con su commit.

## Feedback del piloto
- [ ] **El túnel se cae al cambiar de red o suspender la laptop** y el link cambia. Para el piloto: dejar la laptop fija con internet estable, usar `--protocol http2`, o pasar a link fijo (Cloudflare con dominio, o Railway).

## Ideas
- [ ] **Link fijo** para el túnel de Cloudflare (requiere cuenta de Cloudflare y un dominio), o pasar a Railway. **Además Chrome marca los links `trycloudflare.com` como "sitio peligroso"** (dominio usado para phishing): con dominio propio desaparece el aviso.

## Hecho
- [x] **Roles nuevos:** *Owner* (antes Admin), *Admin de ensayos* y *Moderador de ensayos* (hoy con los mismos permisos que el admin, pero rol aparte). El Equipo de ensayos ya no ve el panel de todas las responsabilidades. *Roles y permisos* muestra una columna por rol y se puede asignar cualquiera. Solo el Owner cambia roles (también desde el formulario del miembro). `56fb0fe`
- [x] **Mis responsabilidades** (*Mi espacio*): cada uno ve sus encargos, los que cubrió y su estado; aviso en Inicio si tiene pendientes. `56fb0fe`
- [x] **Eliminar ensayos** y **editar figuras desde la ficha**: solo admin y moderador de ensayos (y Owner). En la ficha, "✏️ Editar figuras" muestra un selector por figura; lo cambiado queda en el histórico como corrección, con quién lo hizo y el motivo. `56fb0fe`
- [x] **Teams** (Ensayos, Talleres, Logística, Finanzas, RRPP, Comunica): se puede estar en varios; los asigna quien gestiona miembros; se ven en listas, fichas y Mi perfil, con filtro por team. Quien tenía el rol Equipo de ensayos quedó en el team Ensayos. `56fb0fe`
- [x] **Apodos:** cada uno lo pone en *Mi perfil*; se ve como "Enmanuel Vega (Ilo)" en listas, fichas y asistencia, el saludo de Inicio lo usa, y el buscador encuentra por apodo. `56fb0fe`
- [x] **Fecha de nacimiento y cumpleaños:** en *Mi perfil* y en el formulario del admin. Inicio muestra los cumpleaños de los próximos 7 días (solo día y mes, nunca el año) y al cumpleañero lo saluda. `56fb0fe`
- [x] **Respuestas que siguen llegando al Excel:** `db/migracion/importar_respuestas_excel.py` genera un SQL que solo agrega lo nuevo del HISTORICO (se puede correr con cada Excel). `56fb0fe`
- [x] **Biblioteca de figuras** (*Mi espacio → Biblioteca de figuras*): todas las figuras por nivel con tu color, buscador y "solo con video"; cada figura con posición en la mano, descripción y video de Drive/YouTube embebido. "▶ ver" en la autoevaluación y en las fichas. Videos en **Drive restringido al Grupo de Google de SDP** (aviso en el formulario), y recordatorio de agregar/sacar del grupo al dar de alta o de baja. `74169d4`
- [x] **Activar cuentas en lote** (*Miembros → Activar cuentas en lote*): escribes correos, se generan todas las claves, con un mensaje de WhatsApp por persona (con el link) y la lista de correos para el Grupo de Google. `74169d4`
- [x] Permisos: respaldo de revisión cada 20 s (los cambios hechos desde la web ya llegaban al instante). `74169d4`
- [x] La BD se actualiza sola al arrancar la app (migraciones automáticas), sin correr scripts. `74169d4`
- [x] Confirmado: la **"x"** del Excel = no respondió la encuesta (SIN_RESPUESTA), como ya está en la BD.
- [x] El código **AN** se queda como está (en la app; no hace falta en la maqueta).

- [x] Los cambios de rol/permisos/estado llegan al instante (siguiente clic), sin volver a iniciar sesión. `4ded3b3`
- [x] **Bug:** las conexiones del pool podían quedar con una transacción abierta después de guardar una autoevaluación, y lo siguiente no se confirmaba en la BD (se perdía al reiniciar). Ahora cada conexión sale del pool en autocommit. `82cda2c`
- [x] Corregir en el histórico con el mismo color que ya tiene avisa que no hay nada que corregir. `82cda2c`
- [x] Guardar y generar contraseña temporal: guarda todo el formulario; si hay un error, el formulario vuelve con lo escrito. `a38eabd`
- [x] Histórico y *Mi historial* ya no muestran los "Azul → Azul" (se siguen guardando por dentro para saber quién ya se autoevaluó). `a38eabd`
- [x] *Ensayos → Fichas de miembros*: lista con filtros y ficha de cada persona (figuras por nivel con su color, "solo las que le faltan", últimos ensayos y cambios recientes). Desde *Tomar asistencia* el nombre abre su ficha. `a38eabd`
- [x] Eliminar ensayo (solo admin): borra todo lo del ensayo y regresa los colores que subió su autoevaluación. `a38eabd`
- [x] El aviso de autoevaluación se activa al cerrar el ensayo, y el detalle dice qué falta para habilitarlo. `a38eabd`
- [x] Pestañas del ensayo (Resumen · Cronograma · Asistencia · Figuras) en sus 4 pantallas, con "← Volver al ensayo". `a38eabd`
- [x] La clave temporal se muestra en la tarjeta *Acceso al sistema*, con el mensaje para WhatsApp, y no solo en el aviso verde de arriba. `070f4b1`
- [x] *Generar contraseña temporal* guarda el correo y valida que no esté repetido. `070f4b1`
- [x] Claves temporales más seguras (`SDP-XXXX-XXXX`). `070f4b1`
- [x] Diseño mobile first. `3999bff`
- [x] Blindaje de seguridad y despliegue con Docker. `f465e8c`

## Visión a futuro (hacia dónde va el proyecto)
Pasar de "Sistema de Ensayos" a **la página de SDP**:
- Cada miembro puede estar en uno o varios teams (Ensayos, Talleres, Logística, Finanzas, RRPP, Comunica).
- Vistas personalizadas según los teams de cada uno (su Inicio muestra lo de sus teams).
- Roles **por team**: miembro, moderador y administrador del team, cada uno con interfaces propias (no un admin global para todo).
- Ensayos pasa a ser un módulo más, al lado de los de los otros teams.
- Implica: tabla `team`, `miembro_team(id_miembro, id_team, rol_en_team)`, permisos evaluados por team, y menú armado según los teams del usuario. Las decisiones de esta tanda (teams, apodos, etc.) se diseñan pensando en esto.

## Descartado
- Pantallas de celular en Figma: la app ya es mobile first y se probó en celular, no hacen falta mockups.
- Logos en Figma: los logos ya están en el proyecto (`img/`), no hace falta subirlos a Figma.
