<c:set var="titulo" value="Ensayos"/><c:set var="menu" value="Ensayos"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<section class="hero"><div class="titles"><span class="kicker">Ensayos</span><h1>Ensayos del ciclo</h1>
  <p>Todos los ensayos, con su asistencia y cuánto avanzó el grupo en cada uno</p></div>
  <div class="actions"><a class="btn btn-primary" href="${ctx}/ensayos/nuevo">+ Nuevo ensayo</a></div><span class="star"></span></section>

<div class="grid-4">
  <div class="card stat"><b class="c-primary">${ciclo[0]}</b><span>ensayos realizados en el ciclo</span></div>
  <div class="card stat"><b class="c-verde">${ciclo[1]}%</b><span>asistencia promedio</span></div>
  <div class="card stat"><b class="c-primary">${ciclo[2]}</b><span>figuras enseñadas</span></div>
  <div class="card stat"><b class="c-accent">+${ciclo[3]}</b><span>avances de figuras</span></div>
</div>

<div class="row">
  <div class="card flyer w-340">
    <c:choose>
      <c:when test="${empty proximo}"><h2>PRÓXIMO</h2><p>No hay un ensayo publicado.</p><div><a class="btn btn-primary" href="${ctx}/ensayos/nuevo">Crear ensayo</a></div></c:when>
      <c:otherwise>
        <div class="inline"><h2>PRÓXIMO</h2><span class="tag info">Programado</span></div><span class="big-date">${proximo.fechaCorta}</span>
        <ul class="icon-list"><li><sdp:icono n="pin"/><c:out value="${proximo.lugar}"/></li><li><sdp:icono n="clock"/>${proximo.horario}</li>
          <li><sdp:icono n="clip"/>${proximo.confirmados} confirmados · ${proximo.sinRespuesta} sin respuesta</li></ul>
        <div class="inline"><a class="btn btn-primary" href="${ctx}/ensayos/detalle?id=${proximo.idEnsayo}">Abrir ensayo</a><a class="btn btn-secondary" href="${ctx}/ensayos/cronograma?id=${proximo.idEnsayo}">Cronograma</a></div>
      </c:otherwise>
    </c:choose>
  </div>
  <div class="card grow"><h2>Asistencia y avances por ensayo</h2><p>Barras = asistentes · número dorado = figuras mejoradas en la autoevaluación del ensayo</p>
    <div class="chart">
      <c:forEach var="g" items="${grafico}">
        <div class="col"><em>+${g.avances}</em><i style="height:${g.asistieron * 160 / maxGrafico + 2}px"></i><span>${g.asistieron}</span><small>${g.fechaCorta}</small></div>
      </c:forEach>
      <c:if test="${empty grafico}"><p class="empty">Aún no hay ensayos realizados.</p></c:if>
    </div>
  </div>
</div>

<div class="card"><h2>Historial</h2><p>Haz clic en un ensayo para ver su detalle y su reporte</p>
  <div class="pills">
    <a class="pill-link ${empty tipo ? 'on' : ''}" href="${ctx}/ensayos">Todos</a>
    <a class="pill-link ${tipo == 'GENERAL' ? 'on' : ''}" href="${ctx}/ensayos?tipo=GENERAL">Generales</a>
    <a class="pill-link ${tipo == 'COMPETENCIA' ? 'on' : ''}" href="${ctx}/ensayos?tipo=COMPETENCIA">Competencia</a>
    <a class="pill-link ${tipo == 'NIVELACION' ? 'on' : ''}" href="${ctx}/ensayos?tipo=NIVELACION">Nivelación</a>
    <a class="pill-link ${tipo == 'EXHIBICION' ? 'on' : ''}" href="${ctx}/ensayos?tipo=EXHIBICION">Exhibición</a>
  </div>
  <table class="table"><thead><tr><th>Fecha</th><th>Tipo</th><th>Asistencia</th><th>Niveles enseñados</th><th>Figuras</th><th>Avances</th><th>Autoeval.</th><th>Estado</th><th></th></tr></thead><tbody>
  <c:forEach var="e" items="${ensayos}">
    <tr>
      <td>${e.dia} ${e.fechaCorta}</td><td>${e.tipoTexto}</td>
      <td><c:choose><c:when test="${e.pasado}"><span class="inline" style="flex-wrap:nowrap"><span class="bar" style="width:70px"><i style="width:${e.porcentajeAsistencia}%"></i></span><b class="small">${e.asistieron}/${e.esperados}</b></span></c:when>
        <c:otherwise>${e.confirmados} confirm.</c:otherwise></c:choose></td>
      <td><span class="inline" style="gap:4px"><c:forEach var="n" items="${fn:split(e.nivelesEnsenados, ',')}"><span class="dot ${fn:toLowerCase(n)}"></span></c:forEach>${empty e.nivelesEnsenados ? '—' : ''}</span></td>
      <td>${e.figurasEnsenadas == 0 ? '—' : e.figurasEnsenadas}</td><td>${e.avances == 0 ? '—' : '+'.concat(e.avances)}</td>
      <td>${e.autoevaluaron == 0 ? '—' : e.autoevaluaron}</td>
      <td><sdp:tag texto="${e.estadoTexto}" tono="${e.estadoTono}"/></td>
      <td><a href="${ctx}/ensayos/detalle?id=${e.idEnsayo}">${e.pasado ? 'Ver' : 'Abrir'}</a></td>
    </tr>
  </c:forEach>
  <c:if test="${empty ensayos}"><tr><td colspan="9" class="empty">No hay ensayos con ese filtro.</td></tr></c:if>
  </tbody></table>
</div>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
