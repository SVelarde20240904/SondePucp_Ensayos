<c:set var="titulo" value="${m.nombre}"/><c:set var="menu" value="Fichas de miembros"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<section class="hero"><div class="titles"><span class="kicker">Ensayos › Fichas de miembros</span><h1><c:out value="${m.nombre}"/><c:if test="${m.tieneApodo}"> <span class="muted" style="font-size:.6em">“<c:out value="${m.apodo}"/>”</span></c:if></h1>
  <p>${m.rolBaileTexto} · ${m.frecuenciaTexto}${m.activo ? '' : ' · INACTIVO'} · miembro desde ${m.miembroDesde}<c:if test="${not empty m.fechaNacimiento}"> · 🎂 ${m.cumpleTexto}</c:if></p>
  <c:if test="${not empty m.teams}"><div class="chips-teams"><c:forEach var="t" items="${m.teams}"><span class="tag info">Team <c:out value="${t}"/></span></c:forEach></div></c:if></div>
  <div class="actions"><a class="btn btn-secondary" href="${ctx}/ensayos/miembros">← Todas las fichas</a>
    <c:if test="${perm.contains('GESTIONAR_MIEMBROS')}"><a class="btn btn-primary" href="${ctx}/admin/miembros/editar?id=${m.idMiembro}">Editar datos</a></c:if></div>
  <span class="star"></span></section>

<div class="grid-4">
  <div class="card stat"><span>Nivel</span><div><sdp:chip color="${m.nivel}"/></div></div>
  <div class="card stat"><b class="c-primary">${resumen[0]}/${resumen[1]}</b><span>figuras que sabe (Verde o Azul)</span></div>
  <div class="card stat"><b class="c-verde">${porcentaje}%</b><span>asistencia en el ciclo</span></div>
  <div class="card stat"><b class="c-accent">+${resumen[2]}</b><span>mejoras este mes</span></div>
</div>

<div class="row top">
  <c:choose><c:when test="${editar}"><form class="card grow" method="post" action="${ctx}/ensayos/miembro" id="form-figs"><input type="hidden" name="_csrf" value="${sessionScope.csrf}"><input type="hidden" name="idMiembro" value="${m.idMiembro}"></c:when>
    <c:otherwise><div class="card grow"></c:otherwise></c:choose>
    <h2>Figuras por nivel</h2>
    <c:choose>
      <c:when test="${editar}"><p>Cambia el color de las figuras que haga falta y guarda. Cada cambio queda en el histórico como <b>corrección</b>, con tu nombre.</p></c:when>
      <c:otherwise><p>El nivel de la figura es el color del grupo (Azul = base → Rojo = avanzada). Al lado, el color de ${m.nombre}.</p></c:otherwise>
    </c:choose>
    <div class="inline">
      <c:if test="${not editar}">
        <a class="pill-link ${soloFaltan ? '' : 'on'}" href="${ctx}/ensayos/miembro?id=${m.idMiembro}">Todas</a>
        <a class="pill-link ${soloFaltan ? 'on' : ''}" href="${ctx}/ensayos/miembro?id=${m.idMiembro}&faltan=1">Solo las que le faltan</a>
      </c:if>
      <c:if test="${perm.contains('EDITAR_FIGURAS_MIEMBRO')}">
        <c:choose>
          <c:when test="${editar}"><a class="btn btn-secondary btn-sm" href="${ctx}/ensayos/miembro?id=${m.idMiembro}">Cancelar</a></c:when>
          <c:otherwise><a class="btn btn-primary btn-sm" href="${ctx}/ensayos/miembro?id=${m.idMiembro}&editar=1">✏️ Editar figuras</a></c:otherwise>
        </c:choose>
      </c:if>
    </div>
    <c:forEach var="e" items="${porNivel}">
      <div class="nivel-bloque">
        <div class="inline"><c:choose><c:when test="${e.key == 'SIN'}"><span class="chip sin">Figuras nuevas · sin nivel</span></c:when><c:otherwise><sdp:chip color="${e.key}"/></c:otherwise></c:choose>
          <span class="muted small">sabe ${sabe[e.key][0]} de ${sabe[e.key][1]}</span></div>
        <c:choose>
          <c:when test="${editar}">
            <div>
              <c:forEach var="ev" items="${e.value}">
                <label class="fig-edit"><span><c:out value="${ev.figura}"/></span>
                  <select class="input" name="color_${ev.idFigura}" data-antes="${ev.color}" aria-label="Color de ${ev.figura}">
                    <c:if test="${empty ev.color}"><option value="" selected>Sin evaluar</option></c:if>
                    <c:forEach var="col" items="${['ROJO','AMARILLO','VERDE','AZUL']}"><option value="${col}" ${ev.color == col ? 'selected' : ''}>${col == 'ROJO' ? '🔴 Rojo' : (col == 'AMARILLO' ? '🟡 Amarillo' : (col == 'VERDE' ? '🟢 Verde' : '🔵 Azul'))}</option></c:forEach>
                  </select></label>
              </c:forEach>
            </div>
          </c:when>
          <c:otherwise>
        <div class="fig-grid">
          <c:forEach var="ev" items="${e.value}">
            <div class="fig-ficha ${ev.leFalta or empty ev.color ? 'falta' : ''}"><span><a class="fig-link" href="${ctx}/biblioteca/figura?id=${ev.idFigura}"><c:out value="${ev.figura}"/></a></span>
              <c:choose><c:when test="${empty ev.color}"><span class="chip sin">Sin evaluar</span></c:when><c:otherwise><sdp:chip color="${ev.color}"/></c:otherwise></c:choose></div>
          </c:forEach>
        </div>
          </c:otherwise>
        </c:choose>
      </div>
    </c:forEach>
    <c:if test="${empty porNivel}"><p class="empty">¡Se sabe todas! No le falta ninguna figura.</p></c:if>
  <c:choose><c:when test="${editar}">
    <div class="field"><label>Motivo (opcional)</label><input class="input" name="motivo" maxlength="150" placeholder="Ej: la vimos en el ensayo del sábado y ya la domina"></div>
    <div class="inline" style="justify-content:flex-end"><span class="muted small" id="n-cambios">Sin cambios</span><button class="btn btn-primary">Guardar cambios</button></div>
  </form>
  <script>
    (function () {
      var f = document.getElementById('form-figs');
      function contar() {
        var n = 0;
        f.querySelectorAll('select[data-antes]').forEach(function (s) {
          var cambio = s.value !== '' && s.value !== s.getAttribute('data-antes');
          s.closest('.fig-edit').classList.toggle('cambiada', cambio);
          if (cambio) n++;
        });
        document.getElementById('n-cambios').textContent = n === 0 ? 'Sin cambios' : (n === 1 ? '1 figura cambiada' : n + ' figuras cambiadas');
      }
      f.addEventListener('change', contar);
      f.addEventListener('submit', function (e) { if (!f.querySelector('.cambiada')) { e.preventDefault(); alert('No cambiaste ningún color.'); } });
    })();
  </script>
  </c:when><c:otherwise></div></c:otherwise></c:choose>

  <div class="stack w-340">
    <div class="card"><h2>Últimos ensayos</h2>
      <div class="att-grid">
        <c:forEach var="a" items="${asistencias}"><div><span>${a.fechaCorta}</span><sdp:code valor="${empty a.estadoReal ? (a.confirmacion == 'SIN_RESPUESTA' ? 'SR' : '') : a.estadoReal}"/></div></c:forEach>
      </div>
      <c:if test="${empty asistencias}"><p class="empty">Todavía no tiene ensayos registrados.</p></c:if>
    </div>
    <div class="card"><h2>Cambios recientes</h2>
      <c:forEach var="h" items="${historial}">
        <div class="inline" style="justify-content:space-between;flex-wrap:nowrap;border-bottom:1px solid var(--border);padding:6px 0">
          <span class="small"><b><c:out value="${h.figura}"/></b><br><span class="muted xs">${h.fechaTexto} · ${h.fuenteTexto}</span></span>
          <sdp:cambio antes="${h.colorAnterior}" despues="${h.colorNuevo}"/></div>
      </c:forEach>
      <c:if test="${empty historial}"><p class="empty">Sin cambios registrados.</p></c:if>
    </div>
  </div>
</div>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
