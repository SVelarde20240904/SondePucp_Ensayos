<c:set var="titulo" value="Mi cuota"/><c:set var="menu" value="Mi cuota"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<section class="hero"><div class="titles"><span class="kicker">Mi espacio</span><h1>Mi cuota</h1>
  <p>S/ ${montos[0]} hasta el último día del mes · S/ ${montos[1]} desde el 1 del mes siguiente · S/ ${montos[2]} desde el 15 (tope)</p></div>
  <span class="star"></span></section>

<c:forEach var="r" items="${rechazos}">
  <div class="note bad">⚠ Finanzas no pudo validar tu captura del ${r.fechaTexto}: <b><c:out value="${r.motivoRechazo}"/></b>. Vuelve a subirla abajo.</div>
</c:forEach>

<div class="row top">
  <div class="stack grow">
    <c:choose>
      <c:when test="${not empty porPagar}">
        <form class="card" method="post" action="${ctx}/mi-cuota?_csrf=${sessionScope.csrf}" enctype="multipart/form-data">
          <input type="hidden" name="_csrf" value="${sessionScope.csrf}">
          <h2>Pagar</h2>
          <p>1) Yapea a la persona de Finanzas · 2) marca qué mes(es) estás pagando · 3) sube la captura.</p>
          <div class="stack" style="gap:8px">
            <c:forEach var="c" items="${porPagar}" varStatus="st">
              <label class="check-item cuota-item"><input type="checkbox" name="cuota" value="${c.idCuota}" ${st.first ? 'checked' : ''} data-monto="${c.montoHoy}">
                <span><b>${c.mesTexto}</b> <small class="muted">vence el ${c.venceTexto}</small></span>
                <span class="spacer"></span><sdp:tag texto="S/ ${c.montoHoy}" tono="${c.tono}"/></label>
            </c:forEach>
          </div>
          <p class="cuota-total">Total a yapear: <b>S/ <span id="totalCuota">0</span></b></p>
          <div class="form-row">
            <div class="field"><label>Captura del Yape *</label><input class="input" type="file" name="voucher" accept="image/*" required></div>
            <div class="field"><label>N° de operación <span class="muted">(opcional)</span></label><input class="input" name="operacion" maxlength="40" inputmode="numeric" placeholder="Ej: 01234567"></div></div>
          <div class="inline" style="justify-content:flex-end"><button class="btn btn-primary">Enviar captura</button></div>
        </form>
      </c:when>
      <c:otherwise><div class="card"><h2>¡Estás al día! 🎉</h2><p class="muted">No tienes cuotas por pagar. La próxima aparece el 1 del mes.</p></div></c:otherwise>
    </c:choose>
  </div>

  <div class="card w-440"><h2>Mis cuotas</h2><p class="muted small">Pagas desde ${desdeTexto}.</p>
    <table class="table"><thead><tr><th>Mes</th><th>Monto</th><th>Estado</th></tr></thead><tbody>
      <c:forEach var="c" items="${cuotas}">
        <tr><td><b>${c.mesTexto}</b><c:if test="${c.pagada and not empty c.pagoFechaTexto}"><small>Pagó el ${c.pagoFechaTexto}</small></c:if>
              <c:if test="${c.exonerada and not empty c.nota}"><small><c:out value="${c.nota}"/></small></c:if></td>
          <td>S/ ${c.montoHoy}</td><td><sdp:tag texto="${c.situacionTexto}" tono="${c.tono}"/></td></tr>
      </c:forEach>
      <c:if test="${empty cuotas}"><tr><td colspan="3" class="empty">Todavía no tienes cuotas. Pagas desde ${desdeTexto}.</td></tr></c:if>
    </tbody></table></div>
</div>
<script>
  (function () {
    var out = document.getElementById('totalCuota'); if (!out) return;
    function suma() { var t = 0; document.querySelectorAll('.cuota-item input:checked').forEach(function (i) { t += +i.dataset.monto; }); out.textContent = t; }
    document.querySelectorAll('.cuota-item input').forEach(function (i) { i.addEventListener('change', suma); }); suma();
  })();
</script>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
