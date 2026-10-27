<c:set var="titulo" value="Roles y permisos"/><c:set var="menu" value="Roles y permisos"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<section class="hero"><div class="titles"><span class="kicker">Administración</span><h1>Roles y permisos</h1>
  <p>Quién puede hacer qué en el sistema. Todos son Miembro; algunos además son Equipo de ensayos o Admin.</p></div><span class="star"></span></section>

<div class="row top">
  <form class="card grow" method="post" action="${ctx}/admin/roles"><h2>Permisos por rol</h2><p>Los de Miembro son fijos. Admin siempre conserva “Asignar roles”.</p>
    <table class="table perm"><thead><tr><th>Permiso</th><th><span class="tag neutral role">Miembro</span></th><th><span class="tag info role">Equipo de ensayos</span></th><th><span class="tag warn role">Admin</span></th></tr></thead><tbody>
      <c:set var="sec" value=""/>
      <c:forEach var="p" items="${permisos}">
        <c:if test="${p.seccion != sec}"><c:set var="sec" value="${p.seccion}"/>
          <tr class="section"><td colspan="4">${sec == 'MI_ESPACIO' ? 'MI ESPACIO' : (sec == 'ENSAYOS' ? 'ENSAYOS' : 'ADMINISTRACIÓN')}</td></tr></c:if>
        <tr><td><c:out value="${p.descripcion}"/></td>
          <td><input type="checkbox" ${p.miembro ? 'checked' : ''} disabled></td>
          <td><input type="checkbox" name="perm_2_${p.idPermiso}" ${p.equipo ? 'checked' : ''} ${p.seccion == 'MI_ESPACIO' ? 'checked disabled' : ''}></td>
          <td><input type="checkbox" name="perm_3_${p.idPermiso}" ${p.admin ? 'checked' : ''} ${p.seccion == 'MI_ESPACIO' or p.codigo == 'ASIGNAR_ROLES' ? 'checked disabled' : ''}></td></tr>
      </c:forEach>
    </tbody></table>
    <div class="inline" style="justify-content:flex-end"><button class="btn btn-primary" name="action" value="permisos">Guardar permisos</button></div>
  </form>

  <div class="stack w-340">
    <div class="card"><h2>Personas con rol</h2><p>Admin y Equipo de ensayos</p>
      <c:forEach var="m" items="${personas}">
        <div class="person"><span class="avatar${m.admin ? '' : ' blue'}">${m.iniciales}</span><b><c:out value="${m.nombre}"/></b>
          <c:if test="${m.admin}"><form method="post" action="${ctx}/admin/roles" class="inline-form"><input type="hidden" name="idMiembro" value="${m.idMiembro}"><input type="hidden" name="idRol" value="3">
            <button class="tag warn role" style="border:0;cursor:pointer" name="action" value="quitar" title="Quitar rol Admin" onclick="return confirm('¿Quitar el rol Admin?')">Admin ✕</button></form></c:if>
          <c:if test="${m.equipoEnsayos}"><form method="post" action="${ctx}/admin/roles" class="inline-form"><input type="hidden" name="idMiembro" value="${m.idMiembro}"><input type="hidden" name="idRol" value="2">
            <button class="tag info role" style="border:0;cursor:pointer" name="action" value="quitar" title="Quitar rol Equipo de ensayos">Ensayos ✕</button></form></c:if>
        </div>
      </c:forEach>
    </div>
    <form class="card" method="post" action="${ctx}/admin/roles"><h2>Asignar rol</h2>
      <select class="input" name="idMiembro" required><option value="">Elige un miembro…</option><c:forEach var="m" items="${activos}"><option value="${m.idMiembro}"><c:out value="${m.nombre}"/></option></c:forEach></select>
      <div class="pills"><label class="pill"><input type="radio" name="idRol" value="2" checked><span>Equipo de ensayos</span></label><label class="pill"><input type="radio" name="idRol" value="3"><span>Admin</span></label></div>
      <div><button class="btn btn-primary" name="action" value="asignar">+ Asignar rol</button></div>
    </form>
    <div class="card"><h2>Buenas prácticas</h2>
      <p class="muted small">• Mínimo 2 admins para no quedarse sin acceso (la BD no deja quitar al último).</p>
      <p class="muted small">• Al quitar un rol, la persona sigue siendo Miembro.</p>
      <p class="muted small">• Los cambios de permisos se aplican desde el próximo inicio de sesión.</p></div>
  </div>
</div>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
