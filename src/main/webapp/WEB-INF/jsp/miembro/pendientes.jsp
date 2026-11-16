<c:set var="titulo" value="Pendientes"/><c:set var="menu" value="Pendientes"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<section class="hero"><div class="titles"><span class="kicker">Mi espacio</span><h1>Pendientes</h1>
  <p>Todo lo que te falta en un solo lugar: autoevaluaciones, feedback de talleres, cuota y confirmar asistencia</p></div><span class="star"></span></section>
<div class="card">
  <c:set var="maxPend" value="${0}"/><%@ include file="/WEB-INF/jsp/includes/pendientes-lista.jspf" %>
</div>
<div class="note info">Lo urgente sale en rojo (cuota atrasada), lo que vence pronto en dorado y lo demás en azul. Cuando lo completes, desaparece de aquí.</div>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
