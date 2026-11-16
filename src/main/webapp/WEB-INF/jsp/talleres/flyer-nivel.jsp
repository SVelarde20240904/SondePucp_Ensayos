<!DOCTYPE html>
<html lang="es"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>${t.titulo} · imagen por nivel</title>
<link href="https://fonts.googleapis.com/css2?family=League+Spartan:wght@700;800&family=Poppins:ital,wght@0,500;0,600;1,600;1,700&display=swap" rel="stylesheet">
<style>
*{box-sizing:border-box;margin:0} body{font-family:Poppins,system-ui,sans-serif;background:#EEEEF4;color:#23254D;padding:16px}
.barra{display:flex;flex-wrap:wrap;gap:8px;justify-content:center;align-items:center;margin:0 auto 12px;max-width:900px}
.barra a,.barra button{font:600 13px Poppins;padding:8px 14px;border-radius:99px;border:1px solid #2B2E63;background:#fff;color:#2B2E63;text-decoration:none;cursor:pointer}
.barra a.on,.barra button.on{background:#2B2E63;color:#fff} .barra .prim{background:#CFA048;border-color:#CFA048;color:#1C1E45}
.barra form{display:flex;gap:6px;align-items:center} .barra input{font:500 13px Poppins;padding:8px 12px;border-radius:99px;border:1px solid #CFD0E0;min-width:240px}
.lienzo{display:flex;justify-content:center} canvas{width:min(100%,420px);height:auto;border-radius:12px;box-shadow:0 10px 30px rgba(28,30,69,.25);background:#1C1E45}
.ayuda{text-align:center;font-size:12px;color:#62647F;margin-top:10px}
</style></head><body>
<div class="barra">
  <a href="${ctx}/talleres/flyer?id=${t.idTaller}">← Cronograma</a>
  <c:forEach var="o" items="${t.niveles}"><a class="${o == nivel ? 'on' : ''}" href="?id=${t.idTaller}&nivel=${o}${param.v == 'dictado' ? '&v=dictado' : ''}">${o == 'PRINCIPIANTE' ? 'Principiante' : (o == 'BASICO' ? 'Básico' : (o == 'INTRO_RUEDA' ? 'Intro' : 'Rueda básica'))}</a></c:forEach>
</div>
<div class="barra">
  <c:if test="${t.hayDictado}"><a class="${param.v == 'dictado' ? '' : 'on'}" href="?id=${t.idTaller}&nivel=${nivel}">Planeado</a><a class="${param.v == 'dictado' ? 'on' : ''}" href="?id=${t.idTaller}&nivel=${nivel}&v=dictado">Lo dictado</a></c:if>
  <button type="button" id="tono" data-suave="1">Colores: suaves</button>
  <button type="button" class="prim" id="bajar">⬇ Descargar imagen</button>
</div>
<div class="barra">
  <form method="post" action="${ctx}/talleres/flyer-lema"><input type="hidden" name="_csrf" value="${sessionScope.csrf}"><input type="hidden" name="id" value="${t.idTaller}"><input type="hidden" name="nivel" value="${nivel}">
    <input name="lema" id="lema" maxlength="80" aria-label="Frase del pie"><button>Guardar frase</button></form>
</div>
<div class="lienzo"><canvas id="c" width="1080" height="1920" aria-label="Imagen del cronograma del nivel"></canvas></div>
<p class="ayuda">Formato historia (1080 × 1920). La frase del pie se guarda para los próximos talleres.</p>
<script type="application/json" id="datos">${datos}</script>
<script>
(function () {
  'use strict';
  var D = JSON.parse(document.getElementById('datos').textContent);
  var cv = document.getElementById('c'), g = cv.getContext('2d'), W = 1080, H = 1920;
  var suave = true;
  var lema = document.getElementById('lema'); lema.value = D.lema || '';
  var logo = new Image(); logo.src = '${ctx}/img/logo-sdp-blanco.png';
  var P = {
    vivo:  { fondo: ['#0B0B26', '#2F2BB8', '#3A34D0'], pastilla: '#FFD86B', texto: '#2A2A2A', tabla: '#FFFFFF', linea: '#3A3A3A',
             FILTRO: '#F4CCCC', MUSICA: '#C9DAF8', FINAL: '#FFF2CC', barra: '#1C3FA8' },
    suave: { fondo: ['#15172F', '#272A5E', '#33377A'], pastilla: '#E7C77E', texto: '#2B2D45', tabla: '#FBFAF7', linea: '#C9C8D6',
             FILTRO: '#F1DADA', MUSICA: '#DCE3F2', FINAL: '#F6EDD7', barra: '#2B2E63' }
  };

  function lineas(txt, ancho) {
    var palabras = String(txt || '').split(/\s+/), l = [], act = '';
    palabras.forEach(function (p) {
      var prueba = act ? act + ' ' + p : p;
      if (g.measureText(prueba).width > ancho && act) { l.push(act); act = p; } else act = prueba;
    });
    if (act) l.push(act);
    return l.length ? l : [''];
  }

  function rr(x, y, w, h, r) { g.beginPath(); g.moveTo(x + r, y); g.arcTo(x + w, y, x + w, y + h, r); g.arcTo(x + w, y + h, x, y + h, r); g.arcTo(x, y + h, x, y, r); g.arcTo(x, y, x + w, y, r); g.closePath(); }

  function cuba(x, y, w, h) {
    g.save(); rr(x, y, w, h, 14); g.clip();
    for (var i = 0; i < 5; i++) { g.fillStyle = i % 2 ? '#FFFFFF' : '#1F3FA8'; g.fillRect(x, y + i * h / 5, w, h / 5 + 1); }
    g.fillStyle = '#D52B1E'; g.beginPath(); g.moveTo(x, y); g.lineTo(x + w * 0.45, y + h / 2); g.lineTo(x, y + h); g.closePath(); g.fill();
    g.fillStyle = '#fff'; estrella(x + w * 0.16, y + h / 2, h * 0.14); g.restore();
  }
  function peru(x, y, w, h) {
    g.save(); rr(x, y, w, h, 14); g.clip();
    g.fillStyle = '#D91023'; g.fillRect(x, y, w / 3 + 1, h); g.fillRect(x + 2 * w / 3, y, w / 3, h);
    g.fillStyle = '#fff'; g.fillRect(x + w / 3, y, w / 3, h); g.restore();
  }
  function estrella(cx, cy, r) {
    g.beginPath();
    for (var i = 0; i < 10; i++) { var a = -Math.PI / 2 + i * Math.PI / 5, rad = i % 2 ? r * 0.42 : r; g.lineTo(cx + rad * Math.cos(a), cy + rad * Math.sin(a)); }
    g.closePath(); g.fill();
  }

  function dibujar() {
    var c = suave ? P.suave : P.vivo;
    var fondo = g.createLinearGradient(0, 0, W, H * 0.4);
    fondo.addColorStop(0, c.fondo[0]); fondo.addColorStop(0.55, c.fondo[1]); fondo.addColorStop(1, c.fondo[2]);
    g.fillStyle = fondo; g.fillRect(0, 0, W, H);

    g.textAlign = 'center'; g.textBaseline = 'alphabetic'; g.fillStyle = '#FFFFFF';
    var tam = 104; g.font = '800 ' + tam + 'px "League Spartan"';
    while (g.measureText(D.nivel).width > W - 140 && tam > 60) { tam -= 4; g.font = '800 ' + tam + 'px "League Spartan"'; }
    g.fillText(D.nivel, W / 2, 190);
    g.font = 'italic 700 72px Poppins'; g.fillText(D.talleristas || '', W / 2, 300);
    var y = 340;
    if (D.apoyos) { g.font = '500 32px Poppins'; g.globalAlpha = 0.85; g.fillText('Apoyo: ' + D.apoyos, W / 2, 360); g.globalAlpha = 1; y = 390; }

    // encabezados
    var X0 = 110, X1 = W - 110, cH = 190, cT = 170, cM = X1 - X0 - cH - cT - 40;
    var cols = [[X0, cH, 'Hora'], [X0 + cH + 20, cM, D.columna], [X0 + cH + cM + 40, cT, 'Tiempo']];
    cols.forEach(function (k) {
      g.fillStyle = c.pastilla; rr(k[0], y + 10, k[1], 84, 26); g.fill(); g.fillStyle = '#1A1A1A';
      var f = 46; g.font = '600 ' + f + 'px Poppins';
      while (g.measureText(k[2]).width > k[1] - 24 && f > 24) { f -= 2; g.font = '600 ' + f + 'px Poppins'; }
      g.fillText(k[2], k[0] + k[1] / 2, y + 52 + f * 0.35);
    });
    y += 130;

    // filas (si no entra, se achica la letra)
    var fuente = 30, filas, alto;
    var tope = 1560;
    for (; fuente >= 20; fuente -= 2) {
      g.font = '500 ' + fuente + 'px Poppins'; filas = []; alto = 0;
      D.secciones.forEach(function (s) {
        var its = s.items.length ? s.items : [['', null]];
        var hs = its.map(function (it) { return Math.max(fuente * 2.4, lineas(it[0], cM - 30).length * fuente * 1.3 + fuente * 1.1); });
        var total = hs.reduce(function (a, b) { return a + b; }, 0);
        filas.push({ s: s, its: its, hs: hs, total: total }); alto += total;
      });
      if (y + alto <= tope) break;
    }
    var tx = X0 - 10, tw = X1 - X0 + 20;
    g.save(); rr(tx, y, tw, alto, 22); g.clip();
    g.fillStyle = c.tabla; g.fillRect(tx, y, tw, alto);
    var yy = y, xh = X0 + cH + 10, xt = X0 + cH + cM + 30;
    filas.forEach(function (f) {
      var bg = c[f.s.estilo];
      if (bg) { g.fillStyle = bg; g.fillRect(tx, yy, tw, f.total); }
      // hora (una por sección)
      g.fillStyle = c.texto; g.font = '500 ' + fuente + 'px Poppins';
      g.fillText(f.s.hora || '', tx + (xh - tx) / 2, yy + f.total / 2 + fuente * 0.35);
      var yi = yy;
      f.its.forEach(function (it, i) {
        var h = f.hs[i], ls = lineas(it[0], cM - 30);
        var y0 = yi + h / 2 - (ls.length - 1) * fuente * 0.65 + fuente * 0.35;
        g.fillStyle = c.texto; g.font = (f.s.estilo === 'MUSICA' ? '600 ' : '500 ') + fuente + 'px Poppins';
        ls.forEach(function (l, k) { g.fillText(l, (xh + xt) / 2, y0 + k * fuente * 1.3); });
        g.fillText(it[1] || '', xt + (tx + tw - xt) / 2, yi + h / 2 + fuente * 0.35);
        if (i > 0) { g.strokeStyle = c.linea; g.lineWidth = 2; g.beginPath(); g.moveTo(xh, yi); g.lineTo(f.s.estilo === 'FINAL' ? xt : tx + tw, yi); g.stroke(); }
        yi += h;
      });
      g.strokeStyle = c.linea; g.lineWidth = 3; g.beginPath(); g.moveTo(tx, yy + f.total); g.lineTo(tx + tw, yy + f.total); g.stroke();
      yy += f.total;
    });
    g.strokeStyle = c.linea; g.lineWidth = 3;
    g.beginPath(); g.moveTo(xh, y); g.lineTo(xh, y + alto); g.moveTo(xt, y); g.lineTo(xt, y + alto); g.stroke();
    g.restore();

    // pie: banderas, logo y frase
    var py = 1650;
    cuba(110, py, 150, 110); peru(W - 260, py, 150, 110);
    if (logo.complete && logo.naturalWidth) { var lw = 210, lh = lw * logo.naturalHeight / logo.naturalWidth; g.drawImage(logo, W / 2 - lw / 2, py + 55 - lh / 2, lw, lh); }
    g.fillStyle = c.barra; g.fillRect(0, H - 100, W, 100);
    g.fillStyle = '#FFFFFF'; g.font = 'italic 600 46px Poppins'; g.fillText(lema.value || '', W / 2, H - 34);
  }

  document.getElementById('tono').addEventListener('click', function (e) {
    suave = !suave; e.target.textContent = 'Colores: ' + (suave ? 'suaves' : 'como el flyer original'); dibujar();
  });
  lema.addEventListener('input', dibujar);
  document.getElementById('bajar').addEventListener('click', function () {
    cv.toBlob(function (b) {
      var a = document.createElement('a'); a.href = URL.createObjectURL(b);
      a.download = ('${t.titulo} ' + D.nivel).replace(/[^\wáéíóúñ ]+/gi, '').trim().replace(/\s+/g, '-') + '.png';
      document.body.appendChild(a); a.click(); a.remove();
    }, 'image/png');
  });
  var fuentes = ['800 104px "League Spartan"', 'italic 700 72px Poppins', 'italic 600 46px Poppins', '500 30px Poppins', '600 46px Poppins'];
  Promise.all(fuentes.map(function (f) { return document.fonts ? document.fonts.load(f) : Promise.resolve(); }))
    .catch(function () {}).then(function () { dibujar(); });
  if (document.fonts) { document.fonts.ready.then(dibujar); document.fonts.addEventListener('loadingdone', dibujar); }
  logo.onload = dibujar;
})();
</script>
</body></html>
