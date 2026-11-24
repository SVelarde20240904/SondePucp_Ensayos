<c:set var="titulo" value="Comunica"/><c:set var="menu" value="Comunica"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<section class="hero"><div class="titles"><span class="kicker">Team Comunica</span><h1>Tablero</h1>
  <p>Lo que se está haciendo para las redes, lo último que salió y las ideas más votadas.</p></div>
  <div class="actions"><a class="btn btn-primary" href="${ctx}/comunica/proyecto">+ Nuevo proyecto</a><a class="btn btn-secondary" href="${ctx}/comunica/proyecto?estado=IDEA">💡 Guardar idea</a></div>
  <span class="star"></span></section>
<c:set var="grupo" value="comunica"/><c:set var="sub" value="tablero"/><%@ include file="/WEB-INF/jsp/includes/subtabs.jspf" %>

<div class="grid-4">
  <div class="card stat"><b class="c-primary">${resumen.enDesarrollo}</b><span>en desarrollo</span></div>
  <div class="card stat"><b class="c-verde">${resumen.publicadosMes}</b><span>publicados este mes</span></div>
  <div class="card stat"><b>${resumen.ideas}</b><span>ideas en el banco</span></div>
  <div class="card stat"><b class="${resumen.saludosAtrasados gt 0 ? 'c-rojo' : 'c-verde'}">${resumen.saludosAtrasados}</b><span>saludos de cumpleaños atrasados</span></div>
</div>

<c:if test="${not empty misSaludos}">
  <div class="card"><h2>🎂 Tus saludos de cumpleaños</h2>
    <ul class="pend-lista">
      <c:forEach var="s" items="${misSaludos}">
        <li class="pend ${s.atrasado or s.urgente ? 'warn' : 'info'}"><span class="pend-ico" aria-hidden="true">🎂</span>
          <span class="pend-txt"><b><c:out value="${s.nombre}"/> · ${s.fechaTexto}</b><small>${s.cuando} · ${s.estadoTexto}</small></span>
          <a class="btn btn-secondary btn-sm" href="${ctx}/comunica/saludo?id=${s.id}">Abrir</a></li>
      </c:forEach>
    </ul>
  </div>
</c:if>

<div class="kanban">
  <section class="kcol"><header><h2>En desarrollo</h2><span class="badge-n">${fn:length(enDesarrollo)}</span></header>
    <c:forEach var="p" items="${enDesarrollo}"><%@ include file="/WEB-INF/jsp/comunica/tarjeta.jspf" %></c:forEach>
    <c:if test="${empty enDesarrollo}"><p class="empty">Nada en desarrollo. Pasa una idea del banco o crea un proyecto.</p></c:if>
  </section>
  <section class="kcol"><header><h2>Publicado</h2><span class="badge-n">${fn:length(publicados)}</span></header>
    <c:forEach var="p" items="${publicados}"><%@ include file="/WEB-INF/jsp/comunica/tarjeta.jspf" %></c:forEach>
    <c:if test="${empty publicados}"><p class="empty">Todavía no hay nada publicado desde la web.</p></c:if>
    <c:if test="${hayMasPublicados}"><a class="small" href="${ctx}/comunica/archivo">Ver todo en el archivo →</a></c:if>
  </section>
  <section class="kcol"><header><h2>Ideas más votadas</h2><span class="badge-n">${totalIdeas}</span></header>
    <c:forEach var="p" items="${ideasTop}"><%@ include file="/WEB-INF/jsp/comunica/tarjeta.jspf" %></c:forEach>
    <c:if test="${empty ideasTop}"><p class="empty">El banco de ideas está vacío.</p></c:if>
    <a class="small" href="${ctx}/comunica/ideas">Ir al banco de ideas →</a>
  </section>
</div>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
