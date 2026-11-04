<c:set var="titulo" value="Talleres"/><c:set var="menu" value="Talleres"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<section class="hero"><div class="titles"><span class="kicker">Team Talleres</span><h1>Talleres</h1>
  <p>Cronograma de cada taller, quién dictó, qué pasos se enseñaron y el feedback de talleristas y apoyos</p></div>
  <div class="actions"><a class="btn btn-primary" href="${ctx}/talleres/nuevo">+ Nuevo taller</a></div><span class="star"></span></section>

<c:if test="${total == 0}"><div class="card"><p class="empty">Todavía no hay talleres. Crea el primero con “+ Nuevo taller”.</p></div></c:if>
<c:forEach var="e" items="${porCiclo}">
  <div class="card"><h2>Ciclo ${e.key}</h2>
    <table class="table cards-compact"><thead><tr><th>Taller</th><th>Estado</th><th>Formularios</th><th>Feedback</th><th></th></tr></thead><tbody>
    <c:forEach var="t" items="${e.value}">
      <tr><td><a href="${ctx}/talleres/detalle?id=${t.idTaller}"><b>${t.titulo}</b></a><small>${t.fechaTexto}</small></td>
        <td><sdp:tag texto="${t.estadoTexto}" tono="${t.estadoTono}"/><c:if test="${t.finalRegistrado}"> <span class="tag ok">Final ✓</span></c:if></td>
        <td>${t.esperados == 0 ? '—' : t.respondieron.toString().concat('/').concat(t.esperados.toString())}</td>
        <td>${t.feedbacks == 0 ? '—' : t.feedbacks.toString().concat(' · resúmenes ').concat(t.resumenesEnviados.toString()).concat('/').concat(t.receptores.toString())}</td>
        <td class="no-label"><a class="btn btn-ghost btn-sm" href="${ctx}/talleres/detalle?id=${t.idTaller}">Ver</a></td></tr>
    </c:forEach>
    </tbody></table>
  </div>
</c:forEach>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
