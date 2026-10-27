<c:set var="titulo" value="Responsabilidades"/><c:set var="menu" value="Responsabilidades"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<section class="hero"><div class="titles"><span class="kicker">Administración</span><h1>Responsabilidades · semana del ${semanaTexto}</h1>
  <p>El estado se calcula solo con la fecha y hora de cumplimiento; las coberturas se detectan cuando lo cumple otra persona</p></div>
  <div class="actions"><a class="btn btn-secondary" href="${ctx}/admin/responsabilidades?semana=${semanaAnterior}">← Semana anterior</a><a class="btn btn-secondary" href="${ctx}/admin/responsabilidades?semana=${semanaSiguiente}">Siguiente →</a></div>
  <span class="star"></span></section>

<div class="note ok">✓ Automatizados por la web (ya no son encargos): Recopilar autoevaluación · Actualizar la BD con la autoevaluación · Creación y envío del formulario · Actualización completa de la BD · Cronograma semanal</div>

<div class="row top">
  <div class="stack grow">
    <div class="card"><h2>Encargos de la semana</h2>
      <table class="table"><thead><tr><th>Encargo</th><th>Responsable</th><th>Plazo</th><th>Cumplido por</th><th>Estado</th><th></th></tr></thead><tbody>
      <c:forEach var="e" items="${lista}">
        <tr><td><c:out value="${e.tipo}"/></td><td><c:out value="${e.responsable}"/></td><td>${e.plazoTexto}</td>
          <td><c:choose><c:when test="${not empty e.cumplidoPor}"><b><c:out value="${e.cumplidoPor}"/></b><small>${e.cumplimientoTexto}</small></c:when><c:otherwise>—</c:otherwise></c:choose></td>
          <td><sdp:tag texto="${e.estadoTexto}" tono="${e.estadoTono}"/><c:if test="${e.cobertura}"> <span class="tag info">Cobertura</span></c:if><c:if test="${e.justificada}"> <span class="tag neutral" title="${e.motivoJustificacion}">Justificada</span></c:if></td>
          <td><c:if test="${empty e.cumplidoPor}">
            <form method="post" action="${ctx}/admin/responsabilidades" class="inline" style="flex-wrap:nowrap"><input type="hidden" name="semana" value="${semana}"><input type="hidden" name="idEncargo" value="${e.idEncargo}">
              <select class="input" name="cumplidoPor" style="width:150px;padding:6px 10px"><c:forEach var="m" items="${activos}"><option value="${m.idMiembro}" ${m.idMiembro == e.idResponsable ? 'selected' : ''}><c:out value="${m.nombre}"/></option></c:forEach></select>
              <button class="btn btn-ghost btn-sm" name="action" value="cumplir">Cumplido</button>
              <c:if test="${e.estado == 'NO_CUMPLIDA' and not e.justificada}"><input class="input" name="motivo" placeholder="Motivo" style="width:120px;padding:6px 10px"><button class="btn btn-secondary btn-sm" name="action" value="justificar">Justificar</button></c:if>
            </form></c:if></td></tr>
      </c:forEach>
      <c:if test="${empty lista}"><tr><td colspan="6" class="empty">No hay encargos asignados esta semana.</td></tr></c:if>
      </tbody></table>
    </div>
    <form class="card" method="post" action="${ctx}/admin/responsabilidades"><h2>Asignar encargo</h2>
      <input type="hidden" name="semana" value="${semana}">
      <div class="form-row" style="align-items:flex-end">
        <div class="field"><label>Encargo</label><select class="input" name="idTipo"><c:forEach var="t" items="${tipos}"><option value="${t.idTipo}"><c:out value="${t.nombre}"/></option></c:forEach></select></div>
        <div class="field"><label>Responsable</label><select class="input" name="responsable"><c:forEach var="m" items="${activos}"><c:if test="${m.equipoEnsayos}"><option value="${m.idMiembro}"><c:out value="${m.nombre}"/></option></c:if></c:forEach>
          <option disabled>──────────</option><c:forEach var="m" items="${activos}"><c:if test="${not m.equipoEnsayos}"><option value="${m.idMiembro}"><c:out value="${m.nombre}"/></option></c:if></c:forEach></select></div>
        <div class="field"><label>Plazo</label><input class="input" type="datetime-local" name="plazo"></div>
        <div class="field" style="flex:none"><button class="btn btn-primary" name="action" value="asignar">+ Asignar</button></div></div>
    </form>
  </div>
  <div class="stack w-340">
    <div class="card"><h2>Estados</h2><p>Un solo estado + un campo “cumplido por”</p>
      <div class="inline" style="flex-wrap:nowrap"><span style="width:120px;flex:none"><span class="tag neutral">Pendiente</span></span><span class="muted small">Aún dentro del plazo</span></div>
      <div class="inline" style="flex-wrap:nowrap"><span style="width:120px;flex:none"><span class="tag ok">A tiempo</span></span><span class="muted small">Antes de la fecha/hora límite</span></div>
      <div class="inline" style="flex-wrap:nowrap"><span style="width:120px;flex:none"><span class="tag warn">Fuera de plazo</span></span><span class="muted small">Después del plazo</span></div>
      <div class="inline" style="flex-wrap:nowrap"><span style="width:120px;flex:none"><span class="tag bad">No cumplida</span></span><span class="muted small">Plazo vencido sin cumplir</span></div>
      <div class="inline" style="flex-wrap:nowrap"><span style="width:120px;flex:none"><span class="tag neutral">Justificada</span></span><span class="muted small">Marca extra con motivo escrito</span></div>
      <div class="inline" style="flex-wrap:nowrap"><span style="width:120px;flex:none"><span class="tag info">Cobertura</span></span><span class="muted small">Lo cumplió alguien que no era el responsable</span></div>
    </div>
    <div class="card"><h2>${mes} por persona</h2>
      <table class="table"><thead><tr><th>Miembro</th><th>A tiempo</th><th>Coberturas</th></tr></thead><tbody>
        <c:forEach var="r" items="${resumenMes}"><tr><td><c:out value="${r.nombre}"/></td><td>${r.a}/${r.b}</td><td>${r.c}</td></tr></c:forEach>
        <c:if test="${empty resumenMes}"><tr><td colspan="3" class="empty">Sin encargos este mes.</td></tr></c:if>
      </tbody></table></div>
  </div>
</div>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
