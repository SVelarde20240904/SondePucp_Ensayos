<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover"><meta name="theme-color" content="#1C19A9">
<title>Cambiar contraseña · Son de PUCP</title>
<link rel="stylesheet" href="${ctx}/css/sdp.css"><script src="${ctx}/js/sdp.js" defer></script>
</head>
<body class="login">
<section class="login-brand">
  <img src="${ctx}/img/logo-sdp-dorado.png" alt="Son de PUCP">
  <h1>¡Hola,<br><c:out value="${fn:split(sessionScope.usuario.nombre, ' ')[0]}"/>!</h1>
  <p>${sessionScope.usuario.debeCambiarPassword ? 'Entraste con una contraseña temporal. Elige una tuya para seguir.' : 'Elige una contraseña nueva.'}</p>
</section>
<section class="login-side">
  <form class="login-form" method="post" action="${ctx}/cambiar-password"><input type="hidden" name="_csrf" value="${sessionScope.csrf}">
    <div><h2>Nueva contraseña</h2><p class="muted small">Mínimo 8 caracteres</p></div>
    <c:if test="${not empty error}"><div class="flash bad">⚠ <c:out value="${error}"/></div></c:if>
    <div class="field"><label for="actual">Contraseña actual (o temporal)</label><input class="input" id="actual" name="actual" type="password" required autofocus></div>
    <div class="field"><label for="nueva">Nueva contraseña</label><input class="input" id="nueva" name="nueva" type="password" minlength="8" required></div>
    <div class="field"><label for="repite">Repite la nueva contraseña</label><input class="input" id="repite" name="repite" type="password" minlength="8" required></div>
    <button class="btn btn-primary btn-block" type="submit">Guardar y entrar</button>
    <a class="link" href="${ctx}/logout">Cerrar sesión</a>
  </form>
</section>
</body>
</html>
