<c:set var="titulo" value="Visitas a ruedas"/><c:set var="menu" value="Visitas"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<section class="hero"><div class="titles"><span class="kicker">Todos somos RRPP</span><h1>Visitas a ruedas</h1>
  <p>Las visitas formales de SDP: vamos todos juntos con el polo. Confirma si vas para que RRPP organice cómo llegamos.</p></div>
  <span class="star"></span></section>
<c:choose>
  <c:when test="${empty visitas}"><div class="card"><p class="empty">No hay visitas convocadas por ahora. Cuando RRPP convoque una, te aparece aquí y en Pendientes.</p></div></c:when>
  <c:otherwise>
    <div class="kgrid">
      <c:forEach var="v" items="${visitas}">
        <article class="card visita-card" id="v${v.id}">
          <div class="kmeta"><span class="tag ${v.tono}">${v.estado == 'REALIZADA' ? 'Ya fuimos' : v.cuando}</span><c:if test="${not empty v.rueda}"><span class="tag neutral"><c:out value="${v.rueda}"/></span></c:if></div>
          <h2><c:out value="${v.titulo}"/></h2>
          <ul class="icon-list small">
            <li>📅 ${v.fechaTexto}${empty v.horaTexto ? '' : ' · '.concat(v.horaTexto)}</li>
            <c:if test="${not empty v.lugar}"><li>📍 <span><c:out value="${v.lugar}"/><c:if test="${not empty v.linkMapa}"> · <a class="link" href="${fn:escapeXml(v.linkMapa)}" target="_blank" rel="noopener noreferrer">Mapa ↗</a></c:if></span></li></c:if>
            <c:if test="${not empty v.puntoEncuentro}"><li>🤝 <span>Encuentro: <c:out value="${v.puntoEncuentro}"/>${empty v.horaEncuentroTexto ? '' : ' · '.concat(v.horaEncuentroTexto)}</span></li></c:if>
            <c:if test="${not empty v.comoLlegar}"><li>🚌 <span><c:out value="${v.comoLlegar}"/></span></li></c:if>
            <c:if test="${not empty v.costo}"><li>🎟️ <c:out value="${v.costo}"/></li></c:if>
            <c:if test="${not empty v.dressCode}"><li>👕 <c:out value="${v.dressCode}"/></li></c:if>
          </ul>
          <c:if test="${not empty v.notas}"><p class="small pre"><c:out value="${v.notas}"/></p></c:if>
          <p class="small"><b class="c-verde">${v.van} van</b><c:if test="${not empty respuestas[v.id].VOY}">: <c:forEach var="n" items="${respuestas[v.id].VOY}" varStatus="st"><c:out value="${n}"/>${st.last ? '' : ', '}</c:forEach></c:if></p>
          <c:if test="${v.puedeResponder}">
            <form method="post" action="${ctx}/visitas" class="inline"><input type="hidden" name="_csrf" value="${sessionScope.csrf}"><input type="hidden" name="id" value="${v.id}">
              <button class="btn btn-sm ${v.miRespuesta == 'VOY' ? 'btn-primary' : 'btn-secondary'}" name="respuesta" value="VOY">${v.miRespuesta == 'VOY' ? '✓ Voy' : 'Voy'}</button>
              <button class="btn btn-sm ${v.miRespuesta == 'NO_PUEDO' ? 'btn-primary' : 'btn-ghost'}" name="respuesta" value="NO_PUEDO">${v.miRespuesta == 'NO_PUEDO' ? '✓ No puedo' : 'No puedo'}</button></form>
          </c:if>
        </article>
      </c:forEach>
    </div>
  </c:otherwise>
</c:choose>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
