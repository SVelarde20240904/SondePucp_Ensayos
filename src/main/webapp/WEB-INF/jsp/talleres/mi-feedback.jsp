<c:set var="titulo" value="Mi feedback de talleres"/><c:set var="menu" value="Mi feedback"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<section class="hero"><div class="titles"><span class="kicker">Mi espacio</span><h1>Mi feedback de talleres</h1>
  <p>Lo que te dijeron tus compañeros de dictado y apoyos, resumido por el team Talleres (sin nombres)</p></div><span class="star"></span></section>

<c:forEach var="p" items="${pendientes}">
  <div class="note warn inline" style="justify-content:space-between">Tienes pendiente el formulario del ${p.titulo} (${p.fechaCorta}).
    <a class="btn btn-primary btn-sm" href="${ctx}/talleres/formulario?id=${p.idTaller}">Llenarlo</a></div>
</c:forEach>

<div class="grid-4">
  <div class="card stat"><b class="c-primary">${stats[0]}</b><span>veces de tallerista</span></div>
  <div class="card stat"><b>${stats[1]}</b><span>veces de apoyo</span></div>
  <div class="card stat"><b>${stats[2]}</b><span>veces de maestro(a) de taller</span></div>
  <div class="card stat"><b>${stats[5]}</b><span>veces que cubriste a alguien</span></div>
</div>

<div class="card"><h2>Resúmenes</h2>
  <c:forEach var="r" items="${resumenes}">
    <div class="fb-crudo"><span class="muted xs"><b>${r.tallerTexto}</b></span><p><c:out value="${r.texto}"/></p></div>
  </c:forEach>
  <c:if test="${empty resumenes}"><p class="empty">Todavía no tienes feedback de talleres. Cuando dictes y el team arme tu resumen, aparece aquí.</p></c:if>
</div>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
