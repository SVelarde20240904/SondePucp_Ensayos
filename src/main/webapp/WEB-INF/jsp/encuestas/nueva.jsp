<c:set var="titulo" value="Nueva encuesta"/><c:set var="menu" value="Encuestas"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<section class="hero"><div class="titles"><span class="kicker">Encuestas</span><h1>Nueva encuesta</h1>
  <p>Le aparece en Pendientes a todos los que les toca votar.</p></div>
  <div class="actions"><a class="btn btn-secondary" href="${ctx}/encuestas">← Encuestas</a></div>
  <span class="star"></span></section>
<form class="card" method="post" action="${ctx}/encuestas/nueva"><input type="hidden" name="_csrf" value="${sessionScope.csrf}">
  <div class="field"><label for="pregunta">Pregunta</label><input class="input" id="pregunta" name="pregunta" maxlength="200" required placeholder="Ej: ¿Les gustaría que hagamos una visita a Bemberé Callao?"></div>
  <div class="field"><label for="descripcion">Detalles <span class="muted">(opcional)</span></label><textarea class="input" id="descripcion" name="descripcion" rows="3" maxlength="1000" placeholder="Fecha tentativa, costo, contexto…"></textarea></div>
  <div class="field"><label>Tipo</label>
    <div class="pills" id="tipos">
      <label class="pill sm"><input type="radio" name="tipo" value="SI_NO" checked><span>Sí / No</span></label>
      <label class="pill sm"><input type="radio" name="tipo" value="UNA"><span>Una opción</span></label>
      <label class="pill sm"><input type="radio" name="tipo" value="VARIAS"><span>Varias opciones</span></label>
      <label class="pill sm"><input type="radio" name="tipo" value="FECHAS"><span>¿Qué fechas puedes?</span></label>
    </div></div>
  <div class="field" id="campo-opciones" hidden><label for="opciones">Opciones <span class="muted">(una por línea)</span></label>
    <textarea class="input" id="opciones" name="opciones" rows="5" placeholder="Sábado 6 de diciembre&#10;Sábado 13 de diciembre&#10;Sábado 20 de diciembre"></textarea></div>
  <div class="form-row">
    <div class="field"><label for="team">¿Para quiénes?</label><select class="input" id="team" name="team"><option value="">Todo el grupo</option>
      <c:forEach var="t" items="${teams}"><option value="${t.idTeam}">Solo team <c:out value="${t.nombre}"/></option></c:forEach></select></div>
    <div class="field"><label for="cierreFecha">Cierra el</label><div class="inline"><input class="input" type="date" id="cierreFecha" name="cierreFecha" value="${cierreFecha}" required>
      <input class="input" type="time" name="cierreHora" value="23:59" aria-label="Hora de cierre"></div></div>
  </div>
  <label class="check-item"><input type="checkbox" name="anonima"> Anónima (no se muestra quién votó qué)</label>
  <label class="check-item"><input type="checkbox" name="verAntes"> Mostrar resultados antes de votar</label>
  <div><button class="btn btn-primary">Publicar encuesta</button></div>
</form>
<script>
(function () {
  var campo = document.getElementById('campo-opciones'), ta = document.getElementById('opciones');
  function ver() { var t = document.querySelector('input[name=tipo]:checked').value; campo.hidden = t === 'SI_NO'; ta.required = t !== 'SI_NO'; }
  document.querySelectorAll('input[name=tipo]').forEach(function (r) { r.addEventListener('change', ver); }); ver();
})();
</script>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
