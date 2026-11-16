<c:set var="titulo" value="Mi asistencia"/><c:set var="menu" value="Confirmar asistencia"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<section class="hero"><div class="titles"><span class="kicker">Mi espacio</span><h1>Mi asistencia</h1>
  <p>Confirma los ensayos y talleres que vienen, y mira cómo vas en el ciclo (desde el ${desdeTexto})</p></div><span class="star"></span></section>

<div class="grid-4">
  <div class="card stat"><b class="c-primary">${empty porcentaje ? '—' : porcentaje += '%'}</b><span>asistencia a ensayos · ${nAsistio} de ${nRegistrados}</span></div>
  <div class="card stat"><b class="c-amarillo">${nTarde}</b><span>${nTarde == 1 ? 'tardanza' : 'tardanzas'}</span></div>
  <div class="card stat"><b class="c-rojo">${nFJ + nFI}</b><span>faltas · ${nFJ} justificada${nFJ == 1 ? '' : 's'}, ${nFI} sin justificar</span></div>
  <div class="card stat"><b class="c-verde">${empty cumplimiento ? '—' : cumplimiento += '%'}</b><span>de las veces que dijiste “sí voy”, fuiste<c:if test="${nSinRespuesta gt 0}"> · ${nSinRespuesta} sin responder</c:if></span></div>
</div>

<h2 class="section-title">Lo que viene</h2>
<c:choose>
  <c:when test="${empty proximos and empty proximosTalleres}"><div class="card"><p class="empty">No hay ensayos ni talleres publicados por ahora. Cuando los publiquen, los verás aquí.</p></div></c:when>
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
          <input class="input" name="motivo" maxlength="200" placeholder="Si no puedes ir: ¿por qué? (así queda justificada)" value="<c:out value='${e.miMotivo}'/>">
          <div class="inline">
            <button class="btn ${e.miConfirmacion == 'SI' ? 'btn-primary' : (s.first ? 'btn-primary' : 'btn-secondary')}" name="respuesta" value="SI">${e.miConfirmacion == 'SI' ? '✓ Voy' : 'Sí voy'}</button>
            <button class="btn btn-secondary" name="respuesta" value="NO">${e.miConfirmacion == 'SI' ? 'Ya no puedo' : 'No puedo'}</button>
          </div>
        </form>
      </div>
    </c:forEach>
    <c:forEach var="t" items="${proximosTalleres}">
      <div class="card" id="talleres">
        <div class="inline"><b style="font-family:var(--display);font-size:16px">${fn:toUpperCase(t.dia)}</b>
          <c:choose>
            <c:when test="${t.miConfirmacion == 'SI'}"><span class="tag ok">Confirmado</span></c:when>
            <c:when test="${t.miConfirmacion == 'NO'}"><span class="tag neutral">No voy</span></c:when>
            <c:otherwise><span class="tag warn">Sin respuesta</span></c:otherwise>
          </c:choose></div>
        <span class="big-date blue" style="font-size:48px">${t.fechaCorta}</span><b>${t.titulo}</b>
        <ul class="icon-list"><li><sdp:icono n="clock"/>${t.horario}</li>
          <c:if test="${not empty t.lugar}"><li><sdp:icono n="pin"/><c:out value="${t.lugar}"/></li></c:if>
          <c:if test="${not empty t.miPapel}"><li><sdp:icono n="clip"/><b><c:out value="${t.miPapel}"/></b></li></c:if></ul>
        <form method="post" action="${ctx}/confirmar-asistencia" class="stack" style="gap:10px"><input type="hidden" name="_csrf" value="${sessionScope.csrf}">
          <input type="hidden" name="idTaller" value="${t.idTaller}">
          <input class="input" name="motivo" maxlength="200" placeholder="Si no puedes ir: ¿por qué? (opcional)" value="<c:out value='${t.miMotivo}'/>">
          <div class="inline">
            <button class="btn ${t.miConfirmacion == 'SI' ? 'btn-primary' : 'btn-secondary'}" name="respuesta" value="SI">${t.miConfirmacion == 'SI' ? '✓ Voy' : 'Sí voy'}</button>
            <button class="btn btn-secondary" name="respuesta" value="NO">${t.miConfirmacion == 'SI' ? 'Ya no puedo' : 'No puedo'}</button>
            <span class="muted xs">${t.confirmadosSi} van</span>
          </div>
        </form>
      </div>
    </c:forEach>
  </div>
  </c:otherwise>
</c:choose>

<div class="note info">Si avisas antes del ensayo que no puedes ir, con un motivo, tu falta queda como justificada (FJ) en vez de injustificada (FI).</div>

<div class="row top">
  <div class="card grow"><h2>Mis ensayos del ciclo</h2>
    <c:choose>
      <c:when test="${empty historial}"><p class="empty">Todavía no hay ensayos pasados en este ciclo.</p></c:when>
      <c:otherwise>
      <table class="table"><thead><tr><th>Fecha</th><th>Confirmaste</th><th>Asistencia</th><th>Motivo</th></tr></thead><tbody>
        <c:forEach var="a" items="${historial}">
          <tr><td><b>${a.dia} ${a.fechaCorta}</b></td>
            <td><span class="small">${a.confirmacionTexto}</span></td>
            <td><div class="inline" style="gap:6px"><sdp:code valor="${a.estadoReal}"/><span class="small">${a.estadoTexto}</span></div></td>
            <td><span class="small muted"><c:out value="${empty a.motivo ? '—' : a.motivo}"/></span></td></tr>
        </c:forEach>
      </tbody></table></c:otherwise>
    </c:choose>
  </div>
  <div class="card w-440"><h2>Mis talleres</h2>
    <c:choose>
      <c:when test="${empty misTalleres}"><p class="empty">Todavía no confirmaste ni participaste en talleres.</p></c:when>
      <c:otherwise>
      <table class="table"><thead><tr><th>Taller</th><th>Qué hiciste</th></tr></thead><tbody>
        <c:forEach var="t" items="${misTalleres}">
          <tr><td><b>${t.titulo}</b><small>${t.dia} ${t.fechaCorta}</small></td>
            <td><c:choose>
              <c:when test="${not empty t.miPapel}"><span class="small"><c:out value="${t.miPapel}"/></span>
                <c:choose><c:when test="${t.miAsistencia == 'TARDE'}"><sdp:tag texto="Tarde" tono="warn"/></c:when>
                  <c:when test="${t.miAsistencia == 'NO_SE_PRESENTO'}"><sdp:tag texto="No fue" tono="bad"/></c:when>
                  <c:when test="${not empty t.miAsistencia}"><sdp:tag texto="A tiempo" tono="ok"/></c:when></c:choose></c:when>
              <c:when test="${t.miConfirmacion == 'SI'}"><span class="small">Confirmaste que ibas</span></c:when>
              <c:otherwise><span class="small muted">Avisaste que no ibas<c:if test="${not empty t.miMotivo}">: <c:out value="${t.miMotivo}"/></c:if></span></c:otherwise>
            </c:choose></td></tr>
        </c:forEach>
      </tbody></table></c:otherwise>
    </c:choose>
  </div>
</div>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
