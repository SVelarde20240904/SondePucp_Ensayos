<c:set var="nuevo" value="${t.idTaller == 0}"/>
<c:set var="titulo" value="${nuevo ? 'Nuevo taller' : 'Editar cronograma'}"/><c:set var="menu" value="Talleres"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<form method="post" action="${ctx}/talleres/${nuevo ? 'nuevo' : 'editar'}" class="stack" style="gap:24px" id="form-taller"><input type="hidden" name="_csrf" value="${sessionScope.csrf}">
<input type="hidden" name="idTaller" value="${nuevo ? '' : t.idTaller}"><input type="hidden" name="estado" value="${t.estado}">
<section class="hero"><div class="titles"><span class="kicker">Team Talleres</span><h1>${titulo}</h1>
  <p><c:choose><c:when test="${nuevo and not empty plantilla}">Los bloques vienen del ${plantilla.titulo} (${plantilla.fechaCorta}) como plantilla: cámbialos lo que haga falta.</c:when>
    <c:otherwise>Bloques del flyer, pasos planificados y quién dicta cada nivel</c:otherwise></c:choose></p></div>
  <div class="actions"><a class="btn btn-secondary" href="${ctx}/talleres${nuevo ? '' : '/detalle?id='.concat(t.idTaller)}">Cancelar</a>
    <button class="btn btn-secondary" name="publicar" value="borrador">Guardar borrador</button><button class="btn btn-primary" name="publicar" value="si">Guardar cronograma</button></div>
  <span class="star"></span></section>

<div class="card"><h2>Datos del taller</h2>
  <div class="form-row">
    <div class="field"><label>Fecha *</label><input class="input" type="date" name="fecha" value="${t.fechaIso}" required></div>
    <div class="field"><label>Número</label><input class="input" type="number" name="numero" min="1" max="99" value="${t.numero}" placeholder="8"></div>
    <div class="field"><label>Ciclo</label><input class="input" name="ciclo" value="${t.ciclo}" pattern="\d{4}-[12]" placeholder="2026-2"></div></div>
  <div class="form-row">
    <div class="field"><label>Hora inicio</label><input class="input" type="time" name="horaInicio" value="${t.horaInicioIso}"></div>
    <div class="field"><label>Hora fin</label><input class="input" type="time" name="horaFin" value="${t.horaFinIso}"></div>
    <div class="field"><label>Lugar</label><input class="input" name="lugar" value="<c:out value='${t.lugar}'/>" placeholder="Ej: Explanada del polideportivo"></div></div>
</div>

<div class="card"><h2>Maestro(a) de taller y música</h2>
  <div class="form-row">
    <div class="field"><label>Maestro(a) de taller</label><select class="input" name="maestro"><option value="">—</option>
      <c:forEach var="m" items="${activos}"><option value="${m.idMiembro}" ${not empty t.personasDe('MAESTRO', null) and t.personasDe('MAESTRO', null)[0].idMiembro == m.idMiembro ? 'selected' : ''}><c:out value="${m.nombreConApodo}"/></option></c:forEach></select></div>
    <div class="field"><label>Música social (miembro)</label><select class="input" name="musica"><option value="">—</option>
      <c:forEach var="m" items="${activos}"><option value="${m.idMiembro}" ${not empty t.personasDe('MUSICA', null) and t.personasDe('MUSICA', null)[0].idMiembro == m.idMiembro ? 'selected' : ''}><c:out value="${m.nombreConApodo}"/></option></c:forEach></select></div>
    <div class="field"><label>…o cómo sale en el flyer</label><input class="input" name="musicaTexto" maxlength="120" value="<c:out value='${t.musicaTexto}'/>" placeholder="Ej: Didi-Djei chef"></div></div>
</div>

<c:forEach var="n" items="${t.niveles}">
  <c:set var="tals" value="${t.personasDe('TALLERISTA', n)}"/><c:set var="apos" value="${t.personasDe('APOYO', n)}"/>
  <div class="card nivel-card"><h2>${n == 'PRINCIPIANTE' ? 'Principiante' : (n == 'BASICO' ? 'Básico' : (n == 'INTRO_RUEDA' ? 'Intro a la rueda' : 'Rueda básica'))}</h2>
    <div class="form-row">
      <c:forEach var="i" begin="0" end="1">
        <div class="field"><label>Tallerista ${i + 1}</label><select class="input" name="tal_${n}"><option value="">—</option>
          <c:forEach var="m" items="${activos}"><option value="${m.idMiembro}" ${i < fn:length(tals) and tals[i].idMiembro == m.idMiembro ? 'selected' : ''}><c:out value="${m.nombreConApodo}"/></option></c:forEach></select></div>
      </c:forEach></div>
    <div class="form-row">
      <c:forEach var="i" begin="0" end="2">
        <div class="field"><label>Apoyo ${i + 1}</label><select class="input" name="apo_${n}"><option value="">—</option>
          <c:forEach var="m" items="${activos}"><option value="${m.idMiembro}" ${i < fn:length(apos) and apos[i].idMiembro == m.idMiembro ? 'selected' : ''}><c:out value="${m.nombreConApodo}"/></option></c:forEach></select></div>
      </c:forEach></div>

    <span class="label">Cronograma del flyer</span>
    <div class="bloques" data-nivel="${n}">
      <c:forEach var="b" items="${t.bloques[n]}">
        <div class="bloque"><input class="input" name="bh_${n}" value="<c:out value='${b[0]}'/>" placeholder="7:30 - 7:45"><input class="input" name="ba_${n}" value="<c:out value='${b[1]}'/>" placeholder="Actividad"><input class="input" name="bd_${n}" value="<c:out value='${b[2]}'/>" placeholder="15'"><button type="button" class="btn btn-ghost btn-sm quitar" title="Quitar fila">✕</button></div>
      </c:forEach>
      <div class="bloque"><input class="input" name="bh_${n}" placeholder="7:30 - 7:45"><input class="input" name="ba_${n}" placeholder="Actividad (ej: Secuencia, 🎶💃 MÚSICA 🎶💃)"><input class="input" name="bd_${n}" placeholder="15'"><button type="button" class="btn btn-ghost btn-sm quitar" title="Quitar fila">✕</button></div>
    </div>
    <div><button type="button" class="btn btn-ghost btn-sm mas-bloque" data-nivel="${n}">+ Fila</button></div>

    <div class="field"><label>Pasos planificados</label>
      <textarea class="input" name="pasos_${n}" rows="7" placeholder="Un paso por línea. Escribe MÚSICA para separar bloques.&#10;Ej:&#10;Dile que no&#10;Guapeo&#10;MÚSICA&#10;Enchufla"><c:out value="${pasosTexto[n]}"/></textarea>
      <span class="muted xs">Toca un paso de la lista para agregarlo. Si no está, escríbelo: se suma solo al catálogo.</span>
      <div class="chips-pasos" data-destino="pasos_${n}">
        <button type="button" class="chip-paso musica">🎶 MÚSICA</button>
        <c:forEach var="p" items="${catalogo[n]}" end="40"><button type="button" class="chip-paso"><c:out value="${p.nombre}"/></button></c:forEach>
      </div></div>
  </div>
</c:forEach>

<div class="card"><h2>Notas</h2><textarea class="input" name="notas" rows="3" maxlength="1000" placeholder="Opcional"><c:out value="${t.notas}"/></textarea></div>
<div class="inline" style="justify-content:flex-end"><button class="btn btn-secondary" name="publicar" value="borrador">Guardar borrador</button><button class="btn btn-primary" name="publicar" value="si">Guardar cronograma</button></div>
</form>
<script>
  document.querySelectorAll('.mas-bloque').forEach(function (b) {
    b.addEventListener('click', function () {
      var cont = document.querySelector('.bloques[data-nivel="' + b.dataset.nivel + '"]');
      var fila = cont.lastElementChild.cloneNode(true);
      fila.querySelectorAll('input').forEach(function (i) { i.value = ''; });
      cont.appendChild(fila);
    });
  });
  document.addEventListener('click', function (e) {
    if (e.target.classList.contains('quitar')) {
      var cont = e.target.closest('.bloques');
      if (cont.children.length > 1) e.target.closest('.bloque').remove();
      else e.target.closest('.bloque').querySelectorAll('input').forEach(function (i) { i.value = ''; });
    }
    if (e.target.classList.contains('chip-paso')) {
      var ta = document.querySelector('textarea[name="' + e.target.parentElement.dataset.destino + '"]');
      var txt = e.target.classList.contains('musica') ? 'MÚSICA' : e.target.textContent.trim();
      ta.value = ta.value.replace(/\s*$/, '') + (ta.value.trim() ? '\n' : '') + txt + '\n';
      ta.focus();
    }
  });
</script>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
