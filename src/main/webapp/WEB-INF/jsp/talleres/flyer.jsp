<!DOCTYPE html>
<html lang="es"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>${t.titulo} · flyer</title>
<link rel="preconnect" href="https://fonts.googleapis.com"><link href="https://fonts.googleapis.com/css2?family=League+Spartan:wght@600;800&family=Poppins:wght@400;500;600;700&display=swap" rel="stylesheet">
<style>
*{box-sizing:border-box;margin:0} body{font-family:Poppins,system-ui,sans-serif;background:#E8E8FA;color:#14145C;padding:16px}
.barra{display:flex;gap:8px;justify-content:center;margin-bottom:12px} .barra a,.barra button{font:600 13px Poppins;padding:8px 14px;border-radius:99px;border:1px solid #1C19A9;background:#fff;color:#1C19A9;text-decoration:none;cursor:pointer}
.flyer{max-width:1400px;margin:0 auto;background:linear-gradient(135deg,#1C19A9,#14145C);color:#fff;border-radius:18px;padding:28px;overflow:hidden}
header{display:flex;align-items:center;gap:18px;margin-bottom:18px} header img{height:74px}
header small{display:block;font:600 14px Poppins;letter-spacing:.28em;color:#E09B20} header h1{font:800 44px/1 'League Spartan'}
.niveles{display:grid;grid-template-columns:repeat(4,1fr);gap:14px}
.nivel{background:rgba(255,255,255,.08);border-radius:14px;padding:14px}
.nivel h2{font:800 22px 'League Spartan';color:#E09B20;margin-bottom:2px} .nivel .quien{font-size:12px;opacity:.85;margin-bottom:10px}
.b{display:grid;grid-template-columns:78px 1fr 34px;gap:6px;padding:6px 0;border-top:1px solid rgba(255,255,255,.12);font-size:12.5px}
.b .h{font-weight:600;color:#FCEFD6} .b .d{text-align:right;opacity:.8} .b.musica{color:#E09B20;font-weight:600}
.pie{display:flex;flex-wrap:wrap;gap:6px 22px;margin-top:16px;font-size:14px} .pie b{color:#E09B20}
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
        <c:forEach var="b" items="${t.bloques[n]}"><div class="b ${fn:containsIgnoreCase(b[1], 'música') or fn:containsIgnoreCase(b[1], 'musica') ? 'musica' : ''}"><span class="h"><c:out value="${b[0]}"/></span><span><c:out value="${b[1]}"/></span><span class="d"><c:out value="${b[2]}"/></span></div></c:forEach>
        <c:if test="${empty t.bloques[n]}"><p style="opacity:.7;font-size:13px">Sin cronograma.</p></c:if>
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
