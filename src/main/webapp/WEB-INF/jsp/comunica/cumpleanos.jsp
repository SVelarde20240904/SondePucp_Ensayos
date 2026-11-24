<c:set var="titulo" value="Cumpleaños"/><c:set var="menu" value="Comunica"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<section class="hero"><div class="titles"><span class="kicker">Team Comunica</span><h1>Rol de cumpleaños</h1>
  <p>Cada saludo tiene un responsable del team, que lo redacta, arma el video y lo publica. La web los reparte sola para que a todos les toque parejo.</p></div>
  <span class="star"></span></section>
<c:set var="grupo" value="comunica"/><c:set var="sub" value="cumpleanos"/><%@ include file="/WEB-INF/jsp/includes/subtabs.jspf" %>

<div class="row top">
  <div class="card grow">
    <div class="sec-head"><h2>${mesTexto}</h2>
      <div class="inline"><a class="btn btn-ghost btn-sm" href="${ctx}/comunica/cumpleanos?m=${mesAnterior}">← Anterior</a>
        <c:if test="${not esMesActual}"><a class="btn btn-ghost btn-sm" href="${ctx}/comunica/cumpleanos">Este mes</a></c:if>
        <a class="btn btn-ghost btn-sm" href="${ctx}/comunica/cumpleanos?m=${mesSiguiente}">Siguiente →</a></div></div>
    <p class="muted small">${fn:length(saludos)} cumpleaños · ${publicados} publicados</p>
    <c:choose>
      <c:when test="${empty saludos}"><p class="empty">No hay cumpleaños registrados este mes (o el mes todavía no se generó: los saludos se crean con un mes de anticipación).</p></c:when>
      <c:otherwise>
        <div class="scroll-x"><table class="table saludos"><thead><tr><th>Fecha</th><th>Cumpleañero(a)</th><th>Responsable</th><th>Estado</th><th></th></tr></thead><tbody>
          <c:forEach var="s" items="${saludos}">
            <tr class="${s.atrasado ? 'atrasado' : ''}">
              <td><b>${s.fechaTexto}</b><small class="${s.atrasado ? 'c-rojo' : ''}">${s.cuando}</small></td>
              <td><div class="inline"><span class="avatar sm">${s.iniciales}</span><span><b><c:out value="${s.nombre}"/></b><c:if test="${not empty s.apodo}"><small>«<c:out value="${s.apodo}"/>»</small></c:if></span></div></td>
              <td>
                <c:choose>
                  <c:when test="${puedeModerar}">
                    <form method="post" action="${ctx}/comunica/saludo" class="inline-form"><input type="hidden" name="_csrf" value="${sessionScope.csrf}"><input type="hidden" name="id" value="${s.id}"><input type="hidden" name="volver" value="lista">
                      <select class="input input-sm" name="responsable" onchange="this.form.submit()" aria-label="Responsable del saludo de ${fn:escapeXml(s.comoLlamarlo)}">
                        <option value="">— Sin asignar —</option>
                        <c:forEach var="e" items="${equipo}"><c:if test="${e.id != s.idMiembro}"><option value="${e.id}" ${e.id == s.idResponsable ? 'selected' : ''}><c:out value="${e.nombre}"/></option></c:if></c:forEach>
                      </select><input type="hidden" name="action" value="asignar"><noscript><button class="btn btn-ghost btn-sm">OK</button></noscript></form>
                  </c:when>
                  <c:when test="${empty s.responsable and s.idMiembro != u.idMiembro}">
                    <form method="post" action="${ctx}/comunica/saludo" class="inline-form"><input type="hidden" name="_csrf" value="${sessionScope.csrf}"><input type="hidden" name="id" value="${s.id}"><input type="hidden" name="volver" value="lista">
                      <button class="btn btn-secondary btn-sm" name="action" value="tomar">Me lo quedo</button></form>
                  </c:when>
                  <c:otherwise><c:out value="${empty s.responsable ? '—' : s.responsable}"/></c:otherwise>
                </c:choose>
              </td>
              <td><span class="tag ${s.tono}">${s.estadoTexto}</span>
                <c:if test="${s.estado != 'OMITIDO'}"><div class="pasitos" aria-hidden="true"><c:forEach begin="0" end="3" var="i"><i class="${i le s.paso ? 'on' : ''}"></i></c:forEach></div></c:if></td>
              <td class="r"><c:if test="${s.idMiembro != u.idMiembro}"><a class="btn btn-ghost btn-sm" href="${ctx}/comunica/saludo?id=${s.id}">${s.idResponsable == u.idMiembro ? 'Trabajar' : 'Ver'}</a></c:if></td>
            </tr>
          </c:forEach>
        </tbody></table></div>
      </c:otherwise>
    </c:choose>
  </div>
  <div class="stack w-300">
    <div class="card"><h2>Reparto ${anio}</h2><p class="muted small">Saludos asignados en el año a cada miembro del team</p>
      <c:choose>
        <c:when test="${empty carga}"><p class="empty">Todavía no hay nadie en el team Comunica. Agrégalos en Miembros → editar → Teams.</p></c:when>
        <c:otherwise>
          <table class="table"><thead><tr><th>Miembro</th><th class="r">Asignados</th><th class="r">Publicados</th></tr></thead><tbody>
            <c:forEach var="k" items="${carga}"><tr><td><c:out value="${k.nombre}"/></td><td class="r"><b>${k.total}</b></td><td class="r">${k.publicados}</td></tr></c:forEach>
          </tbody></table></c:otherwise>
      </c:choose>
    </div>
    <c:choose>
      <c:when test="${puedeModerar}">
        <form class="card" method="post" action="${ctx}/comunica/cumpleanos"><input type="hidden" name="_csrf" value="${sessionScope.csrf}"><input type="hidden" name="m" value="${mes}">
          <h2>Plantilla del saludo</h2><p class="muted small">Base para redactar cada saludo. <b>{apodo}</b> se cambia por el apodo (o el primer nombre) y <b>{nombre}</b> por el primer nombre.</p>
          <div class="field"><textarea class="input" name="plantilla" rows="5" maxlength="500"><c:out value="${plantillaSaludo}"/></textarea></div>
          <div><button class="btn btn-secondary" name="action" value="plantilla">Guardar plantilla</button></div></form>
      </c:when>
      <c:otherwise>
        <div class="card"><h2>Plantilla del saludo</h2><p class="pre"><c:out value="${plantillaSaludo}"/></p></div>
      </c:otherwise>
    </c:choose>
    <p class="note info">Los videos van como <b>link de Drive</b> (restringido al grupo de Google de SDP), no se suben a la web.</p>
  </div>
</div>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
