<c:set var="nueva" value="${empty f}"/>
<c:set var="titulo" value="${nueva ? 'Nueva figura' : 'Editar figura'}"/><c:set var="menu" value="Figuras"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<form method="post" action="${ctx}/admin/figuras/${nueva ? 'nueva' : 'editar'}" class="stack" style="gap:24px">
<input type="hidden" name="idFigura" value="${nueva ? '' : f.idFigura}">
<section class="hero"><div class="titles"><span class="kicker">Administración › Mano de figuras</span>
  <h1>${nueva ? 'Nueva figura' : 'Editar figura · '}<c:if test="${not nueva}"><c:out value="${f.nombre}"/></c:if></h1>
  <p>${nueva ? 'Aparece sin color (“Sin nivel”) hasta que el grupo la evalúe. Cuando tenga color, puede cambiar el nivel de algunas personas.'
             : (empty f.color ? 'Sin nivel todavía' : 'Nivel '.concat(f.color)).concat(' · ').concat(f.NEvaluaciones).concat(' evaluaciones · enseñada en ').concat(f.NEnsenada).concat(' ensayos')}</p></div>
  <div class="actions"><a class="btn btn-secondary" href="${ctx}/admin/figuras">Cancelar</a><button class="btn btn-primary" name="action" value="guardar">${nueva ? 'Guardar figura' : 'Guardar cambios'}</button></div>
  <span class="star"></span></section>

<c:if test="${not nueva}">
<div class="grid-4">
  <div class="card stat"><b style="display:flex"><sdp:chip color="${f.color}"/></b><span>color actual (mediana)</span></div>
  <div class="card stat"><b>${f.NEvaluaciones}</b><span>evaluaciones</span></div>
  <div class="card stat"><b class="c-amarillo">${f.porcentajeSaben}%</b><span>la saben (Verde o Azul)</span></div>
  <div class="card stat"><b class="c-primary">${f.NEnsenada}</b><span>veces enseñada</span></div>
</div>
</c:if>

<div class="row top">
  <div class="card grow"><h2>Datos de la figura</h2>
    <div class="form-row">
      <div class="field"><label>Nombre *</label><input class="input" name="nombre" value="<c:out value='${f.nombre}'/>" placeholder="Ej: Paseo de la Hélice" required></div>
      <div class="field"><label>Posición en la mano</label><select class="input" name="despuesDe">
        <option value="">${nueva ? 'Al final de la mano' : 'Dejar donde está'}</option>
        <c:forEach var="x" items="${todas}"><c:if test="${nueva or x.idFigura != f.idFigura}"><option value="${x.idFigura}" ${not nueva and x.idFigura == anterior ? 'selected' : ''}>Después de <c:out value="${x.nombre}"/></option></c:if></c:forEach></select></div></div>
    <div class="field"><label>Descripción o nota (opcional)</label><textarea class="input" name="descripcion" rows="3" placeholder="Ej: variación de Hélice con salida por la derecha"><c:out value="${f.descripcion}"/></textarea></div>
  </div>
  <c:choose>
    <c:when test="${nueva}">
      <div class="card w-340"><h2>Qué pasa al guardarla</h2>
        <p class="small muted">• Se agrega a la mano con el chip “Sin nivel”.</p><p class="small muted">• Los miembros la ven en su autoevaluación.</p>
        <p class="small muted">• Cuando tenga evaluaciones, su color será la mediana del grupo.</p>
        <p class="small muted">• Si el nombre ya existía en una figura inactiva, esa figura se reactiva (no se duplica).</p></div>
    </c:when>
    <c:otherwise>
      <div class="stack w-440">
        <div class="card" style="border-color:var(--amarillo-soft)"><h2>${f.activa ? 'Desactivar' : 'Reactivar'}</h2>
          <p>${f.activa ? 'Sale de la mano, de las autoevaluaciones y del cálculo de niveles. Se conserva todo su historial y se puede reactivar cuando quieras.' : 'Vuelve a la mano y a los cálculos con sus evaluaciones anteriores.'}</p>
          <div><button class="btn btn-secondary" name="action" value="${f.activa ? 'desactivar' : 'reactivar'}" formnovalidate>${f.activa ? 'Desactivar figura' : 'Reactivar figura'}</button></div></div>
        <div class="card" style="border-color:var(--rojo-soft)"><h2>Eliminar</h2><p>Borra la figura para siempre. Solo se permite si nunca se evaluó ni se enseñó (por ejemplo, si se creó por error).</p>
          <div><button class="btn btn-danger" name="action" value="eliminar" formnovalidate ${f.puedeEliminar ? '' : 'disabled'} onclick="return confirm('¿Eliminar la figura para siempre?')">Eliminar figura</button></div>
          <c:if test="${not f.puedeEliminar}"><div class="note warn">No disponible: tiene ${f.NEvaluaciones} evaluaciones y se enseñó en ${f.NEnsenada} ensayos. Desactívala en su lugar.</div></c:if></div>
      </div>
    </c:otherwise>
  </c:choose>
</div>
</form>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
