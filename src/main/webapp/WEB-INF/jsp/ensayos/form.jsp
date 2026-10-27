<c:set var="titulo" value="${ensayo.idEnsayo == 0 ? 'Nuevo ensayo' : 'Editar ensayo'}"/><c:set var="menu" value="Ensayos"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<form method="post" action="${ctx}/ensayos/${ensayo.idEnsayo == 0 ? 'nuevo' : 'editar'}" class="stack" style="gap:24px"><input type="hidden" name="_csrf" value="${sessionScope.csrf}">
<input type="hidden" name="idEnsayo" value="${ensayo.idEnsayo == 0 ? '' : ensayo.idEnsayo}">
<section class="hero"><div class="titles"><span class="kicker">Ensayos</span><h1>${titulo}</h1><p>Al publicarlo, todos los miembros pueden confirmar asistencia</p></div>
  <div class="actions"><button class="btn btn-secondary" name="publicar" value="borrador">Guardar borrador</button><button class="btn btn-primary" name="publicar" value="si">Publicar ensayo</button></div>
  <span class="star"></span></section>
<div class="row top">
  <div class="stack grow">
    <div class="card"><h2>Datos del ensayo</h2>
      <div class="field"><span class="label">Tipo</span><div class="pills">
        <c:forEach var="t" items="${['GENERAL','COMPETENCIA','NIVELACION','EXHIBICION']}">
          <label class="pill"><input type="radio" name="tipo" value="${t}" ${ensayo.tipo == t ? 'checked' : ''}><span>${t == 'GENERAL' ? 'Ensayo general' : (t == 'COMPETENCIA' ? 'Ensayo de competencia' : (t == 'NIVELACION' ? 'Ensayo de nivelación' : 'Ensayo de exhibición'))}</span></label>
        </c:forEach></div></div>
      <div class="form-row">
        <div class="field"><label>Fecha</label><input class="input" type="date" name="fecha" value="${ensayo.fechaIso}" required></div>
        <div class="field"><label>Hora inicio</label><input class="input" type="time" name="horaInicio" value="${ensayo.horaInicioIso}" required></div>
        <div class="field"><label>Hora fin</label><input class="input" type="time" name="horaFin" value="${ensayo.horaFinIso}" required></div></div>
      <div class="form-row">
        <div class="field"><label>Lugar</label><input class="input" name="lugar" value="<c:out value='${ensayo.lugar}'/>" required></div>
        <div class="field"><label>Cierre de confirmación</label><input class="input" type="datetime-local" name="cierreConfirmacion" value="${ensayo.cierreIso}"></div></div>
    </div>
    <div class="card"><h2>¿Quiénes del equipo de ensayos dictan?</h2><p>Sirve para emparejar niveles que solo tienen un rol</p>
      <div class="pills">
        <c:forEach var="m" items="${equipo}">
          <label class="pill"><input type="checkbox" name="dictan" value="${m.idMiembro}" ${ensayo.dictantes.contains(m.idMiembro) ? 'checked' : ''}><span><c:out value="${m.nombre}"/> · ${m.rolBaileTexto}</span></label>
        </c:forEach></div></div>
    <div class="card"><h2>Indicaciones</h2><p>Aparecen en el flyer y en la pantalla del miembro (deja vacío para quitar)</p>
      <c:forEach var="i" begin="0" end="3">
        <input class="input" name="indicacion" value="<c:out value='${i < fn:length(ensayo.indicaciones) ? ensayo.indicaciones[i] : ""}'/>" placeholder="Ej: No olvidar llegar temprano">
      </c:forEach>
    </div>
  </div>
  <div class="stack w-440" style="gap:12px">
    <span class="label">VISTA PREVIA DEL FLYER</span>
    <div class="flyer-preview"><h3>${fn:replace(ensayo.tituloFlyer, ' ', '<br>')}</h3>
      <p>Casinerossss, ya están de vuelta los ensayos, prepárense para ruedear con todo!</p>
      <span class="big-date">${ensayo.fechaCorta}</span>
      <ul class="icon-list"><li><sdp:icono n="pin"/><c:out value="${ensayo.lugar}"/></li><li><sdp:icono n="clock"/>${ensayo.horario}</li>
        <c:forEach var="ind" items="${ensayo.indicaciones}" varStatus="s"><li><sdp:icono n="${s.index == 0 ? 'clip' : 'drop'}"/><c:out value="${ind}"/></li></c:forEach></ul>
      <img class="logo" src="${ctx}/img/logo-sdp-dorado.png" alt="Son de PUCP">
      <div class="fbar">TEAM ENSAYOS - SDP<span>⚾😑💃🏻🦉🐥🌈🌕🐧👀🍄🍗</span></div></div>
    <p class="muted xs">La vista previa se actualiza al guardar. El título cambia según el tipo de ensayo.</p>
  </div>
</div>
</form>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
