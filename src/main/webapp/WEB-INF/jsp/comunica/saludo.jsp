<c:set var="titulo" value="Saludo de cumpleaños"/><c:set var="menu" value="Comunica"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<section class="hero"><div class="titles"><span class="kicker">Cumpleaños · ${s.fechaTexto}</span><h1><c:out value="${s.comoLlamarlo}"/> 🎂</h1>
  <p><c:out value="${s.nombre}"/> · ${s.cuando} · Responsable: <c:out value="${empty s.responsable ? 'sin asignar' : s.responsable}"/></p></div>
  <div class="actions"><a class="btn btn-secondary" href="${ctx}/comunica/cumpleanos?m=${fn:substring(s.fecha, 0, 7)}">← Volver al mes</a></div>
  <span class="star"></span></section>

<div class="pasos-saludo" aria-label="Avance del saludo">
  <c:forEach var="e" items="${estados}" varStatus="st"><c:if test="${e != 'OMITIDO'}">
    <span class="${st.index le s.paso and s.estado != 'OMITIDO' ? 'on' : ''}">${st.index + 1}. ${e == 'PENDIENTE' ? 'Redactar' : (e == 'REDACTADO' ? 'Redactado' : (e == 'VIDEO_LISTO' ? 'Video listo' : 'Publicado'))}</span></c:if></c:forEach>
</div>

<div class="row top">
  <form class="card grow" method="post" action="${ctx}/comunica/saludo"><input type="hidden" name="_csrf" value="${sessionScope.csrf}"><input type="hidden" name="id" value="${s.id}">
    <h2>El saludo</h2>
    <div class="field"><label for="texto">Texto <span class="muted">(revisa la ortografía antes de publicar ✍️)</span></label>
      <textarea class="input" id="texto" name="texto" rows="7" maxlength="3000" placeholder="Escribe el saludo…"><c:out value="${s.texto}"/></textarea>
      <c:if test="${not empty sugerido}"><button type="button" class="btn btn-ghost btn-sm" id="usar-plantilla">Usar la plantilla</button>
        <textarea id="sugerido" hidden><c:out value="${sugerido}"/></textarea></c:if></div>
    <div class="field"><label for="linkVideo">Link del video <span class="muted">(Drive restringido al grupo de SDP)</span></label>
      <input class="input" id="linkVideo" name="linkVideo" type="url" maxlength="500" placeholder="https://drive.google.com/…" value="${fn:escapeXml(s.linkVideo)}">
      <c:if test="${not empty s.linkVideo}"><a class="small" href="${fn:escapeXml(s.linkVideo)}" target="_blank" rel="noopener noreferrer">Abrir el video ↗</a></c:if></div>
    <div class="field"><label>¿Dónde se publicó?</label>
      <div class="pills">
        <c:forEach var="d" items="${['Historia de Instagram','Post de Instagram','Telegram','TikTok']}">
          <label class="pill sm"><input type="checkbox" name="publicadoEn" value="${d}" ${fn:contains(s.publicadoEn, d) ? 'checked' : ''}><span>${d}</span></label></c:forEach>
      </div></div>
    <div class="field"><label>Estado</label>
      <div class="pills">
        <c:forEach var="e" items="${estados}">
          <label class="pill sm"><input type="radio" name="estado" value="${e}" ${e == s.estado ? 'checked' : ''}><span>${e == 'PENDIENTE' ? 'Por redactar' : (e == 'REDACTADO' ? 'Redactado' : (e == 'VIDEO_LISTO' ? 'Video listo' : (e == 'PUBLICADO' ? 'Publicado' : 'No se publica')))}</span></label></c:forEach>
      </div></div>
    <div class="inline"><button class="btn btn-primary" name="action" value="guardar">Guardar</button>
      <c:if test="${not empty s.actualizadoPor}"><span class="muted xs">Último cambio: <c:out value="${s.actualizadoPor}"/></span></c:if></div>
  </form>
  <div class="stack w-440">
    <c:choose>
      <c:when test="${puedeModerar}">
        <form class="card" method="post" action="${ctx}/comunica/saludo"><input type="hidden" name="_csrf" value="${sessionScope.csrf}"><input type="hidden" name="id" value="${s.id}">
          <h2>Responsable</h2>
          <div class="field"><select class="input" name="responsable" aria-label="Responsable">
            <option value="">— Sin asignar —</option>
            <c:forEach var="e" items="${equipo}"><c:if test="${e.id != s.idMiembro}"><option value="${e.id}" ${e.id == s.idResponsable ? 'selected' : ''}><c:out value="${e.nombre}"/></option></c:if></c:forEach>
          </select></div>
          <div><button class="btn btn-secondary" name="action" value="asignar">Reasignar</button></div></form>
      </c:when>
      <c:when test="${empty s.responsable}">
        <form class="card" method="post" action="${ctx}/comunica/saludo"><input type="hidden" name="_csrf" value="${sessionScope.csrf}"><input type="hidden" name="id" value="${s.id}">
          <h2>Sin responsable</h2><p class="muted small">Nadie tiene este saludo todavía.</p>
          <div><button class="btn btn-secondary" name="action" value="tomar">Me lo quedo</button></div></form>
      </c:when>
    </c:choose>
    <div class="card"><h2>Cómo va</h2>
      <ol class="small guia">
        <li><b>Redactar</b> el saludo (puedes partir de la plantilla).</li>
        <li><b>Video</b>: arma el video con el decorativo y sube el link de Drive.</li>
        <li><b>Publicar</b> en historias / Telegram y márcalo como publicado.</li>
      </ol>
      <p class="muted xs">Si la persona no quiere saludo público, marca «No se publica».</p>
    </div>
  </div>
</div>
<script>
(function () {
  var b = document.getElementById('usar-plantilla'), t = document.getElementById('texto'), j = document.getElementById('sugerido');
  if (!b || !t || !j) return;
  b.addEventListener('click', function () {
    if (t.value.trim() && !confirm('¿Reemplazar lo que ya escribiste por la plantilla?')) return;
    t.value = j.value; t.focus();
  });
})();
</script>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
