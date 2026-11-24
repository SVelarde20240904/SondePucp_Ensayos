<c:set var="titulo" value="Encuestas"/><c:set var="menu" value="Encuestas"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<section class="hero"><div class="titles"><span class="kicker">Son de PUCP</span><h1>Encuestas</h1>
  <p>Vota en lo que decide el grupo. Puedes cambiar tu voto hasta que cierre.</p></div>
  <c:if test="${perm.contains('CREAR_ENCUESTAS')}"><div class="actions"><a class="btn btn-primary" href="${ctx}/encuestas/nueva">+ Nueva encuesta</a></div></c:if>
  <span class="star"></span></section>

<div class="card"><h2>Abiertas</h2>
  <c:choose>
    <c:when test="${empty abiertas}"><p class="empty">No hay encuestas abiertas para ti.</p></c:when>
    <c:otherwise><ul class="pend-lista">
      <c:forEach var="e" items="${abiertas}">
        <li class="pend ${e.yaVote ? 'ok' : 'info'}"><span class="pend-ico" aria-hidden="true">${e.yaVote ? '✅' : '📊'}</span>
          <span class="pend-txt"><b><c:out value="${e.pregunta}"/></b>
            <small>Cierra el ${e.cierreTexto} · ${e.votantes} de ${e.publico} votaron<c:if test="${not empty e.team}"> · Solo team <c:out value="${e.team}"/></c:if><c:if test="${e.anonima}"> · Anónima</c:if></small></span>
          <a class="btn ${e.yaVote ? 'btn-ghost' : 'btn-primary'} btn-sm" href="${ctx}/encuestas/ver?id=${e.id}">${e.yaVote ? 'Ver' : 'Votar'}</a></li>
      </c:forEach></ul></c:otherwise>
  </c:choose>
</div>
<div class="card"><h2>Cerradas</h2>
  <c:choose>
    <c:when test="${empty cerradas}"><p class="empty">Todavía no hay encuestas cerradas.</p></c:when>
    <c:otherwise><ul class="pend-lista">
      <c:forEach var="e" items="${cerradas}">
        <li class="pend neutral"><span class="pend-ico" aria-hidden="true">🗳️</span>
          <span class="pend-txt"><b><c:out value="${e.pregunta}"/></b><small>Cerró el ${e.cierreTexto} · ${e.votantes} votos</small></span>
          <a class="btn btn-ghost btn-sm" href="${ctx}/encuestas/ver?id=${e.id}">Resultados</a></li>
      </c:forEach></ul></c:otherwise>
  </c:choose>
</div>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
