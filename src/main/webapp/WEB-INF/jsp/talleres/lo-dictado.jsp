<c:set var="titulo" value="Lo que se dictó"/><c:set var="menu" value="Talleres"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<form method="post" action="${ctx}/talleres/lo-dictado" class="stack" style="gap:24px"><input type="hidden" name="_csrf" value="${sessionScope.csrf}">
<input type="hidden" name="id" value="${t.idTaller}">
<section class="hero"><div class="titles"><span class="kicker">Team Talleres · ${t.titulo} · ${t.fechaCorta}</span><h1>¿Qué se dictó al final?</h1>
  <p>${t.hayDictado ? 'Corrige lo que registraste.' : 'Parte del plan: quita lo que no se dictó, agrega lo que se sumó y ajusta las horas.'} El plan original se guarda aparte para comparar.</p></div>
  <div class="actions"><a class="btn btn-secondary" href="${ctx}/talleres/detalle?id=${t.idTaller}">Cancelar</a><button class="btn btn-primary">Guardar lo dictado</button></div>
  <span class="star"></span></section>

<c:forEach var="n" items="${t.niveles}">
  <div class="card"><h2>${n == 'PRINCIPIANTE' ? 'Principiante' : (n == 'BASICO' ? 'Básico' : (n == 'INTRO_RUEDA' ? 'Intro a la rueda' : 'Rueda básica'))}</h2>
    <c:if test="${not empty t.checklistPor[n]}">
      <div class="note info small">Según el checklist de <c:out value="${t.checklistPor[n]}"/>:
        <c:forEach var="p" items="${t.pasos[n]}" varStatus="s">${p.marca} <c:out value="${p.nombre}"/><c:if test="${not p.planificado}"> (agregado)</c:if>${s.last ? '' : ' · '}</c:forEach></div>
    </c:if>
    <%@ include file="/WEB-INF/jsp/includes/crono-editor.jspf" %>
  </div>
</c:forEach>
<div class="inline" style="justify-content:flex-end"><button class="btn btn-primary">Guardar lo dictado</button></div>
</form>
<script src="${ctx}/js/cronograma.js"></script>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
