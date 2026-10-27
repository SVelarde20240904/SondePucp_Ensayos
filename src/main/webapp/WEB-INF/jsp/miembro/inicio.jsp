<c:set var="titulo" value="Inicio"/><c:set var="menu" value="Inicio"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<section class="hero"><div class="titles"><span class="kicker">Mi espacio</span>
  <h1>¡Hola, <c:out value="${fn:split(yo.nombre, ' ')[0]}"/>! 👋</h1>
  <p><c:choose><c:when test="${not empty proximo}">El ${proximo.fechaLarga} es el próximo ensayo</c:when><c:otherwise>Aún no hay un próximo ensayo publicado</c:otherwise></c:choose></p>
</div><span class="star"></span></section>

<c:if test="${not empty pendienteEnsayo}">
  <div class="note warn inline" style="justify-content:space-between">Tienes pendiente la autoevaluación del ensayo del ${pendienteEnsayo.fechaCorta} (toma un minuto).
    <a class="btn btn-primary btn-sm" href="${ctx}/autoevaluacion?ensayo=${pendienteEnsayo.idEnsayo}">Autoevaluarme</a></div>
</c:if>

<div class="grid-3">
  <div class="card"><h2>Mi nivel actual</h2>
    <c:choose>
      <c:when test="${empty yo.nivel}">
        <div class="inline"><sdp:chip color=""/><h1 style="font-size:24px">Sin nivel</h1></div>
        <p class="muted small">Haz tu primera autoevaluación para saber tu nivel.</p>
      </c:when>
      <c:otherwise>
        <div class="inline"><sdp:chip color="${yo.nivel}"/><h1 style="font-size:24px">Nivel ${yo.nivel}</h1></div>
        <p class="muted small"><c:choose>
          <c:when test="${not empty siguienteNivel}">Te faltan ${faltanSuNivel} figuras ${yo.nivel} para subir a ${siguienteNivel}.</c:when>
          <c:otherwise>Ya sabes todas las figuras de los niveles anteriores. ¡A dominar las ROJO!</c:otherwise></c:choose></p>
      </c:otherwise>
    </c:choose>
  </div>

  <div class="card flyer"><h2>PRÓXIMO ENSAYO</h2>
    <c:choose>
      <c:when test="${empty proximo}"><p>Cuando el equipo de ensayos publique uno, aparecerá aquí.</p></c:when>
      <c:otherwise>
        <span class="big-date">${proximo.fechaCorta}</span>
        <ul class="icon-list">
          <li><sdp:icono n="pin"/><c:out value="${proximo.lugar}"/></li>
          <li><sdp:icono n="clock"/>${proximo.horario}</li>
          <c:forEach var="ind" items="${proximo.indicaciones}" varStatus="s"><li><sdp:icono n="${s.index == 0 ? 'clip' : 'drop'}"/><c:out value="${ind}"/></li></c:forEach>
        </ul>
        <form method="post" action="${ctx}/confirmar-asistencia" class="inline"><input type="hidden" name="_csrf" value="${sessionScope.csrf}">
          <input type="hidden" name="idEnsayo" value="${proximo.idEnsayo}">
          <c:choose>
            <c:when test="${proximo.miConfirmacion == 'SI'}"><span class="tag ok">Confirmado</span><a class="btn btn-secondary btn-sm" href="${ctx}/confirmar-asistencia">Ya no puedo</a></c:when>
            <c:otherwise><button class="btn btn-primary" name="respuesta" value="SI">Sí voy</button><a class="btn btn-secondary" href="${ctx}/confirmar-asistencia">No puedo</a></c:otherwise>
          </c:choose>
        </form>
      </c:otherwise>
    </c:choose>
  </div>

  <div class="card"><h2>Autoevaluación mensual</h2>
    <div class="inline">
      <c:choose>
        <c:when test="${mensualRespondida}"><span class="tag ok">Respondida</span></c:when>
        <c:when test="${mensualAbierta}"><span class="tag warn">Pendiente</span><span class="muted small">Cierra el ${cierreMensual}</span></c:when>
        <c:otherwise><span class="tag neutral">Cerrada</span><span class="muted small">Se abre el día 1 de cada mes</span></c:otherwise>
      </c:choose>
    </div>
    <p class="muted small">${nFiguras} figuras · toma ~3 min</p>
    <div><a class="btn btn-ghost" href="${ctx}/autoevaluacion?tipo=mensual">${mensualRespondida ? 'Ver o corregir' : 'Autoevaluarme'}</a></div>
  </div>
</div>

<div class="card"><h2>Figuras que me faltan</h2><p>Ordenadas por nivel: primero completa las de tu nivel actual</p>
  <div class="note info">ℹ El color de una figura es la mediana del grupo: AZUL = la domina casi todo el grupo (base) → ROJO = la domina casi nadie (avanzada).</div>
  <c:choose>
    <c:when test="${empty faltan}"><p class="empty">${empty yo.nivel ? 'Aún no te has autoevaluado.' : '¡No te falta ninguna figura con color! 🎉'}</p></c:when>
    <c:otherwise>
      <table class="table"><thead><tr><th>Figura</th><th>Nivel de la figura</th><th>Mi color</th><th>Última actualización</th></tr></thead><tbody>
      <c:forEach var="e" items="${faltan}"><tr><td><c:out value="${e.figura}"/></td><td><sdp:chip color="${e.colorFigura}"/></td><td><sdp:chip color="${e.color}"/></td><td>${e.actualizacionTexto}</td></tr></c:forEach>
      </tbody></table>
    </c:otherwise>
  </c:choose>
</div>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
