/* Son de PUCP · Sistema de Ensayos
   En celular las tablas se muestran como tarjetas: este script copia el título de
   cada columna (<th>) a sus celdas como data-label, y el CSS lo muestra como etiqueta.
   Las tablas con clase "no-cards" o las del reporte A4 se quedan como tabla. */
(function () {
  function etiquetar(tabla) {
    if (tabla.classList.contains('no-cards') || tabla.closest('.page, .rsec')) return;
    var filasCab = tabla.tHead ? tabla.tHead.rows : [];
    if (!filasCab.length) return;
    var titulos = [];
    Array.prototype.forEach.call(filasCab[filasCab.length - 1].cells, function (th) {
      var span = th.colSpan || 1, texto = th.textContent.trim();
      for (var i = 0; i < span; i++) titulos.push(texto);
    });
    Array.prototype.forEach.call(tabla.tBodies, function (tb) {
      Array.prototype.forEach.call(tb.rows, function (tr) {
        var col = 0;
        Array.prototype.forEach.call(tr.cells, function (td) {
          if (!td.hasAttribute('data-label') && td.colSpan === 1) td.setAttribute('data-label', titulos[col] || '');
          col += td.colSpan || 1;
        });
      });
    });
    tabla.classList.add('js-cards');
  }
  // Reporte A4 (794px de ancho): en pantallas angostas se achica para que entre entero
  function ajustarReporte() {
    var ancho = Math.min(window.innerWidth, document.documentElement.clientWidth) - 32;
    var z = ancho < 794 ? (ancho / 794).toFixed(3) : '';
    Array.prototype.forEach.call(document.querySelectorAll('.report-body .page'), function (p) { p.style.zoom = z; });
  }
  function iniciar() {
    Array.prototype.forEach.call(document.querySelectorAll('table.table'), etiquetar);
    if (document.querySelector('.report-body .page')) {
      ajustarReporte();
      window.addEventListener('resize', ajustarReporte);
      window.addEventListener('beforeprint', function () { Array.prototype.forEach.call(document.querySelectorAll('.page'), function (p) { p.style.zoom = ''; }); });
      window.addEventListener('afterprint', ajustarReporte);
    }
    // Esc cierra el menú lateral del celular
    document.addEventListener('keydown', function (e) {
      var m = document.getElementById('menu');
      if (e.key === 'Escape' && m) m.checked = false;
    });
  }
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', iniciar); else iniciar();
})();
