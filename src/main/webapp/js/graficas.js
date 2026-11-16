/* Gráficas simples en SVG (sin librerías): línea, columnas (1 o 2 series) y barras horizontales.
   Specs: líneas 2px, barras <= 24px con punta redondeada de 4px, rejilla de 1px recesiva,
   tooltip al pasar el mouse (o tocar), leyenda cuando hay 2 series, textos en tinta (nunca del color de la serie). */
(function (w) {
  'use strict';
  var NS = 'http://www.w3.org/2000/svg';
  var C1 = '#3C68C4', C2 = '#C08A2A';   // validadas: CVD ΔE 27.8, normal ΔE 30.8
  var tip;

  function svg(tag, a, kids) {
    var e = document.createElementNS(NS, tag);
    for (var k in a) if (a[k] != null) e.setAttribute(k, a[k]);
    (kids || []).forEach(function (c) { e.appendChild(c); });
    return e;
  }
  function txt(x, y, s, a) { var t = svg('text', Object.assign({ x: x, y: y }, a || {})); t.textContent = s; return t; }
  function soles(v) { v = Math.round(v * 100) / 100; return 'S/ ' + (v % 1 === 0 ? v.toLocaleString('es-PE') : v.toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 })); }
  function limpio(max) {   // tope "redondo" para el eje
    if (max <= 0) return 1;
    var p = Math.pow(10, Math.floor(Math.log10(max))), n = max / p;
    return (n <= 1 ? 1 : n <= 2 ? 2 : n <= 2.5 ? 2.5 : n <= 5 ? 5 : 10) * p;
  }
  function eje(v) { return v >= 10000 ? Math.round(v / 1000) + 'K' : Math.round(v).toLocaleString('es-PE'); }

  function mostrar(e, html) {
    if (!tip) { tip = document.createElement('div'); tip.className = 'graf-tip'; document.body.appendChild(tip); }
    tip.innerHTML = html; tip.style.display = 'block';
    var x = (e.touches ? e.touches[0].clientX : e.clientX), y = (e.touches ? e.touches[0].clientY : e.clientY);
    var r = tip.getBoundingClientRect();
    tip.style.left = Math.min(window.innerWidth - r.width - 8, x + 12) + 'px';
    tip.style.top = Math.max(8, y - r.height - 12) + 'px';
  }
  function ocultar() { if (tip) tip.style.display = 'none'; }
  function hover(el, html) {
    el.addEventListener('mousemove', function (e) { mostrar(e, html); });
    el.addEventListener('mouseleave', ocultar);
    el.addEventListener('touchstart', function (e) { mostrar(e, html); }, { passive: true });
  }

  function marco(cont, alto) {
    var W = Math.max(300, cont.clientWidth || 600), H = alto || 240;
    var s = svg('svg', { viewBox: '0 0 ' + W + ' ' + H, width: '100%', height: H, role: 'img', 'aria-label': cont.getAttribute('aria-label') || '' });
    cont.innerHTML = ''; cont.appendChild(s);
    return { s: s, W: W, H: H };
  }

  function rejilla(f, m, max, fmt) {
    for (var i = 0; i <= 4; i++) {
      var v = max * i / 4, y = m.t + (f.H - m.t - m.b) * (1 - i / 4);
      f.s.appendChild(svg('line', { x1: m.l, x2: f.W - m.r, y1: y, y2: y, 'class': 'graf-grid' }));
      f.s.appendChild(txt(m.l - 6, y + 4, (fmt || eje)(v), { 'class': 'graf-eje', 'text-anchor': 'end' }));
    }
  }

  // columnas con punta redondeada (4px) apoyadas en la base
  function columna(x, y, w, h, color) {
    if (h <= 0) return svg('rect', { x: x, y: y, width: w, height: 0 });
    var r = Math.min(4, h, w / 2);
    var d = 'M' + x + ',' + (y + h) + 'V' + (y + r) + 'Q' + x + ',' + y + ' ' + (x + r) + ',' + y + 'H' + (x + w - r) + 'Q' + (x + w) + ',' + y + ' ' + (x + w) + ',' + (y + r) + 'V' + (y + h) + 'Z';
    return svg('path', { d: d, fill: color });
  }

  /** series: [{nombre, valores[], color}] · etiquetas: [] */
  w.grafColumnas = function (cont, etiquetas, series, opt) {
    opt = opt || {}; etiquetas = sinAnio(etiquetas);
    var f = marco(cont, opt.alto), m = { l: 52, r: 10, t: 14, b: 26 };
    var max = limpio(Math.max.apply(null, series.flatMap(function (s) { return s.valores; }).concat([0])));
    rejilla(f, m, max, opt.fmtEje);
    var banda = (f.W - m.l - m.r) / etiquetas.length, n = series.length;
    var bw = Math.min(24, (banda - 10) / n - 2), alto = f.H - m.t - m.b;
    etiquetas.forEach(function (et, i) {
      var x0 = m.l + banda * i + (banda - (bw * n + 2 * (n - 1))) / 2;
      series.forEach(function (s, k) {
        var v = s.valores[i] || 0, h = alto * v / max;
        f.s.appendChild(columna(x0 + k * (bw + 2), m.t + alto - h, bw, h, s.color || (k ? C2 : C1)));
      });
      // zona de hover más grande que la barra
      var hit = svg('rect', { x: m.l + banda * i, y: m.t, width: banda, height: alto, fill: 'transparent' });
      hover(hit, '<b>' + et + '</b>' + series.map(function (s, k) {
        return '<div><i style="background:' + (s.color || (k ? C2 : C1)) + '"></i>' + s.nombre + ': ' + (opt.fmt || soles)(s.valores[i] || 0) + '</div>';
      }).join(''));
      f.s.appendChild(hit);
      if (etiquetas.length <= 14 || i % 2 === 0) f.s.appendChild(txt(m.l + banda * i + banda / 2, f.H - 8, et, { 'class': 'graf-eje', 'text-anchor': 'middle' }));
    });
    f.s.appendChild(svg('line', { x1: m.l, x2: f.W - m.r, y1: m.t + alto, y2: m.t + alto, 'class': 'graf-base' }));
  };

  /** Línea con área suave y punto final etiquetado. */
  w.grafLinea = function (cont, etiquetas, valores, opt) {
    opt = opt || {}; etiquetas = sinAnio(etiquetas);
    var f = marco(cont, opt.alto), m = { l: 52, r: 16, t: 18, b: 26 };
    var minV = Math.min.apply(null, valores.concat([0])), max = limpio(Math.max.apply(null, valores.concat([1])));
    rejilla(f, m, max, opt.fmtEje);
    var alto = f.H - m.t - m.b, paso = (f.W - m.l - m.r) / Math.max(1, etiquetas.length - 1);
    function X(i) { return m.l + paso * i; } function Y(v) { return m.t + alto * (1 - Math.max(v, minV) / max); }
    var d = valores.map(function (v, i) { return (i ? 'L' : 'M') + X(i) + ',' + Y(v); }).join('');
    f.s.appendChild(svg('path', { d: d + 'L' + X(valores.length - 1) + ',' + (m.t + alto) + 'L' + X(0) + ',' + (m.t + alto) + 'Z', fill: C1, opacity: 0.1 }));
    f.s.appendChild(svg('path', { d: d, fill: 'none', stroke: C1, 'stroke-width': 2, 'stroke-linejoin': 'round', 'stroke-linecap': 'round' }));
    var ult = valores.length - 1;
    f.s.appendChild(svg('circle', { cx: X(ult), cy: Y(valores[ult]), r: 5, fill: C1, stroke: '#fff', 'stroke-width': 2 }));
    f.s.appendChild(txt(Math.min(X(ult), f.W - m.r - 4), Y(valores[ult]) - 10, soles(valores[ult]), { 'class': 'graf-valor', 'text-anchor': 'end' }));
    // cruz al pasar el mouse
    var guia = svg('line', { y1: m.t, y2: m.t + alto, 'class': 'graf-guia', visibility: 'hidden' });
    var punto = svg('circle', { r: 5, fill: C1, stroke: '#fff', 'stroke-width': 2, visibility: 'hidden' });
    f.s.appendChild(guia); f.s.appendChild(punto);
    var capa = svg('rect', { x: m.l - paso / 2, y: m.t, width: f.W - m.l - m.r + paso, height: alto, fill: 'transparent' });
    function mover(e) {
      var r = f.s.getBoundingClientRect(), cx = ((e.touches ? e.touches[0].clientX : e.clientX) - r.left) * f.W / r.width;
      var i = Math.max(0, Math.min(ult, Math.round((cx - m.l) / paso)));
      guia.setAttribute('x1', X(i)); guia.setAttribute('x2', X(i)); guia.setAttribute('visibility', 'visible');
      punto.setAttribute('cx', X(i)); punto.setAttribute('cy', Y(valores[i])); punto.setAttribute('visibility', 'visible');
      mostrar(e, '<b>' + etiquetas[i] + '</b><div>' + (opt.nombre || 'Valor') + ': ' + soles(valores[i]) + '</div>');
    }
    capa.addEventListener('mousemove', mover); capa.addEventListener('touchstart', mover, { passive: true });
    capa.addEventListener('mouseleave', function () { ocultar(); guia.setAttribute('visibility', 'hidden'); punto.setAttribute('visibility', 'hidden'); });
    f.s.appendChild(capa);
    etiquetas.forEach(function (et, i) { if (etiquetas.length <= 14 || i % 2 === 0) f.s.appendChild(txt(X(i), f.H - 8, et, { 'class': 'graf-eje', 'text-anchor': 'middle' })); });
  };

  /** Barras horizontales (de mayor a menor), con el valor en la punta. items: [{k, v}] */
  // ancho de la etiqueta más larga (aprox. 6.6px por letra a 12px)
  function f0(items) { return 16 + Math.max.apply(null, items.map(function (i) { return String(i.k).length * 6.6; })); }
  // "Ene 26", "Feb 26"… -> "Ene", "Feb" si todos son del mismo año
  function sinAnio(et) {
    var a = et.map(function (e) { var m = / (\d\d)$/.exec(e); return m ? m[1] : null; });
    return a.every(function (x) { return x && x === a[0]; }) ? et.map(function (e) { return e.slice(0, -3); }) : et;
  }
  w.grafBarras = function (cont, items, opt) {
    opt = opt || {};
    if (!items.length) { cont.innerHTML = '<p class="empty">Sin datos todavía.</p>'; return; }
    var fila = 34, f = marco(cont, items.length * fila + 8), etq = Math.min(170, Math.max(90, f0(items))), m = { l: etq, r: 80 };
    var max = Math.max.apply(null, items.map(function (i) { return i.v; }));
    items.forEach(function (it, i) {
      var y = 4 + i * fila, ancho = Math.max(2, (f.W - m.l - m.r) * it.v / max), h = 18;
      f.s.appendChild(txt(etq - 10, y + 14 + (fila - 22) / 2, it.k, { 'class': 'graf-etq', 'text-anchor': 'end' }));
      var x = m.l, yy = y + (fila - h) / 2, r = Math.min(4, ancho / 2);
      f.s.appendChild(svg('path', { d: 'M' + x + ',' + yy + 'H' + (x + ancho - r) + 'Q' + (x + ancho) + ',' + yy + ' ' + (x + ancho) + ',' + (yy + r) + 'V' + (yy + h - r) + 'Q' + (x + ancho) + ',' + (yy + h) + ' ' + (x + ancho - r) + ',' + (yy + h) + 'H' + x + 'Z', fill: opt.color || C1 }));
      f.s.appendChild(txt(x + ancho + 6, yy + 13, (opt.fmt || soles)(it.v), { 'class': 'graf-valor' }));
      var hit = svg('rect', { x: 0, y: y, width: f.W, height: fila, fill: 'transparent' });
      hover(hit, '<b>' + it.k + '</b><div>' + (opt.fmt || soles)(it.v) + (opt.extra ? opt.extra(it) : '') + '</div>');
      f.s.appendChild(hit);
    });
  };
})(window);
