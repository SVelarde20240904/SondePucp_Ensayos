<c:set var="titulo" value="${m.nombre}"/><c:set var="menu" value="Fichas de miembros"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<section class="hero"><div class="titles"><span class="kicker">Ensayos › Fichas de miembros</span><h1><c:out value="${m.nombre}"/></h1>
  <p>${m.rolBaileTexto} · ${m.frecuenciaTexto}${m.equipoEnsayos ? ' · Equipo de ensayos' : ''}${m.activo ? '' : ' · INACTIVO'} · miembro desde ${m.miembroDesde}</p></div>
  <div class="actions"><a class="btn btn-secondary" href="${ctx}/ensayos/miembros">← Todas las fichas</a>
    <c:if test="${perm.contains('GESTIONAR_MIEMBROS')}"><a class="btn btn-primary" href="${ctx}/admin/miembros/editar?id=${m.idMiembro}">Editar datos</a></c:if></div>
  <span class="star"></span></section>

<div class="grid-4">
  <div class="card stat"><span>Nivel</span><div><sdp:chip color="${m.nivel}"/></div></div>
  <div class="card stat"><b class="c-primary">${resumen[0]}/${resumen[1]}</b><span>figuras que sabe (Verde o Azul)</span></div>
  <div class="card stat"><b class="c-verde">${porcentaje}%</b><span>asistencia en el ciclo</span></div>
  <div class="card stat"><b class="c-accent">+${resumen[2]}</b><span>mejoras este mes</span></div>
</div>

<div class="row top">
  <div class="card grow"><h2>Figuras por nivel</h2><p>El nivel de la figura es el color del grupo (Azul = base → Rojo = avanzada). Al lado, el color de ${m.nombre}.</p>
    <div class="inline">
      <a class="pill-link ${soloFaltan ? '' : 'on'}" href="${ctx}/ensayos/miembro?id=${m.idMiembro}">Todas</a>
      <a class="pill-link ${soloFaltan ? 'on' : ''}" href="${ctx}/ensayos/miembro?id=${m.idMiembro}&faltan=1">Solo las que le faltan</a>
    </div>
    <c:forEach var="e" items="${porNivel}">
      <div class="nivel-bloque">
        <div class="inline"><c:choose><c:when test="${e.key == 'SIN'}"><span class="chip sin">Figuras nuevas · sin nivel</span></c:when><c:otherwise><sdp:chip color="${e.key}"/></c:otherwise></c:choose>
          <span class="muted small">sabe ${sabe[e.key][0]} de ${sabe[e.key][1]}</span></div>
        <div class="fig-grid">
          <c:forEach var="ev" items="${e.value}">
            <div class="fig-ficha ${ev.leFalta or empty ev.color ? 'falta' : ''}"><span><c:out value="${ev.figura}"/></span>
              <c:choose><c:when test="${empty ev.color}"><span class="chip sin">Sin evaluar</span></c:when><c:otherwise><sdp:chip color="${ev.color}"/></c:otherwise></c:choose></div>
          </c:forEach>
        </div>
      </div>
    </c:forEach>
    <c:if test="${empty porNivel}"><p class="empty">¡Se sabe todas! No le falta ninguna figura.</p></c:if>
  </div>

  <div class="stack w-340">
    <div class="card"><h2>Últimos ensayos</h2>
      <div class="att-grid">
        <c:forEach var="a" items="${asistencias}"><div><span>${a.fechaCorta}</span><sdp:code valor="${empty a.estadoReal ? (a.confirmacion == 'SIN_RESPUESTA' ? 'SR' : '') : a.estadoReal}"/></div></c:forEach>
      </div>
      <c:if test="${empty asistencias}"><p class="empty">Todavía no tiene ensayos registrados.</p></c:if>
    </div>
    <div class="card"><h2>Cambios recientes</h2>
      <c:forEach var="h" items="${historial}">
        <div class="inline" style="justify-content:space-between;flex-wrap:nowrap;border-bottom:1px solid var(--border);padding:6px 0">
          <span class="small"><b><c:out value="${h.figura}"/></b><br><span class="muted xs">${h.fechaTexto} · ${h.fuenteTexto}</span></span>
          <sdp:cambio antes="${h.colorAnterior}" despues="${h.colorNuevo}"/></div>
      </c:forEach>
      <c:if test="${empty historial}"><p class="empty">Sin cambios registrados.</p></c:if>
    </div>
  </div>
</div>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
