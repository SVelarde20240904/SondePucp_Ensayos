<c:set var="nuevo" value="${k.nuevo}"/>
<c:set var="titulo" value="${nuevo ? 'Nuevo contacto' : k.nombre}"/><c:set var="menu" value="RRPP"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<section class="hero"><div class="titles"><span class="kicker">Directorio${nuevo ? '' : ' · '.concat(k.categoriaTexto)}</span><h1><c:out value="${titulo}"/></h1>
  <c:if test="${not nuevo}"><p><c:out value="${k.persona}"/>${empty k.cargo ? '' : ' · '}<c:out value="${k.cargo}"/> · último contacto: ${k.ultimoTexto}</p></c:if></div>
  <div class="actions"><a class="btn btn-secondary" href="${ctx}/rrpp/directorio?cat=${k.categoria}">← Directorio</a></div>
  <span class="star"></span></section>
<c:set var="grupo" value="rrpp"/><c:set var="sub" value="directorio"/><%@ include file="/WEB-INF/jsp/includes/subtabs.jspf" %>

<div class="row top">
  <form class="card grow" method="post" action="${ctx}/rrpp/contacto"><input type="hidden" name="_csrf" value="${sessionScope.csrf}">
    <c:if test="${not nuevo}"><input type="hidden" name="id" value="${k.id}"></c:if>
    <div class="form-row">
      <div class="field"><label for="categoria">Categoría</label><select class="input" id="categoria" name="categoria">
        <c:forEach var="c" items="${categorias}"><option value="${c}" ${c == k.categoria ? 'selected' : ''}>${c == 'RUEDA' ? 'Rueda' : (c == 'ACADEMIA' ? 'Academia' : (c == 'CENTRO_FEDERADO' ? 'Centro federado' : (c == 'H_T' ? 'H o T' : (c == 'AUTORIDAD' ? 'Autoridad u oficina' : 'Otro'))))}</option></c:forEach></select></div>
      <div class="field"><label for="nombre">Nombre</label><input class="input" id="nombre" name="nombre" maxlength="120" required value="${fn:escapeXml(k.nombre)}" placeholder="Ej: Bemberé Callao · CF de Ciencias · H-125 · DAES"></div>
    </div>
    <div class="form-row">
      <div class="field"><label for="persona">Persona de contacto</label><input class="input" id="persona" name="persona" maxlength="120" value="${fn:escapeXml(k.persona)}" placeholder="Director(a), presidente(a), JH…"></div>
      <div class="field"><label for="cargo">Cargo</label><input class="input" id="cargo" name="cargo" maxlength="80" value="${fn:escapeXml(k.cargo)}" placeholder="Ej: Director · JH · Coordinadora"></div>
    </div>
    <div class="form-row">
      <div class="field"><label for="telefono">Celular</label><input class="input" id="telefono" name="telefono" maxlength="30" inputmode="tel" value="${fn:escapeXml(k.telefono)}" placeholder="9xx xxx xxx"></div>
      <div class="field"><label for="instagram">Instagram</label><input class="input" id="instagram" name="instagram" maxlength="60" value="${fn:escapeXml(k.instagram)}" placeholder="@rueda"></div>
    </div>
    <div class="field"><label for="correo">Correo</label><input class="input" type="email" id="correo" name="correo" maxlength="120" value="${fn:escapeXml(k.correo)}"></div>
    <div class="form-row">
      <div class="field"><label for="ubicacion">Dónde</label><input class="input" id="ubicacion" name="ubicacion" maxlength="200" value="${fn:escapeXml(k.ubicacion)}" placeholder="Distrito, local, facultad…"></div>
      <div class="field"><label for="horario">Horario / cuándo hay social</label><input class="input" id="horario" name="horario" maxlength="200" value="${fn:escapeXml(k.horario)}" placeholder="Ej: Social los viernes 8 pm"></div>
    </div>
    <div class="form-row">
      <div class="field"><label for="linkMapa">Google Maps</label><input class="input" type="url" id="linkMapa" name="linkMapa" maxlength="500" value="${fn:escapeXml(k.linkMapa)}" placeholder="https://maps.app.goo.gl/…"></div>
      <div class="field"><label for="ciclo">Ciclo <span class="muted">(H's y T's)</span></label><input class="input" id="ciclo" name="ciclo" maxlength="10" value="${fn:escapeXml(k.ciclo)}" placeholder="2026-2"></div>
    </div>
    <div class="form-row">
      <div class="field"><label for="estado">Estado</label><select class="input" id="estado" name="estado">
        <c:forEach var="e" items="${estados}"><option value="${e}" ${e == k.estado ? 'selected' : ''}>${e == 'SIN_CONTACTAR' ? 'Sin contactar' : (e == 'CONTACTADO' ? 'Contactado' : (e == 'RESPONDIO' ? 'Respondió' : 'Aliado'))}</option></c:forEach></select></div>
      <div class="field"><label for="responsable">Responsable (RRPP)</label><select class="input" id="responsable" name="responsable"><option value="">— Nadie —</option>
        <c:forEach var="e" items="${equipo}"><option value="${e.id}" ${e.id == k.idResponsable ? 'selected' : ''}><c:out value="${e.nombre}"/></option></c:forEach></select></div>
    </div>
    <div class="field"><label for="notas">Notas</label><textarea class="input" id="notas" name="notas" rows="3" maxlength="1000" placeholder="Qué nos interesa con ellos, cómo nos llevamos, qué les ofrecimos…"><c:out value="${k.notas}"/></textarea></div>
    <div class="inline"><button class="btn btn-primary" name="action" value="${nuevo ? 'crear' : 'guardar'}">${nuevo ? 'Agregar' : 'Guardar'}</button>
      <c:if test="${not nuevo and puedeModerar}"><button class="btn btn-ghost" name="action" value="eliminar" formnovalidate onclick="return confirm('¿Eliminar este contacto y su historial?')">Eliminar</button></c:if></div>
  </form>
  <c:if test="${not nuevo}">
  <div class="stack w-440">
    <form class="card" method="post" action="${ctx}/rrpp/contacto"><input type="hidden" name="_csrf" value="${sessionScope.csrf}"><input type="hidden" name="id" value="${k.id}">
      <h2>Anotar contacto</h2>
      <div class="field"><textarea class="input" name="nota" rows="3" maxlength="500" required placeholder="Ej: Le escribí por el taller del jueves, dijo que traen 5 cachimbos"></textarea></div>
      <div class="form-row">
        <div class="field"><label for="fechaNota">Fecha</label><input class="input" type="date" id="fechaNota" name="fechaNota" value="${hoy}" max="${hoy}"></div>
        <div class="field"><label for="nuevoEstado">Nuevo estado</label><select class="input" id="nuevoEstado" name="nuevoEstado"><option value="">Igual</option>
          <c:forEach var="e" items="${estados}"><option value="${e}">${e == 'SIN_CONTACTAR' ? 'Sin contactar' : (e == 'CONTACTADO' ? 'Contactado' : (e == 'RESPONDIO' ? 'Respondió' : 'Aliado'))}</option></c:forEach></select></div>
      </div>
      <div><button class="btn btn-secondary btn-sm" name="action" value="interaccion">Anotar</button></div>
    </form>
    <div class="card"><h2>Historial</h2>
      <c:choose>
        <c:when test="${empty k.historial}"><p class="empty">Sin contactos anotados.</p></c:when>
        <c:otherwise><ul class="timeline-simple">
          <c:forEach var="h" items="${k.historial}"><li><b>${h.fechaTexto}</b> <span class="muted xs"><c:out value="${h.quien}"/></span><p class="small"><c:out value="${h.nota}"/></p></li></c:forEach>
        </ul></c:otherwise>
      </c:choose>
      <c:if test="${not empty k.visitas}"><h3 class="mt">Visitas formales</h3><ul class="icon-list small"><c:forEach var="v" items="${k.visitas}"><li>🚌 <a class="link" href="${ctx}/rrpp/visita?id=${v.id}">${v.fechaTexto} · <c:out value="${v.titulo}"/></a></li></c:forEach></ul></c:if>
    </div>
  </div>
  </c:if>
</div>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
