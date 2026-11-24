<c:set var="titulo" value="Editar plantilla"/><c:set var="menu" value="Comunica"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<section class="hero"><div class="titles"><span class="kicker">Plantillas de flyers</span><h1><c:out value="${pl.nombre}"/></h1>
  <p>Subida por <c:out value="${empty pl.subidoPor ? '—' : pl.subidoPor}"/> el ${pl.fechaTexto}${pl.activa ? '' : ' · archivada'}</p></div>
  <div class="actions"><a class="btn btn-secondary" href="${ctx}/comunica/plantillas${pl.activa ? '' : '?archivo=1'}">← Volver</a></div>
  <span class="star"></span></section>
<c:set var="grupo" value="comunica"/><c:set var="sub" value="plantillas"/><%@ include file="/WEB-INF/jsp/includes/subtabs.jspf" %>

<div class="row top">
  <form class="card grow" method="post" action="${ctx}/comunica/plantilla" enctype="multipart/form-data"><input type="hidden" name="_csrf" value="${sessionScope.csrf}"><input type="hidden" name="id" value="${pl.id}">
    <div class="field"><label for="nombre">Nombre</label><input class="input" id="nombre" name="nombre" maxlength="100" required value="${fn:escapeXml(pl.nombre)}"></div>
    <div class="form-row">
      <div class="field"><label for="tipo">Tipo</label><select class="input" id="tipo" name="tipo"><c:forEach var="t" items="${tiposPlantilla}"><option ${t == pl.tipo ? 'selected' : ''}>${t}</option></c:forEach></select></div>
      <div class="field"><label for="ciclo">Ciclo</label><input class="input" id="ciclo" name="ciclo" maxlength="10" value="${fn:escapeXml(pl.ciclo)}"></div>
    </div>
    <div class="field"><label for="linkCanva">Link de Canva</label><input class="input" type="url" id="linkCanva" name="linkCanva" maxlength="500" value="${fn:escapeXml(pl.linkCanva)}"></div>
    <div class="field"><label for="imagen">Cambiar vista previa <span class="muted">(déjalo vacío para mantenerla)</span></label><input class="input" type="file" id="imagen" name="imagen" accept="image/jpeg,image/png"></div>
    <div class="field"><label for="notas">Notas</label><input class="input" id="notas" name="notas" maxlength="300" value="${fn:escapeXml(pl.notas)}"></div>
    <div class="inline"><button class="btn btn-primary" name="action" value="guardar">Guardar</button>
      <button class="btn btn-secondary" name="action" value="${pl.activa ? 'archivar' : 'activar'}" formnovalidate>${pl.activa ? 'Archivar' : 'Volver a la galería'}</button>
      <c:if test="${puedeModerar}"><button class="btn btn-ghost" name="action" value="eliminar" formnovalidate onclick="return confirm('¿Eliminar esta plantilla para siempre?')">Eliminar</button></c:if></div>
  </form>
  <div class="stack w-440">
    <div class="card">
      <c:choose>
        <c:when test="${pl.conImagen}"><img class="preview-plantilla" src="${ctx}/comunica/plantilla-img?id=${pl.id}" alt="Vista previa"></c:when>
        <c:otherwise><p class="empty">Sin vista previa.</p></c:otherwise>
      </c:choose>
    </div>
  </div>
</div>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
