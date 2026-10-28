# Son de PUCP · Sistema de Ensayos

Aplicación web Java (Servlets + JSP + JDBC + MySQL) que reemplaza al Excel **Matriz de Figuras SDP**.

| Fase | Dónde está |
|---|---|
| 1 · Mockups | Figma: *Son de PUCP – Sistema de Ensayos (Mockups)* |
| 2 · Maqueta HTML/CSS estática | `maqueta/` (abre `maqueta/index.html`; detalles en `maqueta/README-maqueta.md`) |
| 3 · Base de datos MySQL | `db/` (ver `db/README.md`) |
| 4 · Aplicación web | `src/main/java` + `src/main/webapp` (este documento) |

---

## Cómo correrlo en IntelliJ

**Requisitos:** JDK 17 o superior, MySQL 8, **Tomcat 10.1** (Jakarta EE 10) e IntelliJ con soporte para Tomcat (Ultimate o el plugin *Smart Tomcat*).

1. **Base de datos**
   ```bash
   mysql -u root -p < db/schema.sql
   python db/migracion/generar_seed.py Matriz_Figuras_SDP.xlsx > db/seed.sql   # genera el seed desde el Excel (no está en el repo: tiene datos personales)
   mysql -u root -p < db/seed.sql
   mysql -u root -p < db/dev/usuarios_prueba.sql   # SOLO desarrollo: cuentas de prueba + un ensayo próximo
   ```
2. **Conexión:** edita `src/main/resources/db.properties` (usuario y contraseña de tu MySQL).
3. **Abrir el proyecto:** *File → Open →* la carpeta `sdp-ensayos`. IntelliJ lo detecta como proyecto Maven y baja las dependencias.
4. **Tomcat:** *Run → Edit Configurations → + → Tomcat Server → Local*. En *Deployment* agrega el artefacto `sdp-ensayos:war exploded` con contexto `/sdp-ensayos`.
5. Entra a **http://localhost:8080/sdp-ensayos**

**Cuentas de prueba** (contraseña `sdp12345`):

| Correo | Rol |
|---|---|
| `sergio.velarde@sdp.test` | Admin + Equipo de ensayos |
| `antony.palacios@sdp.test` | Equipo de ensayos |
| `camila.reymundo@sdp.test` | Miembro (sin autoevaluación todavía) |
| `esteban.gutierrez@sdp.test` | Miembro con nivel |

> **¿Tomcat 9 en vez de 10?** Tomcat 9 usa `javax.servlet`. Para cambiarlo: reemplaza `jakarta.servlet` por `javax.servlet` en todo `src/main/java`, usa `javax.servlet-api 4.0.1` y `jstl 1.2` en el `pom.xml` y cambia la cabecera del `web.xml` a la versión 4.0.

---

## Diseño mobile first

Lo van a usar sobre todo desde el celular, así que `css/sdp.css` está escrito **mobile first**: las reglas base son para celular y se va agrandando con `min-width`.

| Ancho | Cómo se ve |
|---|---|
| **< 600 px** (celular) | Barra superior con ☰ que abre el menú como cajón. **Barra de pestañas abajo** (Inicio · Asistencia · Autoevaluar · Historial · Perfil). Todo en una columna, y las tablas se vuelven tarjetas |
| **600–899 px** (tablet) | Grillas de 2 columnas y formularios en fila. Sigue con la barra superior y las pestañas |
| **900 px+** (laptop) | Menú lateral fijo, como en los mockups de Figma |
| **1100 px+** | Paneles lado a lado (columna principal más una columna lateral) |

- **Tablas → tarjetas:** `js/sdp.js` copia el título de cada columna a sus celdas (`data-label`) y el CSS las muestra como tarjetas. Con `class="cards-compact"` los tags y chips van en una sola línea (*Tomar asistencia*, *Inicio*), y con `class="no-cards"` una tabla se queda como tabla. Sin JavaScript, la tabla se desliza de costado.
- **Hecho para el dedo:** botones de 44 px mínimo y campos de 16 px de letra (así iPhone no hace zoom al escribir). En la autoevaluación los 4 colores ocupan todo el ancho, y en la asistencia los 7 códigos entran en una fila.
- La fila de la autoevaluación se acomoda según el ancho de **su tarjeta** (*container query*), no de la pantalla.
- El reporte A4 se achica para entrar en el celular y se imprime en tamaño real.
- Se probó sin desbordes horizontales a 360 px, 768 px y 1366 px, en las 23 pantallas de la maqueta y las 17 rutas de la app.

## Estructura

```
src/main/java/pe/sdp/
├── beans/      Miembro, Figura, Evaluacion, HistorialEvaluacion, Ensayo, Asistencia,
│               NivelCronograma, FiguraSugerida, Encargo, TipoEncargo, Permiso, ResumenPersona
├── daos/       BaseDao (conexión) · MiembroDao · FiguraDao · EvaluacionDao · EnsayoDao · EncargoDao · RolDao
├── servlets/   LoginServlet · MiEspacioServlet · EnsayosServlet · AdminServlet (+ BaseServlet)
├── filters/    AuthFilter (sesión obligatoria y cambio de contraseña temporal)
└── util/       Sesion (usuario, permisos, avisos) · Password (BCrypt) · Formato (fechas, colores)
src/main/webapp/
├── WEB-INF/jsp/{miembro,ensayos,admin}/*.jsp   vistas
├── WEB-INF/jsp/includes/                       top.jspf (menú según permisos) · bottom.jspf · taglibs.jspf
├── WEB-INF/tags/                               <sdp:chip> <sdp:tag> <sdp:code> <sdp:cambio> <sdp:icono>
├── css/sdp.css · js/sdp.js · img/              estilos (mobile first), tablas→tarjetas, logos
```

## Rutas

| Ruta | Quién | Qué hace |
|---|---|---|
| `/login`, `/logout`, `/cambiar-password` | todos | Acceso. Con contraseña temporal es obligatorio cambiarla |
| `/inicio` | miembro | Nivel, próximo ensayo, autoevaluaciones pendientes, figuras que faltan |
| `/confirmar-asistencia` | miembro | Sí voy / No puedo + motivo (reemplaza la encuesta de WhatsApp) |
| `/autoevaluacion?tipo=mensual` | miembro | Mensual: reemplaza colores |
| `/autoevaluacion?ensayo=ID` | miembro | Del ensayo: **solo sube** (los colores más bajos salen bloqueados) |
| `/mi-historial`, `/perfil` | miembro | Su historial; sus datos (no puede tocar rol, frecuencia ni estado) |
| `/ensayos` · `/nuevo` · `/editar` · `/detalle` · `/reporte` | equipo de ensayos | Historial del ciclo, crear/editar, detalle con notas, reporte A4 |
| `/ensayos/cronograma` · `/asistencia` · `/figuras` | equipo de ensayos | Cronograma sugerido, asistencia real, figuras enseñadas |
| `/admin/miembros[/nuevo\|/editar]` | admin | Alta con contraseña temporal, edición, inactivar/reactivar, eliminar alta por error |
| `/admin/figuras[/nueva\|/editar]` | admin | Alta, edición y orden, desactivar/reactivar, eliminar si no tiene uso, reglas de conteo |
| `/admin/responsabilidades` | admin (y equipo, según permisos) | Asignar, marcar cumplido (detecta coberturas), justificar |
| `/admin/historico` | admin | Búsqueda con filtros y paginación + correcciones |
| `/admin/roles` | admin | Tabla de permisos y personas con rol |

**Permisos:** cada servlet revisa el permiso con `Sesion.requiere(req, resp, "CODIGO")`, y el menú lateral solo muestra lo permitido. Los permisos salen de la tabla `rol_permiso`, así que cambiarlos en la pantalla *Roles y permisos* tiene efecto real: a cada persona le llegan en máximo 2 minutos, sin cerrar sesión.

## Seguridad incluida
- Contraseñas con **BCrypt**: nunca se guardan en texto plano. Mínimo 8 caracteres, y la temporal se cambia sí o sí al primer ingreso.
- Todo el SQL usa **PreparedStatement**, así que está protegido contra inyección SQL.
- El texto que escriben los usuarios se muestra con `<c:out>`, que evita XSS. Además va una cabecera **Content-Security-Policy**.
- **Tokens CSRF** en todos los formularios POST (campo oculto `_csrf`, que revisa `AuthFilter`). Otra web no puede mandar formularios con tu sesión. La cookie además va con `SameSite=Lax`.
- **Cookie de sesión** `HttpOnly`, solo por cookie (nunca `;jsessionid` en la URL), y `Secure` cuando hay HTTPS. El id de sesión **cambia al iniciar sesión** (contra *session fixation*).
- **Freno contra adivinar contraseñas:** 5 intentos fallidos con un mismo correo (o 20 desde una misma IP) bloquean el login 15 minutos.
- **HTTPS obligatorio en producción** (http → https y cabecera HSTS), más las cabeceras `X-Frame-Options`, `X-Content-Type-Options` y `Referrer-Policy`.
- **Sesión obligatoria** con el filtro `AuthFilter` y permisos por acción. Cada 2 minutos se revisa en la BD que la cuenta siga activa: si el admin inactiva a alguien, su sesión se cierra sola.
- **Pool de conexiones** (`tomcat-jdbc`) en vez de abrir una conexión por consulta.
- Los errores técnicos no se muestran al usuario: solo van al log. Las reglas de la BD (triggers) sí se muestran, porque están escritas para personas.
- Reglas críticas protegidas en la BD con triggers: no quedarse sin admin, no quitar el rol Miembro, no borrar figuras ni miembros con historial.
- En producción **las cuentas de prueba `@sdp.test` no pueden entrar**, aunque alguien las cargue por error.
- **Las claves no van en el código:** en el hosting se configuran con variables de entorno. `db/seed.sql`, los Excel y `.env` están en `.gitignore`.

---

## Probar antes de comprar hosting (gratis)

**1. En tu laptop, igual que en producción** (necesitas [Docker Desktop](https://www.docker.com/products/docker-desktop/)):
```bash
docker compose -f docker-compose.local.yml up -d --build      # la primera vez tarda unos minutos (baja Maven, Tomcat y MySQL)
docker compose -f docker-compose.local.yml exec -T db mysql -uroot -proot-local --default-character-set=utf8mb4 < db/seed.sql
docker compose -f docker-compose.local.yml exec -T db mysql -uroot -proot-local --default-character-set=utf8mb4 < db/dev/usuarios_prueba.sql   # cuentas @sdp.test
```
Entras a **http://localhost:8080** con `sergio.velarde@sdp.test` / `sdp12345`. Es la misma imagen que correría en el hosting.
En PowerShell de Windows, `<` no funciona: usa `Get-Content db/seed.sql -Encoding UTF8 | docker compose -f docker-compose.local.yml exec -T db mysql -uroot -proot-local --default-character-set=utf8mb4`.

**2. Desde tu celular:** conéctalo a la misma Wi-Fi que la laptop y entra a `http://IP-DE-TU-LAPTOP:8080`. La IP la ves con `ipconfig` (Windows) o `ifconfig` (Mac). Si no carga, dale permiso a Docker en el firewall.

**3. Un mini piloto con gente de fuera, con HTTPS:** usa un túnel gratis de Cloudflare (no pide cuenta):
```bash
cloudflared tunnel --url http://localhost:8080
```
Te da un link `https://algo-random.trycloudflare.com` que cualquiera puede abrir desde su celular **mientras tu laptop esté prendida** (el link cambia cada vez). Para eso usa el modo producción:
```bash
SDP_ENTORNO=produccion SDP_ADMIN_CORREO=tu@pucp.edu.pe SDP_ADMIN_CLAVE=una-clave-larga docker compose -f docker-compose.local.yml up -d
```
En producción las cuentas `@sdp.test` quedan bloqueadas. El link es público, así que usa una clave fuerte.

**Probar sin ensuciar los datos reales:** saca un backup antes de las pruebas y restáuralo al terminar (PowerShell):
```powershell
cmd /c "docker compose -f docker-compose.local.yml exec -T db mysqldump -uroot -pTU_ROOT --default-character-set=utf8mb4 sdp_ensayos > antes-de-pruebas.sql"
# ... pruebas ...
cmd /c "docker compose -f docker-compose.local.yml exec -T db mysql -uroot -pTU_ROOT --default-character-set=utf8mb4 sdp_ensayos < antes-de-pruebas.sql"
docker compose -f docker-compose.local.yml restart app
```
Ojo: restaurar también borra lo que otras personas hayan hecho mientras tanto.

**4. Cuando ya te convenza:** pasa a Railway (ver *Publicar*). Te dan crédito de prueba al inicio.

## Publicar (para el piloto)

El proyecto trae un **`Dockerfile`**: compila con Maven, corre en Tomcat 10.1 sin las apps de ejemplo, con un usuario sin privilegios y ya preparado para estar detrás del HTTPS del hosting. Sirve igual para Railway, Render o un VPS. La app queda en la raíz del dominio (`https://tu-dominio/login`).

### Variables de entorno

| Variable | Para qué |
|---|---|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | Conexión a MySQL, por ejemplo `jdbc:mysql://host:3306/sdp_ensayos?serverTimezone=America/Lima&useUnicode=true&characterEncoding=UTF-8` |
| `MYSQLHOST`, `MYSQLPORT`, `MYSQLUSER`, `MYSQLPASSWORD` | Alternativa: las que crea Railway. Se usan con `DB_NAME` (por defecto `sdp_ensayos`) |
| `SDP_ENTORNO` | `produccion` (el Dockerfile ya la pone): activa HTTPS obligatorio y bloquea las cuentas de prueba |
| `SDP_ADMIN_CORREO`, `SDP_ADMIN_CLAVE` | **Admin inicial.** Si ningún admin tiene contraseña (BD recién cargada), al primer admin le pone ese correo y esa clave temporal, y al entrar tiene que cambiarla. Si ya hay un admin con clave no hace nada |
| `SDP_FORZAR_HTTPS` | `false` solo si estás probando sin HTTPS. Nunca en el piloto |

### Cargar los datos (una sola vez)

> ⚠ `schema.sql` empieza con `DROP DATABASE`: **córrelo solo la primera vez**. Si lo vuelves a correr, se borra todo lo que el piloto haya registrado.

```bash
mysql -h HOST -P PUERTO -u USUARIO -p < db/schema.sql
mysql -h HOST -P PUERTO -u USUARIO -p --default-character-set=utf8mb4 < db/seed.sql
```
`seed.sql` sale del Excel con `db/migracion/generar_seed.py`, o lo pides por privado. Nunca se sube a GitHub. **No cargues** `db/dev/usuarios_prueba.sql` en producción.

Al terminar entras con `SDP_ADMIN_CORREO` y `SDP_ADMIN_CLAVE`, cambias la clave y, desde **Miembros**, le pones correo y contraseña temporal a cada persona del piloto.

### Opción A · Railway (la más simple)
1. En railway.com: *New Project → Deploy from GitHub repo →* este repo. Detecta el `Dockerfile` solo.
2. En el mismo proyecto: *+ New → Database → MySQL*.
3. En el servicio de la app, en *Variables*, agrega las referencias `MYSQLHOST`, `MYSQLPORT`, `MYSQLUSER` y `MYSQLPASSWORD` de la base, más `SDP_ADMIN_CORREO` y `SDP_ADMIN_CLAVE`.
4. *Settings → Networking → Generate Domain*: te da `https://algo.up.railway.app`. Para el *healthcheck* usa la ruta `/salud`.
5. Carga los datos con los datos de conexión públicos que muestra la base (pestaña *Connect*).

### Opción B · Render
*New → Web Service →* el repo (runtime Docker), con health check en `/salud` y las variables `DB_URL`, `DB_USER` y `DB_PASSWORD` de un MySQL externo (Render no ofrece MySQL propio). En el plan gratis la app "se duerme" y tarda en despertar.

### Opción C · VPS propio (DigitalOcean, Hetzner…)
Necesitas un dominio que apunte a la IP del servidor.
```bash
git clone <repo> && cd sondepucp_ensayos
cp .env.ejemplo .env        # llena dominio y claves
docker compose up -d --build
docker compose exec -T db mysql -uroot -p"$MYSQL_ROOT_PASSWORD" < db/schema.sql
docker compose exec -T db mysql -uroot -p"$MYSQL_ROOT_PASSWORD" --default-character-set=utf8mb4 < db/seed.sql
docker compose restart app  # para que active el admin inicial
```
`docker-compose.yml` levanta MySQL (sin exponerlo a internet), la app y **Caddy**, que saca el certificado HTTPS gratis.

### Antes de abrir el piloto
- [ ] Entra con el admin inicial y cambia la clave.
- [ ] Confirma que `http://` te manda a `https://`.
- [ ] Verifica que ninguna cuenta `@sdp.test` exista (`SELECT * FROM miembro WHERE correo LIKE '%@sdp.test'`).
- [ ] Activa los **backups** de la BD que ofrece el hosting, o programa un `mysqldump` diario.
- [ ] Deja un segundo admin (la BD no deja quitar al último, pero si uno pierde su clave, el otro se la resetea).

## Probado
Se probó de punta a punta en Tomcat 10.1 + MariaDB, con los datos reales migrados y clics en el navegador:
- Alta de miembro → primer ingreso → cambio de contraseña obligatorio.
- Autoevaluación mensual → confirmación de asistencia → cronograma → toma de asistencia → figuras enseñadas → autoevaluación del ensayo con "solo sube".
- Alta y eliminación de una figura nueva, desactivar y reactivar una figura con historial.
- Las protecciones: no quitar al único admin, un miembro inactivo no puede entrar y un miembro común no entra a Administración.
- Responsabilidades con cobertura, corrección en el histórico y reporte imprimible.
- Seguridad: un POST sin token CSRF se rechaza, el bloqueo llega en el 6.º intento fallido, la sesión de una cuenta inactivada se cierra sola y las cuentas `@sdp.test` no entran en modo producción.
- Modo producción detrás de un proxy HTTPS (con la misma configuración del `Dockerfile`): http → https, cookie `Secure` y HSTS, y el admin inicial desde variables de entorno con cambio de clave obligatorio.
