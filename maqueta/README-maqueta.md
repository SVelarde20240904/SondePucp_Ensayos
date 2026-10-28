# Son de PUCP · Sistema de Ensayos

- **Fase 2 · Maqueta HTML/CSS** → `maqueta/` (este documento)
- **Fase 3 · Base de datos MySQL** → `db/` (ver `db/README.md`)

## Maqueta HTML/CSS (Fase 2)

Maqueta estática de las 19 pantallas del Figma, lista para convertir a JSP en la Fase 4.
La carpeta sigue la estructura de un webapp Maven/Tomcat (`src/main/webapp`), así que puedes copiarla tal cual a tu proyecto de IntelliJ.

## Cómo verla
Abre `maqueta/index.html` en el navegador (el botón **Ingresar** te lleva a Inicio). Todas las páginas están enlazadas desde el menú lateral.
Las fuentes (Poppins y League Spartan) se cargan de Google Fonts, así que se necesita internet para verlas bien.

## Estructura
```
maqueta/
├── css/sdp.css          ← única hoja de estilos (colores, fuentes y componentes)
├── img/                 ← logo dorado y logo blanco (fondo transparente)
├── index.html           01 · Login
├── miembro/
│   ├── inicio.html                 02
│   ├── confirmar-asistencia.html   03
│   ├── autoevaluacion-mensual.html 04
│   ├── autoevaluacion-ensayo.html  05
│   ├── mi-historial.html           06
│   └── mi-perfil.html              07
├── ensayos/
│   ├── historial.html              08
│   ├── nuevo.html                  09
│   ├── detalle.html                10
│   ├── reporte.html                11 (A4, 2 páginas, botón Imprimir/PDF)
│   ├── cronograma.html             12
│   ├── asistencia.html             13
│   └── figuras-ensenadas.html      14
└── admin/
    ├── miembros.html               15
    ├── miembro-form.html           15b nuevo miembro
    ├── miembro-editar.html         15c editar / inactivar
    ├── figuras.html                16 (mano de figuras + inactivas)
    ├── figura-form.html            16b nueva figura
    ├── figura-editar.html          16c editar / desactivar / eliminar
    ├── responsabilidades.html      17
    ├── historico.html              18
    └── roles.html                  19
```

## Piezas que se repiten (futuros `<jsp:include>`)
| Bloque | Clase CSS | En JSP |
|---|---|---|
| Menú lateral | `.sidebar` | `WEB-INF/includes/sidebar.jsp` (muestra secciones según el rol en sesión) |
| Encabezado azul | `.hero` | parte de cada JSP |
| Barra Team Ensayos | `.team-bar` | `WEB-INF/includes/footer.jsp` |

## Formularios → Servlets sugeridos
Los formularios ya usan `method="post"` y `name` en cada campo, para leerlos con `request.getParameter(...)`:

| Pantalla | action | Parámetros principales |
|---|---|---|
| Login | `login` | `correo`, `password` |
| Confirmar asistencia | `asistencia?action=confirmar` | `motivo` |
| Autoevaluación mensual | `autoevaluacion?action=mensual` | `color_<figura>` = ROJO/AMARILLO/VERDE/AZUL |
| Autoevaluación del ensayo | `autoevaluacion?action=ensayo` | `idEnsayo`, `color_<figura>` (solo sube) |
| Mi perfil | `perfil?action=guardar` | `nombre`, `codigo`, `facultad`, `correo`, `celular`, avisos |
| Nuevo ensayo | `ensayos?action=crear` | `tipo`, `fecha`, `horaInicio`, `horaFin`, `lugar`, `cierreConfirmacion`, `dictan[]`, `indicacion[]` |
| Tomar asistencia | `asistencia?action=registrar` | `idEnsayo`, `asis_<miembro>` = AP/TJ/TI/FJ/FI |
| Figuras enseñadas | `ensayos?action=figuras` | `idEnsayo`, `figura[]` |
| Nuevo / editar miembro | `miembros?action=crear` / `editar` | `nombre`, `nombreCompleto`, `codigoPucp`, `facultad`, `correo`, `celular`, `rolBaile`, `frecuencia`, `fechaIngreso`, `rolEquipo`, `rolAdmin`, `passwordTemporal`, `motivoBaja` |
| Nueva / editar figura | `figuras?action=crear` / `editar` | `nombre`, `posicion`, `descripcion`, `idFigura` |
| Mano de figuras | `figuras?action=reglas` | `cuenta_<frecuencia>` |
| Histórico (corrección) | `historico?action=corregir` | `cMiembro`, `cFigura`, `cColor`, `cMotivo` |
| Roles y permisos | `roles?action=guardar` | `perm_<rol>_<permiso>` |

Cuando lo pases a JSP, los nombres tipo `color_2-pa-2` se cambian por el id de la BD (`color_${figura.id}`).

## Notas
- Los datos son de ejemplo (nombres y figuras reales del Excel; porcentajes, avances e historiales inventados).
- Los selectores de color y de asistencia funcionan solo con CSS (radio buttons), sin JavaScript.
- El reporte usa `@page A4` y `@media print`: “Imprimir / Guardar PDF” lo deja listo en dos hojas.
- Hay un responsive básico (el menú pasa arriba en pantallas angostas); se puede afinar después.


## Celular, tablet y laptop
El CSS es **mobile first**. Para ver la versión de celular, abre cualquier página y achica la ventana, o usa el modo dispositivo del navegador (F12 → ícono de celular). Debajo de 900 px aparecen la barra superior con ☰ (menú tipo cajón) y la barra de pestañas de abajo, y las tablas se vuelven tarjetas (`js/sdp.js`).
