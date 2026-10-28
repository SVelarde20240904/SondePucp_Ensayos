<c:set var="titulo" value="Detalle de ensayo"/><c:set var="menu" value="Ensayos"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<section class="hero"><div class="titles"><span class="kicker">Ensayos › Historial</span><h1>Ensayo del ${ensayo.diaLargo} ${ensayo.fechaCorta}</h1>
  <p>${ensayo.tipoTexto} · <c:out value="${ensayo.lugar}"/> · ${ensayo.horario}<c:if test="${not empty ensayo.dictantesTexto}"> · dictan <c:out value="${ensayo.dictantesTexto}"/></c:if></p></div>
  <div class="actions"><a class="btn btn-secondary" href="${ctx}/ensayos/editar?id=${ensayo.idEnsayo}">Editar</a>
    <c:if test="${perm.contains('IMPRIMIR_REPORTES')}"><a class="btn btn-primary" href="${ctx}/ensayos/reporte?id=${ensayo.idEnsayo}">🖨 Imprimir reporte</a></c:if></div>
  <span class="star"></span></section>

<c:set var="tab" value="resumen"/><%@ include file="/WEB-INF/jsp/includes/ensayo-tabs.jspf" %>
<div class="inline">
  <span class="spacer"></span><sdp:tag texto="${ensayo.estadoTexto}" tono="${ensayo.estadoTono}"/>
  <c:if test="${ensayo.estado == 'PROGRAMADO' or ensayo.estado == 'BORRADOR'}">
    <form method="post" action="${ctx}/ensayos/detalle" class="inline-form"><input type="hidden" name="_csrf" value="${sessionScope.csrf}"><input type="hidden" name="id" value="${ensayo.idEnsayo}">
      <c:if test="${ensayo.pasado or ensayo.estado == 'PROGRAMADO'}"><button class="btn btn-secondary btn-sm" name="action" value="cerrar">Marcar como cerrado</button></c:if>
      <button class="btn btn-danger btn-sm" name="action" value="cancelar" onclick="return confirm('¿Cancelar este ensayo?')">Cancelar ensayo</button></form>
  </c:if>
  <c:if test="${perm.contains('ELIMINAR_ENSAYOS')}">
    <form method="post" action="${ctx}/ensayos/detalle" class="inline-form"><input type="hidden" name="_csrf" value="${sessionScope.csrf}"><input type="hidden" name="id" value="${ensayo.idEnsayo}">
      <button class="btn btn-danger btn-sm" name="action" value="eliminar"
        onclick="return confirm('¿ELIMINAR este ensayo para siempre?\n\nSe borran su asistencia, confirmaciones, figuras enseñadas y encargos.${ensayo.autoevaluaron > 0 ? '\\n' += ensayo.autoevaluaron += ' persona(s) se autoevaluaron: los colores que subieron se regresan.' : ''}\n\nÚsalo solo para ensayos de prueba o creados por error.')">🗑 Eliminar</button></form>
  </c:if>
</div>

<c:if test="${ensayo.estado != 'CANCELADO' and ensayo.estado != 'BORRADOR'}">
  <c:set var="okAsis" value="${ensayo.asistieron > 0}"/><c:set var="okFig" value="${ensayo.figurasEnsenadas > 0}"/><c:set var="okFecha" value="${ensayo.fechaHabilitaAutoeval}"/>
  <c:choose>
    <c:when test="${okAsis and okFig and okFecha and not ensayo.autoevalVencida}">
      <div class="note ok">✓ Autoevaluación del ensayo habilitada: los ${ensayo.asistieron} asistentes ven el aviso en su Inicio hasta el ${ensayo.autoevalHasta} (${ensayo.autoevaluaron} ya la hicieron).</div>
    </c:when>
    <c:when test="${ensayo.autoevalVencida}">
      <div class="note info">La autoevaluación de este ensayo ya cerró (se podía hasta el ${ensayo.autoevalHasta}). ${ensayo.autoevaluaron} persona(s) la hicieron.</div>
    </c:when>
    <c:otherwise>
      <div class="note warn">Para que los asistentes vean el aviso de autoevaluación falta:
        <c:if test="${not okAsis}"> · <a href="${ctx}/ensayos/asistencia?id=${ensayo.idEnsayo}">tomar asistencia</a></c:if>
        <c:if test="${not okFig}"> · <a href="${ctx}/ensayos/figuras?id=${ensayo.idEnsayo}">registrar las figuras enseñadas</a></c:if>
        <c:if test="${not okFecha}"> · que llegue el día del ensayo o marcarlo como cerrado</c:if>
      </div>
    </c:otherwise>
  </c:choose>
</c:if>

<div class="grid-6" id="resumen">
  <div class="card stat"><b class="c-primary">${ensayo.asistieron}/${ensayo.esperados}</b><span>asistieron (${ensayo.porcentajeAsistencia}%)</span></div>
  <div class="card stat"><b class="c-verde">${ensayo.puntuales}</b><span>puntuales</span></div>
  <div class="card stat"><b class="c-amarillo">${ensayo.tardanzas}</b><span>tardanzas</span></div>
  <div class="card stat"><b class="c-rojo">${ensayo.faltas}</b><span>faltas (${ensayo.faltasJustificadas} FJ · ${ensayo.faltas - ensayo.faltasJustificadas} FI)</span></div>
  <div class="card stat"><b class="c-primary">${ensayo.figurasEnsenadas}</b><span>figuras enseñadas</span></div>
  <div class="card stat"><b class="c-accent">+${ensayo.avances}</b><span>avances (${ensayo.autoevaluaron} se autoevaluaron)</span></div>
</div>

<div class="row top">
  <div class="card grow"><h2>Figuras enseñadas por nivel</h2><p>Lo que se trabajó en el ensayo</p>
    <c:choose>
      <c:when test="${empty ensenadasPorNivel}"><p class="empty">Aún no se registran las figuras enseñadas.
        <c:if test="${perm.contains('REGISTRAR_FIGURAS_ENSENADAS')}"><a href="${ctx}/ensayos/figuras?id=${ensayo.idEnsayo}">Registrarlas</a></c:if></p></c:when>
      <c:otherwise>
        <table class="table"><thead><tr><th>Nivel</th><th>Figuras enseñadas</th><th>Dictó</th></tr></thead><tbody>
          <c:forEach var="entry" items="${ensenadasPorNivel}"><tr><td><sdp:chip color="${entry.key}"/></td>
            <td><c:forEach var="f" items="${entry.value}" varStatus="s"><c:out value="${f.figura}"/>${s.last ? '' : ', '}</c:forEach></td>
            <td><c:out value="${entry.value[0].dicto}"/></td></tr></c:forEach>
        </tbody></table>
      </c:otherwise>
    </c:choose>
  </div>
  <div class="card w-540"><h2>Avances del ensayo</h2><p>Autoevaluación del ensayo (solo sube)</p>
    <c:choose>
      <c:when test="${empty avances}"><p class="empty">Todavía nadie mejoró de color en este ensayo.</p></c:when>
      <c:otherwise>
        <table class="table"><thead><tr><th>Miembro</th><th>Figura</th><th>Cambio</th></tr></thead><tbody>
          <c:forEach var="h" items="${avances}" end="7"><tr><td><c:out value="${h.miembro}"/></td><td><c:out value="${h.figura}"/></td><td><sdp:cambio antes="${h.colorAnterior}" despues="${h.colorNuevo}"/></td></tr></c:forEach>
        </tbody></table>
        <c:if test="${fn:length(avances) > 8}"><a class="link" href="${ctx}/ensayos/reporte?id=${ensayo.idEnsayo}">Ver los ${fn:length(avances)} avances en el reporte →</a></c:if>
      </c:otherwise>
    </c:choose>
  </div>
</div>

<div class="row top">
  <div class="card grow"><h2>Asistencia</h2><p>${ensayo.confirmados} confirmaron · ${ensayo.sinRespuesta} sin respuesta</p>
    <div class="att-grid">
      <c:forEach var="a" items="${filas}"><c:if test="${not empty a.estadoReal or a.confirmacion == 'SI'}">
        <div><c:out value="${a.miembro}"/><sdp:code valor="${empty a.estadoReal ? '' : a.estadoReal}"/></div></c:if></c:forEach>
    </div>
  </div>
  <form class="card w-540" method="post" action="${ctx}/ensayos/detalle"><input type="hidden" name="_csrf" value="${sessionScope.csrf}"><h2>Notas del ensayo</h2>
    <input type="hidden" name="id" value="${ensayo.idEnsayo}">
    <textarea class="input" name="notas" rows="5" placeholder="Qué se trabajó, qué faltó, pendientes para el próximo…"><c:out value="${ensayo.notas}"/></textarea>
    <div class="inline"><button class="btn btn-secondary btn-sm" name="action" value="notas">Guardar notas</button>
      <c:if test="${not empty ensayo.notasAutor}"><span class="muted xs">Última edición: <c:out value="${ensayo.notasAutor}"/></span></c:if></div>
  </form>
</div>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
