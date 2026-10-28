<c:set var="titulo" value="Autoevaluación mensual"/><c:set var="menu" value="Autoevaluación"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<form method="post" action="${ctx}/autoevaluacion" class="stack" style="gap:24px"><input type="hidden" name="_csrf" value="${sessionScope.csrf}">
<section class="hero"><div class="titles"><span class="kicker">Mi espacio</span><h1>Autoevaluación mensual · ${mes}</h1>
  <p>Marca qué tan bien sabes cada figura. Esta evaluación reemplaza tus colores anteriores (puede subir o bajar).</p></div>
  <div class="actions"><div style="min-width:220px"><p class="small bold" style="text-align:right;margin-bottom:6px">${respondidas} de ${total} con color</p>
    <div class="bar"><i style="width:${total == 0 ? 0 : respondidas * 100 / total}%;background:var(--accent)"></i></div></div></div>
  <span class="star"></span></section>

<c:if test="${not abierta}"><div class="note warn">La autoevaluación mensual está cerrada (se abre del 1 al 7 de cada mes). Puedes revisarla, pero si guardas igual se registra.</div></c:if>

<div class="legend"><span><sdp:chip color="ROJO"/> No la sé</span><span><sdp:chip color="AMARILLO"/> La sé a medias</span><span><sdp:chip color="VERDE"/> La sé bien</span><span><sdp:chip color="AZUL"/> La domino</span></div>

<div class="grid-2">
  <c:forEach var="col" items="${[columnaA, columnaB]}">
  <div class="card" style="gap:0">
    <c:forEach var="e" items="${col}">
      <div class="eval-row"><div class="name"><c:out value="${e.figura}"/><c:if test="${conVideo.contains(e.idFigura)}"><a class="ver-fig" href="${ctx}/biblioteca/figura?id=${e.idFigura}" target="_blank" title="Ver el video de la figura">▶ ver</a></c:if><c:if test="${empty e.colorFigura}"><small>Nueva · aún sin nivel</small></c:if></div>
        <c:forEach var="c" items="${['ROJO','AMARILLO','VERDE','AZUL']}">
          <label class="opt ${fn:toLowerCase(c)}"><input type="radio" name="color_${e.idFigura}" value="${c}" ${e.color == c ? 'checked' : ''}><span>${fn:substring(c,0,1)}${fn:toLowerCase(fn:substring(c,1,10))}</span></label>
        </c:forEach>
      </div>
    </c:forEach>
  </div>
  </c:forEach>
</div>
<div class="inline" style="justify-content:flex-end"><a class="btn btn-secondary" href="${ctx}/inicio">Cancelar</a><button class="btn btn-primary" type="submit">Enviar autoevaluación</button></div>
</form>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
