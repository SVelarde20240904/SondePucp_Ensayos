<!DOCTYPE html>
<html lang="es"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>${t.titulo} · flyer</title>
<link rel="preconnect" href="https://fonts.googleapis.com"><link href="https://fonts.googleapis.com/css2?family=League+Spartan:wght@600;800&family=Poppins:wght@400;500;600;700&display=swap" rel="stylesheet">
<style>
*{box-sizing:border-box;margin:0} body{font-family:Poppins,system-ui,sans-serif;background:#EEEEF4;color:#23254D;padding:16px}
.barra{display:flex;gap:8px;justify-content:center;margin-bottom:12px} .barra a,.barra button{font:600 13px Poppins;padding:8px 14px;border-radius:99px;border:1px solid #2B2E63;background:#fff;color:#2B2E63;text-decoration:none;cursor:pointer}
.flyer{max-width:1400px;margin:0 auto;background:linear-gradient(160deg,#2F3268,#23264F);color:#F3F1EA;border-radius:18px;padding:28px;overflow:hidden}
header{display:flex;align-items:center;gap:18px;margin-bottom:18px} header img{height:74px}
header small{display:block;font:600 14px Poppins;letter-spacing:.28em;color:#D4AE66} header h1{font:800 44px/1 'League Spartan'}
.niveles{display:grid;grid-template-columns:repeat(4,1fr);gap:14px}
.nivel{background:rgba(255,255,255,.055);border:1px solid rgba(255,255,255,.07);border-radius:14px;padding:14px}
.nivel h2{font:800 22px 'League Spartan';color:#D9B56E;margin-bottom:2px} .nivel .quien{font-size:12px;opacity:.85;margin-bottom:10px}
.sec{display:grid;grid-template-columns:78px 1fr;gap:6px;padding:6px 0;border-top:1px solid rgba(255,255,255,.09);font-size:12.5px;line-height:1.4}
.sec .h{font-weight:600;color:#E8E2D2} .its{display:flex;flex-direction:column;gap:4px} .it{display:grid;grid-template-columns:1fr 34px;gap:6px} .it .d{text-align:right;opacity:.8}
.sec.musica{color:#C9A869;font-weight:500;background:rgba(207,160,72,.07);margin:0 -6px;padding:6px}
.sec.filtro{background:rgba(214,120,110,.08);margin:0 -6px;padding:6px} .sec.final{background:rgba(255,255,255,.04);margin:0 -6px;padding:6px}
.descargar{display:inline-block;margin-top:12px;font:600 12px Poppins;color:#D9B56E;text-decoration:none} @media print{.descargar{display:none}}
.pie{display:flex;flex-wrap:wrap;gap:6px 22px;margin-top:16px;font-size:14px} .pie b{color:#D9B56E}
@media (max-width:900px){.niveles{grid-template-columns:1fr 1fr}} @media (max-width:560px){.niveles{grid-template-columns:1fr} header h1{font-size:32px}}
@media print{body{background:#fff;padding:0}.barra{display:none}.flyer{border-radius:0}}
</style></head><body>
<div class="barra"><a href="${ctx}/talleres/detalle?id=${t.idTaller}">← Volver</a><button onclick="window.print()">Imprimir / guardar PDF</button></div>
<div class="flyer">
  <header><img src="${ctx}/img/logo-sdp-dorado.png" alt="Son de PUCP"><div><small>${fn:toUpperCase(t.fechaTexto)} · ${t.horario}</small><h1>${t.titulo}</h1></div></header>
  <div class="niveles">
    <c:forEach var="n" items="${t.niveles}">
      <div class="nivel"><h2>${n == 'PRINCIPIANTE' ? 'Principiante' : (n == 'BASICO' ? 'Básico' : (n == 'INTRO_RUEDA' ? 'Intro a la rueda' : 'Rueda básica'))}</h2>
        <div class="quien"><c:forEach var="p" items="${t.personasDe('TALLERISTA', n)}" varStatus="s">${s.first ? '' : ' · '}<c:out value="${p.nombre}"/></c:forEach>
          <c:if test="${not empty t.personasDe('APOYO', n)}"><br>Apoyos: <c:forEach var="p" items="${t.personasDe('APOYO', n)}" varStatus="s">${s.first ? '' : ' · '}<c:out value="${p.nombre}"/></c:forEach></c:if></div>
        <c:forEach var="sec" items="${t.plan[n]}">
          <div class="sec ${fn:toLowerCase(sec.estilo)}"><span class="h"><c:out value="${sec.hora}"/></span>
            <div class="its"><c:forEach var="it" items="${sec.items}"><div class="it"><span><c:out value="${it[0]}"/></span><span class="d"><c:out value="${it[1]}"/></span></div></c:forEach>
              <c:if test="${empty sec.items}"><div class="it"><span style="opacity:.5">—</span></div></c:if></div></div>
        </c:forEach>
        <c:if test="${empty t.plan[n]}"><p style="opacity:.7;font-size:13px">Sin cronograma.</p></c:if>
        <a class="descargar" href="${ctx}/talleres/flyer-nivel?id=${t.idTaller}&nivel=${n}">⬇ Imagen para publicar</a>
      </div>
    </c:forEach>
  </div>
  <div class="pie">
    <c:forEach var="p" items="${t.personasDe('MAESTRO', null)}"><span>Maestro(a) de taller: <b><c:out value="${p.nombre}"/></b></span></c:forEach>
    <c:choose><c:when test="${not empty t.musicaTexto}"><span>Música social: <b><c:out value="${t.musicaTexto}"/></b></span></c:when>
      <c:otherwise><c:forEach var="p" items="${t.personasDe('MUSICA', null)}"><span>Música social: <b><c:out value="${p.nombre}"/></b></span></c:forEach></c:otherwise></c:choose>
    <c:if test="${not empty t.lugar}"><span>📍 <c:out value="${t.lugar}"/></span></c:if>
  </div>
</div>
</body></html>
