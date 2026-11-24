<c:set var="titulo" value="Encuesta"/><c:set var="menu" value="Encuestas"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<section class="hero"><div class="titles"><span class="kicker">Encuesta${e.abierta ? '' : ' · cerrada'}${e.anonima ? ' · anónima' : ''}</span>
  <h1 class="h1-pregunta"><c:out value="${e.pregunta}"/></h1>
  <p>${e.abierta ? 'Cierra el ' : 'Cerró el '}${e.cierreTexto} · por <c:out value="${empty e.creador ? '—' : e.creador}"/><c:if test="${not empty e.team}"> · solo team <c:out value="${e.team}"/></c:if></p></div>
  <div class="actions"><a class="btn btn-secondary" href="${ctx}/encuestas">← Encuestas</a></div>
  <span class="star"></span></section>

<div class="row top">
  <div class="card grow">
    <c:if test="${not empty e.descripcion}"><p class="pre"><c:out value="${e.descripcion}"/></p></c:if>
    <c:choose>
      <c:when test="${e.abierta and (not e.yaVote or editarVoto)}">
        <form method="post" action="${ctx}/encuestas/ver" class="stack"><input type="hidden" name="_csrf" value="${sessionScope.csrf}"><input type="hidden" name="id" value="${e.id}">
          <p class="muted small">${e.multiple ? (e.tipo == 'FECHAS' ? 'Marca todas las fechas que puedes.' : 'Puedes marcar varias.') : 'Elige una.'}</p>
          <c:forEach var="o" items="${e.opciones}">
            <label class="check-item"><input type="${e.multiple ? 'checkbox' : 'radio'}" name="opcion" value="${o.id}" ${o.mio ? 'checked' : ''}> <c:out value="${o.texto}"/></label>
          </c:forEach>
          <div><button class="btn btn-primary" name="action" value="votar">${e.yaVote ? 'Cambiar mi voto' : 'Votar'}</button></div>
        </form>
      </c:when>
      <c:when test="${e.abierta and e.yaVote}">
        <p class="note ok">✓ Ya votaste. <a class="link" href="${ctx}/encuestas/ver?id=${e.id}&cambiar=1">Cambiar mi voto</a></p>
      </c:when>
    </c:choose>

    <c:choose>
      <c:when test="${verResultados}">
        <h2 class="mt">Resultados</h2>
        <p class="muted small">${e.votantes} de ${e.publico} votaron (${e.participacion}%)</p>
        <div class="resultados">
          <c:forEach var="o" items="${e.opciones}">
            <div class="res${o.gana and not e.abierta ? ' gana' : ''}">
              <div class="res-top"><span><c:out value="${o.texto}"/><c:if test="${o.mio}"> <span class="tag info">tu voto</span></c:if></span><b>${o.votos} · ${o.pct}%</b></div>
              <div class="res-barra"><i style="width:${o.pct}%"></i></div>
              <c:if test="${not e.anonima and not empty o.nombres}"><small class="muted"><c:forEach var="n" items="${o.nombres}" varStatus="st"><c:out value="${n}"/>${st.last ? '' : ', '}</c:forEach></small></c:if>
            </div>
          </c:forEach>
        </div>
      </c:when>
      <c:otherwise><p class="muted small mt">Los resultados se ven después de votar.</p></c:otherwise>
    </c:choose>
  </div>
  <div class="stack w-300">
    <c:if test="${perm.contains('GESTIONAR_RRPP')}">
      <div class="card"><h2>¿Es para una visita?</h2><p class="muted small">Si ganó el «sí», organízala como visita formal: se convoca a todos y cada uno confirma.</p>
        <a class="btn btn-secondary btn-sm" href="${ctx}/rrpp/visita?encuesta=${e.id}">Organizar visita formal →</a></div>
    </c:if>
    <c:if test="${duenio}">
      <form class="card" method="post" action="${ctx}/encuestas/ver"><input type="hidden" name="_csrf" value="${sessionScope.csrf}"><input type="hidden" name="id" value="${e.id}">
        <h2>Administrar</h2>
        <div class="inline"><c:if test="${e.abierta}"><button class="btn btn-secondary btn-sm" name="action" value="cerrar">Cerrar ya</button></c:if>
          <button class="btn btn-ghost btn-sm" name="action" value="eliminar" onclick="return confirm('¿Eliminar la encuesta y todos sus votos?')">Eliminar</button></div></form>
    </c:if>
  </div>
</div>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
