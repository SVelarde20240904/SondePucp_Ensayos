<c:set var="titulo" value="Archivo"/><c:set var="menu" value="Comunica"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<section class="hero"><div class="titles"><span class="kicker">Team Comunica</span><h1>Archivo</h1>
  <p>Todo lo que ya salió (y lo archivado), con su link, para reutilizar ideas y no repetir contenido.</p></div>
  <span class="star"></span></section>
<c:set var="grupo" value="comunica"/><c:set var="sub" value="archivo"/><%@ include file="/WEB-INF/jsp/includes/subtabs.jspf" %>

<c:if test="${not empty etiquetas}">
  <nav class="inline ktags filtro" aria-label="Filtrar por etiqueta">
    <a class="pill-link${empty tag ? ' on' : ''}" href="${ctx}/comunica/archivo">Todo</a>
    <c:forEach var="t" items="${etiquetas}"><a class="pill-link${t == tag ? ' on' : ''}" href="${ctx}/comunica/archivo?tag=${fn:escapeXml(t)}">#<c:out value="${t}"/></a></c:forEach>
  </nav>
</c:if>
<div class="card">
  <c:choose>
    <c:when test="${empty proyectos}"><p class="empty">${empty tag ? 'Todavía no hay proyectos publicados o archivados.' : 'Nada con esa etiqueta.'}</p></c:when>
    <c:otherwise>
      <div class="scroll-x"><table class="table"><thead><tr><th>Publicado</th><th>Proyecto</th><th>Tipo</th><th>Responsable</th><th></th></tr></thead><tbody>
        <c:forEach var="p" items="${proyectos}">
          <tr><td>${empty p.fechaPublicacionTexto ? '—' : p.fechaPublicacionTexto}</td>
            <td><a class="link" href="${ctx}/comunica/proyecto?id=${p.id}"><b><c:out value="${p.titulo}"/></b></a>
              <c:if test="${not empty p.tags}"><small><c:forEach var="t" items="${p.tags}">#<c:out value="${t}"/> </c:forEach></small></c:if></td>
            <td><span class="tag info">${p.tipoIcono} ${p.tipoTexto}</span> <span class="muted xs">${p.redTexto}</span>
              <c:if test="${p.estado == 'ARCHIVADO'}"><span class="tag neutral">Archivado</span></c:if></td>
            <td><c:out value="${empty p.responsable ? '—' : p.responsable}"/></td>
            <td class="r"><c:if test="${not empty p.linkPublicacion}"><a class="btn btn-ghost btn-sm" href="${fn:escapeXml(p.linkPublicacion)}" target="_blank" rel="noopener noreferrer">Ver ↗</a></c:if></td></tr>
        </c:forEach>
      </tbody></table></div>
    </c:otherwise>
  </c:choose>
</div>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
