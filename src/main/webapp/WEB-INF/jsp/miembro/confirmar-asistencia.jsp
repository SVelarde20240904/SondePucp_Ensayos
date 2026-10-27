<c:set var="titulo" value="Confirmar asistencia"/><c:set var="menu" value="Confirmar asistencia"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<section class="hero"><div class="titles"><span class="kicker">Mi espacio</span><h1>Confirmar asistencia</h1>
  <p>Reemplaza la encuesta de WhatsApp. Si no respondes, quedas como “Sin respuesta”.</p></div><span class="star"></span></section>

<c:choose>
  <c:when test="${empty proximos}"><div class="card"><p class="empty">No hay ensayos publicados por ahora. Cuando el equipo de ensayos publique uno, lo verás aquí.</p></div></c:when>
  <c:otherwise>
  <div class="grid-3">
    <c:forEach var="e" items="${proximos}" varStatus="s">
      <div class="card ${s.first ? 'flyer' : ''}">
        <div class="inline"><b style="font-family:var(--display);font-size:16px">${fn:toUpperCase(e.dia)}</b>
          <c:choose>
            <c:when test="${e.miConfirmacion == 'SI'}"><span class="tag ok">Confirmado</span></c:when>
            <c:when test="${e.miConfirmacion == 'NO'}"><span class="tag neutral">No voy${empty e.miMotivo ? '' : ' · justificado'}</span></c:when>
            <c:otherwise><span class="tag warn">Sin respuesta</span></c:otherwise>
          </c:choose></div>
        <span class="big-date${s.first ? '' : ' blue'}" style="font-size:48px">${e.fechaCorta}</span><b>${e.tipoTexto}</b>
        <ul class="icon-list"><li><sdp:icono n="pin"/><c:out value="${e.lugar}"/></li><li><sdp:icono n="clock"/>${e.horario}</li></ul>
        <form method="post" action="${ctx}/confirmar-asistencia" class="stack" style="gap:10px"><input type="hidden" name="_csrf" value="${sessionScope.csrf}">
          <input type="hidden" name="idEnsayo" value="${e.idEnsayo}">
          <input class="input" name="motivo" placeholder="Si no puedes ir: ¿por qué? (opcional)" value="<c:out value='${e.miMotivo}'/>">
          <div class="inline">
            <button class="btn ${e.miConfirmacion == 'SI' ? 'btn-primary' : (s.first ? 'btn-primary' : 'btn-secondary')}" name="respuesta" value="SI">${e.miConfirmacion == 'SI' ? '✓ Voy' : 'Sí voy'}</button>
            <button class="btn btn-secondary" name="respuesta" value="NO">${e.miConfirmacion == 'SI' ? 'Ya no puedo' : 'No puedo'}</button>
          </div>
        </form>
      </div>
    </c:forEach>
  </div>
  </c:otherwise>
</c:choose>

<div class="note info">Si avisas antes del ensayo que no puedes ir, con un motivo, tu falta queda como justificada (FJ) en vez de injustificada (FI).</div>

<div class="card"><h2>Mi asistencia reciente</h2><p>Últimos ensayos · ${porcentaje}% de asistencia en el ciclo</p>
  <div class="inline">
    <c:forEach var="a" items="${misAsistencias}">
      <div class="card center" style="padding:12px 18px;gap:6px;background:var(--bg);border:0"><span class="muted small bold">${a.fechaCorta}</span>
        <sdp:code valor="${empty a.estadoReal ? (a.confirmacion == 'SIN_RESPUESTA' ? 'SR' : '') : a.estadoReal}"/></div>
    </c:forEach>
    <c:if test="${empty misAsistencias}"><p class="empty">Todavía no tienes asistencias registradas.</p></c:if>
  </div>
</div>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
