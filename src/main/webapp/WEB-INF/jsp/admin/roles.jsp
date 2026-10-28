<c:set var="titulo" value="Roles y permisos"/><c:set var="menu" value="Roles y permisos"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<section class="hero"><div class="titles"><span class="kicker">Administración</span><h1>Roles y permisos</h1>
  <p>Quién puede hacer qué en el sistema. Todos son Miembro; algunos además tienen otros roles (se suman).</p></div><span class="star"></span></section>

<div class="row top">
  <form class="card grow" method="post" action="${ctx}/admin/roles"><input type="hidden" name="_csrf" value="${sessionScope.csrf}"><h2>Permisos por rol</h2><p>Los de Miembro son fijos. El Owner siempre conserva “Asignar roles”.</p>
    <table class="table perm"><thead><tr><th>Permiso</th><th><span class="tag neutral role">Miembro</span></th>
      <c:forEach var="r" items="${roles}"><th><span class="tag ${r.tono} role"><c:out value="${r.nombre}"/></span></th></c:forEach></tr></thead><tbody>
      <c:set var="sec" value=""/>
      <c:forEach var="p" items="${permisos}">
        <c:if test="${p.seccion != sec}"><c:set var="sec" value="${p.seccion}"/>
          <tr class="section"><td colspan="${fn:length(roles) + 2}">${sec == 'MI_ESPACIO' ? 'MI ESPACIO' : (sec == 'ENSAYOS' ? 'ENSAYOS' : 'ADMINISTRACIÓN')}</td></tr></c:if>
        <tr><td><c:out value="${p.descripcion}"/></td>
          <td><input type="checkbox" ${p.tiene(1) ? 'checked' : ''} disabled aria-label="Miembro"></td>
          <c:forEach var="r" items="${roles}">
            <td><input type="checkbox" name="perm_${r.idRol}_${p.idPermiso}" aria-label="${r.nombre}" ${p.tiene(r.idRol) ? 'checked' : ''}
                       ${p.seccion == 'MI_ESPACIO' or (r.codigo == 'ADMIN' and p.codigo == 'ASIGNAR_ROLES') ? 'checked disabled' : ''}></td>
          </c:forEach></tr>
      </c:forEach>
    </tbody></table>
    <div class="inline" style="justify-content:flex-end"><button class="btn btn-primary" name="action" value="permisos">Guardar permisos</button></div>
  </form>

  <div class="stack w-340">
    <div class="card"><h2>Personas con rol</h2><p>Toca una etiqueta para quitar ese rol</p>
      <c:forEach var="m" items="${personas}">
        <div class="person"><span class="avatar${m.admin ? '' : ' blue'}">${m.iniciales}</span><b><c:out value="${m.nombreConApodo}"/></b>
          <span class="inline" style="gap:4px;flex-wrap:wrap">
          <c:forEach var="r" items="${roles}"><c:if test="${m.tieneRol(r.idRol)}">
            <form method="post" action="${ctx}/admin/roles" class="inline-form"><input type="hidden" name="_csrf" value="${sessionScope.csrf}"><input type="hidden" name="idMiembro" value="${m.idMiembro}"><input type="hidden" name="idRol" value="${r.idRol}">
              <button class="tag ${r.tono} role" style="border:0;cursor:pointer" name="action" value="quitar" title="Quitar rol ${r.nombre}" onclick="return confirm('¿Quitar el rol ${r.nombre}?')">${r.corto} ✕</button></form>
          </c:if></c:forEach></span>
        </div>
      </c:forEach>
    </div>
    <form class="card" method="post" action="${ctx}/admin/roles"><input type="hidden" name="_csrf" value="${sessionScope.csrf}"><h2>Asignar rol</h2>
      <select class="input" name="idMiembro" required><option value="">Elige un miembro…</option><c:forEach var="m" items="${activos}"><option value="${m.idMiembro}"><c:out value="${m.nombreConApodo}"/></option></c:forEach></select>
      <div class="pills"><c:forEach var="r" items="${roles}" varStatus="st"><label class="pill"><input type="radio" name="idRol" value="${r.idRol}" ${st.first ? 'checked' : ''}><span><c:out value="${r.nombre}"/></span></label></c:forEach></div>
      <div><button class="btn btn-primary" name="action" value="asignar">+ Asignar rol</button></div>
    </form>
    <div class="card"><h2>Cómo funcionan</h2>
      <p class="muted small">• <b>Equipo de ensayos</b>: ensayos, cronograma, asistencia, figuras enseñadas, fichas y reportes.</p>
      <p class="muted small">• <b>Moderador</b> y <b>Admin de ensayos</b>: lo anterior + figuras, histórico, todas las responsabilidades, eliminar ensayos y editar figuras desde la ficha. Hoy tienen lo mismo; son roles distintos para darle más cosas al admin después.</p>
      <p class="muted small">• <b>Owner</b>: todo, incluidos Miembros y Roles. La BD no deja quitar al último Owner.</p>
      <p class="muted small">• Los cambios le llegan a cada persona al toque, en su siguiente clic (sin volver a iniciar sesión).</p></div>
  </div>
</div>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
