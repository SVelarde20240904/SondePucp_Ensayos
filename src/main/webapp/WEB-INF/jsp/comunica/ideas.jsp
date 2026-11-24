<c:set var="titulo" value="Banco de ideas"/><c:set var="menu" value="Comunica"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<section class="hero"><div class="titles"><span class="kicker">Team Comunica</span><h1>Banco de ideas</h1>
  <p>TikToks que quieren grabar, videos de referencia para inspirarse y propuestas de cualquier miembro. Voten las mejores y pásenlas a desarrollo.</p></div>
  <div class="actions"><a class="btn btn-primary" href="${ctx}/comunica/proyecto?estado=IDEA">+ Guardar idea</a></div>
  <span class="star"></span></section>
<c:set var="grupo" value="comunica"/><c:set var="sub" value="ideas"/><%@ include file="/WEB-INF/jsp/includes/subtabs.jspf" %>

<c:if test="${not empty etiquetas}">
  <nav class="inline ktags filtro" aria-label="Filtrar por etiqueta">
    <a class="pill-link${empty tag ? ' on' : ''}" href="${ctx}/comunica/ideas">Todas</a>
    <c:forEach var="t" items="${etiquetas}"><a class="pill-link${t == tag ? ' on' : ''}" href="${ctx}/comunica/ideas?tag=${fn:escapeXml(t)}">#<c:out value="${t}"/></a></c:forEach>
  </nav>
</c:if>

<div class="kgrid">
  <c:forEach var="p" items="${ideas}"><%@ include file="/WEB-INF/jsp/comunica/tarjeta.jspf" %></c:forEach>
</div>
<c:if test="${empty ideas}"><div class="card"><p class="empty">${empty tag ? 'Todavía no hay ideas. Guarda la primera: un TikTok que viste, un meme, un tutorial…' : 'No hay ideas con esa etiqueta.'}</p></div></c:if>
<p class="muted small">Cualquier miembro de SDP puede proponer ideas desde <b>Comunica → Proponer una idea</b> en el menú.</p>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
