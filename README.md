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
├── css/sdp.css · img/                          estilos y logos
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

**Permisos:** cada servlet revisa el permiso con `Sesion.requiere(req, resp, "CODIGO")`, y el menú lateral solo muestra lo permitido. Los permisos salen de la tabla `rol_permiso`, así que cambiarlos en la pantalla *Roles y permisos* tiene efecto real desde el siguiente inicio de sesión.

## Seguridad incluida
- Contraseñas con **BCrypt**: nunca se guardan en texto plano.
- Todo el SQL usa **PreparedStatement**, así que está protegido contra inyección SQL.
- El texto que escriben los usuarios se muestra con `<c:out>`, que evita XSS.
- **Sesión obligatoria** con el filtro `AuthFilter`, más permisos por acción.
- Reglas críticas protegidas en la BD con triggers: no quedarse sin admin, no quitar el rol Miembro, no borrar figuras ni miembros con historial.

**Pendiente para producción:** tokens CSRF en los formularios, HTTPS y un pool de conexiones (DataSource de Tomcat) en lugar de `DriverManager`.

## Probado
Se probó de punta a punta en Tomcat 10.1 + MariaDB, con los datos reales migrados y clics en el navegador:
- Alta de miembro → primer ingreso → cambio de contraseña obligatorio.
- Autoevaluación mensual → confirmación de asistencia → cronograma → toma de asistencia → figuras enseñadas → autoevaluación del ensayo con "solo sube".
- Alta y eliminación de una figura nueva, desactivar y reactivar una figura con historial.
- Las protecciones: no quitar al único admin, un miembro inactivo no puede entrar y un miembro común no entra a Administración.
- Responsabilidades con cobertura, corrección en el histórico y reporte imprimible.
