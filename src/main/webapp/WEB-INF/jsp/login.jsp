<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover"><meta name="theme-color" content="#2B2E63">
<title>Iniciar sesión · Son de PUCP</title>
<link rel="stylesheet" href="${ctx}/css/sdp.css"><script src="${ctx}/js/sdp.js" defer></script>
</head>
<body class="login">
<section class="login-brand">
  <img src="${ctx}/img/logo-sdp-dorado.png" alt="Son de PUCP">
  <h1>La web<br>de SDP</h1>
  <p>Casinerossss, aquí está todo lo de Son de PUCP: los ensayos y talleres, tus responsabilidades, tu feedback y cómo vas con las figuras.</p>
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
<footer class="team-bar"><strong>SON DE PUCP</strong><span class="emojis">⚾😑💃🏻🦉🐥🌈🌕🐧👀🍄🍗</span></footer>
</body>
</html>
