<c:set var="titulo" value="${t.titulo}"/><c:set var="menu" value="Talleres"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<section class="hero"><div class="titles"><span class="kicker">Team Talleres · ciclo ${t.ciclo}</span><h1>${t.titulo} · ${t.fechaCorta}</h1>
  <p>${t.fechaTexto} · ${t.horario}<c:if test="${not empty t.lugar}"> · <c:out value="${t.lugar}"/></c:if></p>
  <div class="inline"><sdp:tag texto="${t.estadoTexto}" tono="${t.estadoTono}"/><c:if test="${t.finalRegistrado}"><span class="tag ok">Dictado final registrado</span></c:if></div></div>
  <div class="actions"><a class="btn btn-secondary" href="${ctx}/talleres">← Talleres</a>
    <a class="btn btn-secondary" href="${ctx}/talleres/editar?id=${t.idTaller}">Editar cronograma</a>
    <a class="btn btn-secondary" href="${ctx}/talleres/flyer?id=${t.idTaller}" target="_blank">Ver flyer</a>
    <a class="btn btn-primary" href="${ctx}/talleres/feedback?id=${t.idTaller}">Feedback (${nFeedback})</a></div>
  <span class="star"></span></section>

<c:if test="${yoEnTaller}"><div class="note info inline" style="justify-content:space-between">Estuviste en este taller.
  <a class="btn btn-primary btn-sm" href="${ctx}/talleres/formulario?id=${t.idTaller}">${respuestas.containsKey(u.idMiembro) ? 'Ver o corregir mi formulario' : 'Llenar mi formulario'}</a></div></c:if>

<div class="grid-2 niveles-taller">
<c:forEach var="n" items="${t.niveles}">
  <div class="card"><h2>${n == 'PRINCIPIANTE' ? 'Principiante' : (n == 'BASICO' ? 'Básico' : (n == 'INTRO_RUEDA' ? 'Intro a la rueda' : 'Rueda básica'))}</h2>
    <c:forEach var="p" items="${t.personasDe('TALLERISTA', n)}"><div class="persona-taller"><span><b><c:out value="${p.nombre}"/></b><small>Tallerista${p.planeado ? '' : (empty p.reemplazaNombre ? ' · se sumó' : ' · reemplazó a '.concat(p.reemplazaNombre))}</small></span><sdp:tag texto="${p.asistenciaTexto}" tono="${p.asistenciaTono}"/></div></c:forEach>
    <c:forEach var="p" items="${t.personasDe('APOYO', n)}"><div class="persona-taller"><span><c:out value="${p.nombre}"/><small>Apoyo${p.planeado ? '' : (empty p.reemplazaNombre ? ' · se sumó' : ' · reemplazó a '.concat(p.reemplazaNombre))}</small></span><sdp:tag texto="${p.asistenciaTexto}" tono="${p.asistenciaTono}"/></div></c:forEach>
    <c:if test="${empty t.personasDe('TALLERISTA', n)}"><p class="muted small">Sin talleristas asignados.</p></c:if>
    <span class="label" style="margin-top:8px">Pasos</span>
    <c:choose>
      <c:when test="${empty t.pasos[n]}"><p class="muted small">Sin pasos registrados.</p></c:when>
      <c:otherwise>
        <ul class="pasos-lista">
          <c:set var="sec" value="1"/>
          <c:forEach var="p" items="${t.pasos[n]}">
            <c:if test="${p.planificado and p.seccion > sec}"><li class="musica">🎶 Música</li><c:set var="sec" value="${p.seccion}"/></c:if>
            <li>${p.marca} <c:out value="${p.nombre}"/><c:if test="${not p.planificado}"> <span class="tag info">agregado</span></c:if></li>
          </c:forEach>
        </ul>
        <p class="muted xs">${empty t.checklistPor[n] ? 'Checklist pendiente (lo llena uno de los talleristas).' : 'Checklist de '.concat(t.checklistPor[n])}</p>
      </c:otherwise>
    </c:choose>
  </div>
</c:forEach>
</div>
<c:set var="sinNivel" value="false"/><c:forEach var="p" items="${t.personas}"><c:if test="${empty p.nivel}"><c:set var="sinNivel" value="true"/></c:if></c:forEach>
<c:if test="${sinNivel or not empty t.musicaTexto}"><div class="card"><h2>Maestro(a), música y apoyos generales</h2>
  <c:forEach var="p" items="${t.personas}"><c:if test="${empty p.nivel}"><div class="persona-taller"><span><c:out value="${p.nombre}"/><small>${p.funcionTexto}${p.planeado ? '' : ' · se sumó'}</small></span><sdp:tag texto="${p.asistenciaTexto}" tono="${p.asistenciaTono}"/></div></c:if></c:forEach>
  <c:if test="${not empty t.musicaTexto}"><div class="persona-taller"><span><c:out value="${t.musicaTexto}"/><small>Música social (como sale en el flyer)</small></span></div></c:if>
</div></c:if>

<div class="row top">
  <form class="card grow" method="post" action="${ctx}/talleres/detalle"><input type="hidden" name="_csrf" value="${sessionScope.csrf}"><input type="hidden" name="id" value="${t.idTaller}">
    <h2>Dictado final</h2><p>Lo que pasó de verdad: lo marca cualquiera del team${t.finalRegistrado ? ' · último registro: '.concat(t.finalPor) : ''}</p>
    <table class="table cards-compact"><thead><tr><th>Persona</th><th>Función</th><th>¿Cómo llegó?</th></tr></thead><tbody>
      <c:forEach var="p" items="${t.personas}">
        <tr><td><b><c:out value="${p.nombre}"/></b><c:if test="${not p.planeado}"><small>${empty p.reemplazaNombre ? 'Se sumó' : 'Reemplazó a '.concat(p.reemplazaNombre)}</small></c:if></td>
          <td>${p.funcionTexto}${empty p.nivel ? '' : ' · '.concat(p.nivelTexto)}</td>
          <td class="no-label"><div class="inline" style="flex-wrap:nowrap"><select class="input" name="asis_${p.id}" style="min-width:150px">
            <option value="">Sin registrar</option>
            <option value="A_TIEMPO" ${p.asistencia == 'A_TIEMPO' ? 'selected' : ''}>A tiempo</option>
            <option value="TARDE" ${p.asistencia == 'TARDE' ? 'selected' : ''}>Llegó tarde</option>
            <option value="NO_SE_PRESENTO" ${p.asistencia == 'NO_SE_PRESENTO' ? 'selected' : ''}>No se presentó</option></select>
            <c:if test="${not p.planeado}"><button class="btn btn-ghost btn-sm" name="action" value="quitarExtra" formnovalidate onclick="this.form.idPersona.value='${p.id}'" title="Quitar">✕</button></c:if></div></td></tr>
      </c:forEach>
      <c:if test="${empty t.personas}"><tr><td colspan="3" class="empty">Primero arma el cronograma con quién dicta cada nivel.</td></tr></c:if>
    </tbody></table>
    <input type="hidden" name="idPersona" value="">
    <span class="label">¿Alguien se sumó o reemplazó a otra persona?</span>
    <div class="form-row">
      <div class="field"><label>Quién</label><select class="input" name="extraMiembro"><option value="">—</option><c:forEach var="m" items="${activos}"><option value="${m.idMiembro}"><c:out value="${m.nombreConApodo}"/></option></c:forEach></select></div>
      <div class="field"><label>Como</label><select class="input" name="extraFuncion"><option value="TALLERISTA">Tallerista</option><option value="APOYO">Apoyo</option><option value="MAESTRO">Maestro(a) de taller</option><option value="MUSICA">Música social</option></select></div>
      <div class="field"><label>Nivel</label><select class="input" name="extraNivel"><c:forEach var="n" items="${t.niveles}"><option value="${n}">${n == 'PRINCIPIANTE' ? 'Principiante' : (n == 'BASICO' ? 'Básico' : (n == 'INTRO_RUEDA' ? 'Intro a la rueda' : 'Rueda básica'))}</option></c:forEach></select></div></div>
    <div class="form-row">
      <div class="field"><label>Reemplazó a (opcional)</label><select class="input" name="extraReemplaza"><option value="">— nadie, se sumó —</option><c:forEach var="p" items="${t.planeadas}"><option value="${p.idMiembro}"><c:out value="${p.nombre}"/> (${p.codigo})</option></c:forEach></select></div>
      <div class="field"><label>Llegó</label><select class="input" name="extraAsistencia"><option value="A_TIEMPO">A tiempo</option><option value="TARDE">Tarde</option></select></div></div>
    <div class="inline" style="justify-content:flex-end"><button class="btn btn-primary" name="action" value="final">Guardar dictado final</button></div>
  </form>

  <div class="stack w-340">
    <div class="card"><h2>Formularios</h2><p>Hasta el ${t.plazoTexto}${t.plazoVencido ? ' (ya venció)' : ''}</p>
      <c:set var="hayEsperados" value="false"/>
      <c:forEach var="p" items="${t.personas}"><c:if test="${(p.funcion == 'TALLERISTA' or p.funcion == 'APOYO') and p.efectivo}"><c:set var="hayEsperados" value="true"/>
        <div class="persona-taller"><span><c:out value="${p.nombre}"/><small>${p.codigo}</small></span>
          <c:choose><c:when test="${respuestas.containsKey(p.idMiembro)}"><span class="tag ok">✓ ${respuestas[p.idMiembro]}</span></c:when><c:otherwise><span class="tag warn">Pendiente</span></c:otherwise></c:choose></div>
      </c:if></c:forEach>
      <c:if test="${not hayEsperados}"><p class="empty">Nadie asignado todavía.</p></c:if>
    </div>
    <div class="card"><h2>Sugerencias para el taller</h2>
      <c:forEach var="s" items="${sugerencias}"><div class="sugerencia"><p><c:out value="${s.texto}"/></p><span class="muted xs">${s.de} · ${s.fechaTexto}</span></div></c:forEach>
      <c:if test="${empty sugerencias}"><p class="empty">Sin sugerencias todavía.</p></c:if>
    </div>
    <form class="card" method="post" action="${ctx}/talleres/detalle"><input type="hidden" name="_csrf" value="${sessionScope.csrf}"><input type="hidden" name="id" value="${t.idTaller}"><h2>Estado del taller</h2>
      <div class="inline">
        <c:choose><c:when test="${t.estado == 'CANCELADO'}"><button class="btn btn-secondary btn-sm" name="action" value="reactivar">Reactivar</button></c:when>
          <c:otherwise><button class="btn btn-secondary btn-sm" name="action" value="cancelar" onclick="return confirm('¿Cancelar este taller?')">Cancelar taller</button></c:otherwise></c:choose>
        <c:if test="${perm.contains('ASIGNAR_RESPONSABILIDADES_TALLERES')}"><button class="btn btn-danger btn-sm" name="action" value="eliminar" onclick="return confirm('¿ELIMINAR este taller para siempre? Se borran su cronograma, dictado, pasos y feedback.')">🗑 Eliminar</button></c:if>
      </div></form>
  </div>
</div>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
