<c:set var="titulo" value="Autoevaluación del ensayo"/><c:set var="menu" value="Autoevaluación"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<form method="post" action="${ctx}/autoevaluacion" class="stack" style="gap:24px">
<input type="hidden" name="idEnsayo" value="${ensayo.idEnsayo}">
<section class="hero"><div class="titles"><span class="kicker">Mi espacio</span><h1>¿Cómo te fue en el ensayo?</h1>
  <p>${ensayo.fechaLarga} · se trabajaron ${fn:length(figuras)} figuras de tu nivel</p></div>
  <div class="actions"><a class="btn btn-secondary" href="${ctx}/autoevaluacion?tipo=mensual">Ir a la mensual</a></div><span class="star"></span></section>

<div class="note warn">Regla “solo sube”: aquí solo puedes mejorar tu color. Si sientes que retrocediste en una figura, corrígelo en la autoevaluación mensual.</div>
<div class="legend"><span><sdp:chip color="ROJO"/> No la sé</span><span><sdp:chip color="AMARILLO"/> La sé a medias</span><span><sdp:chip color="VERDE"/> La sé bien</span><span><sdp:chip color="AZUL"/> La domino</span></div>

<div class="card" style="gap:0"><h2>Figuras trabajadas</h2><p style="margin:-10px 0 6px">Toma menos de un minuto</p>
  <c:if test="${empty figuras}"><p class="empty">No hay figuras para autoevaluar en este ensayo (o no tienes asistencia registrada).</p></c:if>
  <c:forEach var="f" items="${figuras}">
    <c:set var="act" value="${actual[f.idFigura]}"/>
    <c:set var="minimo" value="${act == 'AZUL' ? 4 : (act == 'VERDE' ? 3 : (act == 'AMARILLO' ? 2 : (act == 'ROJO' ? 1 : 0)))}"/>
    <div class="eval-row"><div class="name"><c:out value="${f.figura}"/><small>Actual: <sdp:chip color="${act}"/></small></div>
      <c:forEach var="c" items="${['ROJO','AMARILLO','VERDE','AZUL']}" varStatus="s">
        <label class="opt ${fn:toLowerCase(c)}"><input type="radio" name="color_${f.idFigura}" value="${c}" ${act == c ? 'checked' : ''} ${s.count < minimo ? 'disabled' : ''}><span>${fn:substring(c,0,1)}${fn:toLowerCase(fn:substring(c,1,10))}</span></label>
      </c:forEach>
    </div>
  </c:forEach>
</div>
<div class="inline" style="justify-content:flex-end"><button class="btn btn-primary" type="submit" ${empty figuras ? 'disabled' : ''}>Enviar</button></div>
</form>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
