<c:set var="titulo" value="Cronograma sugerido"/><c:set var="menu" value="Cronograma sugerido"/>
<%@ include file="/WEB-INF/jsp/includes/top.jspf" %>
<section class="hero"><div class="titles"><span class="kicker">Ensayos</span><h1>Ensayo del ${ensayo.diaLargo} ${ensayo.fechaCorta}</h1>
  <p>Se recalcula solo cada vez que alguien confirma o cancela su asistencia</p></div>
  <div class="actions"><a class="btn btn-secondary" href="${ctx}/ensayos/detalle?id=${ensayo.idEnsayo}">Ver ensayo</a>
    <c:if test="${perm.contains('REGISTRAR_FIGURAS_ENSENADAS')}"><a class="btn btn-primary" href="${ctx}/ensayos/figuras?id=${ensayo.idEnsayo}">Registrar figuras enseñadas</a></c:if></div>
  <span class="star"></span></section>
<c:set var="tab" value="cronograma"/><%@ include file="/WEB-INF/jsp/includes/ensayo-tabs.jspf" %>

<div class="grid-4">
  <div class="card stat"><b>${ensayo.confirmados}</b><span>confirmados (Sí voy)</span></div>
  <div class="card stat"><b>${fn:length(equipoAsiste)}</b><span>del equipo de ensayos</span></div>
  <div class="card stat"><b class="c-verde">${fn:length(abiertos)} de 4</b><span>niveles se abren</span></div>
  <div class="card stat"><b class="c-amarillo">${fn:length(sinNivel)}</b><span>sin autoevaluación</span></div>
</div>

<div class="row top">
  <div class="card grow"><h2>Resumen por nivel</h2><p>Se abre con 1 leader + 1 follower, o con un solo rol + alguien del equipo de ensayos del rol contrario</p>
    <table class="table"><thead><tr><th>Nivel</th><th>Leaders</th><th>Followers</th><th>¿Se abre?</th><th>Pareja</th></tr></thead><tbody>
      <c:forEach var="n" items="${niveles}"><tr><td><sdp:chip color="${n.nivel}"/></td>
        <td>${n.leaders == 0 ? '—' : n.leaders.toString().concat(' · ').concat(n.nombresLeaders)}</td>
        <td>${n.followers == 0 ? '—' : n.followers.toString().concat(' · ').concat(n.nombresFollowers)}</td>
        <td><sdp:tag texto="${n.seAbre ? 'Sí' : 'No'}" tono="${n.seAbre ? 'ok' : 'bad'}"/></td><td>${n.pareja}</td></tr></c:forEach>
    </tbody></table>
  </div>
  <div class="card w-300"><h2>Equipo de ensayos que asiste</h2>
    <c:forEach var="e" items="${equipoAsiste}"><c:set var="p" value="${fn:split(e, '|')}"/>
      <div class="person"><b><c:out value="${p[0]}"/></b><span class="tag info role">${fn:length(p) > 1 ? (p[1] == 'LEADER' ? 'Leader' : 'Follower') : '—'}</span></div></c:forEach>
    <c:if test="${empty equipoAsiste}"><p class="empty">Nadie del equipo confirmó todavía.</p></c:if>
    <c:forEach var="n" items="${niveles}"><c:if test="${not n.seAbre and (n.leaders > 0 or n.followers > 0)}">
      <p class="note bad">⚠ Nivel ${n.nivel}: ${fn:toLowerCase(n.pareja)}.</p></c:if></c:forEach>
    <c:if test="${not empty sinNivel}"><p class="muted small">Sin nivel (aún sin autoevaluación): <c:forEach var="s" items="${sinNivel}" varStatus="st"><c:out value="${s}"/>${st.last ? '' : ', '}</c:forEach></p></c:if>
  </div>
</div>

<div class="card"><h2>Figuras sugeridas</h2><p>Solo niveles abiertos · ordenadas de la que más asistentes necesitan a la que menos</p>
  <table class="table"><thead><tr><th>#</th><th>Figura</th><th>Nivel</th><th>Les falta a</th><th class="num">N° asistentes</th></tr></thead><tbody>
    <c:forEach var="f" items="${sugeridas}" varStatus="s"><tr><td>${s.count}</td><td><c:out value="${f.figura}"/><c:if test="${f.ensenada}"> <span class="tag ok">enseñada</span></c:if></td>
      <td><sdp:chip color="${f.nivel}"/></td><td><c:out value="${f.lesFaltaA}"/></td><td class="num">${f.NAsistentes}</td></tr></c:forEach>
    <c:if test="${empty sugeridas}"><tr><td colspan="5" class="empty">No hay figuras sugeridas: no se abre ningún nivel o a nadie le falta nada. Revisa la asistencia o dedica el ensayo a repaso libre.</td></tr></c:if>
  </tbody></table>
</div>
<%@ include file="/WEB-INF/jsp/includes/bottom.jspf" %>
