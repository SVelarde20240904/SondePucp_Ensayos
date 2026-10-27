<c:set var="titulo" value="Mano de figuras"/><c:set var="menu" value="Figuras"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<section class="hero"><div class="titles"><span class="kicker">Administración</span><h1>Mano de figuras</h1>
  <p>${total} figuras activas. El nivel de cada figura es la mediana de las autoevaluaciones y se recalcula solo.</p></div>
  <div class="actions"><a class="btn btn-primary" href="${ctx}/admin/figuras/nueva">+ Nueva figura</a></div><span class="star"></span></section>

<div class="grid-4">
  <c:forEach var="lv" items="${['AZUL','VERDE','AMARILLO','ROJO']}">
    <div class="card stat soft-${fn:toLowerCase(lv)}"><b>${fn:length(porNivel[lv])}</b><span>figuras nivel ${fn:toLowerCase(lv)}</span></div>
  </c:forEach>
</div>

<div class="grid-4" style="align-items:start">
  <c:forEach var="lv" items="${['AZUL','VERDE','AMARILLO','ROJO']}">
    <div class="card" style="gap:8px">
      <div class="inline"><sdp:chip color="${lv}"/><span class="muted small bold">${fn:length(porNivel[lv])} figuras</span></div>
      <h2 style="font-family:var(--display);font-size:20px">Nivel ${lv}</h2>
      <p style="margin-top:-6px">${lv == 'AZUL' ? 'Base: la domina casi todo el grupo' : (lv == 'VERDE' ? 'Intermedias' : (lv == 'AMARILLO' ? 'Avanzadas' : 'Las domina casi nadie'))}</p>
      <c:forEach var="f" items="${porNivel[lv]}">
        <a class="fig-item" href="${ctx}/admin/figuras/editar?id=${f.idFigura}" style="text-decoration:none;color:inherit">
          <div><c:out value="${f.nombre}"/><b style="color:var(--${fn:toLowerCase(lv)})">${f.porcentajeSaben}%</b></div>
          <span class="bar thin ${fn:toLowerCase(lv)}"><i style="width:${f.porcentajeSaben}%"></i></span></a>
      </c:forEach>
    </div>
  </c:forEach>
</div>

<c:if test="${not empty porNivel['SIN']}">
<div class="card"><h2>Sin nivel todavía</h2><p>Figuras nuevas: tendrán color cuando el grupo las evalúe</p>
  <div class="inline"><c:forEach var="f" items="${porNivel['SIN']}"><a class="inline" style="border:1px dashed var(--border);border-radius:10px;padding:8px 12px" href="${ctx}/admin/figuras/editar?id=${f.idFigura}"><sdp:chip color=""/><span class="small"><c:out value="${f.nombre}"/> · ${f.NEvaluaciones} evaluaciones</span></a></c:forEach></div></div>
</c:if>

<div class="card"><h2>Figuras inactivas</h2><p>No aparecen en la mano, en las autoevaluaciones ni en los cálculos. Su historial se conserva.</p>
  <table class="table"><thead><tr><th>Figura</th><th>Evaluaciones</th><th>Desactivada</th><th>Por</th><th></th></tr></thead><tbody>
    <c:forEach var="f" items="${inactivas}"><tr><td><c:out value="${f.nombre}"/></td><td>${f.NEvaluaciones}</td><td>${f.fechaBajaTexto}</td><td><c:out value="${f.bajaPor}"/></td>
      <td><form method="post" action="${ctx}/admin/figuras/editar" class="inline-form"><input type="hidden" name="idFigura" value="${f.idFigura}"><button class="btn btn-ghost btn-sm" name="action" value="reactivar">Reactivar</button></form></td></tr></c:forEach>
    <c:if test="${empty inactivas}"><tr><td colspan="5" class="empty">No hay figuras inactivas.</td></tr></c:if>
  </tbody></table>
</div>

<form class="card" method="post" action="${ctx}/admin/figuras"><h2>¿Quién cuenta para el color de las figuras?</h2><p>La mediana solo usa a los miembros activos con estas frecuencias</p>
  <div class="inline" style="gap:12px">
    <c:forEach var="r" items="${reglas}">
      <label class="setting card" style="flex-direction:row;padding:10px 14px;background:var(--bg);border:0"><span class="bold">${r.key == 'EQUIPO_ENSAYOS' ? 'Equipo de ensayos' : fn:substring(r.key,0,1).concat(fn:toLowerCase(fn:substring(r.key,1,20)))}</span>
        <span class="switch"><input type="checkbox" name="cuenta_${r.key}" ${r.value ? 'checked' : ''}><span></span></span></label>
    </c:forEach>
    <button class="btn btn-secondary btn-sm">Guardar reglas</button>
  </div>
</form>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
