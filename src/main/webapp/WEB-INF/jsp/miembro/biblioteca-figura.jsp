<c:set var="titulo" value="${f.nombre}"/><c:set var="menu" value="Biblioteca"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<section class="hero"><div class="titles"><span class="kicker">Biblioteca de figuras</span><h1><c:out value="${f.nombre}"/></h1>
  <p>Figura ${posicion} de ${totalMano} de la mano<c:if test="${not empty anterior}"> · va después de <c:out value="${anterior.nombre}"/></c:if></p></div>
  <div class="actions"><a class="btn btn-secondary" href="${ctx}/biblioteca">← Todas las figuras</a>
    <c:if test="${perm.contains('GESTIONAR_FIGURAS')}"><a class="btn btn-primary" href="${ctx}/admin/figuras/editar?id=${f.idFigura}">Editar</a></c:if></div>
  <span class="star"></span></section>

<div class="row top">
  <div class="card grow"><h2>Video</h2>
    <c:choose>
      <c:when test="${not empty f.videoEmbed}">
        <div class="video-box"><iframe src="${f.videoEmbed}" title="Video de <c:out value='${f.nombre}'/>" allow="autoplay; fullscreen; picture-in-picture" allowfullscreen loading="lazy" referrerpolicy="strict-origin-when-cross-origin"></iframe></div>
        <p class="muted xs">¿No carga o dice que necesitas permiso? Abre tu cuenta de Google con el correo que está en el grupo de SDP, o ábrelo directo:
          <a href="<c:out value='${f.videoUrl}'/>" target="_blank" rel="noopener">abrir en ${f.videoOrigen} ↗</a></p>
      </c:when>
      <c:when test="${f.tieneVideo}"><p><a class="btn btn-primary" href="<c:out value='${f.videoUrl}'/>" target="_blank" rel="noopener">▶ Ver video ↗</a></p></c:when>
      <c:otherwise><p class="empty">Esta figura todavía no tiene video.<c:if test="${perm.contains('GESTIONAR_FIGURAS')}"> Agrégalo desde <a href="${ctx}/admin/figuras/editar?id=${f.idFigura}">Editar</a>.</c:if></p></c:otherwise>
    </c:choose>
    <c:if test="${not empty f.descripcion}"><h2>Descripción</h2><p class="pre"><c:out value="${f.descripcion}"/></p></c:if>
  </div>
  <div class="stack w-340">
    <div class="card"><h2>Nivel</h2>
      <div class="inline"><span class="muted small" style="width:110px">De la figura</span><sdp:chip color="${f.color}"/></div>
      <div class="inline"><span class="muted small" style="width:110px">Tu color</span><c:choose><c:when test="${empty miColor[f.idFigura]}"><span class="chip sin">Sin evaluar</span></c:when><c:otherwise><sdp:chip color="${miColor[f.idFigura]}"/></c:otherwise></c:choose></div>
      <p class="muted xs">${f.porcentajeSaben}% del grupo la sabe (Verde o Azul).</p>
    </div>
    <div class="card"><h2>En la mano</h2>
      <c:if test="${not empty anterior}"><a class="fig-ficha fig-link" href="${ctx}/biblioteca/figura?id=${anterior.idFigura}"><span>← <c:out value="${anterior.nombre}"/></span><span class="muted xs">anterior</span></a></c:if>
      <c:if test="${not empty siguiente}"><a class="fig-ficha fig-link" href="${ctx}/biblioteca/figura?id=${siguiente.idFigura}"><span><c:out value="${siguiente.nombre}"/> →</span><span class="muted xs">siguiente</span></a></c:if>
    </div>
  </div>
</div>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
