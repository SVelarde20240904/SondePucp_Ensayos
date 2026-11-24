<c:set var="titulo" value="Plantillas de flyers"/><c:set var="menu" value="Comunica"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<section class="hero"><div class="titles"><span class="kicker">Team Comunica</span><h1>Plantillas de flyers</h1>
  <p>Los modelos que más usan, con su vista previa y el link de Canva para duplicarlos y adaptarlos rápido.</p></div>
  <span class="star"></span></section>
<c:set var="grupo" value="comunica"/><c:set var="sub" value="plantillas"/><%@ include file="/WEB-INF/jsp/includes/subtabs.jspf" %>

<div class="inline"><a class="pill-link${verArchivo ? '' : ' on'}" href="${ctx}/comunica/plantillas">En uso</a><a class="pill-link${verArchivo ? ' on' : ''}" href="${ctx}/comunica/plantillas?archivo=1">Archivadas</a></div>

<div class="row top">
  <div class="grow">
    <c:choose>
      <c:when test="${empty plantillas}"><div class="card"><p class="empty">${verArchivo ? 'No hay plantillas archivadas.' : 'Todavía no hay plantillas. Sube la primera: el clásico «Hoy no hay taller».'}</p></div></c:when>
      <c:otherwise>
        <div class="galeria">
          <c:forEach var="pl" items="${plantillas}">
            <article class="gplantilla">
              <c:choose>
                <c:when test="${pl.conImagen}"><a href="${ctx}/comunica/plantilla-img?id=${pl.id}" target="_blank" class="gimg"><img src="${ctx}/comunica/plantilla-img?id=${pl.id}" alt="Vista previa de ${fn:escapeXml(pl.nombre)}" loading="lazy"></a></c:when>
                <c:otherwise><div class="gimg vacia" aria-hidden="true">🎨</div></c:otherwise>
              </c:choose>
              <div class="gtxt"><b><c:out value="${pl.nombre}"/></b>
                <div class="kmeta"><span class="tag info"><c:out value="${pl.tipo}"/></span><c:if test="${not empty pl.ciclo}"><span class="tag neutral">Ciclo <c:out value="${pl.ciclo}"/></span></c:if></div>
                <c:if test="${not empty pl.notas}"><small><c:out value="${pl.notas}"/></small></c:if>
                <div class="inline">
                  <c:if test="${not empty pl.linkCanva}"><a class="btn btn-primary btn-sm" href="${fn:escapeXml(pl.linkCanva)}" target="_blank" rel="noopener noreferrer">Abrir en Canva ↗</a></c:if>
                  <a class="btn btn-ghost btn-sm" href="${ctx}/comunica/plantilla?id=${pl.id}">Editar</a>
                </div>
              </div>
            </article>
          </c:forEach>
        </div>
      </c:otherwise>
    </c:choose>
  </div>
  <c:if test="${not verArchivo}">
  <div class="stack w-440">
    <form class="card" method="post" action="${ctx}/comunica/plantillas" enctype="multipart/form-data"><input type="hidden" name="_csrf" value="${sessionScope.csrf}">
      <h2>Agregar plantilla</h2>
      <div class="field"><label for="nombre">Nombre</label><input class="input" id="nombre" name="nombre" maxlength="100" required placeholder="Ej: Hoy no hay taller (feriado)"></div>
      <div class="form-row">
        <div class="field"><label for="tipo">Tipo</label><select class="input" id="tipo" name="tipo"><c:forEach var="t" items="${tiposPlantilla}"><option>${t}</option></c:forEach></select></div>
        <div class="field"><label for="ciclo">Ciclo</label><input class="input" id="ciclo" name="ciclo" maxlength="10" value="${cicloActual}" placeholder="2026-2"></div>
      </div>
      <div class="field"><label for="linkCanva">Link de Canva</label><input class="input" type="url" id="linkCanva" name="linkCanva" maxlength="500" placeholder="https://www.canva.com/design/…"></div>
      <div class="field"><label for="imagen">Vista previa <span class="muted">(JPG o PNG)</span></label><input class="input" type="file" id="imagen" name="imagen" accept="image/jpeg,image/png"></div>
      <div class="field"><label for="notas">Notas <span class="muted">(opcional)</span></label><input class="input" id="notas" name="notas" maxlength="300" placeholder="Qué cambiar: fecha, nivel, foto…"></div>
      <div><button class="btn btn-primary" name="action" value="agregar">Agregar</button></div>
      <p class="muted xs">En Canva, compártelo como «cualquiera con el link puede usarlo como plantilla» o solo con el team.</p>
    </form>
  </div>
  </c:if>
</div>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
