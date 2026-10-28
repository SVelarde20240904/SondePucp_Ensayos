<c:set var="titulo" value="Fichas de miembros"/><c:set var="menu" value="Fichas de miembros"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<section class="hero"><div class="titles"><span class="kicker">Ensayos</span><h1>Fichas de miembros</h1>
  <p>Entra a la ficha de cualquier persona para ver qué figuras sabe, cuáles le faltan y cómo viene su asistencia</p></div><span class="star"></span></section>

<div class="card">
  <form class="inline" method="get" action="${ctx}/ensayos/miembros">
    <input type="hidden" name="filtro" value="<c:out value='${param.filtro}'/>"><input type="hidden" name="nivel" value="<c:out value='${param.nivel}'/>">
    <input class="input" name="q" value="<c:out value='${param.q}'/>" placeholder="Buscar por nombre…" style="max-width:280px">
    <button class="btn btn-secondary btn-sm">Buscar</button>
  </form>
  <div class="inline">
    <c:forEach var="f" items="${['','LEADER','FOLLOWER','EQUIPO']}">
      <c:url var="u1" value="/ensayos/miembros"><c:param name="filtro" value="${f}"/><c:param name="nivel" value="${param.nivel}"/><c:param name="q" value="${param.q}"/></c:url>
      <a class="pill-link ${param.filtro == f or (empty param.filtro and empty f) ? 'on' : ''}" href="${u1}">${empty f ? 'Todos' : (f == 'LEADER' ? 'Leaders' : (f == 'FOLLOWER' ? 'Followers' : 'Equipo de ensayos'))}</a>
    </c:forEach>
  </div>
  <div class="inline">
    <span class="muted small">Nivel:</span>
    <c:forEach var="n" items="${['','AZUL','VERDE','AMARILLO','ROJO','SIN']}">
      <c:url var="u2" value="/ensayos/miembros"><c:param name="filtro" value="${param.filtro}"/><c:param name="nivel" value="${n}"/><c:param name="q" value="${param.q}"/></c:url>
      <a class="pill-link ${param.nivel == n or (empty param.nivel and empty n) ? 'on' : ''}" href="${u2}">${empty n ? 'Todos' : (n == 'SIN' ? 'Sin nivel' : fn:substring(n,0,1).concat(fn:toLowerCase(fn:substring(n,1,10))))}</a>
    </c:forEach>
  </div>
  <table class="table cards-compact"><thead><tr><th>Miembro</th><th>Rol</th><th>Nivel</th><th>Frecuencia</th><th>Asistencia (mes)</th><th></th></tr></thead><tbody>
  <c:forEach var="m" items="${lista}">
    <tr><td><a href="${ctx}/ensayos/miembro?id=${m.idMiembro}"><c:out value="${m.nombre}"/></a><c:if test="${m.equipoEnsayos}"> <span class="tag info">ENS</span></c:if></td>
      <td class="no-label"><span class="tag info role">${m.rolBaileTexto}</span></td>
      <td class="no-label"><sdp:chip color="${m.nivel}"/></td>
      <td>${m.frecuenciaTexto}</td><td>${m.asistenciaMesTexto}</td>
      <td class="no-label"><a class="btn btn-ghost btn-sm" href="${ctx}/ensayos/miembro?id=${m.idMiembro}">Ver ficha</a></td></tr>
  </c:forEach>
  <c:if test="${empty lista}"><tr><td colspan="6" class="empty">No hay miembros con ese filtro.</td></tr></c:if>
  </tbody></table>
  <p class="muted xs">${fn:length(lista)} miembro(s)</p>
</div>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
