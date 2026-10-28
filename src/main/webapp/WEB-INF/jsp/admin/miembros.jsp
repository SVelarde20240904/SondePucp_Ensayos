<c:set var="titulo" value="Miembros"/><c:set var="menu" value="Miembros"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<section class="hero"><div class="titles"><span class="kicker">Administración</span><h1>Miembros</h1>
  <p>${activos} activos · nunca se borra a nadie: se marca Inactivo para no perder su historial</p></div>
  <div class="actions"><a class="btn btn-secondary" href="${ctx}/admin/miembros/activar">🔑 Activar cuentas en lote</a><a class="btn btn-primary" href="${ctx}/admin/miembros/nuevo">+ Nuevo miembro</a></div><span class="star"></span></section>

<div class="card">
  <form class="inline" method="get" action="${ctx}/admin/miembros">
    <input type="hidden" name="filtro" value="${filtro}">
    <input class="input" name="q" value="<c:out value='${q}'/>" placeholder="Buscar miembro…" style="max-width:280px">
    <c:forEach var="f" items="${['TODOS','LEADER','FOLLOWER','EQUIPO','INACTIVOS']}">
      <a class="pill-link ${filtro == f ? 'on' : ''}" href="${ctx}/admin/miembros?filtro=${f}">${f == 'TODOS' ? 'Todos' : (f == 'LEADER' ? 'Leaders' : (f == 'FOLLOWER' ? 'Followers' : (f == 'EQUIPO' ? 'Equipo de ensayos' : 'Inactivos')))}</a>
    </c:forEach>
  </form>
  <table class="table"><thead><tr><th>Miembro</th><th>Rol</th><th>Frecuencia</th><th>¿Cuenta para color?</th><th>Equipo ensayos</th><th>Nivel</th><th>Asistencia (mes)</th><th>Estado</th><th></th></tr></thead><tbody>
  <c:forEach var="m" items="${lista}">
    <tr><td><c:out value="${m.nombre}"/><small><c:out value="${empty m.correo ? 'sin correo' : m.correo}"/>${m.cuentaActivada ? '' : ' · cuenta sin activar'}</small></td>
      <td>${m.rolBaileTexto}</td><td>${m.frecuenciaTexto}</td>
      <td><sdp:tag texto="${m.cuentaColor ? 'Sí' : (m.equipoEnsayos ? 'No (ensayos)' : 'No')}" tono="${m.cuentaColor ? 'ok' : 'neutral'}"/></td>
      <td>${m.equipoEnsayos ? '<span class="tag info">Sí</span>' : '—'}</td>
      <td><sdp:chip color="${m.nivel}"/></td><td>${m.asistenciaMesTexto}</td>
      <td><sdp:tag texto="${m.activo ? 'Activo' : 'Inactivo'}" tono="${m.activo ? 'ok' : 'bad'}"/></td>
      <td><a href="${ctx}/admin/miembros/editar?id=${m.idMiembro}">${m.activo ? 'Editar' : 'Ver / reactivar'}</a></td></tr>
  </c:forEach>
  <c:if test="${empty lista}"><tr><td colspan="9" class="empty">No hay miembros con ese filtro.</td></tr></c:if>
  </tbody></table>
</div>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
