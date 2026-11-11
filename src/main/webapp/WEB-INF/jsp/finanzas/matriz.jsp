<c:set var="titulo" value="Matriz de cuotas"/><c:set var="menu" value="Cuotas"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<section class="hero"><div class="titles"><span class="kicker">Team Finanzas</span><h1>Matriz de cuotas</h1>
  <p>Cada fila es un miembro y cada columna un mes. Toca un nombre para ver su cuenta.</p></div><span class="star"></span></section>
<c:set var="grupo" value="finanzas"/><c:set var="sub" value="matriz"/><%@ include file="/WEB-INF/jsp/includes/subtabs.jspf" %>
<div class="inline"><a class="pill-link ${mios ? '' : 'on'}" href="${ctx}/finanzas/matriz">Todos</a><a class="pill-link ${mios ? 'on' : ''}" href="${ctx}/finanzas/matriz?mios=1">Mis cobrados</a></div>
<div class="inline small"><span class="tag ok">Pagada</span><span class="tag info">En revisión</span><span class="tag neutral">Pendiente / exonerada</span><span class="tag warn">Vencida (S/ 7.50)</span><span class="tag bad">Deuda (S/ 9)</span><span class="muted">— no le tocaba</span></div>
<div class="card"><div class="tabla-scroll">
  <table class="table no-cards matriz-cuotas"><thead><tr><th>Miembro</th><th>Cobra</th>
    <c:forEach var="t" items="${titulos}" varStatus="st"><th class="center"><a href="${ctx}/finanzas/mes?m=${claves[st.index]}">${t}</a></th></c:forEach><th class="right">Debe</th></tr></thead><tbody>
    <c:forEach var="e" items="${filas}">
      <tr><td><a href="${ctx}/finanzas/miembro?id=${e.key}"><b><c:out value="${nombres[e.key]}"/></b></a></td><td class="small muted">${cobradorDe[e.key]}</td>
        <c:forEach var="k" items="${claves}"><c:set var="c" value="${e.value[k]}"/>
          <td class="center"><c:choose><c:when test="${empty c}"><span class="muted">—</span></c:when>
            <c:otherwise><span class="tag ${c.tono}" title="${c.situacionTexto}${empty c.nota ? '' : ' · '.concat(c.nota)}">${c.exonerada ? 'EXO' : (c.pagada and c.montoHoy == 0 ? '✓' : 'S/ '.concat(c.montoTexto))}</span></c:otherwise></c:choose></td>
        </c:forEach>
        <td class="right">${empty deudaDe[e.key] ? '—' : 'S/ '.concat(deudaDe[e.key])}</td></tr>
    </c:forEach>
    <c:if test="${empty filas}"><tr><td class="empty">Todavía no hay cuotas.</td></tr></c:if>
  </tbody></table></div></div>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
