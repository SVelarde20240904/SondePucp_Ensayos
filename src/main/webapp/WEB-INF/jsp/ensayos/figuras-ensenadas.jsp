<c:set var="titulo" value="Figuras enseñadas"/><c:set var="menu" value="Figuras enseñadas"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<form method="post" action="${ctx}/ensayos/figuras" class="stack" style="gap:24px"><input type="hidden" name="_csrf" value="${sessionScope.csrf}">
<input type="hidden" name="idEnsayo" value="${ensayo.idEnsayo}">
<section class="hero"><div class="titles"><span class="kicker">Ensayos</span><h1>¿Qué figuras se enseñaron?</h1>
  <p>${ensayo.fechaLarga} · al guardar, cada asistente puede hacer su autoevaluación del ensayo</p></div>
  <div class="actions"><button class="btn btn-primary" type="submit">Guardar y habilitar autoevaluación</button></div><span class="star"></span></section>
<c:set var="tab" value="figuras"/><%@ include file="/WEB-INF/jsp/includes/ensayo-tabs.jspf" %>

<div class="card" id="quien"><h2>¿Cómo se trabajó y quién enseñó?</h2>
  <div class="pills">
    <label class="pill"><input type="radio" name="porNiveles" value="1" ${ensayo.porNiveles ? 'checked' : ''}><span>Por niveles</span></label>
    <label class="pill"><input type="radio" name="porNiveles" value="0" ${ensayo.porNiveles ? '' : 'checked'}><span>Todos juntos, sin niveles</span></label>
  </div>
  <p class="muted small">Sin niveles: cada asistente autoevalúa todas las figuras que se vieron. Por niveles: las de su nivel (y las de otro nivel si en Asistencia marcaste que también estuvo ahí).</p>
  <div class="dicto-filas" data-modo="niveles">
    <c:forEach var="lv" items="${['AZUL','VERDE','AMARILLO','ROJO']}">
      <div class="dicto-fila"><span class="inline" style="gap:6px"><sdp:chip color="${lv}"/></span>
        <div class="pills">
          <c:forEach var="m" items="${equipo}"><label class="pill sm"><input type="checkbox" name="dicto_${lv}" value="${m.idMiembro}" ${dicto[lv].contains(m.idMiembro) ? 'checked' : ''}><span><c:out value="${m.nombre}"/></span></label></c:forEach>
        </div></div>
    </c:forEach>
  </div>
  <div class="dicto-filas" data-modo="todos">
    <div class="dicto-fila"><b class="small">Enseñaron</b>
      <div class="pills">
        <c:forEach var="m" items="${equipo}"><label class="pill sm"><input type="checkbox" name="dicto_TODOS" value="${m.idMiembro}" ${dicto['TODOS'].contains(m.idMiembro) ? 'checked' : ''}><span><c:out value="${m.nombre}"/></span></label></c:forEach>
      </div></div>
  </div>
  <p class="muted xs">Marca a todos los que dictaron: pueden ser varios en un nivel (ej. Antony + Sergio en Rueda) y uno en varios niveles.</p>
</div>
<script>
  (function () {
    var radios = document.querySelectorAll('input[name=porNiveles]');
    function modo() {
      var sin = document.querySelector('input[name=porNiveles]:checked').value === '0';
      document.querySelector('.dicto-filas[data-modo=niveles]').hidden = sin;
      document.querySelector('.dicto-filas[data-modo=todos]').hidden = !sin;
    }
    radios.forEach(function (r) { r.addEventListener('change', modo); }); modo();
  })();
</script>

<c:if test="${not empty sugeridas}"><div class="card"><h2>Sugeridas por el cronograma</h2><p>Las que más asistentes necesitan (márcalas abajo, en su nivel)</p>
  <div class="inline"><c:forEach var="f" items="${sugeridas}"><span class="tag info role"><c:out value="${f.figura}"/> · ${f.NAsistentes}</span></c:forEach></div></div></c:if>

<div class="grid-4" style="align-items:start">
  <c:forEach var="lv" items="${['AZUL','VERDE','AMARILLO','ROJO']}">
    <div class="card" style="gap:8px"><div class="inline"><sdp:chip color="${lv}"/><h2 style="font-family:var(--display);font-size:18px">Nivel ${lv}</h2></div>
      <p style="margin-top:-6px">${not ensayo.porNiveles ? 'Todos juntos' : (abiertos.contains(lv) ? 'Nivel abierto hoy' : 'No se abrió hoy')}</p>
      <c:forEach var="f" items="${porNivel[lv]}">
        <label class="check-item"><input type="checkbox" name="figura" value="${f.idFigura}" ${marcadas.contains(f.idFigura) ? 'checked' : ''}><c:out value="${f.nombre}"/></label>
      </c:forEach>
      <c:if test="${empty porNivel[lv]}"><p class="empty">Sin figuras de este color.</p></c:if>
    </div>
  </c:forEach>
</div>
<c:if test="${not empty porNivel['SIN']}"><div class="card"><h2>Figuras nuevas (sin color aún)</h2><div class="pills">
  <c:forEach var="f" items="${porNivel['SIN']}"><label class="check-item"><input type="checkbox" name="figura" value="${f.idFigura}" ${marcadas.contains(f.idFigura) ? 'checked' : ''}><c:out value="${f.nombre}"/></label></c:forEach></div></div></c:if>

</form>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
