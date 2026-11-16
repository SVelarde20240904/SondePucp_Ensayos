<c:set var="titulo" value="Histórico"/><c:set var="menu" value="Histórico"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<section class="hero"><div class="titles"><span class="kicker">Administración</span><h1>La mano en el tiempo</h1>
  <p>Cómo estaba la mano de figuras en otra fecha (el mes pasado, después de un ensayo…) y cuánto avanzó el grupo y cada uno</p></div>
  <span class="star"></span></section>
<c:set var="grupo" value="figuras"/><c:set var="sub" value="historico"/><%@ include file="/WEB-INF/jsp/includes/subtabs.jspf" %>
<div class="inline"><a class="pill-link on" href="${ctx}/admin/historico">La mano en el tiempo</a><a class="pill-link" href="${ctx}/admin/historico?vista=cambios">Cambio por cambio</a></div>

<form class="form-row" method="get" action="${ctx}/admin/historico" style="align-items:flex-end">
  <div class="field"><label>Comparar desde</label><select class="input" name="a">
    <c:forEach var="c" items="${cortes}"><option value="${c.clave()}" ${c.clave() == a ? 'selected' : ''}>${c.etiqueta()}</option></c:forEach></select></div>
  <div class="field"><label>Hasta</label><select class="input" name="b">
    <c:forEach var="c" items="${cortes}"><option value="${c.clave()}" ${c.clave() == b ? 'selected' : ''}>${c.etiqueta()}</option></c:forEach></select></div>
  <c:if test="${not empty quien}"><input type="hidden" name="m" value="${quien}"></c:if>
  <div class="field" style="flex:none"><button class="btn btn-secondary">Ver</button></div>
</form>

<div class="grid-4">
  <div class="card stat"><b class="c-primary">${pctA}% → ${pctB}%</b><span>de la mano dominada (azul o verde)</span></div>
  <div class="card stat"><b class="c-verde">${subieron}</b><span>${subieron == 1 ? 'persona domina' : 'personas dominan'} más figuras que antes</span></div>
  <div class="card stat"><b class="c-accent">${cambiaron}</b><span>colores cambiaron entre las dos fechas</span></div>
  <div class="card stat"><b class="c-primary">${fn:length(gente)} × ${nFiguras}</b><span>miembros activos × figuras activas</span></div>
</div>

<div class="grid-2">
  <div class="card"><h2>Evolución del grupo</h2><p>% de la mano dominada después de cada ensayo y a fin de cada mes</p>
    <div class="graf" id="g-grupo" aria-label="Porcentaje de la mano dominada en el tiempo"></div></div>
  <div class="card"><h2>Evolución de una persona</h2>
    <form method="get" action="${ctx}/admin/historico" class="inline"><input type="hidden" name="a" value="${a}"><input type="hidden" name="b" value="${b}">
      <select class="input" name="m" onchange="this.form.submit()" style="max-width:280px"><option value="">Elige un miembro…</option>
        <c:forEach var="m" items="${gente}"><option value="${m.idMiembro}" ${m.idMiembro == quien ? 'selected' : ''}><c:out value="${m.nombre}"/></option></c:forEach></select></form>
    <c:choose>
      <c:when test="${empty quien}"><p class="muted small">Elige a alguien (o haz clic en su nombre en la tabla) para ver cuántas figuras domina en cada fecha.</p></c:when>
      <c:otherwise><p>Figuras que domina <c:out value="${quienNombre}"/> (de ${nFiguras})</p><div class="graf" id="g-uno" aria-label="Figuras que domina en el tiempo"></div></c:otherwise>
    </c:choose></div>
</div>

<div class="card"><h2>La mano · ${etiquetaB}</h2><p>Cada cuadrito es una persona en una figura. Con borde: cambió desde “${fn:toLowerCase(etiquetaA)}”. Pasa el mouse para ver el antes y el después.</p>
  <div class="legend"><span><sdp:chip color="AZUL"/> La domina</span><span><sdp:chip color="VERDE"/> La sabe bien</span><span><sdp:chip color="AMARILLO"/> A medias</span><span><sdp:chip color="ROJO"/> No la sabe</span><span><i class="mano-celda cambio" style="display:inline-block"></i> Cambió</span></div>
  <div class="scroll-x">
    <table class="mano"><thead><tr><th></th><c:forEach var="f" items="${figs}"><th title="${fn:escapeXml(f.nombre)}"><span><c:out value="${f.nombre}"/></span></th></c:forEach></tr></thead><tbody>
      <c:forEach var="m" items="${gente}">
        <tr><th><a href="?a=${a}&b=${b}&m=${m.idMiembro}#g-uno"><c:out value="${m.nombre}"/></a></th>
          <c:forEach var="f" items="${figs}"><c:set var="k" value="${m.idMiembro * 100000 + f.idFigura}"/><c:set var="cel" value="${celdas[k]}"/>
            <td><i class="mano-celda ${cel[0]}${cel[1] == '1' ? ' cambio' : ''}" title="${fn:escapeXml(m.nombre)} · ${fn:escapeXml(f.nombre)}: ${cel[2]} → ${cel[3]}"></i></td></c:forEach></tr>
      </c:forEach>
    </tbody></table>
  </div>
</div>

<div class="card"><h2>Quién avanzó más</h2><p>Figuras que domina cada uno: ${fn:toLowerCase(etiquetaA)} → ${fn:toLowerCase(etiquetaB)}</p>
  <table class="table"><thead><tr><th>Miembro</th><th class="r">Antes</th><th class="r">Ahora</th><th class="r">Cambio</th></tr></thead><tbody>
    <c:forEach var="r" items="${ranking}">
      <tr><td><a href="?a=${a}&b=${b}&m=${r.id}#g-uno"><c:out value="${r.nombre}"/></a></td><td class="r">${r.a}</td><td class="r"><b>${r.b}</b></td>
        <td class="r"><b class="${r.delta > 0 ? 'c-verde' : (r.delta < 0 ? 'c-rojo' : 'muted')}">${r.delta > 0 ? '+' : ''}${r.delta}</b></td></tr>
    </c:forEach>
  </tbody></table>
</div>
<script type="application/json" id="datos-mano">${datosMano}</script>
<script src="${ctx}/js/graficas.js"></script>
<script>
(function () {
  var D = JSON.parse(document.getElementById('datos-mano').textContent);
  function dibujar() {
    grafColumnas(document.getElementById('g-grupo'), D.cortes, [{ nombre: 'Mano dominada', valores: D.grupo }],
      { fmt: function (v) { return v + '%'; }, fmtEje: function (v) { return Math.round(v) + '%'; } });
    var uno = document.getElementById('g-uno');
    if (uno) grafColumnas(uno, D.cortes, [{ nombre: 'Figuras que domina', valores: D.miembro }], { fmt: function (v) { return v + ' figuras'; } });
  }
  dibujar();
  var t; window.addEventListener('resize', function () { clearTimeout(t); t = setTimeout(dibujar, 150); });
})();
</script>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
