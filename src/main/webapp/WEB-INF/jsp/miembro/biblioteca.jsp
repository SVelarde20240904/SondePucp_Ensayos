<c:set var="titulo" value="Biblioteca de figuras"/><c:set var="menu" value="Biblioteca"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<section class="hero"><div class="titles"><span class="kicker">Mi espacio</span><h1>Biblioteca de figuras</h1>
  <p>Las ${total} figuras de la mano, de la más básica a la más avanzada · ${conVideo} con video</p></div><span class="star"></span></section>

<div class="card">
  <form class="inline" method="get" action="${ctx}/biblioteca">
    <input class="input" name="q" value="<c:out value='${param.q}'/>" placeholder="Buscar figura…" style="max-width:280px">
    <c:if test="${param.video == '1'}"><input type="hidden" name="video" value="1"></c:if>
    <button class="btn btn-secondary btn-sm">Buscar</button>
    <c:url var="uVid" value="/biblioteca"><c:param name="q" value="${param.q}"/><c:if test="${param.video != '1'}"><c:param name="video" value="1"/></c:if></c:url>
    <a class="pill-link ${param.video == '1' ? 'on' : ''}" href="${uVid}">▶ Solo con video</a>
  </form>
  <p class="muted xs">El nivel de la figura sale del grupo (Azul = la sabe casi todo el grupo → Rojo = avanzada). Al lado, tu color.</p>
</div>

<c:forEach var="e" items="${porNivel}">
  <div class="card">
    <div class="inline"><c:choose><c:when test="${e.key == 'SIN'}"><span class="chip sin">Figuras nuevas · aún sin nivel</span></c:when><c:otherwise><sdp:chip color="${e.key}"/></c:otherwise></c:choose>
      <span class="muted small">${fn:length(e.value)} figura(s)</span></div>
    <div class="fig-grid">
      <c:forEach var="f" items="${e.value}">
        <a class="fig-ficha fig-link" href="${ctx}/biblioteca/figura?id=${f.idFigura}">
          <span>${f.tieneVideo ? '▶ ' : ''}<c:out value="${f.nombre}"/></span>
          <c:choose><c:when test="${empty miColor[f.idFigura]}"><span class="chip sin">Sin evaluar</span></c:when><c:otherwise><sdp:chip color="${miColor[f.idFigura]}"/></c:otherwise></c:choose>
        </a>
      </c:forEach>
    </div>
  </div>
</c:forEach>
<c:if test="${empty porNivel}"><div class="card"><p class="empty">No encontré figuras con esa búsqueda.</p></div></c:if>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
