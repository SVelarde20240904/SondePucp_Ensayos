<c:set var="titulo" value="Cuotas"/><c:set var="menu" value="Cuotas"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<section class="hero"><div class="titles"><span class="kicker">Team Finanzas</span><h1>Cuotas</h1>
  <p>Capturas por confirmar, quién debe y recordatorios listos para mandar</p></div>
  <div class="actions"><a class="btn btn-primary" href="${ctx}/finanzas/mes?m=${mesClave}">Registrar pagos de ${mesTexto}</a></div><span class="star"></span></section>
<c:set var="grupo" value="finanzas"/><c:set var="sub" value="resumen"/><%@ include file="/WEB-INF/jsp/includes/subtabs.jspf" %>

<div class="grid-4">
  <div class="card stat"><b class="c-verde">${pagaronMes}/${totalMes}</b><span>pagaron ${mesTexto}</span></div>
  <div class="card stat"><b>S/ ${recaudadoMes}</b><span>recaudado en ${mesTexto}</span></div>
  <div class="card stat"><b class="c-primary">${fn:length(porConfirmar)}</b><span>capturas por confirmar</span></div>
  <div class="card stat"><b class="c-rojo">S/ ${deudaTotal}</b><span>de deuda (${fn:length(deudores)} personas)</span></div>
</div>

<div class="row top">
  <div class="stack grow">
    <div class="card"><h2>Capturas por confirmar</h2>
      <c:forEach var="p" items="${porConfirmar}">
        <div class="pago-revision">
          <a href="${ctx}/finanzas/voucher?id=${p.idPago}" target="_blank" rel="noopener" class="voucher-mini"><img src="${ctx}/finanzas/voucher?id=${p.idPago}" alt="Captura de ${p.nombre}" loading="lazy"></a>
          <div class="stack" style="gap:6px;flex:1">
            <div><b><a href="${ctx}/finanzas/miembro?id=${p.idMiembro}"><c:out value="${p.nombre}"/></a></b> <span class="muted small">· ${p.fechaTexto}</span></div>
            <div class="small">${p.meses} · <b>S/ ${p.total}</b><c:if test="${not empty p.operacion}"> · Op. <c:out value="${p.operacion}"/></c:if></div>
            <div class="inline">
              <form method="post" action="${ctx}/finanzas" class="inline"><input type="hidden" name="_csrf" value="${sessionScope.csrf}"><input type="hidden" name="idPago" value="${p.idPago}">
                <button class="btn btn-primary btn-sm" name="action" value="confirmar">✓ Confirmar</button></form>
              <details class="rechazo"><summary class="btn btn-ghost btn-sm">Rechazar</summary>
                <form method="post" action="${ctx}/finanzas" class="inline" style="margin-top:8px"><input type="hidden" name="_csrf" value="${sessionScope.csrf}"><input type="hidden" name="idPago" value="${p.idPago}">
                  <input class="input" name="motivo" maxlength="200" required placeholder="Ej: no se ve el monto" style="max-width:260px">
                  <button class="btn btn-secondary btn-sm" name="action" value="rechazar">Rechazar</button></form></details>
            </div>
          </div>
        </div>
      </c:forEach>
      <c:if test="${empty porConfirmar}"><p class="empty">No hay capturas por revisar.</p></c:if>
    </div>

    <div class="card"><h2>Deben</h2><p class="muted small">Cuotas vencidas, con el monto de hoy. El botón de WhatsApp abre el chat con el mensaje listo.</p>
      <table class="table"><thead><tr><th>Miembro</th><th>Meses</th><th>Total</th><th></th></tr></thead><tbody>
        <c:forEach var="d" items="${deudores}">
          <tr><td><a href="${ctx}/finanzas/miembro?id=${d.id}"><b><c:out value="${d.nombre}"/></b></a></td><td class="small">${d.meses}</td><td><b class="c-rojo">S/ ${d.total}</b></td>
            <td class="right"><c:if test="${not empty d.whatsapp}"><a class="btn btn-ghost btn-sm" href="${d.whatsapp}" target="_blank" rel="noopener">WhatsApp</a></c:if>
              <button type="button" class="btn btn-ghost btn-sm copiar" data-texto="<c:out value='${d.mensaje}'/>">Copiar mensaje</button></td></tr>
        </c:forEach>
        <c:if test="${empty deudores}"><tr><td colspan="4" class="empty">Nadie debe. 🙌</td></tr></c:if>
      </tbody></table></div>
  </div>

  <div class="card w-340"><h2>Recordatorio para el grupo</h2><p class="muted small">Para mandarlo al grupo de WhatsApp de SDP.</p>
    <textarea class="input" rows="7" readonly onclick="this.select()"><c:out value="${recordatorioGeneral}"/></textarea>
    <button type="button" class="btn btn-secondary btn-sm copiar" data-texto="<c:out value='${recordatorioGeneral}'/>">Copiar</button></div>
</div>
<script>
  document.addEventListener('click', function (e) {
    var b = e.target.closest('.copiar'); if (!b) return;
    navigator.clipboard.writeText(b.dataset.texto).then(function () { var t = b.textContent; b.textContent = '¡Copiado!'; setTimeout(function () { b.textContent = t; }, 1500); });
  });
</script>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
