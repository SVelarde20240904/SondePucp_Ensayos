"""
Carga la HISTORIA de la mano de figuras de antes de la web: las matrices del Excel que se armaban para cada ensayo
(carpetas "Para el DD_MM_AA", con su "Actualización Final"). Solo sirve para ver la evolución (Histórico y Mi progreso):
NO cambia los colores actuales de nadie.

Uso:  python importar_matriz_historica.py CARPETA_SDP-2026 > actualizacion_matriz_historica.sql

- De cada carpeta "Para el ..." se toma la "Actualización Final" (si no hay, Matriz_Figuras_SDP.xlsx).
- La primera matriz entra completa; de las siguientes solo lo que cambió respecto a la anterior.
- Cada matriz queda con la fecha de ese ensayo (es la mano con la que se llegó a ese ensayo).
- Es idempotente: borra lo cargado antes de esta misma fuente (MATRIZ_EXCEL) y lo vuelve a cargar.
Requiere: pip install openpyxl
"""
import sys, os, re, glob, datetime as dt
import openpyxl

# nombre corto de la matriz -> nombre en la web
MIEMBROS = {
    "Abigail": "Abigail Delgado", "Adrian": "Adrian Lopez", "Alessia": "Alessia Leon", "Alex": "Alexander Gil",
    "Andre": "André La Hoz", "Antony": "Antony Palacios", "Arturo": "Arturo Custodio", "Beteta": "Alexander Beteta",
    "Brian": "Brian Chero", "Cielo": "Cielo Camayo", "Claudia": "Claudia Alonso", "Daniela": "Daniela Llano",
    "Deyvid": "Deyvid Usco", "Diana": "Diana Levano", "Esteban": "Esteban Gutiérrez", "Haniel": "Haniel Ghiggo",
    "Heidy": "Heidy Sanchez", "Ilo": "Enmanuel Vega", "José": "José Pérez", "Mafer": "Maria Fernanda Alvarez",
    "Maricielo": "Maricielo Mamani", "Mayte": "Mayte Malpartida", "Mimi": "Miranda Cueva", "Natalia": "Natalia Vidal",
    "Paola": "Paola Nieto", "Pierina": "Pierina Perales", "Sergio": "Sergio Velarde", "Ximena": "Ximena Robles",
}
# figuras que cambiaron de nombre -> nombre en la web ("Paseos" se separó después en tres: no se carga)
FIGURAS = {"Enredala compl.": "Enredala complicado", "Festival de pelotas": "Festival de pelotas SDP",
           "H Derecha M izq": "H Derecha M Izquierda", "Botala para atrás": "Botala por atrás"}
NO_CARGAR = {"Paseos"}
COLORES = ("ROJO", "AMARILLO", "VERDE", "AZUL")


def q(s):
    return "'" + str(s).strip().replace("\\", "\\\\").replace("'", "''") + "'"


def matrices(carpeta):
    elegidas = {}
    for f in glob.glob(os.path.join(carpeta, "**", "*.xlsx"), recursive=True):
        if os.path.basename(f).startswith("~$"): continue
        m = re.search(r"Para el (\d\d)[_-](\d\d)[_-](\d\d)", f)
        if not m: continue
        fecha = dt.date(2000 + int(m.group(3)), int(m.group(2)), int(m.group(1)))
        prioridad = 2 if "final" in f.lower() else (1 if os.path.basename(f) == "Matriz_Figuras_SDP.xlsx" else 0)
        if fecha not in elegidas or prioridad > elegidas[fecha][0]: elegidas[fecha] = (prioridad, f)
    return {d: v[1] for d, v in sorted(elegidas.items())}


def leer(f):
    wb = openpyxl.load_workbook(f, data_only=True)
    ws = wb["MATRIZ DE FIGURAS"] if "MATRIZ DE FIGURAS" in wb.sheetnames else wb[wb.sheetnames[0]]
    filas = list(ws.iter_rows(values_only=True))
    cols = []
    for j, h in enumerate(filas[0][1:], 1):
        if h is None or not str(h).strip(): break
        cols.append((j, str(h).strip()))
    mano = {}
    for r in filas[1:]:
        if r[0] is None or not str(r[0]).strip(): break
        fig = str(r[0]).strip()
        if fig in NO_CARGAR: continue
        fig = FIGURAS.get(fig, fig)
        for j, corto in cols:
            color = re.sub(r"[^A-Z]", "", str(r[j] or "").upper())
            if color in COLORES: mano[(MIEMBROS.get(corto, corto), fig)] = color
    return mano


def main(carpeta):
    filas, antes = [], {}
    for fecha, f in matrices(carpeta).items():
        mano = leer(f)
        for k, color in sorted(mano.items()):
            if antes.get(k) != color: filas.append((fecha, k[0], k[1], antes.get(k), color))
        antes.update(mano)
    o = print
    o("-- =====================================================================")
    o(f"--  Historia de la mano (matrices del Excel por ensayo) · generado {dt.date.today()} · {len(filas)} cambios")
    o("--  Solo histórico (fuente MATRIZ_EXCEL): no cambia los colores actuales")
    o("-- =====================================================================")
    o("SET NAMES utf8mb4;")
    o("START TRANSACTION;")
    o("CREATE TEMPORARY TABLE tmp_mano (fecha DATE, nombre VARCHAR(80), figura VARCHAR(80), antes VARCHAR(10), color VARCHAR(10));")
    for i in range(0, len(filas), 300):
        o("INSERT INTO tmp_mano VALUES\n  " + ",\n  ".join(
            f"('{r[0]}',{q(r[1])},{q(r[2])},{'NULL' if r[3] is None else q(r[3])},'{r[4]}')" for r in filas[i:i + 300]) + ";")
    o("""
SELECT DISTINCT CONCAT('NO ENCONTRADO: ', t.nombre) AS aviso FROM tmp_mano t LEFT JOIN miembro m ON m.nombre = t.nombre WHERE m.id_miembro IS NULL;
SELECT DISTINCT CONCAT('NO ENCONTRADA: ', t.figura) AS aviso FROM tmp_mano t LEFT JOIN figura f ON f.nombre = t.figura WHERE f.id_figura IS NULL;
DELETE FROM historial_evaluacion WHERE fuente = 'MATRIZ_EXCEL';
INSERT INTO historial_evaluacion (id_miembro, id_figura, color_anterior, color_nuevo, fuente, aplicado, motivo, fecha)
SELECT m.id_miembro, f.id_figura, t.antes, t.color, 'MATRIZ_EXCEL', 1, CONCAT('Matriz del Excel para el ', DATE_FORMAT(t.fecha, '%d/%m')), t.fecha
  FROM tmp_mano t JOIN miembro m ON m.nombre = t.nombre JOIN figura f ON f.nombre = t.figura ORDER BY t.fecha;
SELECT DATE_FORMAT(fecha, '%d/%m/%Y') AS matriz, COUNT(*) AS cambios FROM historial_evaluacion WHERE fuente = 'MATRIZ_EXCEL' GROUP BY fecha ORDER BY fecha;
DROP TEMPORARY TABLE tmp_mano;
COMMIT;""")


if __name__ == "__main__":
    main(sys.argv[1])
