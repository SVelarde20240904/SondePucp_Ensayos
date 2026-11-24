<c:set var="titulo" value="Directorio"/><c:set var="menu" value="RRPP"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<section class="hero"><div class="titles"><span class="kicker">Team RRPP · privado</span><h1>Directorio</h1>
  <p>Ruedas, academias, centros federados, H's y T's y autoridades: con quién hablar, cómo y cuándo fue la última vez.</p></div>
  <div class="actions"><a class="btn btn-primary" href="${ctx}/rrpp/contacto${empty cat ? '' : '?cat='.concat(cat)}">+ Contacto</a></div>
  <span class="star"></span></section>
<c:set var="grupo" value="rrpp"/><c:set var="sub" value="directorio"/><%@ include file="/WEB-INF/jsp/includes/subtabs.jspf" %>

<nav class="inline" aria-label="Categorías">
  <a class="pill-link${empty cat ? ' on' : ''}" href="${ctx}/rrpp/directorio">Todos</a>
  <c:forEach var="k" items="${dir.categorias}"><a class="pill-link${cat == k.codigo ? ' on' : ''}" href="${ctx}/rrpp/directorio?cat=${k.codigo}">${k.texto} · ${k.n}</a></c:forEach>
</nav>
<form class="inline" method="get" action="${ctx}/rrpp/directorio">
  <c:if test="${not empty cat}"><input type="hidden" name="cat" value="${cat}"></c:if>
  <input class="input" name="q" value="${fn:escapeXml(q)}" placeholder="Buscar por nombre, persona, Instagram o notas" aria-label="Buscar">
  <c:if test="${cat == 'H_T'}"><input class="input input-sm" name="ciclo" value="${fn:escapeXml(cicloFiltro)}" placeholder="Ciclo (${cicloActual})" aria-label="Ciclo"></c:if>
  <button class="btn btn-secondary btn-sm">Buscar</button>
</form>
<c:if test="${cat == 'H_T'}"><p class="note info">Ciclo ${cicloActual}: ${dir.htContactados} de ${dir.htTotal} H's y T's contactados. La idea: invitar a sus cachimbos al taller del jueves.</p></c:if>

<div class="card">
  <c:choose>
    <c:when test="${empty contactos}"><p class="empty">${empty q ? 'Todavía no hay contactos aquí. Agrega el primero.' : 'Nada con esa búsqueda.'}</p></c:when>
    <c:otherwise>
      <div class="scroll-x"><table class="table"><thead><tr><th>Contacto</th><th>Persona</th><th>Cómo contactar</th><th>Estado</th><th>Último contacto</th></tr></thead><tbody>
        <c:forEach var="k" items="${contactos}">
          <tr><td><a class="link" href="${ctx}/rrpp/contacto?id=${k.id}"><b><c:out value="${k.nombre}"/></b></a>
                <small>${k.categoriaTexto}<c:if test="${not empty k.ciclo}"> · ciclo <c:out value="${k.ciclo}"/></c:if><c:if test="${not empty k.horario}"> · <c:out value="${k.horario}"/></c:if></small></td>
            <td><c:out value="${k.persona}"/><c:if test="${not empty k.cargo}"><small><c:out value="${k.cargo}"/></small></c:if></td>
            <td class="contacto-links">
              <c:if test="${not empty k.waLink}"><a class="btn btn-ghost btn-sm" href="${k.waLink}" target="_blank" rel="noopener noreferrer">WhatsApp</a></c:if>
              <c:if test="${not empty k.igLink}"><a class="btn btn-ghost btn-sm" href="${fn:escapeXml(k.igLink)}" target="_blank" rel="noopener noreferrer"><c:out value="${k.instagram}"/></a></c:if>
              <c:if test="${not empty k.correo}"><a class="btn btn-ghost btn-sm" href="mailto:${fn:escapeXml(k.correo)}">Correo</a></c:if></td>
            <td><span class="tag ${k.tono}">${k.estadoTexto}</span></td>
            <td class="${k.olvidado ? 'c-rojo' : ''}">${k.ultimoTexto}<c:if test="${not empty k.responsable}"><small>👤 <c:out value="${k.responsable}"/></small></c:if></td></tr>
        </c:forEach>
      </tbody></table></div>
    </c:otherwise>
  </c:choose>
</div>
<p class="muted small">🔒 Solo lo ve RRPP (y el Owner). Hay celulares de personas de afuera: no los compartas fuera del team.</p>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
