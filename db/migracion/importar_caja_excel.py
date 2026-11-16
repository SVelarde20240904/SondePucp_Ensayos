"""
Pasa a la web la hoja "CAJA SDP 2026" del Excel de Finanzas (CAJA_2026.xlsx): ingresos por cuotas de cada mes
(hasta octubre 2026; desde noviembre las cuotas entran solas por la web) y cada gasto con su categoría.

Uso:  python importar_caja_excel.py CAJA_2026.xlsx > actualizacion_caja_2026.sql
      (luego se carga el .sql en la BD como los demás)

- El Excel solo dice el mes: los gastos quedan con fecha 15 de ese mes y los ingresos de cuotas con el último día.
- Es idempotente: un movimiento con la misma fecha, concepto y monto no se vuelve a insertar.
- Las celdas con texto en vez de número (ej. '55.40') igual se toman como monto.
Requiere: pip install openpyxl
"""
import sys, calendar, datetime as dt
import openpyxl

MESES = ["enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"]
ANIO = 2026
CUOTAS_WEB_DESDE = 11          # noviembre: desde aquí las cuotas las suma la web
# palabra del concepto -> (categoría, proyecto)
REGLAS = [
    ("bocadito", ("Talleres", "Primer taller 2026-2")),
    ("aniversario", ("Eventos", "Aniversario taller SDP")), ("torta", ("Eventos", "Aniversario taller SDP")),
    ("bautizo", ("Eventos", "Bautizo")), ("casillero", ("Casillero", None)), ("micro", ("Equipos", None)),
    ("taxi", ("Movilidad", None)), ("almuerzo", ("Comida", None)), ("pines", ("Materiales y merch", None)),
    ("cinta", ("Materiales y merch", None)), ("taller", ("Talleres", None)),
]


def q(s):
    return "NULL" if s is None else "'" + str(s).strip().replace("\\", "\\\\").replace("'", "''") + "'"


def numero(v):
    if v is None or v == "": return None
    try: return round(float(str(v).replace("S/", "").replace(",", ".").strip()), 2)
    except ValueError: return None


def clasificar(concepto):
    c = concepto.lower()
    for clave, res in REGLAS:
        if clave in c: return res
    return ("Otros", None)


def main(xlsx):
    ws = openpyxl.load_workbook(xlsx, data_only=True)["CAJA SDP 2026"]
    filas = [list(r) for r in ws.iter_rows(values_only=True)]
    movs = []
    seccion = None
    for r in filas:
        etiqueta = str(r[0] or "").strip()
        if etiqueta.lower().startswith("ingresos"): seccion = "INGRESO"; continue
        if etiqueta.lower().startswith("gastos"): seccion = "GASTO"; continue
        if not etiqueta or etiqueta.lower().startswith(("total", "caja")) or seccion is None: continue
        for i in range(12):
            monto = numero(r[1 + i] if len(r) > 1 + i else None)
            if not monto or monto <= 0: continue
            mes = i + 1
            if seccion == "INGRESO":
                if mes >= CUOTAS_WEB_DESDE: continue
                fecha = dt.date(ANIO, mes, calendar.monthrange(ANIO, mes)[1])
                movs.append((fecha, "INGRESO", "Cuotas", f"Cuotas de {MESES[i]} (Excel)", monto, None))
            else:
                cat, proy = clasificar(etiqueta)
                movs.append((dt.date(ANIO, mes, 15), "GASTO", cat, etiqueta[:1].upper() + etiqueta[1:], monto, proy))

    o = print
    o("-- =====================================================================")
    o(f"--  Caja de Finanzas (hoja CAJA SDP 2026) · generado {dt.date.today()} · {len(movs)} movimientos")
    o("--  Saldo inicial (caja final 2025) y fecha de inicio quedan en config: caja.saldo_inicial / caja.desde")
    o("-- =====================================================================")
    o("SET NAMES utf8mb4;")
    o("START TRANSACTION;")
    for f, tipo, cat, concepto, monto, proy in movs:
        o(f"INSERT INTO caja_movimiento (fecha, tipo, categoria, concepto, monto, proyecto) SELECT '{f}', '{tipo}', {q(cat)}, {q(concepto)}, {monto:.2f}, {q(proy)} "
          f"FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM caja_movimiento WHERE fecha='{f}' AND concepto={q(concepto)} AND monto={monto:.2f});")
    o("SELECT tipo, COUNT(*) AS movimientos, SUM(monto) AS total FROM caja_movimiento GROUP BY tipo;")
    o("COMMIT;")


if __name__ == "__main__":
    main(sys.argv[1])
