<c:set var="titulo" value="Mis responsabilidades"/><c:set var="menu" value="Mis responsabilidades"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<section class="hero"><div class="titles"><span class="kicker">Mi espacio</span><h1>Mis responsabilidades</h1>
  <p>Los encargos que te asignaron y los que cubriste. El estado se calcula solo con la fecha en que se cumplió.</p></div><span class="star"></span></section>

<c:set var="pend" value="0"/><c:set var="ok" value="0"/><c:set var="mal" value="0"/><c:set var="cub" value="0"/>
<c:forEach var="e" items="${lista}">
  <c:if test="${e.idResponsable == u.idMiembro and e.estado == 'PENDIENTE'}"><c:set var="pend" value="${pend + 1}"/></c:if>
  <c:if test="${e.idResponsable == u.idMiembro and e.estado == 'A_TIEMPO'}"><c:set var="ok" value="${ok + 1}"/></c:if>
  <c:if test="${e.idResponsable == u.idMiembro and e.estado == 'NO_CUMPLIDA' and not e.justificada}"><c:set var="mal" value="${mal + 1}"/></c:if>
  <c:if test="${e.idResponsable != u.idMiembro}"><c:set var="cub" value="${cub + 1}"/></c:if>
</c:forEach>
<div class="grid-4">
  <div class="card stat"><b class="c-primary">${pend}</b><span>pendientes</span></div>
  <div class="card stat"><b class="c-verde">${ok}</b><span>cumplidas a tiempo</span></div>
  <div class="card stat"><b>${mal}</b><span>no cumplidas</span></div>
  <div class="card stat"><b>${cub}</b><span>coberturas que hiciste</span></div>
</div>

<div class="card"><h2>Mis encargos</h2><p>Si no vas a poder con uno, avísale al equipo de ensayos para que alguien te cubra.</p>
  <table class="table cards-compact"><thead><tr><th>Semana</th><th>Encargo</th><th>Plazo</th><th>Cumplido por</th><th>Estado</th></tr></thead><tbody>
  <c:forEach var="e" items="${lista}">
    <tr><td>${e.semanaTexto}</td>
      <td><c:out value="${e.tipo}"/><c:if test="${e.idResponsable != u.idMiembro}"><small>Cubriste a <c:out value="${e.responsable}"/></small></c:if></td>
      <td>${e.plazoTexto}</td>
      <td><c:choose><c:when test="${not empty e.cumplidoPor}"><c:out value="${e.cumplidoPor}"/><small>${e.cumplimientoTexto}</small></c:when><c:otherwise>—</c:otherwise></c:choose></td>
      <td><sdp:tag texto="${e.estadoTexto}" tono="${e.estadoTono}"/><c:if test="${e.cobertura and e.idResponsable == u.idMiembro}"> <span class="tag info">Te cubrieron</span></c:if><c:if test="${e.justificada}"> <span class="tag neutral">Justificada</span></c:if></td></tr>
  </c:forEach>
  <c:if test="${empty lista}"><tr><td colspan="5" class="empty">Aún no tienes responsabilidades asignadas.</td></tr></c:if>
  </tbody></table>
</div>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
