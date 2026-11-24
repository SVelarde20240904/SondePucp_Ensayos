<c:set var="titulo" value="RRPP"/><c:set var="menu" value="RRPP"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<section class="hero"><div class="titles"><span class="kicker">Team RRPP</span><h1>Relaciones públicas</h1>
  <p>Que SDP vuelva a dejarse ver entre las ruedas de Lima: externos del jueves, visitas formales y los contactos de todos.</p></div>
  <div class="actions"><a class="btn btn-primary" href="${ctx}/rrpp/visita">+ Organizar visita</a><a class="btn btn-secondary" href="${ctx}/rrpp/contacto">+ Contacto</a></div>
  <span class="star"></span></section>
<c:set var="grupo" value="rrpp"/><c:set var="sub" value="resumen"/><%@ include file="/WEB-INF/jsp/includes/subtabs.jspf" %>

<div class="grid-4">
  <div class="card stat"><b class="${visitasHechas ge meta ? 'c-verde' : 'c-primary'}">${visitasHechas} / ${meta}</b><span>visitas formales en ${mesTexto}${visitasConvocadas gt 0 ? ' · '.concat(visitasConvocadas).concat(' convocada(s)') : ''}</span></div>
  <div class="card stat"><b>${dir.htContactados} / ${dir.htTotal}</b><span>H's y T's contactados (ciclo ${ciclo})</span></div>
  <div class="card stat"><b class="${dir.ruedasOlvidadas gt 0 ? 'c-rojo' : 'c-verde'}">${dir.ruedasOlvidadas}</b><span>ruedas o academias sin contacto hace +60 días</span></div>
  <div class="card stat"><b>${fn:length(encuestas)}</b><span>encuestas abiertas</span></div>
</div>

<div class="grid-2">
  <div class="card"><h2>🎟️ Externos del jueves ${sem.juevesCorto}</h2>
    <%@ include file="/WEB-INF/jsp/rrpp/linea-externos.jspf" %>
    <a class="btn btn-secondary btn-sm" href="${ctx}/rrpp/externos">Gestionar externos →</a>
  </div>
  <div class="card"><h2>🚌 Próxima visita formal</h2>
    <c:choose>
      <c:when test="${empty proxima}"><p class="empty">No hay visitas programadas. La meta es ${meta} al mes.</p>
        <c:if test="${perm.contains('CREAR_ENCUESTAS')}"><a class="btn btn-secondary btn-sm" href="${ctx}/encuestas/nueva">Preguntar al grupo con una encuesta</a></c:if></c:when>
      <c:otherwise>
        <p><b><c:out value="${proxima.titulo}"/></b> · ${proxima.fechaTexto}${empty proxima.horaTexto ? '' : ' · '.concat(proxima.horaTexto)}</p>
        <p class="small"><span class="tag ${proxima.tono}">${proxima.estadoTexto}</span> ${proxima.van} van · ${proxima.noPueden} no pueden · checklist ${proxima.checkHechos}/5</p>
        <a class="btn btn-secondary btn-sm" href="${ctx}/rrpp/visita?id=${proxima.id}">Abrir →</a>
      </c:otherwise>
    </c:choose>
  </div>
  <div class="card"><h2>📇 Directorio</h2>
    <ul class="icon-list">
      <c:forEach var="k" items="${dir.categorias}"><li><a class="link" href="${ctx}/rrpp/directorio?cat=${k.codigo}">${k.texto}</a> <span class="muted">· ${k.n}</span></li></c:forEach>
    </ul>
  </div>
  <div class="card"><h2>📊 Encuestas abiertas</h2>
    <c:choose>
      <c:when test="${empty encuestas}"><p class="empty">No hay encuestas abiertas.</p></c:when>
      <c:otherwise><ul class="icon-list"><c:forEach var="e" items="${encuestas}"><li><a class="link" href="${ctx}/encuestas/ver?id=${e.id}"><c:out value="${e.pregunta}"/></a> <span class="muted">· ${e.votantes} votos</span></li></c:forEach></ul></c:otherwise>
    </c:choose>
    <c:if test="${perm.contains('CREAR_ENCUESTAS')}"><a class="btn btn-ghost btn-sm" href="${ctx}/encuestas/nueva">+ Nueva encuesta</a></c:if>
  </div>
</div>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
