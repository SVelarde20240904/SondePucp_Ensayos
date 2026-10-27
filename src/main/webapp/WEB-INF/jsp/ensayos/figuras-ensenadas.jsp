<c:set var="titulo" value="Figuras enseñadas"/><c:set var="menu" value="Figuras enseñadas"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<form method="post" action="${ctx}/ensayos/figuras" class="stack" style="gap:24px">
<input type="hidden" name="idEnsayo" value="${ensayo.idEnsayo}">
<section class="hero"><div class="titles"><span class="kicker">Ensayos</span><h1>¿Qué figuras se enseñaron?</h1>
  <p>${ensayo.fechaLarga} · al guardar, cada asistente puede hacer su autoevaluación del ensayo</p></div>
  <div class="actions"><button class="btn btn-primary" type="submit">Guardar y habilitar autoevaluación</button></div><span class="star"></span></section>

<c:if test="${not empty sugeridas}"><div class="card"><h2>Sugeridas por el cronograma</h2><p>Las que más asistentes necesitan (márcalas abajo, en su nivel)</p>
  <div class="inline"><c:forEach var="f" items="${sugeridas}"><span class="tag info role"><c:out value="${f.figura}"/> · ${f.NAsistentes}</span></c:forEach></div></div></c:if>

<div class="grid-4" style="align-items:start">
  <c:forEach var="lv" items="${['AZUL','VERDE','AMARILLO','ROJO']}">
    <div class="card" style="gap:8px"><div class="inline"><sdp:chip color="${lv}"/><h2 style="font-family:var(--display);font-size:18px">Nivel ${lv}</h2></div>
      <p style="margin-top:-6px">${abiertos.contains(lv) ? 'Nivel abierto hoy' : 'No se abrió hoy'}</p>
      <c:forEach var="f" items="${porNivel[lv]}">
        <label class="check-item"><input type="checkbox" name="figura" value="${f.idFigura}" ${marcadas.contains(f.idFigura) ? 'checked' : ''}><c:out value="${f.nombre}"/></label>
      </c:forEach>
      <c:if test="${empty porNivel[lv]}"><p class="empty">Sin figuras de este color.</p></c:if>
    </div>
  </c:forEach>
</div>
<c:if test="${not empty porNivel['SIN']}"><div class="card"><h2>Figuras nuevas (sin color aún)</h2><div class="pills">
  <c:forEach var="f" items="${porNivel['SIN']}"><label class="check-item"><input type="checkbox" name="figura" value="${f.idFigura}" ${marcadas.contains(f.idFigura) ? 'checked' : ''}><c:out value="${f.nombre}"/></label></c:forEach></div></div></c:if>

<div class="card"><h2>¿Quién dictó?</h2>
  <select class="input" name="dicto" style="max-width:320px"><option value="">—</option>
    <c:forEach var="m" items="${equipo}"><option value="${m.idMiembro}" ${m.idMiembro == u.idMiembro ? 'selected' : ''}><c:out value="${m.nombre}"/></option></c:forEach></select></div>
</form>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
