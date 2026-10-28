<!DOCTYPE html>
<html lang="es">
<head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover"><meta name="theme-color" content="#1C19A9">
<title>Reporte de ensayo ${ensayo.fechaCorta} · Son de PUCP</title><link rel="stylesheet" href="${ctx}/css/sdp.css"><script src="${ctx}/js/sdp.js" defer></script></head>
<body class="report-body">
<div class="toolbar"><a class="btn btn-secondary" href="${ctx}/ensayos/detalle?id=${ensayo.idEnsayo}">← Volver al ensayo</a><span class="spacer"></span>
  <button class="btn btn-primary" onclick="window.print()">🖨 Imprimir / Guardar PDF</button></div>

<section class="page">
  <header><img src="${ctx}/img/logo-sdp-dorado.png" alt=""><div><h1>REPORTE DE ENSAYO</h1><p>${ensayo.fechaLarga} · ${ensayo.tipoTexto} · página 1 de 2</p></div></header>
  <main>
    <div class="rsec"><h2>DATOS</h2><div class="rdata">
      <div><small>Lugar</small><c:out value="${ensayo.lugar}"/></div><div><small>Horario</small>${ensayo.horario}</div>
      <div><small>Dictaron</small><c:out value="${empty ensayo.dictantesTexto ? '—' : ensayo.dictantesTexto}"/></div>
      <div><small>Confirmaron</small>${ensayo.confirmados} · ${ensayo.sinRespuesta} sin respuesta</div></div></div>
    <div class="rsec"><h2>RESUMEN</h2><div class="rbox">
      <div><b>${ensayo.asistieron}/${ensayo.esperados}</b><span>asistieron</span></div><div><b>${ensayo.puntuales}</b><span>puntuales</span></div>
      <div><b>${ensayo.tardanzas}</b><span>tardanzas</span></div><div><b>${ensayo.faltas}</b><span>faltas</span></div>
      <div><b>${ensayo.figurasEnsenadas}</b><span>figuras</span></div><div><b>+${ensayo.avances}</b><span>avances</span></div></div></div>
    <div class="rsec"><h2>FIGURAS ENSEÑADAS POR NIVEL</h2>
      <table class="table"><thead><tr><th>Nivel</th><th>Figuras</th><th>Dictó</th></tr></thead><tbody>
        <c:forEach var="entry" items="${ensenadasPorNivel}"><tr><td><sdp:chip color="${entry.key}"/></td>
          <td><c:forEach var="f" items="${entry.value}" varStatus="s"><c:out value="${f.figura}"/>${s.last ? '' : ', '}</c:forEach></td><td><c:out value="${entry.value[0].dicto}"/></td></tr></c:forEach>
        <c:if test="${empty ensenadasPorNivel}"><tr><td colspan="3">No se registraron figuras enseñadas.</td></tr></c:if>
      </tbody></table></div>
    <div class="rsec"><h2>AVANCES DEL ENSAYO (${fn:length(avances)})</h2>
      <table class="table"><thead><tr><th>Miembro</th><th>Figura</th><th>Antes</th><th>Después</th></tr></thead><tbody>
        <c:forEach var="h" items="${avances}" end="21"><tr><td><c:out value="${h.miembro}"/></td><td><c:out value="${h.figura}"/></td>
          <td>${empty h.colorAnterior ? '—' : fn:substring(h.colorAnterior,0,1)}${empty h.colorAnterior ? '' : fn:toLowerCase(fn:substring(h.colorAnterior,1,10))}</td>
          <td>${fn:substring(h.colorNuevo,0,1)}${fn:toLowerCase(fn:substring(h.colorNuevo,1,10))}</td></tr></c:forEach>
        <c:if test="${empty avances}"><tr><td colspan="4">Aún no hay avances registrados.</td></tr></c:if>
      </tbody></table></div>
  </main>
  <footer><strong>TEAM ENSAYOS - SDP</strong><span>Generado el ${hoy} · sistema de ensayos SDP</span></footer>
</section>

<section class="page">
  <header><img src="${ctx}/img/logo-sdp-dorado.png" alt=""><div><h1>REPORTE DE ENSAYO</h1><p>${ensayo.fechaLarga} · ${ensayo.tipoTexto} · página 2 de 2</p></div></header>
  <main>
    <div class="rsec"><h2>ASISTENCIA · ESTE ENSAYO Y LOS ANTERIORES</h2>
      <p class="xs muted" style="margin-bottom:6px"><sdp:code valor="AP"/> Asistencia puntual · <sdp:code valor="AN"/> No planeada · <sdp:code valor="TJ"/> Tardanza justificada · <sdp:code valor="TI"/> Tardanza injustificada · <sdp:code valor="FJ"/> Falta justificada · <sdp:code valor="FI"/> Falta injustificada · <sdp:code valor="SR"/> Sin respuesta</p>
      <table class="table"><thead><tr><th>Miembro</th><th>Rol</th><th>Nivel</th><c:forEach var="f" items="${fechasHist}"><th class="center">${f}</th></c:forEach><th class="num">% asist.</th></tr></thead><tbody>
        <c:forEach var="r" items="${historialAsis}"><tr><td><c:out value="${r.nombre}"/></td><td>${r.rol}</td><td>${empty r.nivel ? '—' : (r.nivel == 'Ensayos' ? r.nivel : fn:substring(r.nivel,0,1).concat(fn:toLowerCase(fn:substring(r.nivel,1,10))))}</td>
          <c:forEach var="cod" items="${r.codigos}" varStatus="s"><td class="center" ${s.last ? 'style="background:var(--accent-soft)"' : ''}><sdp:code valor="${cod}"/></td></c:forEach>
          <td class="num bold">${r.porcentaje}%</td></tr></c:forEach>
      </tbody></table></div>
    <div class="rsec"><h2>RESUMEN DE ASISTENCIA</h2>
      <c:set var="conFi" value="0"/><c:set var="conSr" value="0"/><c:set var="alertas" value=""/>
      <c:forEach var="r" items="${historialAsis}">
        <c:if test="${r.a >= 2}"><c:set var="conFi" value="${conFi + 1}"/><c:set var="alertas" value="${alertas}${empty alertas ? '' : ', '}${r.nombre} (${r.a} FI)"/></c:if>
        <c:if test="${r.b >= 2}"><c:set var="conSr" value="${conSr + 1}"/><c:set var="alertas" value="${alertas}${empty alertas ? '' : ', '}${r.nombre} (sin respuesta ${r.b} veces)"/></c:if>
      </c:forEach>
      <div class="rbox" style="grid-template-columns:repeat(4,1fr)"><div><b>${ensayo.porcentajeAsistencia}%</b><span>asistencia de hoy</span></div>
        <div><b>${ensayo.confirmados}</b><span>confirmaron</span></div><div><b>${conFi}</b><span>con 2+ faltas injustificadas</span></div><div><b>${conSr}</b><span>sin respuesta reiterada</span></div></div>
      <c:if test="${not empty alertas}"><p class="note bad" style="margin-top:8px">Alertas: <c:out value="${alertas}"/>.</p></c:if></div>
    <div class="rsec"><h2>OBSERVACIONES</h2><p class="small"><c:out value="${empty ensayo.notas ? 'Sin observaciones.' : ensayo.notas}"/></p></div>
  </main>
  <footer><strong>TEAM ENSAYOS - SDP</strong><span>Generado el ${hoy} · sistema de ensayos SDP</span></footer>
</section>
</body></html>
