<c:set var="titulo" value="Mi perfil"/><c:set var="menu" value="Mi perfil"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<form method="post" action="${ctx}/perfil" class="stack" style="gap:24px"><input type="hidden" name="_csrf" value="${sessionScope.csrf}">
<section class="hero"><div class="titles"><span class="kicker">Mi espacio</span><h1>Mi perfil</h1><p>Tus datos dentro de Son de PUCP</p></div>
  <div class="actions"><button class="btn btn-primary" type="submit">Guardar cambios</button></div><span class="star"></span></section>
<div class="row top">
  <div class="card flyer w-300 center" style="align-items:center;padding:28px 24px">
    <div class="avatar" style="width:132px;height:132px;font-size:44px;background:var(--ink);color:var(--accent);border:6px solid var(--accent);font-family:var(--display)">${yo.iniciales}</div>
    <h1 style="font-size:22px"><c:out value="${yo.nombre}"/></h1>
    <c:if test="${yo.tieneApodo}"><p class="muted" style="margin-top:-6px">“<c:out value="${yo.apodo}"/>”</p></c:if>
    <div class="inline" style="justify-content:center"><span class="tag info role">${yo.rolBaileTexto}</span><sdp:chip color="${yo.nivel}"/></div>
    <ul class="icon-list"><li><sdp:icono n="clip"/>Miembro desde ${yo.miembroDesde}</li><li><sdp:icono n="clock"/>Frecuencia: ${yo.frecuenciaTexto}</li>
      <c:if test="${not empty yo.fechaNacimiento}"><li>🎂 Cumpleaños: ${yo.cumpleTexto}</li></c:if></ul>
    <c:if test="${not empty yo.teams}"><div class="inline" style="justify-content:center;flex-wrap:wrap;gap:6px"><c:forEach var="t" items="${yo.teams}"><span class="tag info">Team <c:out value="${t}"/></span></c:forEach></div></c:if>
  </div>
  <div class="stack grow">
    <div class="card"><h2>Datos personales</h2><p>Los mismos que pedimos en la convocatoria</p>
      <div class="form-row">
        <div class="field"><label>Nombre completo</label><input class="input" name="nombreCompleto" value="<c:out value='${yo.nombreCompleto}'/>" required></div>
        <div class="field"><label>Nombre en el sistema</label><input class="input" value="<c:out value='${yo.nombre}'/>" readonly title="Lo cambia el admin"></div></div>
      <div class="form-row">
        <div class="field"><label>Apodo</label><input class="input" name="apodo" value="<c:out value='${yo.apodo}'/>" maxlength="30" placeholder="Cómo te dicen en el grupo (opcional)"></div>
        <div class="field"><label>Fecha de nacimiento</label><input class="input" type="date" name="fechaNacimiento" value="${yo.fechaNacimientoIso}" max="${maxNacimiento}">
          <span class="muted xs">Para saludarte en tu cumple 🎂. Los demás solo ven el día y el mes, nunca el año.</span></div></div>
      <div class="form-row">
        <div class="field"><label>Código PUCP</label><input class="input" name="codigoPucp" value="<c:out value='${yo.codigoPucp}'/>" pattern="\d{8}" placeholder="8 dígitos"></div>
        <div class="field"><label>Facultad</label><select class="input" name="facultad"><option value="">Elige tu facultad</option>
          <c:forEach var="f" items="${facultades}"><option ${yo.facultad == f ? 'selected' : ''}>${f}</option></c:forEach>
          <c:if test="${not empty yo.facultad and not facultades.contains(yo.facultad)}"><option selected><c:out value="${yo.facultad}"/></option></c:if></select></div></div>
      <div class="form-row">
        <div class="field"><label>Correo PUCP</label><input class="input" type="email" name="correo" value="<c:out value='${yo.correo}'/>" required></div>
        <div class="field"><label>Celular</label><input class="input" type="tel" name="celular" value="<c:out value='${yo.celular}'/>"></div></div>
    </div>
    <div class="card"><h2>En el grupo</h2><p>Solo el admin puede cambiar estos datos. Tu rol de baile se registra una sola vez al ingresar.</p>
      <div class="field"><label>Teams</label><input class="input" value="🔒  ${empty yo.teams ? 'Sin team asignado' : 'Team '.concat(fn:join(yo.teams.toArray(), ' · Team '))}" readonly></div>
      <div class="form-row">
        <div class="field"><label>Rol de baile</label><input class="input" value="🔒  ${yo.rolBaileTexto}" readonly></div>
        <div class="field"><label>Frecuencia</label><input class="input" value="🔒  ${yo.frecuenciaTexto}" readonly></div>
        <div class="field"><label>Estado</label><input class="input" value="🔒  ${yo.activo ? 'Activo' : 'Inactivo'}" readonly></div></div>
    </div>
    <div class="card"><h2>Cuenta y avisos</h2>
      <label class="setting"><span>Recordarme confirmar asistencia</span><span class="switch"><input type="checkbox" name="avisoAsistencia" ${yo.avisoAsistencia ? 'checked' : ''}><span></span></span></label>
      <label class="setting"><span>Avisarme cuando abra la autoevaluación mensual</span><span class="switch"><input type="checkbox" name="avisoMensual" ${yo.avisoMensual ? 'checked' : ''}><span></span></span></label>
      <label class="setting"><span>Avisarme cuando haya autoevaluación del ensayo</span><span class="switch"><input type="checkbox" name="avisoEnsayo" ${yo.avisoEnsayo ? 'checked' : ''}><span></span></span></label>
      <div class="inline"><a class="btn btn-secondary" href="${ctx}/cambiar-password">Cambiar contraseña</a><a class="btn btn-ghost" href="${ctx}/logout">Cerrar sesión</a></div>
    </div>
  </div>
</div>
</form>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
