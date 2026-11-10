<c:set var="titulo" value="Historial de dictado"/><c:set var="menu" value="Dictado de talleres"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<section class="hero"><div class="titles"><span class="kicker">Team Talleres</span><h1>Historial de dictado</h1>
  <p>Quién hizo qué en cada taller (el dictado final; si no se registró, el planeado)</p></div><span class="star"></span></section>
<c:set var="grupo" value="talleres"/><c:set var="sub" value="dictado"/><%@ include file="/WEB-INF/jsp/includes/subtabs.jspf" %>
<div class="card">
  <div class="inline"><span class="muted small">Ciclo:</span>
    <c:forEach var="c" items="${ciclos}"><a class="pill-link ${c == ciclo ? 'on' : ''}" href="${ctx}/talleres/dictado?ciclo=${c}">${c}</a></c:forEach></div>
  <div class="tabla-scroll">
  <table class="table dictado no-cards"><thead><tr><th>Miembro</th>
    <c:forEach var="t" items="${talleres}"><th><a href="${ctx}/talleres/detalle?id=${t.idTaller}">${t.fechaCorta}</a></th></c:forEach></tr></thead><tbody>
    <c:forEach var="e" items="${matriz}"><tr><td><c:out value="${e.key}"/></td>
      <c:forEach var="t" items="${talleres}"><c:set var="cod" value="${e.value[t.idTaller]}"/>
        <td class="cod ${empty cod ? 'vacio' : ''} ${fn:contains(cod, '✗') ? 'falto' : ''} ${fn:contains(cod, '*') ? 'tarde' : ''}">${empty cod ? '·' : cod}</td></c:forEach></tr></c:forEach>
    <c:if test="${empty matriz}"><tr><td class="empty">Sin talleres en este ciclo.</td></tr></c:if>
  </tbody></table></div>
  <p class="muted xs">TP/TB/TI/TR = tallerista de Principiante, Básico, Intro a la rueda, Rueda básica · AP = apoyo · MT = maestro(a) de taller · MU = música · * llegó tarde · ✗ no se presentó</p>
</div>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
