/* Editor del cronograma de talleres por secciones (llaves de horas con varios pasos adentro).
   Cada nivel tiene <div class="crono" data-nivel> con un <textarea hidden name="crono_NIVEL"> que se va reescribiendo:
     ## 8:15 - 8:40 | NORMAL
     Cero | 15'
     Kentucky | 10'
   Así el servidor lo lee igual sin JSON. */
(function () {
  'use strict';
  var ESTILOS = [['NORMAL', 'Pasos'], ['FILTRO', 'Filtros'], ['MUSICA', 'Música'], ['INICIO', 'Inicio'], ['FINAL', 'Cierre']];
  var MUSICA = '🎶💃 MÚSICA 🎶💃';

  function el(tag, attrs, kids) {
    var e = document.createElement(tag);
    Object.keys(attrs || {}).forEach(function (k) {
      if (attrs[k] == null) return;
      if (k === 'text') e.textContent = attrs[k];
      else if (k.indexOf('on') === 0) e.addEventListener(k.slice(2), attrs[k]);
      else e.setAttribute(k, attrs[k]);
    });
    (kids || []).forEach(function (c) { if (c) e.appendChild(c); });
    return e;
  }

  function leer(txt) {
    var secs = [], act = null;
    (txt || '').split(/\r?\n/).forEach(function (l) {
      if (!l.trim()) return;
      if (l.indexOf('##') === 0) {
        var p = l.slice(2).split('|');
        act = { hora: (p[0] || '').trim(), estilo: (p[1] || 'NORMAL').trim(), items: [] };
        secs.push(act);
      } else {
        if (!act) { act = { hora: '', estilo: 'NORMAL', items: [] }; secs.push(act); }
        var q = l.split('|');
        act.items.push({ texto: (q[0] || '').trim(), dur: (q[1] || '').trim() });
      }
    });
    return secs;
  }

  function limpio(s) { return (s || '').replace(/\|/g, '/').replace(/\n/g, ' ').trim(); }
  function min(d) { var m = /(\d+)/.exec(d || ''); return m ? +m[1] : 0; }

  function iniciar(cont) {
    var ta = cont.querySelector('textarea');
    var lista = cont.querySelector('.crono-secciones');
    var secs = leer(ta.value);
    var activa = null;

    function escribir() {
      var out = '';
      secs.forEach(function (s) {
        out += '## ' + limpio(s.hora) + ' | ' + s.estilo + '\n';
        s.items.forEach(function (i) { if (limpio(i.texto)) out += limpio(i.texto) + ' | ' + limpio(i.dur) + '\n'; });
      });
      ta.value = out;
    }

    function pintar() {
      lista.innerHTML = '';
      secs.forEach(function (s, si) {
        var total = s.items.reduce(function (a, i) { return a + min(i.dur); }, 0);
        var items = el('div', { 'class': 'sec-items' });
        s.items.forEach(function (it, ii) {
          items.appendChild(el('div', { 'class': 'sec-item' }, [
            el('input', { 'class': 'input', value: it.texto, placeholder: s.estilo === 'FILTRO' ? 'FILTROS (dile que no, guapeo…)' : 'Paso (ej. Kentucky)', 'aria-label': 'Paso',
              oninput: function (e) { it.texto = e.target.value; escribir(); }, onfocus: function () { activa = s; } }),
            el('input', { 'class': 'input dur', value: it.dur, placeholder: "10'", 'aria-label': 'Minutos',
              oninput: function (e) { it.dur = e.target.value; escribir(); tot.textContent = totalDe(s); } }),
            el('button', { type: 'button', 'class': 'btn btn-ghost btn-sm', title: 'Quitar paso', text: '✕',
              onclick: function () { s.items.splice(ii, 1); escribir(); pintar(); } })
          ]));
        });
        var sel = el('select', { 'class': 'input estilo', 'aria-label': 'Tipo de sección',
          onchange: function (e) {
            s.estilo = e.target.value;
            if (s.estilo === 'MUSICA' && !s.items.length) s.items.push({ texto: MUSICA, dur: "5'" });
            escribir(); pintar();
          } });
        ESTILOS.forEach(function (o) { var op = el('option', { value: o[0], text: o[1] }); if (o[0] === s.estilo) op.selected = true; sel.appendChild(op); });
        var tot = el('span', { 'class': 'sec-total muted xs', text: totalDe(s) });
        var caja = el('div', { 'class': 'sec sec-' + s.estilo.toLowerCase() + (s === activa ? ' activa' : ''), onclick: function () { activa = s; marcar(); } }, [
          el('div', { 'class': 'sec-head' }, [
            el('input', { 'class': 'input hora', value: s.hora, placeholder: '8:15 - 8:40', 'aria-label': 'Horas de la sección',
              oninput: function (e) { s.hora = e.target.value; escribir(); } }),
            sel, tot,
            el('span', { 'class': 'grow' }),
            el('button', { type: 'button', 'class': 'btn btn-ghost btn-sm', title: 'Subir', text: '↑', disabled: si === 0 ? 'disabled' : null,
              onclick: function () { if (si > 0) { secs.splice(si - 1, 0, secs.splice(si, 1)[0]); escribir(); pintar(); } } }),
            el('button', { type: 'button', 'class': 'btn btn-ghost btn-sm', title: 'Bajar', text: '↓',
              onclick: function () { if (si < secs.length - 1) { secs.splice(si + 1, 0, secs.splice(si, 1)[0]); escribir(); pintar(); } } }),
            el('button', { type: 'button', 'class': 'btn btn-ghost btn-sm', title: 'Quitar sección', text: '✕',
              onclick: function () { secs.splice(si, 1); escribir(); pintar(); } })
          ]),
          items,
          s.estilo === 'MUSICA' ? null : el('button', { type: 'button', 'class': 'btn btn-ghost btn-sm mas-item', text: '+ Paso en esta sección',
            onclick: function () { s.items.push({ texto: '', dur: '' }); escribir(); pintar(); var ins = lista.querySelectorAll('.sec')[si].querySelectorAll('.sec-item input:not(.dur)'); ins[ins.length - 1].focus(); } })
        ]);
        lista.appendChild(caja);
        if (s.estilo === 'MUSICA') caja.querySelectorAll('.sec-item input:not(.dur)').forEach(function (i) { i.readOnly = true; });
      });
    }

    function totalDe(s) {
      var t = s.items.reduce(function (a, i) { return a + min(i.dur); }, 0);
      return t ? t + "'" : '';
    }

    function marcar() { lista.querySelectorAll('.sec').forEach(function (n, i) { n.classList.toggle('activa', secs[i] === activa); }); }

    // horas automáticas: desde la hora de inicio del taller, sumando los minutos de cada sección
    function calcular() {
      var ini = (document.querySelector('[name=horaInicio]') || {}).value || cont.dataset.inicio || '19:30';
      var p = ini.split(':'), t = (+p[0]) * 60 + (+p[1] || 0);
      function fmt(m) { var h = Math.floor(m / 60) % 24; h = h > 12 ? h - 12 : h; return h + ':' + ('0' + (m % 60)).slice(-2); }
      secs.forEach(function (s) {
        var d = s.items.reduce(function (a, i) { return a + min(i.dur); }, 0);
        if (!d && s.estilo === 'MUSICA') d = 5;
        s.hora = (s.estilo === 'MUSICA' || s.estilo === 'FILTRO' || !d) ? fmt(t) : fmt(t) + ' - ' + fmt(t + d);
        t += d;
      });
      escribir(); pintar();
    }

    cont.querySelector('.crono-mas-seccion').addEventListener('click', function () {
      var s = { hora: '', estilo: 'NORMAL', items: [{ texto: '', dur: '' }] };
      secs.push(s); activa = s; escribir(); pintar();
    });
    cont.querySelector('.crono-mas-musica').addEventListener('click', function () {
      secs.push({ hora: '', estilo: 'MUSICA', items: [{ texto: MUSICA, dur: "5'" }] }); escribir(); pintar();
    });
    cont.querySelector('.crono-calcular').addEventListener('click', calcular);
    var copiar = cont.querySelector('.crono-copiar');
    if (copiar) copiar.addEventListener('change', function () {
      var otro = document.querySelector('.crono[data-nivel="' + copiar.value + '"] textarea');
      if (otro && confirm('¿Reemplazar este cronograma por una copia del otro nivel?')) { secs = leer(otro.value); escribir(); pintar(); }
      copiar.value = '';
    });
    // catálogo: tocar un paso lo agrega a la sección activa (o a la última de pasos)
    cont.querySelectorAll('.chip-paso').forEach(function (b) {
      b.addEventListener('click', function () {
        var dest = activa && activa.estilo !== 'MUSICA' ? activa : null;
        if (!dest) for (var i = secs.length - 1; i >= 0; i--) if (secs[i].estilo === 'NORMAL') { dest = secs[i]; break; }
        if (!dest) { dest = { hora: '', estilo: 'NORMAL', items: [] }; secs.push(dest); }
        var vacio = dest.items.filter(function (i) { return !i.texto.trim(); })[0];
        if (vacio) vacio.texto = b.textContent.trim(); else dest.items.push({ texto: b.textContent.trim(), dur: '' });
        activa = dest; escribir(); pintar();
      });
    });
    escribir(); pintar();
  }

  document.querySelectorAll('.crono').forEach(iniciar);
})();
