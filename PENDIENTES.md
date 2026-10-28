# Pendientes y feedback

Lista viva de mejoras. Lo que se termina pasa a **Hecho** con su commit.

## Feedback del piloto
- [ ] **Generar contraseña temporal sin perder lo escrito:** si llenas los datos de una persona y, sin guardar, le das a *Generar contraseña temporal*, se pierde lo que escribiste (hoy solo se guarda el correo). Debería guardar todo el formulario y recién después generar la clave.

- [ ] **Histórico sin registros redundantes:** al llenar la autoevaluación mensual aparecen cambios "Azul → Azul" (el mismo color). Solo debería registrarse cuando el color cambia, o al menos ocultar esos registros en la pantalla de *Histórico*.
- [ ] **El túnel se cae al cambiar de red o suspender la laptop** y el link cambia. Para el piloto: dejar la laptop fija con internet estable, usar `--protocol http2`, o pasar a link fijo (Cloudflare con dominio, o Railway).

## Por decidir / confirmar
- [ ] La **"x"** del Excel: ¿significa *no marcó la encuesta* (así está hoy: SIN_RESPUESTA) o *marcó que no venía*? Si es lo segundo, hay un UPDATE en `db/README.md`.
- [ ] Agregar el código **AN** (asistencia no planeada) a la maqueta y a Figma (en la app ya está).

## Ideas
- [ ] **Activar cuentas en lote:** una tabla con los que no tienen cuenta, escribes los correos y se generan todas las claves temporales de una, con un mensaje de WhatsApp por persona.
- [ ] **Link fijo** para el túnel de Cloudflare (requiere cuenta de Cloudflare y un dominio), o pasar a Railway.
- [ ] Pantallas de **celular en Figma** (los mockups están solo en versión laptop).
- [ ] Subir los logos reales a Figma.

## Hecho
- [x] La clave temporal se muestra en la tarjeta *Acceso al sistema*, con el mensaje para WhatsApp, y no solo en el aviso verde de arriba. `070f4b1`
- [x] *Generar contraseña temporal* guarda el correo y valida que no esté repetido. `070f4b1`
- [x] Claves temporales más seguras (`SDP-XXXX-XXXX`). `070f4b1`
- [x] Diseño mobile first. `3999bff`
- [x] Blindaje de seguridad y despliegue con Docker. `f465e8c`
