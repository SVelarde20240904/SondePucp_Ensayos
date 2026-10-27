<c:set var="nuevo" value="${m.idMiembro == 0}"/>
<c:set var="titulo" value="${nuevo ? 'Nuevo miembro' : 'Editar miembro'}"/><c:set var="menu" value="Miembros"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<form method="post" action="${ctx}/admin/miembros/${nuevo ? 'nuevo' : 'editar'}" class="stack" style="gap:24px">
<input type="hidden" name="idMiembro" value="${nuevo ? '' : m.idMiembro}">
<section class="hero"><div class="titles"><span class="kicker">Administración › Miembros</span><h1>${titulo}</h1>
  <p><c:choose><c:when test="${nuevo}">Al guardar, aparece en las listas y puede confirmar asistencia. Tendrá nivel cuando haga su primera autoevaluación.</c:when>
    <c:otherwise><c:out value="${m.nombre}"/> · ${empty m.nivel ? 'sin nivel' : 'nivel '.concat(m.nivel)} · miembro desde ${m.miembroDesde}${m.activo ? '' : ' · INACTIVO'}</c:otherwise></c:choose></p></div>
  <div class="actions"><a class="btn btn-secondary" href="${ctx}/admin/miembros">Cancelar</a><button class="btn btn-primary" name="action" value="guardar">Guardar</button></div>
  <span class="star"></span></section>

<div class="row top">
  <div class="stack grow">
    <div class="card"><h2>Datos personales</h2><p>Los mismos que pedimos en la convocatoria</p>
      <div class="form-row">
        <div class="field"><label>Nombre en el sistema *</label><input class="input" name="nombre" value="<c:out value='${m.nombre}'/>" placeholder="Nombre + apellido (único)" required></div>
        <div class="field"><label>Nombre completo *</label><input class="input" name="nombreCompleto" value="<c:out value='${m.nombreCompleto}'/>" required></div></div>
      <div class="form-row">
        <div class="field"><label>Código PUCP</label><input class="input" name="codigoPucp" value="<c:out value='${m.codigoPucp}'/>" pattern="\d{8}" placeholder="8 dígitos"></div>
        <div class="field"><label>Facultad</label><select class="input" name="facultad"><option value="">—</option>
          <c:forEach var="f" items="${facultades}"><option ${m.facultad == f ? 'selected' : ''}>${f}</option></c:forEach>
          <c:if test="${not empty m.facultad and not facultades.contains(m.facultad)}"><option selected><c:out value="${m.facultad}"/></option></c:if></select></div></div>
      <div class="form-row">
        <div class="field"><label>Correo PUCP *</label><input class="input" type="email" name="correo" value="<c:out value='${m.correo}'/>" placeholder="a2026xxxx@pucp.edu.pe" required></div>
        <div class="field"><label>Celular</label><input class="input" type="tel" name="celular" value="<c:out value='${m.celular}'/>" placeholder="9xx xxx xxx"></div></div>
    </div>
    <div class="card"><h2>En el grupo</h2><p>Estos datos solo los puede cambiar un admin. El rol de baile se registra una vez.</p>
      <div class="field"><span class="label">Rol de baile *</span><div class="pills">
        <label class="pill"><input type="radio" name="rolBaile" value="LEADER" ${m.rolBaile == 'LEADER' ? 'checked' : ''} required><span>Leader</span></label>
        <label class="pill"><input type="radio" name="rolBaile" value="FOLLOWER" ${m.rolBaile == 'FOLLOWER' ? 'checked' : ''}><span>Follower</span></label></div></div>
      <div class="form-row">
        <div class="field"><label>Frecuencia *</label><select class="input" name="frecuencia">
          <c:forEach var="f" items="${['REGULAR','OCASIONAL','EVENTUAL']}"><option value="${f}" ${m.frecuencia == f ? 'selected' : ''}>${f == 'REGULAR' ? 'Regular' : (f == 'OCASIONAL' ? 'Ocasional' : 'Eventual')}</option></c:forEach></select></div>
        <div class="field"><label>Fecha de ingreso</label><input class="input" type="date" name="fechaIngreso" value="${m.fechaIngresoIso}"></div></div>
    </div>
  </div>

  <div class="stack w-440">
    <div class="card"><h2>Roles en el sistema</h2><p>Define qué pantallas puede ver (ver Roles y permisos)</p>
      <label class="check-item"><input type="checkbox" checked disabled> Miembro <span class="muted xs">(todos)</span></label>
      <label class="check-item"><input type="checkbox" name="rolEquipo" value="1" ${m.equipoEnsayos ? 'checked' : ''}> Equipo de ensayos <span class="muted xs">(no cuenta para el color)</span></label>
      <label class="check-item"><input type="checkbox" name="rolAdmin" value="1" ${m.admin ? 'checked' : ''}> Admin</label>
    </div>
    <c:choose>
      <c:when test="${nuevo}">
        <div class="card"><h2>Acceso al sistema</h2><p>Entra con su correo PUCP y esta contraseña temporal; la cambiará en su primer ingreso.</p>
          <div class="field"><label>Contraseña temporal</label><input class="input" name="passwordTemporal" value="${passwordTemporal}" required minlength="6"></div>
          <p class="muted xs">Anótala o envíasela por WhatsApp: después de guardar no se puede volver a ver (solo restablecer).</p></div>
      </c:when>
      <c:otherwise>
        <div class="card"><h2>Acceso al sistema</h2>
          <div class="inline"><sdp:tag texto="${m.cuentaActivada ? (m.debeCambiarPassword ? 'Con contraseña temporal' : 'Cuenta activada') : 'Sin contraseña'}" tono="${m.cuentaActivada and not m.debeCambiarPassword ? 'ok' : 'warn'}"/></div>
          <div><button class="btn btn-secondary" name="action" value="restablecer" formnovalidate>Generar contraseña temporal</button></div></div>
        <div class="card" style="border-color:var(--rojo-soft)">
          <c:choose>
            <c:when test="${m.activo}">
              <h2>Dar de baja</h2><p>Si se retira del grupo, se inactiva: deja de contar para el color de las figuras y de aparecer en las listas, pero se conserva su historial. Se puede reactivar.</p>
              <div class="field"><label>Motivo</label><input class="input" name="motivoBaja" placeholder="Ej: se retiró del grupo"></div>
              <div><button class="btn btn-secondary" name="action" value="inactivar" formnovalidate onclick="return confirm('¿Inactivar a este miembro?')">Inactivar miembro</button></div>
            </c:when>
            <c:otherwise>
              <h2>Miembro inactivo</h2><p>Dado de baja el ${m.fechaBaja.toLocalDate()}<c:if test="${not empty m.motivoBaja}"> · <c:out value="${m.motivoBaja}"/></c:if></p>
              <div><button class="btn btn-primary" name="action" value="reactivar" formnovalidate>Reactivar</button></div>
            </c:otherwise>
          </c:choose>
          <c:choose>
            <c:when test="${m.puedeEliminar}"><div><button class="btn btn-danger" name="action" value="eliminar" formnovalidate onclick="return confirm('¿Eliminar para siempre? Solo para altas hechas por error.')">Eliminar (alta por error)</button></div></c:when>
            <c:otherwise><div class="note warn">Eliminar no está disponible: tiene asistencias, autoevaluaciones o encargos registrados.</div></c:otherwise>
          </c:choose>
        </div>
      </c:otherwise>
    </c:choose>
  </div>
</div>
</form>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
