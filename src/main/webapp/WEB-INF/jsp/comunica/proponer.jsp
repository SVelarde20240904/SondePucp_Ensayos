<c:set var="titulo" value="Proponer una idea"/><c:set var="menu" value="Proponer idea"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<section class="hero"><div class="titles"><span class="kicker">Para Team Comunica</span><h1>Proponer una idea 💡</h1>
  <p>¿Viste un TikTok que podríamos hacer? ¿Se te ocurrió un meme, un tutorial o algo para las historias? Cuéntaselo a Comunica.</p></div>
  <span class="star"></span></section>

<div class="row top">
  <form class="card grow" method="post" action="${ctx}/proponer-idea"><input type="hidden" name="_csrf" value="${sessionScope.csrf}">
    <div class="field"><label for="titulo">La idea en una línea</label>
      <input class="input" id="titulo" name="titulo" maxlength="120" required placeholder="Ej: Trend de «¿quién es más probable que…?» con los talleristas"></div>
    <div class="field"><label>¿Qué sería?</label>
      <div class="pills">
        <c:forEach var="t" items="${['TIKTOK','MEME','TUTORIAL','HISTORIA','POST','OTRO']}">
          <label class="pill sm"><input type="radio" name="tipo" value="${t}" ${t == 'TIKTOK' ? 'checked' : ''}><span>${t == 'TIKTOK' ? 'TikTok' : (t == 'MEME' ? 'Meme' : (t == 'TUTORIAL' ? 'Tutorial' : (t == 'HISTORIA' ? 'Historia' : (t == 'POST' ? 'Post' : 'Otro'))))}</span></label></c:forEach>
      </div></div>
    <div class="field"><label for="linkReferencia">Link de referencia <span class="muted">(opcional)</span></label>
      <input class="input" type="url" id="linkReferencia" name="linkReferencia" maxlength="500" placeholder="https://www.tiktok.com/@…"></div>
    <div class="field"><label for="descripcion">Detalles <span class="muted">(opcional)</span></label>
      <textarea class="input" id="descripcion" name="descripcion" rows="4" maxlength="1500" placeholder="¿Quiénes saldrían? ¿Dónde se grabaría? ¿Te animas a ayudar?"></textarea></div>
    <div><button class="btn btn-primary">Enviar a Comunica</button></div>
  </form>
  <div class="stack w-440">
    <div class="card"><h2>Mis ideas</h2>
      <c:choose>
        <c:when test="${empty misPropuestas}"><p class="empty">Todavía no has propuesto nada.</p></c:when>
        <c:otherwise>
          <ul class="icon-list small">
            <c:forEach var="p" items="${misPropuestas}">
              <li>${p.tipoIcono} <span><b><c:out value="${p.titulo}"/></b><br><span class="tag ${p.estado == 'PUBLICADO' ? 'ok' : (p.estado == 'EN_DESARROLLO' ? 'info' : 'neutral')}">${p.estado == 'IDEA' ? 'En el banco de ideas' : p.estadoTexto}</span>
                <c:if test="${p.votos gt 0}"> <span class="muted xs">👍 ${p.votos}</span></c:if>
                <c:if test="${not empty p.linkPublicacion}"> <a class="small" href="${fn:escapeXml(p.linkPublicacion)}" target="_blank" rel="noopener noreferrer">Ver ↗</a></c:if></span></li>
            </c:forEach>
          </ul>
        </c:otherwise>
      </c:choose>
    </div>
  </div>
</div>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
