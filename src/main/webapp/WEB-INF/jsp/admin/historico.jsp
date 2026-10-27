<c:set var="titulo" value="Histórico"/><c:set var="menu" value="Histórico"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<section class="hero"><div class="titles"><span class="kicker">Administración</span><h1>Histórico</h1>
  <p>Cada cambio de color queda registrado. Aquí también se hacen las correcciones (antes se editaba la Matriz a mano).</p></div>
  <div class="actions"><a class="btn btn-primary" href="#correccion">+ Registrar corrección</a></div><span class="star"></span></section>

<form class="form-row" method="get" action="${ctx}/admin/historico" style="align-items:flex-end">
  <div class="field"><label>Miembro</label><select class="input" name="fMiembro"><option value="">Todos</option>
    <c:forEach var="m" items="${miembrosLista}"><option value="${m.idMiembro}" ${param.fMiembro == m.idMiembro ? 'selected' : ''}><c:out value="${m.nombre}"/></option></c:forEach></select></div>
  <div class="field"><label>Figura</label><select class="input" name="fFigura"><option value="">Todas</option>
    <c:forEach var="f" items="${figurasLista}"><option value="${f.idFigura}" ${param.fFigura == f.idFigura ? 'selected' : ''}><c:out value="${f.nombre}"/></option></c:forEach></select></div>
  <div class="field"><label>Fuente</label><select class="input" name="fFuente"><option value="">Todas</option>
    <c:forEach var="s" items="${['MENSUAL','ENSAYO','CORRECCION','CARGA_INICIAL']}"><option value="${s}" ${param.fFuente == s ? 'selected' : ''}>${s == 'ENSAYO' ? 'Del ensayo' : (s == 'CORRECCION' ? 'Corrección' : (s == 'CARGA_INICIAL' ? 'Carga inicial' : 'Mensual'))}</option></c:forEach></select></div>
  <div class="field"><label>Desde</label><input class="input" type="date" name="fDesde" value="${param.fDesde}"></div>
  <div class="field"><label>Hasta</label><input class="input" type="date" name="fHasta" value="${param.fHasta}"></div>
  <div class="field" style="flex:none"><button class="btn btn-secondary">Filtrar</button></div>
</form>

<div class="grid-4">
  <div class="card stat"><b class="c-primary">${cuentas[0]}</b><span>registros</span></div>
  <div class="card stat"><b class="c-primary">${cuentas[1]}</b><span>mensual</span></div>
  <div class="card stat"><b class="c-primary">${cuentas[2]}</b><span>del ensayo</span></div>
  <div class="card stat"><b class="c-primary">${cuentas[3]}</b><span>correcciones</span></div>
</div>

<div class="card">
  <table class="table"><thead><tr><th>Fecha</th><th>Miembro</th><th>Figura</th><th>Cambio</th><th>Fuente</th><th>Registrado por</th></tr></thead><tbody>
  <c:forEach var="h" items="${lista}"><tr><td>${h.fechaTexto}</td><td><c:out value="${h.miembro}"/></td><td><c:out value="${h.figura}"/></td>
    <td><sdp:cambio antes="${h.colorAnterior}" despues="${h.colorNuevo}"/></td><td><sdp:tag texto="${h.fuenteTexto}" tono="${h.fuenteTono}"/></td>
    <td><c:choose><c:when test="${not h.aplicado}"><span class="tag neutral">Ignorado · solo sube</span></c:when><c:otherwise><c:out value="${h.registradoPor}"/><c:if test="${not empty h.motivo}"><small><c:out value="${h.motivo}"/></small></c:if></c:otherwise></c:choose></td></tr></c:forEach>
  <c:if test="${empty lista}"><tr><td colspan="6" class="empty">No hay registros con esos filtros.</td></tr></c:if>
  </tbody></table>
  <c:url var="base" value="/admin/historico"><c:param name="fMiembro" value="${param.fMiembro}"/><c:param name="fFigura" value="${param.fFigura}"/><c:param name="fFuente" value="${param.fFuente}"/><c:param name="fDesde" value="${param.fDesde}"/><c:param name="fHasta" value="${param.fHasta}"/></c:url>
  <div class="inline"><span class="muted small">Mostrando ${desdeFila}–${hastaFila} de ${cuentas[0]}</span><span class="spacer"></span>
    <c:if test="${pagina > 1}"><a class="pill-link" href="${base}&p=${pagina - 1}">← Anterior</a></c:if>
    <span class="pill-link on">${pagina} / ${paginas}</span>
    <c:if test="${pagina < paginas}"><a class="pill-link" href="${base}&p=${pagina + 1}">Siguiente →</a></c:if></div>
</div>

<form class="card" id="correccion" method="post" action="${ctx}/admin/historico"><h2>Registrar corrección</h2><p>Ej.: alguien eligió el nombre equivocado en su autoevaluación (flujo B4 de la guía)</p>
  <div class="form-row" style="align-items:flex-end">
    <div class="field"><label>Miembro</label><select class="input" name="cMiembro" required><option value="">—</option><c:forEach var="m" items="${miembrosLista}"><option value="${m.idMiembro}"><c:out value="${m.nombre}"/></option></c:forEach></select></div>
    <div class="field"><label>Figura</label><select class="input" name="cFigura" required><option value="">—</option><c:forEach var="f" items="${figurasLista}"><option value="${f.idFigura}"><c:out value="${f.nombre}"/></option></c:forEach></select></div>
    <div class="field" style="flex:none"><span class="label">Nuevo color</span><div class="inline" style="flex-wrap:nowrap">
      <c:forEach var="c" items="${['ROJO','AMARILLO','VERDE','AZUL']}"><label class="opt ${fn:toLowerCase(c)}"><input type="radio" name="cColor" value="${c}" required><span>${fn:substring(c,0,1)}${fn:toLowerCase(fn:substring(c,1,10))}</span></label></c:forEach></div></div>
    <div class="field"><label>Motivo</label><input class="input" name="cMotivo" placeholder="Marcó con el nombre de otra persona" required></div>
    <div class="field" style="flex:none"><button class="btn btn-primary">Guardar</button></div>
  </div>
</form>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
