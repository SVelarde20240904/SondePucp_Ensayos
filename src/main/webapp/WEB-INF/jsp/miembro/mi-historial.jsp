<c:set var="titulo" value="Mi historial"/><c:set var="menu" value="Mi historial"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<section class="hero"><div class="titles"><span class="kicker">Mi espacio</span><h1>Mi historial</h1><p>Cómo has ido avanzando en figuras y asistencia</p></div><span class="star"></span></section>

<div class="grid-4">
  <div class="card stat"><b class="c-verde">${resumen[0]} / ${resumen[1]}</b><span>figuras que sé (Verde o Azul)</span></div>
  <div class="card stat"><b class="c-primary">+${resumen[2]}</b><span>figuras mejoradas este mes</span></div>
  <div class="card stat"><b>${porcentaje}%</b><span>asistencia del ciclo</span></div>
  <div class="card stat"><b style="display:flex;align-items:center;gap:8px"><sdp:chip color="${yo.nivel}"/></b><span>nivel actual · miembro desde ${yo.miembroDesde}</span></div>
</div>

<div class="card"><h2>Cambios en mis figuras</h2><p>Cada autoevaluación queda guardada con su fecha</p>
  <div class="pills">
    <a class="pill-link ${empty fuente ? 'on' : ''}" href="${ctx}/mi-historial">Todo</a>
    <a class="pill-link ${fuente == 'MENSUAL' ? 'on' : ''}" href="${ctx}/mi-historial?fuente=MENSUAL">Mensual</a>
    <a class="pill-link ${fuente == 'ENSAYO' ? 'on' : ''}" href="${ctx}/mi-historial?fuente=ENSAYO">Del ensayo</a>
    <a class="pill-link ${fuente == 'CORRECCION' ? 'on' : ''}" href="${ctx}/mi-historial?fuente=CORRECCION">Correcciones</a>
  </div>
  <c:choose>
    <c:when test="${empty historial}"><p class="empty">No hay cambios registrados con ese filtro.</p></c:when>
    <c:otherwise>
      <table class="table"><thead><tr><th>Fecha</th><th>Figura</th><th>Cambio</th><th>Fuente</th></tr></thead><tbody>
        <c:forEach var="h" items="${historial}"><tr>
          <td>${h.fechaLarga}</td><td><c:out value="${h.figura}"/></td>
          <td><sdp:cambio antes="${h.colorAnterior}" despues="${h.colorNuevo}"/><c:if test="${not h.aplicado}"> <span class="tag neutral">ignorado · solo sube</span></c:if></td>
          <td><sdp:tag texto="${h.fuenteTexto}${h.fuente == 'CORRECCION' ? ' · '.concat(h.registradoPor) : ''}" tono="${h.fuenteTono}"/></td></tr></c:forEach>
      </tbody></table>
    </c:otherwise>
  </c:choose>
</div>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
