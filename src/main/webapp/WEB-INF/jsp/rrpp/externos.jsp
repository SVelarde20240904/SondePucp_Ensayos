<c:set var="titulo" value="Externos del jueves"/><c:set var="menu" value="RRPP"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<section class="hero"><div class="titles"><span class="kicker">Team RRPP</span><h1>Externos del jueves</h1>
  <p>El Form para que la gente de afuera entre al taller. La web recuerda cada paso: publicar el link, avisar a todos y enviar la lista a la DAES.</p></div>
  <span class="star"></span></section>
<c:set var="grupo" value="rrpp"/><c:set var="sub" value="externos"/><%@ include file="/WEB-INF/jsp/includes/subtabs.jspf" %>

<div class="row top">
  <div class="card grow"><h2>Semana del taller del ${sem.juevesTexto}</h2>
    <%@ include file="/WEB-INF/jsp/rrpp/linea-externos.jspf" %>
    <c:if test="${not sem.sinTaller}">
      <form method="post" action="${ctx}/rrpp/externos" class="stack"><input type="hidden" name="_csrf" value="${sessionScope.csrf}"><input type="hidden" name="jueves" value="${sem.jueves}">
        <div class="field"><label for="link">Link del Form de esta semana</label>
          <div class="inline"><input class="input" type="url" id="link" name="link" maxlength="500" placeholder="https://forms.gle/…" value="${fn:escapeXml(sem.link)}">
            <button class="btn btn-primary btn-sm" name="action" value="publicar">${empty sem.link ? 'Publicar' : 'Actualizar'}</button></div>
          <small class="muted">Hasta que envíes la lista, a todos les sale en Inicio «¿Traes a alguien al taller?» con este link.</small></div>
        <div class="inline">
          <c:choose>
            <c:when test="${sem.enviado}"><button class="btn btn-ghost btn-sm" name="action" value="deshacer">Desmarcar envío</button></c:when>
            <c:otherwise><button class="btn btn-secondary btn-sm" name="action" value="enviado" onclick="return confirm('¿Ya enviaste la lista? Se cierra el Form para todos.')">✓ Ya envié la lista (cierra el Form)</button></c:otherwise>
          </c:choose>
          <button class="btn btn-ghost btn-sm" name="action" value="sinTaller" onclick="return confirm('¿Esta semana no hay taller?')">No hay taller esta semana</button>
        </div>
      </form>
    </c:if>
    <c:if test="${sem.sinTaller}">
      <form method="post" action="${ctx}/rrpp/externos"><input type="hidden" name="_csrf" value="${sessionScope.csrf}"><input type="hidden" name="jueves" value="${sem.jueves}">
        <button class="btn btn-secondary btn-sm" name="action" value="conTaller">Sí hay taller</button></form>
    </c:if>
    <c:if test="${puedeModerar}">
      <form method="post" action="${ctx}/rrpp/externos" class="inline mt"><input type="hidden" name="_csrf" value="${sessionScope.csrf}"><input type="hidden" name="jueves" value="${sem.jueves}">
        <select class="input input-sm" name="encargado" aria-label="Encargado de la semana"><option value="">— Nadie —</option>
          <c:forEach var="e" items="${equipo}"><option value="${e.id}" ${e.id == sem.idEncargado ? 'selected' : ''}><c:out value="${e.nombre}"/></option></c:forEach></select>
        <button class="btn btn-ghost btn-sm" name="action" value="encargado">Cambiar encargado</button></form>
    </c:if>
  </div>
  <div class="stack w-440">
    <c:choose>
      <c:when test="${puedeModerar}">
        <form class="card" method="post" action="${ctx}/rrpp/externos"><input type="hidden" name="_csrf" value="${sessionScope.csrf}">
          <h2>Ciclo semanal</h2><p class="muted small">Se calcula hacia atrás desde cada jueves.</p>
          <div class="form-row">
            <div class="field"><label for="diaPublicar">Se publica el</label><select class="input" id="diaPublicar" name="diaPublicar">
              <c:forEach var="d" items="${dias}"><option value="${d[0]}" ${d[0] == cfg.diaPublicar ? 'selected' : ''}>${d[1]}</option></c:forEach></select></div>
            <div class="field"><label for="diaLimite">Se envía a la DAES a más tardar el</label><select class="input" id="diaLimite" name="diaLimite">
              <c:forEach var="d" items="${dias}"><option value="${d[0]}" ${d[0] == cfg.diaLimite ? 'selected' : ''}>${d[1]}</option></c:forEach></select></div>
          </div>
          <p class="muted small">El Form se cierra cuando el encargado marca «Ya envié la lista». Si termina ese día sin envío, queda como falta (F).</p>
          <div class="field"><label for="encargado">Encargado</label><select class="input" id="encargado" name="encargado"><option value="">Rota cada semana (al que menos le tocó)</option>
            <c:forEach var="e" items="${equipo}"><option value="${e.id}" ${e.id == cfg.encargado ? 'selected' : ''}>Siempre <c:out value="${e.nombre}"/></option></c:forEach></select></div>
          <div class="field"><label for="meta">Meta de visitas formales al mes</label><input class="input" type="number" id="meta" name="meta" min="0" max="10" value="${cfg.metaVisitas}"></div>
          <div><button class="btn btn-secondary" name="action" value="config">Guardar ciclo</button></div>
        </form>
      </c:when>
      <c:otherwise><div class="card"><h2>Cómo funciona</h2><p class="small">El encargado de la semana recibe en Pendientes «Publica el link» y luego «Envía la lista a la DAES». El Form se cierra cuando se envía la lista, a más tardar el lunes; si no, es falta y también le llega al admin de RRPP.</p></div></c:otherwise>
    </c:choose>
  </div>
</div>

<div class="card"><h2>Historial</h2>
  <div class="scroll-x"><table class="table"><thead><tr><th>Taller</th><th>Encargado</th><th>Link publicado</th><th>Enviado a la DAES</th></tr></thead><tbody>
    <c:forEach var="h" items="${historial}">
      <tr><td><b>${h.juevesCorto}</b></td><td><c:out value="${empty h.encargado ? '—' : h.encargado}"/></td>
        <c:choose>
          <c:when test="${h.sinTaller}"><td colspan="2"><span class="tag neutral">Sin taller</span></td></c:when>
          <c:otherwise>
            <td>${empty h.publicadoEn ? '<span class="tag bad">No</span>' : '<span class="tag ok">'.concat(h.publicadoEn).concat('</span>')}</td>
            <td>${h.enviado ? (h.falta ? '<span class="tag bad">F · tarde '.concat(h.enviadoEn).concat('</span>') : '<span class="tag ok">'.concat(h.enviadoEn).concat('</span>')) : (h.falta ? '<span class="tag bad">F · no se envió</span>' : '<span class="tag neutral">Pendiente</span>')}</td>
          </c:otherwise>
        </c:choose></tr>
    </c:forEach>
  </tbody></table></div>
</div>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
