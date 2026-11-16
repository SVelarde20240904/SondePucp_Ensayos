<c:set var="titulo" value="Inicio"/><c:set var="menu" value="Inicio"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<section class="hero"><div class="titles"><span class="kicker">Mi espacio</span>
  <h1><c:choose><c:when test="${yo.cumpleHoy}">¡Feliz cumpleaños, <c:out value="${yo.comoLlamarlo}"/>! 🎂🎉</c:when><c:otherwise>¡Hola, <c:out value="${yo.comoLlamarlo}"/>! 👋</c:otherwise></c:choose></h1>
  <p><c:choose><c:when test="${empty pendientes}">Estás al día con todo. ¡Nos vemos en el próximo ensayo!</c:when>
    <c:otherwise>Tienes ${fn:length(pendientes)} ${fn:length(pendientes) == 1 ? 'cosa pendiente' : 'cosas pendientes'}</c:otherwise></c:choose></p>
</div><span class="star"></span></section>

<div class="grid-2">
  <%-- Próximo ensayo --%>
  <div class="card flyer"><h2>PRÓXIMO ENSAYO</h2>
    <c:choose>
      <c:when test="${empty proximo}"><p>Cuando el equipo de ensayos publique uno, aparecerá aquí.</p></c:when>
      <c:otherwise>
        <span class="big-date">${proximo.fechaCorta}</span>
        <ul class="icon-list">
          <li><sdp:icono n="pin"/><c:out value="${proximo.lugar}"/></li>
          <li><sdp:icono n="clock"/>${proximo.dia} · ${proximo.horario}</li>
          <c:forEach var="ind" items="${proximo.indicaciones}" varStatus="s"><li><sdp:icono n="${s.index == 0 ? 'clip' : 'drop'}"/><c:out value="${ind}"/></li></c:forEach>
        </ul>
        <form method="post" action="${ctx}/confirmar-asistencia" class="inline"><input type="hidden" name="_csrf" value="${sessionScope.csrf}">
          <input type="hidden" name="idEnsayo" value="${proximo.idEnsayo}"><input type="hidden" name="volver" value="inicio">
          <c:choose>
            <c:when test="${proximo.miConfirmacion == 'SI'}"><span class="tag ok">Confirmado</span><a class="btn btn-secondary btn-sm" href="${ctx}/confirmar-asistencia">Ya no puedo</a></c:when>
            <c:when test="${proximo.miConfirmacion == 'NO'}"><span class="tag neutral">Avisaste que no vas</span><button class="btn btn-secondary btn-sm" name="respuesta" value="SI">Al final sí voy</button></c:when>
            <c:otherwise><button class="btn btn-primary" name="respuesta" value="SI">Sí voy</button><a class="btn btn-secondary" href="${ctx}/confirmar-asistencia">No puedo</a></c:otherwise>
          </c:choose>
        </form>
      </c:otherwise>
    </c:choose>
  </div>

  <%-- Taller del jueves (team Talleres) --%>
  <div class="card taller-prox"><h2>PRÓXIMO TALLER</h2>
    <c:choose>
      <c:when test="${empty proximoTaller}"><p class="muted">Cuando team talleres programe el taller del jueves, aparecerá aquí para que confirmes.</p></c:when>
      <c:otherwise><c:set var="tp" value="${proximoTaller}"/>
        <div class="inline" style="align-items:baseline;gap:10px"><span class="big-date blue">${tp.fechaCorta}</span><b>${tp.titulo}</b></div>
        <ul class="icon-list">
          <li><sdp:icono n="clock"/>${tp.dia} · ${tp.horario}</li>
          <c:if test="${not empty tp.lugar}"><li><sdp:icono n="pin"/><c:out value="${tp.lugar}"/></li></c:if>
          <c:if test="${not empty tp.miPapel}"><li><sdp:icono n="clip"/><b><c:out value="${tp.miPapel}"/></b></li></c:if>
        </ul>
        <form method="post" action="${ctx}/confirmar-asistencia" class="inline"><input type="hidden" name="_csrf" value="${sessionScope.csrf}">
          <input type="hidden" name="idTaller" value="${tp.idTaller}"><input type="hidden" name="volver" value="inicio">
          <c:choose>
            <c:when test="${tp.miConfirmacion == 'SI'}"><span class="tag ok">Confirmado</span><a class="btn btn-secondary btn-sm" href="${ctx}/confirmar-asistencia#talleres">Ya no puedo</a></c:when>
            <c:when test="${tp.miConfirmacion == 'NO'}"><span class="tag neutral">Avisaste que no vas</span><button class="btn btn-secondary btn-sm" name="respuesta" value="SI">Al final sí voy</button></c:when>
            <c:otherwise><button class="btn btn-primary" name="respuesta" value="SI">Sí voy</button><a class="btn btn-secondary" href="${ctx}/confirmar-asistencia#talleres">No puedo</a></c:otherwise>
          </c:choose>
          <span class="muted xs">${tp.confirmadosSi} ${tp.confirmadosSi == 1 ? 'confirmó' : 'confirmaron'}</span>
        </form>
      </c:otherwise>
    </c:choose>
  </div>
</div>

<div class="row top">
  <div class="card grow"><div class="inline" style="justify-content:space-between"><h2>Mis pendientes</h2>
      <c:if test="${not empty pendientes}"><a class="small" href="${ctx}/pendientes">Ver todo</a></c:if></div>
    <c:set var="maxPend" value="${4}"/><%@ include file="/WEB-INF/jsp/includes/pendientes-lista.jspf" %>
  </div>
  <div class="stack w-440">
    <div class="card"><h2>Mi nivel</h2>
      <c:choose>
        <c:when test="${empty yo.nivel}"><div class="inline"><sdp:chip color=""/><b>Sin nivel</b></div>
          <p class="muted small">Haz tu primera autoevaluación para saber tu nivel.</p></c:when>
        <c:otherwise><div class="inline"><sdp:chip color="${yo.nivel}"/><b>Nivel ${yo.nivel}</b></div>
          <p class="muted small"><c:choose>
            <c:when test="${not empty siguienteNivel}">Te faltan ${faltanSuNivel} figuras ${yo.nivel} para subir a ${siguienteNivel}.</c:when>
            <c:otherwise>Ya sabes todas las figuras de los niveles anteriores. ¡A dominar las ROJO!</c:otherwise></c:choose></p></c:otherwise>
      </c:choose>
      <div class="inline"><a class="btn btn-ghost btn-sm" href="${ctx}/mi-progreso">Ver mi progreso</a>
        <c:if test="${not empty porcentaje and porcentaje gt 0}"><span class="muted xs">${porcentaje}% de asistencia</span></c:if></div>
    </div>
    <c:if test="${not empty cumples}">
      <div class="card cumples"><h2>🎂 Cumpleaños de la semana</h2>
        <div class="cumple-lista">
          <c:forEach var="c" items="${cumples}">
            <div class="cumple${c.cumpleHoy ? ' hoy' : ''}"><span class="avatar">${c.iniciales}</span>
              <span><b><c:out value="${c.nombreConApodo}"/></b><small>${c.cumpleHoy ? '¡Hoy! Salúdalo 🎉' : c.cumpleTexto}</small></span></div>
          </c:forEach>
        </div>
      </div>
    </c:if>
  </div>
</div>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
