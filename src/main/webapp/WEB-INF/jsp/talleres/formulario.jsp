<c:set var="titulo" value="Formulario del taller"/><c:set var="menu" value="Inicio"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<form method="post" action="${ctx}/talleres/formulario" class="stack" style="gap:20px"><input type="hidden" name="_csrf" value="${sessionScope.csrf}"><input type="hidden" name="id" value="${t.idTaller}">
<section class="hero"><div class="titles"><span class="kicker">Team Talleres</span><h1>${t.titulo} · ${t.fechaCorta}</h1>
  <p>${yaRespondio ? 'Ya mandaste tu formulario; puedes corregirlo.' : 'Completa tu formulario de preferencia hoy mismo.'} Plazo: ${t.plazoTexto}${t.plazoVencido ? ' (ya venció, pero igual cuenta)' : ''}</p></div>
  <span class="star"></span></section>

<c:forEach var="m" items="${mias}">
  <c:if test="${m.funcion == 'TALLERISTA'}">
    <div class="card"><h2>Tu nivel: ${m.nivelTexto}</h2>
      <c:choose>
        <c:when test="${not empty t.checklistPor[m.nivel]}"><p>✓ El checklist ya lo llenó <b><c:out value="${t.checklistPor[m.nivel]}"/></b>. Si algo no cuadra, corrígelo; si está bien, déjalo así.</p></c:when>
        <c:otherwise><p>Marca ✅ o ❌ al lado de cada paso. Basta con que lo llene uno de los dos talleristas del nivel.</p></c:otherwise>
      </c:choose>
      <c:set var="hayPlan" value="false"/>
      <c:forEach var="p" items="${t.pasos[m.nivel]}"><c:if test="${p.planificado}"><c:set var="hayPlan" value="true"/>
        <div class="check-paso"><span><c:out value="${p.nombre}"/></span>
          <span class="pills"><label class="pill"><input type="radio" name="chk_${m.nivel}_${p.idPaso}" value="1" ${p.ensenado == true ? 'checked' : ''}><span>✅</span></label>
            <label class="pill"><input type="radio" name="chk_${m.nivel}_${p.idPaso}" value="0" ${p.ensenado == false ? 'checked' : ''}><span>❌</span></label></span></div>
      </c:if></c:forEach>
      <c:if test="${not hayPlan}"><p class="muted small">El cronograma no tenía pasos planificados para tu nivel: anota abajo lo que enseñaron.</p></c:if>
      <div class="field"><label>¿Agregaste algún paso? (uno por línea)</label>
        <textarea class="input" name="extra_${m.nivel}" rows="2" placeholder="Ninguno"><c:forEach var="p" items="${t.pasos[m.nivel]}"><c:if test="${not p.planificado}">${p.nombre}
</c:if></c:forEach></textarea></div>
    </div>
  </c:if>
</c:forEach>

<c:forEach var="d" items="${destinos}">
  <div class="card"><h2>${d[0] == 'COMPANERO' ? 'Tu compañero(a) de dictado: ' : 'Feedback para '}<c:out value="${d[2]}"/></h2>
    <p>${d[0] == 'COMPANERO' ? '¿Cómo fue trabajar con tu compañero(a) de dictado? ¿Qué destacarías y qué podrían mejorar como equipo?' : '¿Cómo viste a '.concat(d[2]).concat(' dictando ').concat(d[3]).concat('? ¿Qué destacarías y qué podría mejorar?')}</p>
    <textarea class="input" name="fb_${d[0]}_${d[1]}" rows="4" maxlength="2000" required><c:out value="${previo[d[0].concat('_').concat(d[1])]}"/></textarea>
    <span class="muted xs">Lo lee el team Talleres; a <c:out value="${fn:split(d[2], ' ')[0]}"/> le llega un resumen sin tu nombre.</span>
  </div>
</c:forEach>

<div class="card"><h2>¿Tienes alguna sugerencia para mejorar los talleres?</h2>
  <p>Se guarda aparte, como feedback del taller (parlante, micros, tiempos del cronograma, foto…)</p>
  <textarea class="input" name="sugerencia" rows="3" maxlength="2000" placeholder="Opcional"><c:out value="${previo['TALLER_']}"/></textarea></div>

<div class="inline" style="justify-content:flex-end"><button class="btn btn-primary">${yaRespondio ? 'Guardar cambios' : 'Enviar formulario'}</button></div>
</form>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
