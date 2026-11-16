<c:set var="titulo" value="Mi cuota"/><c:set var="menu" value="Mi cuota"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<section class="hero"><div class="titles"><span class="kicker">Mi espacio</span><h1>Mi cuota</h1>
  <p>Cuánto debes, cuánto sube si esperas y todo lo que ya pagaste</p></div>
  <span class="star"></span></section>

<c:forEach var="r" items="${rechazos}">
  <div class="note bad">⚠ Finanzas no pudo validar tu captura del ${r.fechaTexto}: <b><c:out value="${r.motivoRechazo}"/></b>. Vuelve a subirla abajo.</div>
</c:forEach>

<div class="grid-4">
  <div class="card stat"><c:choose>
    <c:when test="${debeAlgo}"><b class="c-rojo">S/ ${deudaTexto}</b><span>debes hoy · desde ${deudaDesde}</span></c:when>
    <c:otherwise><b class="c-verde">S/ 0</b><span>estás al día 🎉</span></c:otherwise></c:choose></div>
  <div class="card stat"><c:choose>
    <c:when test="${not empty sube}"><b class="c-amarillo">S/ ${sube[0][1]}</b><span>si pagas desde el ${sube[0][0]}<c:if test="${fn:length(sube) > 1}"> · S/ ${sube[1][1]} desde el ${sube[1][0]}</c:if></span></c:when>
    <c:otherwise><b>S/ 6</b><span>la cuota si pagas dentro del mes</span></c:otherwise></c:choose></div>
  <div class="card stat"><b class="c-primary">S/ ${pagadoAnio}</b><span>pagaste en ${anio} · ${mesesAnio} ${mesesAnio == 1 ? 'mes' : 'meses'}<c:if test="${not empty aTiempo}">, ${aTiempo} a tiempo</c:if></span></div>
  <div class="card stat"><c:choose>
    <c:when test="${not empty cobrador}"><b style="font-size:22px"><c:out value="${cobrador}"/></b><span>tu encargado(a) de cobro
      <c:if test="${not empty cobradorWa}"> · <a href="${cobradorWa}" target="_blank" rel="noopener">escribirle por WhatsApp</a></c:if></span></c:when>
    <c:otherwise><b style="font-size:22px">Finanzas</b><span>todavía no tienes encargado(a) de cobro</span></c:otherwise></c:choose></div>
</div>

<div class="row top">
  <div class="stack grow">
    <c:choose>
      <c:when test="${not empty porPagar or anualAbierto}">
        <form class="card" method="post" action="${ctx}/mi-cuota?_csrf=${sessionScope.csrf}" enctype="multipart/form-data">
          <input type="hidden" name="_csrf" value="${sessionScope.csrf}">
          <h2>Pagar</h2>
          <p>1) Yapea a ${empty cobrador ? 'la persona de Finanzas' : cobrador} · 2) marca qué mes(es) estás pagando · 3) sube la captura.</p>
          <div class="stack" style="gap:8px">
            <c:if test="${anualAbierto}">
              <label class="check-item cuota-item anual"><input type="checkbox" name="anual" value="1" data-monto="66">
                <span><b>Pagar todo ${anio}</b> <small class="muted">los 12 meses por S/ 66 (te ahorras un mes) · solo en enero</small></span>
                <span class="spacer"></span><sdp:tag texto="S/ 66" tono="ok"/></label>
            </c:if>
            <c:forEach var="c" items="${porPagar}" varStatus="st">
              <label class="check-item cuota-item mensual"><input type="checkbox" name="cuota" value="${c.idCuota}" ${st.first ? 'checked' : ''} data-monto="${c.montoHoy}">
                <span><b>${c.mesTexto}</b> <small class="muted">${c.parcial ? 'ya abonaste S/ '.concat(c.abonadoTexto).concat(' · ') : ''}${c.vencida ? 'vencida' : 'vence el '.concat(c.venceTexto)}</small></span>
                <span class="spacer"></span><sdp:tag texto="S/ ${c.montoTexto}" tono="${c.tono}"/></label>
            </c:forEach>
          </div>
          <p class="cuota-total">Total a yapear: <b>S/ <span id="totalCuota">0</span></b></p>
          <div class="field"><label>Captura del Yape *</label><input class="input" type="file" name="voucher" accept="image/*" required></div>
          <div class="inline" style="justify-content:flex-end"><button class="btn btn-primary">Enviar captura</button></div>
        </form>
      </c:when>
      <c:otherwise><div class="card"><h2>¡Estás al día! 🎉</h2><p class="muted">No tienes cuotas por pagar. La próxima aparece el 1 del mes.</p></div></c:otherwise>
    </c:choose>

    <div class="card"><h2>Mis pagos</h2>
      <c:if test="${empty pagos}"><p class="empty">Todavía no hay pagos registrados en la web.</p></c:if>
      <c:if test="${not empty pagos}">
      <table class="table"><thead><tr><th>Fecha</th><th>Qué pagaste</th><th class="r">Monto</th><th>Estado</th></tr></thead><tbody>
        <c:forEach var="p" items="${pagos}">
          <tr><td>${p.fechaCorta}<small>${p.medioTexto}</small></td>
            <td><c:out value="${p.meses}"/><c:if test="${p.rechazado and not empty p.motivoRechazo}"><small>Motivo: <c:out value="${p.motivoRechazo}"/></small></c:if></td>
            <td class="r">S/ ${p.totalTexto}</td>
            <td><sdp:tag texto="${p.estadoTexto}" tono="${p.tono}"/><c:if test="${p.voucher}"> <a class="xs" href="${ctx}/finanzas/voucher?id=${p.idPago}" target="_blank" rel="noopener">captura</a></c:if></td></tr>
        </c:forEach>
      </tbody></table></c:if>
    </div>
  </div>

  <div class="card w-440"><h2>Mis cuotas</h2><p class="muted small">Pagas desde ${desdeTexto}. S/ 6 hasta fin de mes, S/ 7.50 desde el 1 del mes siguiente y S/ 9 desde el 8.</p>
    <table class="table"><thead><tr><th>Mes</th><th class="r">Monto</th><th>Estado</th></tr></thead><tbody>
      <c:forEach var="c" items="${cuotas}">
        <tr><td><b>${c.mesTexto}</b><c:if test="${c.pagada and not empty c.pagoFechaTexto}"><small>Pagó el ${c.pagoFechaTexto}</small></c:if>
              <c:if test="${c.exonerada and not empty c.nota}"><small><c:out value="${c.nota}"/></small></c:if></td>
          <td class="r">S/ ${c.montoTexto}</td><td><sdp:tag texto="${c.situacionTexto}" tono="${c.tono}"/></td></tr>
      </c:forEach>
      <c:if test="${empty cuotas}"><tr><td colspan="3" class="empty">Todavía no tienes cuotas. Pagas desde ${desdeTexto}.</td></tr></c:if>
    </tbody></table></div>
</div>
<script>
  (function () {
    var out = document.getElementById('totalCuota'); if (!out) return;
    var anual = document.querySelector('.cuota-item.anual input');
    function suma() {
      var t = 0;
      if (anual && anual.checked) t = 66;
      else document.querySelectorAll('.cuota-item.mensual input:checked').forEach(function (i) { t += +i.dataset.monto; });
      out.textContent = t % 1 === 0 ? t : t.toFixed(2);
    }
    document.querySelectorAll('.cuota-item input').forEach(function (i) {
      i.addEventListener('change', function () {
        // el anual reemplaza a los meses sueltos
        if (anual && i === anual && i.checked) document.querySelectorAll('.cuota-item.mensual input').forEach(function (x) { x.checked = false; });
        if (anual && i !== anual && i.checked) anual.checked = false;
        suma();
      });
    });
    suma();
  })();
</script>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
