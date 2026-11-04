<c:set var="titulo" value="Feedback · ${t.titulo}"/><c:set var="menu" value="Talleres"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<section class="hero"><div class="titles"><span class="kicker">Team Talleres · ${t.titulo} · ${t.fechaCorta}</span><h1>Feedback</h1>
  <p>Lo ve todo el team. A cada tallerista le llega solo el <b>resumen</b>, sin nombres: escríbelo con buen tono y envíalo.</p></div>
  <div class="actions"><a class="btn btn-secondary" href="${ctx}/talleres/detalle?id=${t.idTaller}">← Volver al taller</a></div><span class="star"></span></section>

<c:if test="${empty receptores}"><div class="card"><p class="empty">Todavía no hay talleristas ni feedback en este taller.</p></div></c:if>
<c:forEach var="e" items="${receptores}">
  <c:set var="lista" value="${porPersona[e.key]}"/><c:set var="res" value="${resumenes[e.key]}"/>
  <div class="card" id="p${e.key}"><div class="inline" style="justify-content:space-between"><h2>Para <c:out value="${e.value}"/></h2>
      <c:choose><c:when test="${not empty res and res.enviado}"><span class="tag ok">Resumen enviado</span></c:when><c:when test="${not empty res}"><span class="tag warn">Resumen sin enviar</span></c:when><c:otherwise><span class="tag neutral">Sin resumen</span></c:otherwise></c:choose></div>
    <c:forEach var="f" items="${lista}">
      <div class="fb-crudo"><span class="muted xs"><b>De <c:out value="${f.de}"/></b> · ${f.tipoTexto} · ${f.fechaTexto}</span><p><c:out value="${f.texto}"/></p></div>
    </c:forEach>
    <c:if test="${empty lista}"><p class="muted small">Todavía no le llega feedback (su compañero o sus apoyos no han mandado el formulario).</p></c:if>
    <form method="post" action="${ctx}/talleres/feedback" class="stack" style="gap:8px"><input type="hidden" name="_csrf" value="${sessionScope.csrf}"><input type="hidden" name="id" value="${t.idTaller}"><input type="hidden" name="para" value="${e.key}">
      <label class="label" for="r${e.key}">Resumen para <c:out value="${e.value}"/> (anónimo)</label>
      <textarea class="input" id="r${e.key}" name="resumen" rows="5" maxlength="2000" placeholder="Junta lo que le dijeron sin nombres: qué destacan y qué podría mejorar."><c:out value="${res.texto}"/></textarea>
      <div class="inline" style="justify-content:flex-end"><button class="btn btn-secondary btn-sm" name="action" value="guardar">Guardar</button>
        <button class="btn btn-primary btn-sm" name="action" value="enviar">${not empty res and res.enviado ? 'Guardar cambios (ya enviado)' : 'Guardar y enviar'}</button></div>
    </form>
    <c:if test="${not empty res and res.enviado}">
      <span class="label">Mensaje para WhatsApp</span>
      <textarea class="input wa" rows="6" readonly onclick="this.select()">Hola <c:out value="${fn:split(e.value, ' ')[0]}"/>! 💃🏻 Este es tu feedback del ${t.titulo} (${t.fechaCorta}):

<c:out value="${res.texto}"/>

¡Gracias por dictar! 🙌🇨🇺 — Team Talleres SDP</textarea>
      <span class="muted xs">También lo ve en la web, en “Mi feedback de talleres”. Toca el mensaje para seleccionarlo.</span>
    </c:if>
  </div>
</c:forEach>

<div class="card"><h2>Sugerencias para el taller</h2><p>Se guardan aparte del feedback de las personas</p>
  <c:forEach var="s" items="${sugerencias}"><div class="sugerencia"><p><c:out value="${s.texto}"/></p><span class="muted xs">${s.de} · ${s.fechaTexto}</span></div></c:forEach>
  <c:if test="${empty sugerencias}"><p class="empty">Sin sugerencias.</p></c:if>
</div>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
