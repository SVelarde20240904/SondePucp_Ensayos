<c:set var="titulo" value="Visitas formales"/><c:set var="menu" value="RRPP"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<section class="hero"><div class="titles"><span class="kicker">Team RRPP</span><h1>Visitas formales</h1>
  <p>Cuando vamos tooodos juntos a otra rueda con el polo: se convoca, cada uno confirma y RRPP lleva el checklist.</p></div>
  <div class="actions"><a class="btn btn-primary" href="${ctx}/rrpp/visita">+ Organizar visita</a></div>
  <span class="star"></span></section>
<c:set var="grupo" value="rrpp"/><c:set var="sub" value="visitas"/><%@ include file="/WEB-INF/jsp/includes/subtabs.jspf" %>
<p class="note ${visitasHechas ge meta ? 'ok' : 'info'}">Este mes: ${visitasHechas} de ${meta} visita(s) formal(es) realizada(s).</p>
<div class="card"><h2>Próximas</h2>
  <c:choose>
    <c:when test="${empty proximas}"><p class="empty">No hay visitas programadas. Pregunten al grupo con una encuesta y conviértanla en visita.</p></c:when>
    <c:otherwise><ul class="pend-lista">
      <c:forEach var="v" items="${proximas}">
        <li class="pend ${v.tono}"><span class="pend-ico" aria-hidden="true">🚌</span>
          <span class="pend-txt"><b><c:out value="${v.titulo}"/></b><small>${v.fechaTexto}${empty v.horaTexto ? '' : ' · '.concat(v.horaTexto)} · ${v.cuando} · <span class="tag ${v.tono}">${v.estadoTexto}</span> · ${v.van} van · checklist ${v.checkHechos}/5</small></span>
          <a class="btn btn-secondary btn-sm" href="${ctx}/rrpp/visita?id=${v.id}">Abrir</a></li>
      </c:forEach></ul></c:otherwise>
  </c:choose>
</div>
<div class="card"><h2>Pasadas</h2>
  <c:choose>
    <c:when test="${empty pasadas}"><p class="empty">Todavía no hay visitas pasadas.</p></c:when>
    <c:otherwise><div class="scroll-x"><table class="table"><thead><tr><th>Fecha</th><th>Visita</th><th>Estado</th><th class="r">Fueron</th><th></th></tr></thead><tbody>
      <c:forEach var="v" items="${pasadas}"><tr><td>${v.fechaTexto}</td><td><b><c:out value="${v.titulo}"/></b></td><td><span class="tag ${v.tono}">${v.estadoTexto}</span></td>
        <td class="r">${v.van}</td><td class="r"><a class="btn btn-ghost btn-sm" href="${ctx}/rrpp/visita?id=${v.id}">Ver</a></td></tr></c:forEach>
    </tbody></table></div></c:otherwise>
  </c:choose>
</div>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
