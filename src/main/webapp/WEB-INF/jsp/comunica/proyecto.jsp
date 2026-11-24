<c:set var="nuevo" value="${p.nuevo}"/>
<c:set var="titulo" value="${nuevo ? (p.estado == 'IDEA' ? 'Nueva idea' : 'Nuevo proyecto') : p.titulo}"/><c:set var="menu" value="Comunica"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<section class="hero"><div class="titles"><span class="kicker">Team Comunica · ${nuevo ? (p.estado == 'IDEA' ? 'Banco de ideas' : 'Proyecto') : p.estadoTexto}</span>
  <h1><c:out value="${titulo}"/></h1>
  <c:if test="${not nuevo}"><p>${p.tipoIcono} ${p.tipoTexto} · ${p.redTexto}<c:if test="${not empty p.propuestoPor}"> · Propuesto por <c:out value="${p.propuestoPor}"/> el ${p.fechaCreacionTexto}</c:if></p></c:if></div>
  <div class="actions"><a class="btn btn-secondary" href="${ctx}/comunica${p.estado == 'IDEA' ? '/ideas' : (p.estado == 'EN_DESARROLLO' or nuevo ? '' : '/archivo')}">← Volver</a></div>
  <span class="star"></span></section>
<c:set var="grupo" value="comunica"/><c:set var="sub" value="${p.estado == 'IDEA' ? 'ideas' : (p.estado == 'EN_DESARROLLO' ? 'tablero' : 'archivo')}"/><%@ include file="/WEB-INF/jsp/includes/subtabs.jspf" %>

<c:if test="${not nuevo}">
  <form method="post" action="${ctx}/comunica/proyecto" class="estado-flujo"><input type="hidden" name="_csrf" value="${sessionScope.csrf}"><input type="hidden" name="id" value="${p.id}"><input type="hidden" name="action" value="estado">
    <c:forEach var="e" items="${estadosProyecto}">
      <button class="${e == p.estado ? 'on' : ''}" name="estado" value="${e}" ${e == p.estado ? 'disabled' : ''}>${e == 'IDEA' ? '💡 Idea' : (e == 'EN_DESARROLLO' ? '🛠 En desarrollo' : (e == 'PUBLICADO' ? '✅ Publicado' : '🗄 Archivado'))}</button>
    </c:forEach>
  </form>
</c:if>

<div class="row top">
  <form class="card grow" method="post" action="${ctx}/comunica/proyecto"><input type="hidden" name="_csrf" value="${sessionScope.csrf}">
    <c:if test="${not nuevo}"><input type="hidden" name="id" value="${p.id}"></c:if>
    <input type="hidden" name="estado" value="${p.estado}">
    <div class="field"><label for="titulo">Título</label>
      <input class="input" id="titulo" name="titulo" maxlength="120" required value="${fn:escapeXml(p.titulo)}" placeholder="${p.estado == 'IDEA' ? 'Ej: Trend de «¿quién es más probable que…?» con los talleristas' : 'Ej: Flyer del taller de aniversario'}"></div>
    <div class="form-row">
      <div class="field"><label for="tipo">Tipo</label><select class="input" id="tipo" name="tipo">
        <c:forEach var="t" items="${tipos}"><option value="${t}" ${t == p.tipo ? 'selected' : ''}>${t == 'TIKTOK' ? 'TikTok' : (t == 'REEL' ? 'Reel' : (t == 'POST' ? 'Post' : (t == 'HISTORIA' ? 'Historia' : (t == 'DESTACADA' ? 'Historia destacada' : (t == 'FLYER' ? 'Flyer' : (t == 'MEME' ? 'Meme' : (t == 'TUTORIAL' ? 'Tutorial' : (t == 'COMUNICADO' ? 'Comunicado' : 'Otro'))))))))}</option></c:forEach></select></div>
      <div class="field"><label for="red">Red</label><select class="input" id="red" name="red">
        <c:forEach var="r" items="${redes}"><option value="${r}" ${r == p.red ? 'selected' : ''}>${r == 'INSTAGRAM' ? 'Instagram' : (r == 'TIKTOK' ? 'TikTok' : (r == 'TELEGRAM' ? 'Telegram' : 'Varias redes'))}</option></c:forEach></select></div>
    </div>
    <div class="field"><label for="descripcion">${p.estado == 'IDEA' ? 'La idea' : 'Descripción e indicaciones'}</label>
      <textarea class="input" id="descripcion" name="descripcion" rows="5" maxlength="3000" placeholder="${p.estado == 'IDEA' ? '¿Qué se haría? ¿Quiénes salen? ¿Por qué funcionaría?' : 'Guion, quiénes salen, música, indicaciones para grabar…'}"><c:out value="${p.descripcion}"/></textarea></div>
    <div class="form-row">
      <div class="field"><label for="responsable">Responsable</label><select class="input" id="responsable" name="responsable">
        <option value="">— Nadie todavía —</option>
        <c:forEach var="e" items="${equipo}"><option value="${e.id}" ${e.id == p.idResponsable ? 'selected' : ''}><c:out value="${e.nombre}"/></option></c:forEach></select></div>
      <div class="field"><label for="fechaObjetivo">Para cuándo <span class="muted">(opcional)</span></label>
        <input class="input" type="date" id="fechaObjetivo" name="fechaObjetivo" value="${p.fechaObjetivo}"></div>
    </div>
    <div class="field"><label for="etiquetas">Etiquetas <span class="muted">(separadas por comas)</span></label>
      <input class="input" id="etiquetas" name="etiquetas" maxlength="200" list="lista-etiquetas" value="${fn:escapeXml(p.etiquetas)}" placeholder="Ej: tendencia, meme, talleristas">
      <datalist id="lista-etiquetas"><c:forEach var="t" items="${etiquetas}"><option value="<c:out value='${t}'/>"></c:forEach></datalist></div>
    <div class="field"><label for="linkReferencia">Video o post de referencia</label>
      <input class="input" type="url" id="linkReferencia" name="linkReferencia" maxlength="500" placeholder="https://www.tiktok.com/@…" value="${fn:escapeXml(p.linkReferencia)}"></div>
    <div class="field"><label for="linkDrive">Carpeta o archivo en Drive <span class="muted">(material grabado, edición)</span></label>
      <input class="input" type="url" id="linkDrive" name="linkDrive" maxlength="500" placeholder="https://drive.google.com/…" value="${fn:escapeXml(p.linkDrive)}"></div>
    <div class="field"><label for="linkPublicacion">Link de la publicación <span class="muted">(cuando ya salió)</span></label>
      <input class="input" type="url" id="linkPublicacion" name="linkPublicacion" maxlength="500" placeholder="https://www.instagram.com/p/…" value="${fn:escapeXml(p.linkPublicacion)}"></div>
    <div class="inline"><button class="btn btn-primary" name="action" value="${nuevo ? 'crear' : 'guardar'}">${nuevo ? (p.estado == 'IDEA' ? 'Guardar idea' : 'Crear proyecto') : 'Guardar cambios'}</button>
      <c:if test="${not nuevo and (puedeModerar or (p.idPropuestoPor == u.idMiembro and p.estado == 'IDEA'))}">
        <button class="btn btn-ghost" name="action" value="eliminar" formnovalidate onclick="return confirm('¿Eliminar este proyecto para siempre? No se puede deshacer.')">Eliminar</button></c:if></div>
    <p class="muted xs">Los videos no se suben a la web: van como link de Drive restringido al grupo de Google de SDP.</p>
  </form>

  <div class="stack w-440">
    <c:if test="${not nuevo}">
      <div class="card"><h2>Checklist</h2>
        <c:choose>
          <c:when test="${empty p.listaTareas}"><p class="muted small">Divide el trabajo: guion, grabar, editar, revisar ortografía, publicar…</p></c:when>
          <c:otherwise>
            <ul class="tareas">
              <c:forEach var="t" items="${p.listaTareas}">
                <li class="${t.hecha ? 'hecha' : ''}"><form method="post" action="${ctx}/comunica/proyecto" class="inline-form"><input type="hidden" name="_csrf" value="${sessionScope.csrf}"><input type="hidden" name="id" value="${p.id}"><input type="hidden" name="idTarea" value="${t.id}">
                  <button class="tcheck" name="action" value="marcar" aria-label="${t.hecha ? 'Desmarcar' : 'Marcar como hecha'}">${t.hecha ? '☑' : '☐'}</button>
                  <span><c:out value="${t.texto}"/></span>
                  <button class="btn btn-ghost btn-sm" name="action" value="quitar" aria-label="Quitar">✕</button></form></li>
              </c:forEach>
            </ul>
          </c:otherwise>
        </c:choose>
        <form method="post" action="${ctx}/comunica/proyecto" class="inline"><input type="hidden" name="_csrf" value="${sessionScope.csrf}"><input type="hidden" name="id" value="${p.id}">
          <input class="input" name="tarea" maxlength="200" placeholder="Ej: Grabar el jueves en el taller" required aria-label="Nueva tarea">
          <button class="btn btn-secondary btn-sm" name="action" value="tarea">Agregar</button></form>
      </div>
      <div class="card"><h2>Links</h2>
        <ul class="icon-list small">
          <li>🔗 <c:choose><c:when test="${not empty p.linkReferencia}"><a class="link" href="${fn:escapeXml(p.linkReferencia)}" target="_blank" rel="noopener noreferrer">Referencia ↗</a></c:when><c:otherwise><span class="muted">Sin referencia</span></c:otherwise></c:choose></li>
          <li>📁 <c:choose><c:when test="${not empty p.linkDrive}"><a class="link" href="${fn:escapeXml(p.linkDrive)}" target="_blank" rel="noopener noreferrer">Drive ↗</a></c:when><c:otherwise><span class="muted">Sin Drive</span></c:otherwise></c:choose></li>
          <li>📣 <c:choose><c:when test="${not empty p.linkPublicacion}"><a class="link" href="${fn:escapeXml(p.linkPublicacion)}" target="_blank" rel="noopener noreferrer">Publicación ↗</a></c:when><c:otherwise><span class="muted">Aún no se publica</span></c:otherwise></c:choose></li>
        </ul>
        <c:if test="${p.estado == 'IDEA'}"><p class="muted small">👍 ${p.votos} ${p.votos == 1 ? 'voto' : 'votos'}</p></c:if>
      </div>
    </c:if>
    <c:if test="${nuevo}">
      <div class="card"><h2>${p.estado == 'IDEA' ? '¿Qué va al banco de ideas?' : 'Un proyecto es…'}</h2>
        <p class="small">${p.estado == 'IDEA' ? 'Cualquier cosa que quieran hacer algún día: un TikTok que vieron, un trend, un meme, un tutorial. Peguen el link de referencia y pónganle etiquetas para encontrarlo después.' : 'Algo concreto que se está haciendo: un TikTok, un flyer, una historia destacada… Con responsable, fecha y checklist. Cuando salga, pongan el link y márquenlo como publicado.'}</p></div>
    </c:if>
  </div>
</div>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
