<c:set var="titulo" value="Activar cuentas en lote"/><c:set var="menu" value="Miembros"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<section class="hero"><div class="titles"><span class="kicker">Administración › Miembros</span><h1>Activar cuentas en lote</h1>
  <p>Escribe los correos PUCP, marca a quiénes activar y se generan todas las claves temporales de una</p></div>
  <div class="actions"><a class="btn btn-secondary" href="${ctx}/admin/miembros">← Miembros</a></div><span class="star"></span></section>

<%-- Resultado de la última activación: se muestra UNA sola vez --%>
<c:if test="${not empty sessionScope.loteErrores}">
  <div class="note warn"><b>No se activaron:</b><c:forEach var="e" items="${sessionScope.loteErrores}"><br>· <c:out value="${e}"/></c:forEach></div>
</c:if>
<c:if test="${not empty sessionScope.loteListos}">
  <div class="card" style="border-color:var(--verde)"><h2>✓ Cuentas activadas: copia y envía cada mensaje</h2>
    <p>Las claves solo se ven ahora. Cada persona tendrá que cambiarla al entrar.</p>
    <c:forEach var="r" items="${sessionScope.loteListos}" varStatus="st">
      <div class="lote-msg">
        <div class="inline" style="justify-content:space-between"><b><c:out value="${r[0]}"/></b><span class="muted small"><c:out value="${r[1]}"/> · <b style="user-select:all"><c:out value="${r[2]}"/></b></span></div>
        <textarea class="input" id="msg${st.index}" rows="3" readonly>Hola <c:out value="${r[0]}"/>! Ya tienes cuenta en el Sistema de Ensayos de SDP 💃🏻
Entra a <c:out value="${enlace}"/> con tu correo <c:out value="${r[1]}"/> y esta clave temporal: <c:out value="${r[2]}"/>
Al entrar te va a pedir que pongas una tuya.</textarea>
        <button type="button" class="btn btn-ghost btn-sm" data-copiar="msg${st.index}">📋 Copiar mensaje</button>
      </div>
    </c:forEach>
    <h2>Correos para el Grupo de Google de los videos</h2>
    <p>Pégalos en <i>Agregar miembros</i> del grupo para que puedan ver la biblioteca de figuras.</p>
    <textarea class="input" id="correosGrupo" rows="2" readonly><c:forEach var="r" items="${sessionScope.loteListos}" varStatus="st"><c:out value="${r[1]}"/>${st.last ? '' : ', '}</c:forEach></textarea>
    <div><button type="button" class="btn btn-ghost btn-sm" data-copiar="correosGrupo">📋 Copiar correos</button></div>
  </div>
</c:if>
<c:remove var="loteListos" scope="session"/><c:remove var="loteErrores" scope="session"/>

<form class="card" method="post" action="${ctx}/admin/miembros/activar"><input type="hidden" name="_csrf" value="${sessionScope.csrf}">
  <h2>Pendientes (${fn:length(pendientes)})</h2>
  <p>Miembros activos que todavía no entraron nunca. Si alguien ya tenía clave temporal, se le genera una nueva.</p>
  <div class="inline"><button type="button" class="btn btn-secondary btn-sm" id="marcarConCorreo">Marcar todos los que tienen correo</button><span class="spacer"></span>
    <button class="btn btn-primary">Activar marcados</button></div>
  <table class="table cards-compact no-cards-label"><thead><tr><th>Miembro</th><th>Estado</th><th>Correo PUCP</th><th>Activar</th></tr></thead><tbody>
  <c:forEach var="m" items="${pendientes}">
    <tr><td><c:out value="${m.nombre}"/><small>${m.rolBaileTexto}${m.equipoEnsayos ? ' · Equipo de ensayos' : ''}</small></td>
      <td class="no-label"><sdp:tag texto="${m.cuentaActivada ? 'Clave temporal sin estrenar' : 'Sin cuenta'}" tono="${m.cuentaActivada ? 'warn' : 'neutral'}"/></td>
      <td class="no-label"><input class="input lote-correo" type="email" name="correo_${m.idMiembro}" value="<c:out value='${m.correo}'/>" placeholder="a2024xxxx@pucp.edu.pe" data-id="${m.idMiembro}"></td>
      <td class="no-label"><label class="check-item" style="padding:8px 10px"><input type="checkbox" name="activar" value="${m.idMiembro}" id="act${m.idMiembro}"> Activar</label></td></tr>
  </c:forEach>
  <c:if test="${empty pendientes}"><tr><td colspan="4" class="empty">¡Todos los miembros activos ya tienen su cuenta!</td></tr></c:if>
  </tbody></table>
  <div class="inline" style="justify-content:flex-end"><button class="btn btn-primary">Activar marcados</button></div>
</form>
<script>
  // al escribir un correo se marca la casilla de esa persona
  document.querySelectorAll('.lote-correo').forEach(function (i) {
    i.addEventListener('input', function () { var c = document.getElementById('act' + i.dataset.id); if (c) c.checked = i.value.trim() !== ''; });
  });
  var b = document.getElementById('marcarConCorreo');
  if (b) b.addEventListener('click', function () {
    document.querySelectorAll('.lote-correo').forEach(function (i) { var c = document.getElementById('act' + i.dataset.id); if (c && i.value.trim() !== '') c.checked = true; });
  });
  document.querySelectorAll('[data-copiar]').forEach(function (btn) {
    btn.addEventListener('click', function () {
      var t = document.getElementById(btn.dataset.copiar); t.select();
      (navigator.clipboard ? navigator.clipboard.writeText(t.value) : Promise.reject()).then(
        function () { btn.textContent = '✓ Copiado'; }, function () { document.execCommand('copy'); btn.textContent = '✓ Copiado'; });
    });
  });
</script>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
