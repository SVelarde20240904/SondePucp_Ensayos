<c:set var="titulo" value="Pasos por nivel"/><c:set var="menu" value="Pasos de talleres"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<section class="hero"><div class="titles"><span class="kicker">Team Talleres</span><h1>Pasos por nivel</h1>
  <p>El catálogo para armar los cronogramas. Salió de los cronogramas pasados y crece solo cuando alguien escribe un paso nuevo.</p></div><span class="star"></span></section>
<c:set var="grupo" value="talleres"/><c:set var="sub" value="pasos"/><%@ include file="/WEB-INF/jsp/includes/subtabs.jspf" %>
<div class="grid-2">
<c:forEach var="e" items="${porNivel}">
  <div class="card" id="${e.key}"><h2>${e.key == 'PRINCIPIANTE' ? 'Principiante' : (e.key == 'BASICO' ? 'Básico' : (e.key == 'INTRO_RUEDA' ? 'Intro a la rueda' : 'Rueda básica'))}</h2>
    <form class="inline" method="post" action="${ctx}/talleres/pasos"><input type="hidden" name="_csrf" value="${sessionScope.csrf}"><input type="hidden" name="nivel" value="${e.key}">
      <input class="input" name="nombre" maxlength="80" placeholder="Nuevo paso" required style="max-width:240px"><button class="btn btn-secondary btn-sm" name="action" value="agregar">+ Agregar</button></form>
    <c:forEach var="p" items="${e.value}">
      <form class="paso-fila ${p.activo ? '' : 'inactivo'}" method="post" action="${ctx}/talleres/pasos"><input type="hidden" name="_csrf" value="${sessionScope.csrf}"><input type="hidden" name="nivel" value="${e.key}"><input type="hidden" name="idPaso" value="${p.idPaso}">
        <input class="input" name="nombre" value="<c:out value='${p.nombre}'/>" maxlength="80" aria-label="Nombre del paso">
        <span class="muted xs" title="Veces que salió en un taller">${p.usos}×</span>
        <button class="btn btn-ghost btn-sm" name="action" value="renombrar" title="Guardar nombre">✓</button>
        <button class="btn btn-ghost btn-sm" name="action" value="${p.activo ? 'desactivar' : 'activar'}" title="${p.activo ? 'Ocultar de las sugerencias' : 'Volver a sugerir'}">${p.activo ? 'Ocultar' : 'Mostrar'}</button></form>
    </c:forEach>
    <c:if test="${empty e.value}"><p class="empty">Sin pasos todavía.</p></c:if>
  </div>
</c:forEach>
</div>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
