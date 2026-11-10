<c:set var="titulo" value="Formulario del taller"/><c:set var="menu" value="Inicio"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<%-- Orden pensado como lo responde un tallerista: 1) ¿dictaste los pasos? 2) ¿agregaste alguno? 3) feedback a tu compañero(a) 4) feedback del taller --%>
<form method="post" action="${ctx}/talleres/formulario" class="stack" style="gap:20px"><input type="hidden" name="_csrf" value="${sessionScope.csrf}"><input type="hidden" name="id" value="${t.idTaller}">
<section class="hero"><div class="titles"><span class="kicker">Team Talleres</span><h1>${t.titulo} · ${t.fechaCorta}</h1>
  <p>${yaRespondio ? 'Ya mandaste tu formulario; puedes corregirlo.' : 'Son 2 minutos.'} Plazo: ${t.plazoTexto}${t.plazoVencido ? ' (ya venció, pero igual cuenta)' : ''}</p></div>
  <span class="star"></span></section>

<c:set var="paso" value="1"/>
<c:forEach var="m" items="${mias}">
  <c:if test="${m.funcion == 'TALLERISTA'}">
    <c:set var="planeados" value="0"/>
    <c:forEach var="p" items="${t.pasos[m.nivel]}"><c:if test="${p.planificado}"><c:set var="planeados" value="${planeados + 1}"/></c:if></c:forEach>
    <div class="card"><span class="kicker c-amarillo">${paso} · ${m.nivelTexto}</span><c:set var="paso" value="${paso + 1}"/>
      <h2>¿Dictaste todos los pasos?</h2>
      <c:choose>
        <c:when test="${planeados > 0}">
          <p>Marca ✅ lo que sí se enseñó y ❌ lo que no llegaron a ver.
            <c:if test="${conCompanero.contains(m.nivelTexto)}"><span class="muted">Basta con que lo marque uno de los dos talleristas.</span></c:if></p>
          <c:if test="${not empty t.checklistPor[m.nivel]}"><div class="note ok">✓ Ya lo marcó <b><c:out value="${t.checklistPor[m.nivel]}"/></b>. Si algo no cuadra, corrígelo.</div></c:if>
          <c:forEach var="p" items="${t.pasos[m.nivel]}"><c:if test="${p.planificado}">
            <div class="check-paso"><span><c:out value="${p.nombre}"/></span>
              <span class="pills"><label class="pill"><input type="radio" name="chk_${m.nivel}_${p.idPaso}" value="1" ${p.ensenado == true ? 'checked' : ''}><span>✅</span></label>
                <label class="pill"><input type="radio" name="chk_${m.nivel}_${p.idPaso}" value="0" ${p.ensenado == false ? 'checked' : ''}><span>❌</span></label></span></div>
          </c:if></c:forEach>
        </c:when>
        <c:otherwise><div class="note warn">El cronograma de este taller no tenía pasos planificados para ${m.nivelTexto}. Anota abajo los que enseñaste.</div></c:otherwise>
      </c:choose>
      <div class="field" style="margin-top:12px"><label>${planeados > 0 ? '¿Agregaste algún paso que no estaba en el cronograma?' : '¿Qué pasos enseñaste?'} <span class="muted">(uno por línea)</span></label>
        <textarea class="input" name="extra_${m.nivel}" rows="${planeados > 0 ? 2 : 4}" placeholder="${planeados > 0 ? 'Ninguno' : 'Un paso por línea'}"><c:forEach var="p" items="${t.pasos[m.nivel]}"><c:if test="${not p.planificado}">${p.nombre}
</c:if></c:forEach></textarea></div>
    </div>
  </c:if>
</c:forEach>

<c:forEach var="d" items="${destinos}">
  <div class="card"><span class="kicker c-amarillo">${paso} · ${d[0] == 'COMPANERO' ? 'Tu compañero(a) de dictado' : 'Feedback para los talleristas'}</span><c:set var="paso" value="${paso + 1}"/>
    <h2><c:out value="${d[2]}"/></h2>
    <p>${d[0] == 'COMPANERO' ? '¿Cómo fue dictar juntos? ¿Qué destacarías y qué podría mejorar?' : '¿Cómo viste a '.concat(d[2]).concat(' dictando ').concat(d[3]).concat('? ¿Qué destacarías y qué podría mejorar?')}</p>
    <textarea class="input" name="fb_${d[0]}_${d[1]}" rows="4" maxlength="2000" required><c:out value="${previo[d[0].concat('_').concat(d[1])]}"/></textarea>
    <span class="muted xs">Lo lee el team Talleres; a <c:out value="${fn:split(d[2], ' ')[0]}"/> le llega un resumen sin tu nombre.</span>
  </div>
</c:forEach>
<c:forEach var="m" items="${mias}">
  <c:if test="${m.funcion == 'TALLERISTA' and not conCompanero.contains(m.nivelTexto)}">
    <div class="card"><span class="kicker c-amarillo">${paso} · Tu compañero(a) de dictado</span><c:set var="paso" value="${paso + 1}"/>
      <p class="muted">En ${m.nivelTexto} dictaste solo(a) en este taller, así que no hay feedback de compañero(a). ¡Pasa al siguiente!</p></div>
  </c:if>
</c:forEach>

<div class="card"><span class="kicker c-amarillo">${paso} · El taller</span>
  <h2>¿Qué te pareció la estructura del taller?</h2>
  <p>El cronograma, los tiempos, cómo se repartieron los niveles, la música, el espacio… Lo que funcionó y lo que cambiarías.</p>
  <textarea class="input" name="sugerencia" rows="4" maxlength="2000" placeholder="Opcional"><c:out value="${previo['TALLER_']}"/></textarea>
  <span class="muted xs">Se guarda aparte del feedback de las personas.</span></div>

<div class="inline" style="justify-content:flex-end"><button class="btn btn-primary">${yaRespondio ? 'Guardar cambios' : 'Enviar formulario'}</button></div>
</form>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
