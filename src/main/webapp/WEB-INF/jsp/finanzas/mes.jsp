<c:set var="titulo" value="Cuotas de ${mesTexto}"/><c:set var="menu" value="Cuotas"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<section class="hero"><div class="titles"><span class="kicker">Team Finanzas</span><h1>${mesTexto}</h1>
  <p>Marca a todos los que ya pagaron y guarda una sola vez (por ejemplo, los que yapearon antes de que existiera la web)</p></div><span class="star"></span></section>
<c:set var="grupo" value="finanzas"/><c:set var="sub" value="mes"/><%@ include file="/WEB-INF/jsp/includes/subtabs.jspf" %>
<div class="inline"><span class="muted small">Mes:</span>
  <c:forEach var="k" items="${meses}" varStatus="st"><a class="pill-link ${k == ym ? 'on' : ''}" href="${ctx}/finanzas/mes?m=${k}">${mesesTexto[st.index]}</a></c:forEach></div>
<form class="card" method="post" action="${ctx}/finanzas/mes?m=${ym}"><input type="hidden" name="_csrf" value="${sessionScope.csrf}">
  <div class="inline" style="justify-content:space-between"><h2>Miembros</h2>
    <label class="check-item" style="padding:6px 10px"><input type="checkbox" id="todos"> Marcar todos los pendientes</label></div>
  <table class="table"><thead><tr><th></th><th>Miembro</th><th>Monto hoy</th><th>Estado</th></tr></thead><tbody>
    <c:forEach var="c" items="${cuotas}">
      <tr><td>${c.pendiente ? '<input type="checkbox" class="pend" name="cuota" value="'.concat(c.idCuota).concat('">') : ''}</td>
        <td><a href="${ctx}/finanzas/miembro?id=${c.idMiembro}"><c:out value="${c.nombre}"/></a></td><td>S/ ${c.montoHoy}</td>
        <td><sdp:tag texto="${c.situacionTexto}" tono="${c.tono}"/><c:if test="${c.pagada}"> <small class="muted">${c.pagoFechaTexto}</small></c:if></td></tr>
    </c:forEach>
  </tbody></table>
  <div class="form-row" style="align-items:flex-end">
    <div class="field"><label>Fecha en que pagaron</label><input class="input" type="date" name="fecha" value="${hoy}" max="${hoy}"></div>
    <div class="field"><label>Medio</label><select class="input" name="medio"><option value="YAPE">Yape</option><option value="EFECTIVO">Efectivo</option><option value="OTRO">Otro</option></select></div>
    <div class="field"><button class="btn btn-primary">Marcar como pagados</button></div></div>
  <p class="muted xs">El monto se calcula con esa fecha: hasta fin de mes S/ 6, después S/ 9 y desde el 15 del mes siguiente S/ 15.</p>
</form>
<script>
  document.getElementById('todos').addEventListener('change', function (e) { document.querySelectorAll('.pend').forEach(function (i) { i.checked = e.target.checked; }); });
</script>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
