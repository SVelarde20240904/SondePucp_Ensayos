<c:set var="titulo" value="Cuenta de ${m.nombre}"/><c:set var="menu" value="Cuotas"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<section class="hero"><div class="titles"><span class="kicker">Team Finanzas · cuenta</span><h1><c:out value="${m.nombre}"/></h1>
  <p>Paga desde ${desdeTexto}${desdeManual ? ' (lo puso Finanzas)' : ' (regla: el mes siguiente a su ingreso)'}<c:if test="${deuda.total > 0}"> · <b>debe S/ ${deuda.totalTexto}</b></c:if></p></div>
  <span class="star"></span></section>
<c:set var="grupo" value="finanzas"/><c:set var="sub" value="matriz"/><%@ include file="/WEB-INF/jsp/includes/subtabs.jspf" %>

<div class="row top">
  <form class="card grow" method="post" action="${ctx}/finanzas/miembro"><input type="hidden" name="_csrf" value="${sessionScope.csrf}"><input type="hidden" name="id" value="${m.idMiembro}">
    <h2>Cuotas</h2>
    <table class="table"><thead><tr><th></th><th>Mes</th><th>Monto</th><th>Estado</th><th></th></tr></thead><tbody>
      <c:forEach var="c" items="${cuotas}">
        <tr><td>${c.pendiente and not c.enRevision ? '<input type="checkbox" name="cuota" value="'.concat(c.idCuota).concat('">') : ''}</td>
          <td><b>${c.mesTexto}</b><c:if test="${not empty c.nota}"><small><c:out value="${c.nota}"/></small></c:if></td>
          <td>S/ ${c.montoTexto}</td>
          <td><sdp:tag texto="${c.situacionTexto}" tono="${c.tono}"/><c:if test="${c.pagada}"><small>${c.pagoFechaTexto}${empty c.pagoMedio ? '' : ' · '.concat(c.pagoMedio == 'YAPE' ? 'Yape' : (c.pagoMedio == 'EFECTIVO' ? 'Efectivo' : 'Otro'))}</small></c:if></td>
          <td class="right"><c:if test="${perm.contains('EXONERAR_CUOTAS')}">
            <c:if test="${c.pagada or c.exonerada or c.parcial}"><button class="btn btn-ghost btn-sm" name="action" value="anular" formnovalidate onclick="this.form.idCuota.value='${c.idCuota}'">Volver a pendiente</button></c:if>
            <c:if test="${c.pendiente and not c.enRevision}"><button type="button" class="btn btn-ghost btn-sm exo" data-id="${c.idCuota}" data-mes="${c.mesTexto}">Exonerar</button></c:if></c:if>
            <c:if test="${c.enRevision}"><a class="btn btn-ghost btn-sm" href="${ctx}/finanzas">Ver captura</a></c:if></td></tr>
      </c:forEach>
      <c:if test="${empty cuotas}"><tr><td colspan="5" class="empty">Todavía no tiene cuotas (paga desde ${desdeTexto}).</td></tr></c:if>
    </tbody></table>
    <input type="hidden" name="idCuota" value="">
    <div class="form-row" style="align-items:flex-end">
      <div class="field"><label>Fecha del pago</label><input class="input" type="date" name="fecha" value="${hoy}" max="${hoy}"></div>
      <div class="field"><label>Medio</label><select class="input" name="medio"><option value="YAPE">Yape</option><option value="EFECTIVO">Efectivo</option><option value="OTRO">Otro</option></select></div>
      <div class="field"><label>Monto <span class="muted">(vacío = completo)</span></label><input class="input" name="monto" inputmode="decimal" placeholder="Ej: 6" style="max-width:120px"></div>
      <div class="field"><button class="btn btn-primary" name="action" value="pagar">Marcar como pagado</button></div></div>
    <p class="muted xs">Si pones un monto menor a lo que debe, se aplica desde el mes más antiguo y lo que falta queda pendiente.</p>
    <c:if test="${anualAbierto}"><div class="inline" style="margin-top:8px"><button class="btn btn-secondary btn-sm" name="action" value="anual" formnovalidate>Registrar pago anual ${anio} (S/ 66)</button></div></c:if>
    <div class="exo-panel" hidden><div class="form-row" style="align-items:flex-end">
      <div class="field"><label>Motivo de la exoneración de <span class="exo-mes"></span></label><input class="input" name="nota" maxlength="200" placeholder="Ej: licencia por viaje"></div>
      <div class="field"><button class="btn btn-secondary" name="action" value="exonerar">Exonerar</button></div></div></div>
  </form>

  <div class="stack w-340">
    <form class="card" method="post" action="${ctx}/finanzas/miembro"><input type="hidden" name="_csrf" value="${sessionScope.csrf}"><input type="hidden" name="id" value="${m.idMiembro}">
      <h2>Encargado de cobro</h2>
      <div class="field"><select class="input" name="idCobrador"><option value="">— sin asignar —</option>
        <c:forEach var="x" items="${cobradores}"><option value="${x[0]}" ${x[0] == idCobrador ? 'selected' : ''}><c:out value="${x[1]}"/></option></c:forEach></select></div>
      <button class="btn btn-secondary btn-sm" name="action" value="cobrador">Guardar</button></form>
    <c:if test="${deuda.total > 0}">
      <div class="card"><h2>Recordatorio</h2><p class="small">${deuda.meses}</p>
        <div class="inline"><c:if test="${not empty deuda.whatsapp}"><a class="btn btn-primary btn-sm" href="${deuda.whatsapp}" target="_blank" rel="noopener">Abrir WhatsApp</a></c:if>
          <button type="button" class="btn btn-secondary btn-sm copiar" data-texto="<c:out value='${deuda.mensaje}'/>">Copiar mensaje</button></div>
        <c:if test="${empty deuda.whatsapp}"><p class="muted xs">No tiene celular registrado: copia el mensaje y mándalo tú.</p></c:if></div>
    </c:if>
    <form class="card" method="post" action="${ctx}/finanzas/miembro"><input type="hidden" name="_csrf" value="${sessionScope.csrf}"><input type="hidden" name="id" value="${m.idMiembro}">
      <h2>¿Desde qué mes paga?</h2>
      <p class="muted small">Por defecto, desde el mes siguiente a su ingreso${empty m.fechaIngreso ? '' : ' (entró el '.concat(m.fechaIngresoTexto).concat(')')}. Cámbialo solo si es un caso especial.</p>
      <div class="field"><label>Paga desde</label><input class="input" type="month" name="desde" value="${desde}"></div>
      <div class="inline"><button class="btn btn-secondary btn-sm" name="action" value="desde">Guardar</button>
        <c:if test="${desdeManual}"><button class="btn btn-ghost btn-sm" name="action" value="desde" onclick="this.form.desde.value=''">Volver a la regla</button></c:if></div>
    </form>
  </div>
</div>
<script>
  document.addEventListener('click', function (e) {
    var b = e.target.closest('.copiar');
    if (b) navigator.clipboard.writeText(b.dataset.texto).then(function () { var t = b.textContent; b.textContent = '¡Copiado!'; setTimeout(function () { b.textContent = t; }, 1500); });
    var x = e.target.closest('.exo');
    if (x) { var p = document.querySelector('.exo-panel'); p.hidden = false; p.querySelector('.exo-mes').textContent = x.dataset.mes; x.form.idCuota.value = x.dataset.id; p.querySelector('input').focus(); }
  });
</script>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
