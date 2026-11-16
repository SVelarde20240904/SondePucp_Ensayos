<c:set var="titulo" value="Caja"/><c:set var="menu" value="Caja"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<section class="hero"><div class="titles"><span class="kicker">Team Finanzas</span><h1>Caja</h1>
  <p>Cuánta plata hay, en qué se usa y cómo cambia mes a mes. Las cuotas pagadas por la web (desde ${cuotasWebDesde}) entran solas.</p></div>
  <span class="star"></span></section>
<c:set var="grupo" value="finanzas"/><c:set var="sub" value="caja"/><%@ include file="/WEB-INF/jsp/includes/subtabs.jspf" %>

<div class="grid-4">
  <div class="card stat"><b class="c-primary">S/ ${saldo}</b><span>en caja hoy · empezó con S/ ${saldoInicial} el ${desdeTexto}</span></div>
  <div class="card stat"><b class="c-verde">S/ ${ingresos}</b><span>ingresos desde el ${desdeTexto}</span></div>
  <div class="card stat"><b class="c-rojo">S/ ${gastos}</b><span>gastos desde el ${desdeTexto}</span></div>
  <div class="card stat"><b class="${netoMesNegativo ? 'c-rojo' : 'c-verde'}">${netoMesNegativo ? '−' : '+'} S/ ${netoMes}</b><span>balance de ${mesTexto}</span></div>
</div>

<div class="grid-2">
  <div class="card"><h2>Saldo de la caja</h2><p>Cómo queda la caja al cierre de cada mes</p>
    <div class="graf" id="g-saldo" aria-label="Saldo de la caja al cierre de cada mes"></div></div>
  <div class="card"><h2>Ingresos y gastos por mes</h2>
    <div class="graf-leyenda"><span><i style="background:#3C68C4"></i>Ingresos</span><span><i style="background:#C08A2A"></i>Gastos</span></div>
    <div class="graf" id="g-mes" aria-label="Ingresos y gastos por mes"></div></div>
  <div class="card"><h2>¿En qué se gasta?</h2><p>Gastos por categoría</p>
    <div class="graf" id="g-cat" aria-label="Gastos por categoría"></div></div>
  <div class="card"><h2>Cuotas al día</h2><p>% de miembros que pagaron (o están exonerados) cada mes</p>
    <div class="graf" id="g-aldia" aria-label="Porcentaje de miembros al día por mes"></div></div>
  <div class="card"><h2>Deuda por encargado de cobro</h2><p>Cuotas vencidas sin pagar de los miembros activos</p>
    <div class="graf" id="g-cob" aria-label="Deuda por encargado de cobro"></div></div>
  <div class="card"><h2>Proyectos y eventos</h2><p>Lo que dejó o costó cada uno (según los movimientos con proyecto)</p>
    <c:choose>
      <c:when test="${empty proyectos}"><p class="empty">Todavía no hay movimientos con proyecto. Al registrar uno, ponle el nombre del evento (ej. “Aniversario taller SDP”).</p></c:when>
      <c:otherwise>
      <table class="table"><thead><tr><th>Proyecto</th><th class="r">Ingresos</th><th class="r">Gastos</th><th class="r">Neto</th></tr></thead><tbody>
        <c:forEach var="p" items="${proyectos}"><tr><td><b><c:out value="${p.nombre}"/></b></td><td class="r">S/ ${p.in}</td><td class="r">S/ ${p.out}</td>
          <td class="r"><b class="${p.negativo == 'true' ? 'c-rojo' : 'c-verde'}">S/ ${p.neto}</b></td></tr></c:forEach>
      </tbody></table></c:otherwise>
    </c:choose>
  </div>
</div>

<div class="row top">
  <div class="card grow"><h2>Movimientos</h2><p>Todo lo registrado (lo del Excel tiene fecha aproximada: el mes)</p>
    <div class="scroll-x"><table class="table"><thead><tr><th>Fecha</th><th>Concepto</th><th>Categoría</th><th class="r">Monto</th><c:if test="${perm.contains('GESTIONAR_CAJA')}"><th></th></c:if></tr></thead><tbody>
      <c:forEach var="m" items="${movimientos}">
        <tr><td>${m.fechaTexto}</td>
          <td><b><c:out value="${m.concepto}"/></b><c:if test="${not empty m.proyecto}"><small><c:out value="${m.proyecto}"/></small></c:if></td>
          <td><span class="tag ${m.tipo == 'INGRESO' ? 'ok' : 'neutral'}"><c:out value="${m.categoria}"/></span></td>
          <td class="r"><b class="${m.tipo == 'INGRESO' ? 'c-verde' : 'c-rojo'}">${m.tipo == 'INGRESO' ? '+' : '−'} S/ ${m.montoTexto}</b></td>
          <c:if test="${perm.contains('GESTIONAR_CAJA')}"><td class="r"><form method="post" action="${ctx}/finanzas/caja" class="inline-form"><input type="hidden" name="_csrf" value="${sessionScope.csrf}"><input type="hidden" name="idMov" value="${m.id}">
            <button class="btn btn-ghost btn-sm" name="action" value="eliminar" title="Eliminar" onclick="return confirm('¿Eliminar este movimiento?')">✕</button></form></td></c:if></tr>
      </c:forEach>
      <c:if test="${empty movimientos}"><tr><td colspan="5" class="empty">Todavía no hay movimientos.</td></tr></c:if>
    </tbody></table></div>
  </div>
  <div class="stack w-440">
    <c:if test="${perm.contains('GESTIONAR_CAJA')}">
    <form class="card" method="post" action="${ctx}/finanzas/caja"><input type="hidden" name="_csrf" value="${sessionScope.csrf}"><h2>Registrar movimiento</h2>
      <div class="pills"><label class="pill"><input type="radio" name="tipo" value="GASTO" checked><span>Gasto</span></label><label class="pill"><input type="radio" name="tipo" value="INGRESO"><span>Ingreso</span></label></div>
      <div class="form-row"><div class="field"><label>Fecha</label><input class="input" type="date" name="fecha" value="${hoy}" required></div>
        <div class="field"><label>Monto (S/)</label><input class="input" name="monto" inputmode="decimal" placeholder="Ej: 63.70" required></div></div>
      <div class="field"><label>Concepto</label><input class="input" name="concepto" maxlength="200" placeholder="Ej: Torta del aniversario" required></div>
      <div class="field"><label>Categoría</label><input class="input" name="categoria" list="cats" maxlength="40" placeholder="Ej: Eventos">
        <datalist id="cats"><c:forEach var="k" items="${catGasto}"><option value="${k}"></c:forEach><c:forEach var="k" items="${catIngreso}"><option value="${k}"></c:forEach></datalist></div>
      <div class="field"><label>Proyecto o evento <span class="muted">(opcional)</span></label><input class="input" name="proyecto" list="proys" maxlength="80" placeholder="Ej: Aniversario taller SDP">
        <datalist id="proys"><c:forEach var="k" items="${proyectosUsados}"><option value="<c:out value='${k}'/>"></c:forEach></datalist></div>
      <div><button class="btn btn-primary">Registrar</button></div>
    </form>
    </c:if>
    <div class="card"><h2>Mes a mes</h2>
      <table class="table"><thead><tr><th>Mes</th><th class="r">Entró</th><th class="r">Salió</th><th class="r">Saldo</th></tr></thead><tbody>
        <c:forEach var="t" items="${tablaMeses}"><tr><td>${t.mes}</td><td class="r">${t.in}</td><td class="r">${t.out}</td><td class="r"><b>${t.saldo}</b></td></tr></c:forEach>
      </tbody></table></div>
  </div>
</div>
<script type="application/json" id="datos-caja">${datosGraficas}</script>
<script src="${ctx}/js/graficas.js"></script>
<script>
(function () {
  var D = JSON.parse(document.getElementById('datos-caja').textContent);
  function dibujar() {
    var meses = D.meses.map(function (m) { return m.mes; });
    grafLinea(document.getElementById('g-saldo'), meses, D.meses.map(function (m) { return m.saldo; }), { nombre: 'Saldo' });
    grafColumnas(document.getElementById('g-mes'), meses, [
      { nombre: 'Ingresos', valores: D.meses.map(function (m) { return m['in']; }) },
      { nombre: 'Gastos', valores: D.meses.map(function (m) { return m.out; }) }]);
    grafBarras(document.getElementById('g-cat'), D.categorias, { color: '#C08A2A' });
    grafColumnas(document.getElementById('g-aldia'), D.alDia.map(function (m) { return m.mes; }),
      [{ nombre: 'Al día', valores: D.alDia.map(function (m) { return m.total ? Math.round(100 * m.ok / m.total) : 0; }) }],
      { fmt: function (v) { return v + '%'; }, fmtEje: function (v) { return Math.round(v) + '%'; } });
    grafBarras(document.getElementById('g-cob'), D.cobradores);
  }
  dibujar();
  var t; window.addEventListener('resize', function () { clearTimeout(t); t = setTimeout(dibujar, 150); });
})();
</script>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
