<c:set var="titulo" value="Mi progreso"/><c:set var="menu" value="Mi progreso"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<section class="hero"><div class="titles"><span class="kicker">Ensayos</span><h1>Mi progreso</h1>
  <p>Tu nivel, cómo están tus figuras y cuáles te faltan para subir</p></div><span class="star"></span></section>

<div class="grid-4">
  <div class="card stat"><c:choose><c:when test="${empty yo.nivel}"><b>—</b><span>sin nivel todavía</span></c:when>
    <c:otherwise><div class="inline"><sdp:chip color="${yo.nivel}"/><b style="font-size:24px">${yo.nivel}</b></div><span>tu nivel actual</span></c:otherwise></c:choose></div>
  <div class="card stat"><b class="c-primary">${empty siguienteNivel ? '—' : faltanSuNivel}</b><span>${empty siguienteNivel ? 'ya completaste los niveles anteriores' : 'figuras '.concat(yo.nivel).concat(' para subir a ').concat(siguienteNivel)}</span></div>
  <div class="card stat"><b class="c-verde">${cuentaColores['AZUL'] + cuentaColores['VERDE']}</b><span>figuras que dominas (azul o verde) de ${nFiguras}</span></div>
  <div class="card stat"><b class="c-amarillo">${cuentaColores['AMARILLO'] + cuentaColores['ROJO']}</b><span>figuras por practicar (amarillo o rojo)</span></div>
</div>

<c:if test="${nCortes > 1}">
<div class="card"><h2>Mi evolución</h2><p>Figuras que dominas (azul o verde) después de cada ensayo y a fin de cada mes</p>
  <div class="graf" id="g-evo" aria-label="Figuras que dominas en el tiempo"></div></div>
<script type="application/json" id="datos-evo">${datosEvolucion}</script>
<script src="${ctx}/js/graficas.js"></script>
<script>
  (function () {
    var D = JSON.parse(document.getElementById('datos-evo').textContent);
    function d() { grafColumnas(document.getElementById('g-evo'), D.cortes, [{ nombre: 'Figuras que dominas', valores: D.valores }], { fmt: function (v) { return v + ' figuras'; } }); }
    d(); var t; window.addEventListener('resize', function () { clearTimeout(t); t = setTimeout(d, 150); });
  })();
</script>
</c:if>

<div class="card"><h2>Figuras que me faltan</h2><p>Ordenadas por nivel: primero completa las de tu nivel actual</p>
  <div class="note info">ℹ El color de una figura es la mediana del grupo: AZUL = la domina casi todo el grupo (base) → ROJO = la domina casi nadie (avanzada).</div>
  <c:choose>
    <c:when test="${empty faltan}"><p class="empty">${empty yo.nivel ? 'Aún no te has autoevaluado.' : '¡No te falta ninguna figura con color! 🎉'}</p></c:when>
    <c:otherwise>
      <table class="table cards-compact"><thead><tr><th>Figura</th><th>Nivel de la figura</th><th>Mi color</th><th>Última actualización</th></tr></thead><tbody>
      <c:forEach var="e" items="${faltan}"><tr><td><c:out value="${e.figura}"/></td><td><sdp:chip color="${e.colorFigura}"/></td><td><sdp:chip color="${e.color}"/></td><td>${e.actualizacionTexto}</td></tr></c:forEach>
      </tbody></table>
    </c:otherwise>
  </c:choose>
  <div><a class="btn btn-ghost" href="${ctx}/autoevaluacion?tipo=mensual">Actualizar mis colores</a></div>
</div>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
