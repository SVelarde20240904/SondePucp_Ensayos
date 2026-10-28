<%@ page isErrorPage="true" %>
<!DOCTYPE html>
<html lang="es"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover"><meta name="theme-color" content="#1C19A9"><title>Algo salió mal · Son de PUCP</title><link rel="stylesheet" href="${ctx}/css/sdp.css"><script src="${ctx}/js/sdp.js" defer></script></head>
<body class="login">
<section class="login-brand"><img src="${ctx}/img/logo-sdp-dorado.png" alt="Son de PUCP"><h1>Ups</h1>
<p>${pageContext.errorData.statusCode == 404 ? 'Esa página no existe.' : 'Algo salió mal al procesar tu pedido. Si se repite, avísale al admin.'}</p></section>
<section class="login-side"><div class="login-form"><h2>¿Y ahora?</h2>
<c:if test="${pageContext.errorData.statusCode != 404}"><p class="muted small"><c:out value="${pageContext.exception.message}"/></p></c:if>
<a class="btn btn-primary btn-block" href="${ctx}/inicio">Volver al inicio</a></div></section>
</body></html>
