<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover"><meta name="theme-color" content="#1C19A9">
<title>Iniciar sesión · Son de PUCP</title>
<link rel="stylesheet" href="${ctx}/css/sdp.css"><script src="${ctx}/js/sdp.js" defer></script>
</head>
<body class="login">
<section class="login-brand">
  <img src="${ctx}/img/logo-sdp-dorado.png" alt="Son de PUCP">
  <h1>Sistema de<br>ensayos</h1>
  <p>Casinerossss, entra para confirmar tu asistencia, autoevaluarte y ver qué figuras se ruedean en el próximo ensayo.</p>
  <ul class="icon-list">
    <li><sdp:icono n="pin"/>Explanada del polideportivo</li>
    <li><sdp:icono n="clock"/>Ensayos generales · sábados 2:00 – 4:00 PM</li>
  </ul>
</section>
<section class="login-side">
  <form class="login-form" method="post" action="${ctx}/login"><input type="hidden" name="_csrf" value="${sessionScope.csrf}">
    <div><h2>Iniciar sesión</h2><p class="muted small">Usa tu correo PUCP</p></div>
    <c:if test="${not empty error}"><div class="flash bad">⚠ <c:out value="${error}"/></div></c:if>
    <c:if test="${empty error and not empty sessionScope.flashError}"><div class="flash bad">⚠ <c:out value="${sessionScope.flashError}"/></div></c:if><c:remove var="flashError" scope="session"/>
    <div class="field"><label for="correo">Correo</label><input class="input" id="correo" name="correo" type="email" value="<c:out value='${correo}'/>" placeholder="a2024xxxx@pucp.edu.pe" required autofocus></div>
    <div class="field"><label for="password">Contraseña</label><input class="input" id="password" name="password" type="password" placeholder="••••••••" required></div>
    <button class="btn btn-primary btn-block" type="submit">Ingresar</button>
    <p class="muted xs">¿Olvidaste tu contraseña? Pídele al admin que te genere una temporal.</p>
  </form>
</section>
<footer class="team-bar"><strong>TEAM ENSAYOS - SDP</strong><span class="emojis">⚾😑💃🏻🦉🐥🌈🌕🐧👀🍄🍗</span></footer>
</body>
</html>
