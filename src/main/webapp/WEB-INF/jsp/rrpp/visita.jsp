<c:set var="nuevo" value="${v.nuevo}"/>
<c:set var="titulo" value="${nuevo ? 'Nueva visita formal' : v.titulo}"/><c:set var="menu" value="RRPP"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<section class="hero"><div class="titles"><span class="kicker">Visita formal${nuevo ? '' : ' · '.concat(v.estadoTexto)}</span><h1><c:out value="${titulo}"/></h1>
  <c:if test="${not nuevo}"><p>${v.fechaTexto}${empty v.horaTexto ? '' : ' · '.concat(v.horaTexto)}${empty v.cuando ? '' : ' · '.concat(v.cuando)}</p></c:if></div>
  <div class="actions"><a class="btn btn-secondary" href="${ctx}/rrpp/visitas">← Visitas</a></div>
  <span class="star"></span></section>
<c:set var="grupo" value="rrpp"/><c:set var="sub" value="visitas"/><%@ include file="/WEB-INF/jsp/includes/subtabs.jspf" %>

<c:if test="${not nuevo}">
  <form method="post" action="${ctx}/rrpp/visita" class="estado-flujo"><input type="hidden" name="_csrf" value="${sessionScope.csrf}"><input type="hidden" name="id" value="${v.id}"><input type="hidden" name="action" value="estado">
    <button class="${v.estado == 'BORRADOR' ? 'on' : ''}" name="estado" value="BORRADOR" ${v.estado == 'BORRADOR' ? 'disabled' : ''}>📝 Borrador</button>
    <button class="${v.estado == 'CONVOCADA' ? 'on' : ''}" name="estado" value="CONVOCADA" ${v.estado == 'CONVOCADA' ? 'disabled' : ''}>📣 Convocar a todos</button>
    <button class="${v.estado == 'REALIZADA' ? 'on' : ''}" name="estado" value="REALIZADA" ${v.estado == 'REALIZADA' ? 'disabled' : ''}>✅ Realizada</button>
    <button class="${v.estado == 'CANCELADA' ? 'on' : ''}" name="estado" value="CANCELADA" ${v.estado == 'CANCELADA' ? 'disabled' : ''}>✕ Cancelada</button>
  </form>
  <c:if test="${v.estado == 'BORRADOR'}"><p class="note info">Es un borrador: nadie más lo ve. Cuando esté listo, «Convocar a todos» y a cada miembro le sale en Pendientes.</p></c:if>
</c:if>

<div class="row top">
  <form class="card grow" method="post" action="${ctx}/rrpp/visita"><input type="hidden" name="_csrf" value="${sessionScope.csrf}">
    <c:if test="${not nuevo}"><input type="hidden" name="id" value="${v.id}"></c:if>
    <c:if test="${nuevo and not empty v.idEncuesta}"><input type="hidden" name="idEncuesta" value="${v.idEncuesta}"><p class="note info">Sale de una encuesta del grupo.</p></c:if>
    <div class="field"><label for="titulo">Título</label><input class="input" id="titulo" name="titulo" maxlength="120" required value="${fn:escapeXml(v.titulo)}" placeholder="Ej: Visita al social de Bemberé Callao"></div>
    <div class="form-row">
      <div class="field"><label for="idContacto">Rueda o academia <span class="muted">(del directorio)</span></label><select class="input" id="idContacto" name="idContacto"><option value="">— Otra / evento —</option>
        <c:forEach var="k" items="${ruedas}"><option value="${k.id}" ${k.id == v.idContacto ? 'selected' : ''}><c:out value="${k.nombre}"/></option></c:forEach></select></div>
      <div class="field"><label for="coordinador">Coordina (RRPP)</label><select class="input" id="coordinador" name="coordinador"><option value="">— Nadie todavía —</option>
        <c:forEach var="e" items="${equipo}"><option value="${e.id}" ${e.id == v.idCoordinador ? 'selected' : ''}><c:out value="${e.nombre}"/></option></c:forEach></select></div>
    </div>
    <div class="form-row">
      <div class="field"><label for="fecha">Fecha</label><input class="input" type="date" id="fecha" name="fecha" required value="${v.fecha}"></div>
      <div class="field"><label for="hora">Hora del evento</label><input class="input" type="time" id="hora" name="hora" value="${v.hora}"></div>
    </div>
    <div class="field"><label for="lugar">Lugar</label><input class="input" id="lugar" name="lugar" maxlength="200" value="${fn:escapeXml(v.lugar)}" placeholder="Ej: Local de Bemberé, Av. … (Callao)"></div>
    <div class="field"><label for="linkMapa">Link de Google Maps</label><input class="input" type="url" id="linkMapa" name="linkMapa" maxlength="500" value="${fn:escapeXml(v.linkMapa)}" placeholder="https://maps.app.goo.gl/…"></div>
    <div class="form-row">
      <div class="field"><label for="puntoEncuentro">Punto de encuentro</label><input class="input" id="puntoEncuentro" name="puntoEncuentro" maxlength="200" value="${fn:escapeXml(v.puntoEncuentro)}" placeholder="Ej: Puerta principal PUCP"></div>
      <div class="field"><label for="horaEncuentro">Hora de encuentro</label><input class="input" type="time" id="horaEncuentro" name="horaEncuentro" value="${v.horaEncuentro}"></div>
    </div>
    <div class="field"><label for="comoLlegar">Cómo llegamos</label><textarea class="input" id="comoLlegar" name="comoLlegar" rows="2" maxlength="500" placeholder="Quién jala carro, en qué bus, taxis compartidos…"><c:out value="${v.comoLlegar}"/></textarea></div>
    <div class="form-row">
      <div class="field"><label for="costo">Entrada</label><input class="input" id="costo" name="costo" maxlength="80" value="${fn:escapeXml(v.costo)}" placeholder="Ej: S/ 15 · Libre"></div>
      <div class="field"><label for="dressCode">Dress code</label><input class="input" id="dressCode" name="dressCode" maxlength="120" value="${fn:escapeXml(v.dressCode)}"></div>
    </div>
    <div class="field"><label for="notas">Notas</label><textarea class="input" id="notas" name="notas" rows="2" maxlength="1000"><c:out value="${v.notas}"/></textarea></div>
    <div class="field"><label for="linkFoto">Fotos de la visita <span class="muted">(Drive restringido al grupo de SDP)</span></label><input class="input" type="url" id="linkFoto" name="linkFoto" maxlength="500" value="${fn:escapeXml(v.linkFoto)}" placeholder="https://drive.google.com/…"></div>
    <div class="inline"><button class="btn btn-primary" name="action" value="${nuevo ? 'crear' : 'guardar'}">${nuevo ? 'Crear borrador' : 'Guardar cambios'}</button>
      <c:if test="${not nuevo and puedeModerar}"><button class="btn btn-ghost" name="action" value="eliminar" formnovalidate onclick="return confirm('¿Eliminar esta visita y sus respuestas?')">Eliminar</button></c:if></div>
  </form>

  <c:if test="${not nuevo}">
  <div class="stack w-440">
    <div class="card"><h2>Checklist de RRPP</h2>
      <ul class="tareas">
        <c:forEach var="p" items="${v.checklist}">
          <li class="${p.hecho ? 'hecha' : ''}"><form method="post" action="${ctx}/rrpp/visita" class="inline-form"><input type="hidden" name="_csrf" value="${sessionScope.csrf}"><input type="hidden" name="id" value="${v.id}">
            <input type="hidden" name="paso" value="${p.codigo}"><input type="hidden" name="hecho" value="${p.hecho ? '0' : '1'}">
            <button class="tcheck" name="action" value="check" aria-label="${p.hecho ? 'Desmarcar' : 'Marcar'}">${p.hecho ? '☑' : '☐'}</button><span>${p.texto}</span></form></li>
        </c:forEach>
      </ul>
    </div>
    <div class="card"><h2>¿Quiénes van?</h2>
      <p class="small"><b class="c-verde">${v.van} van</b> · ${v.noPueden} no pueden · ${activos - v.van - v.noPueden} sin responder</p>
      <c:if test="${not empty resp.VOY}"><p class="small"><b>Van:</b> <c:forEach var="n" items="${resp.VOY}" varStatus="st"><c:out value="${n}"/>${st.last ? '' : ', '}</c:forEach></p></c:if>
      <c:if test="${not empty resp.NO_PUEDO}"><p class="small muted"><b>No pueden:</b> <c:forEach var="n" items="${resp.NO_PUEDO}" varStatus="st"><c:out value="${n}"/>${st.last ? '' : ', '}</c:forEach></p></c:if>
    </div>
    <c:if test="${not empty v.idProyecto}"><p class="note ok">📸 Comunica ya tiene el proyecto del post. <a class="link" href="${ctx}/comunica/proyecto?id=${v.idProyecto}">Verlo</a></p></c:if>
    <c:if test="${empty v.idProyecto}"><p class="muted small">Al marcarla como realizada, a Comunica le llega el proyecto del post con el link de las fotos.</p></c:if>
  </div>
  </c:if>
</div>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
