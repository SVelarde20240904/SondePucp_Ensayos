"""
Pasa al sistema las respuestas de los Forms que siguen llegando al Excel (hoja HISTORICO
de Matriz_Figuras_SDP_v2_6.xlsx), mientras dure la transición Excel -> web.

Uso:  python importar_respuestas_excel.py Matriz_Figuras_SDP_v2_6.xlsx > actualizacion_respuestas.sql
      (luego se carga el .sql en la BD como el seed)

Qué hace con cada fila del HISTORICO que la BD todavía no tiene:
  - "Mensual"            -> autoevaluación mensual: deja ese color (como en la web).
  - "Semanal"            -> autoevaluación del ensayo: solo sube el color, nunca lo baja.
  - "Semanal (ignorada)" -> se guarda en el historial como no aplicada (el Excel ya la ignoró).
  - Si esa persona ya cambió esa figura DESDE LA WEB después de la respuesta del Excel, gana la web
    (la fila queda en el historial como no aplicada).
Es idempotente: si se carga dos veces, la segunda no cambia nada (cada fila se reconoce por
miembro + figura + fuente + fecha). Así se puede volver a correr con cada Excel nuevo.
Requiere: pip install openpyxl
"""
import sys, datetime as dt
import openpyxl

ALIAS = {"Ilo": "Enmanuel Vega", "Mimi": "Miranda Cueva"}   # por si algún Form llega con apodo
# Figuras que se renombraron en el Excel: nombre viejo -> nombre nuevo (el sistema adopta el nuevo)
RENOMBRADAS = {"Enredala compl.": "Enredala complicado", "Festival de pelotas": "Festival de pelotas SDP",
               "H Derecha M izq": "H Derecha M Izquierda"}
FUENTE = {"mensual": ("MENSUAL", True), "semanal": ("ENSAYO", True), "semanal (ignorada)": ("ENSAYO", False)}
COLORES = ("ROJO", "AMARILLO", "VERDE", "AZUL")


def q(s):
    return "'" + str(s).strip().replace("\\", "\\\\").replace("'", "''") + "'"


def main(xlsx):
    wb = openpyxl.load_workbook(xlsx, data_only=True)
    h = wb["HISTORICO"]
    filas = []
    for r in h.iter_rows(min_row=2, max_col=5, values_only=True):
        fecha, nombre, figura, color, fuente = r
        if not isinstance(fecha, dt.datetime) or not nombre or not figura: continue
        color = str(color or "").strip().upper()
        f = FUENTE.get(str(fuente or "").strip().lower())
        if color not in COLORES or not f:
            print(f"-- fila ignorada (color/fuente raros): {r}", file=sys.stderr); continue
        filas.append((fecha.replace(microsecond=0), ALIAS.get(nombre.strip(), nombre.strip()), figura.strip(), color, *f))
    filas.sort()

    o = print
    o("-- =====================================================================")
    o(f"--  Respuestas de los Forms (hoja HISTORICO de {xlsx.replace(chr(92), '/').split('/')[-1]})")
    o(f"--  Generado {dt.date.today()} · {len(filas)} filas en el Excel; solo entran las que la BD no tiene")
    o("-- =====================================================================")
    o("SET NAMES utf8mb4;")
    o("START TRANSACTION;")
    # periodo mensual de cada mes que aparece
    for a, m in sorted({(f[0].year, f[0].month) for f in filas if f[4] == "MENSUAL"}):
        fin = (dt.date(a + (m == 12), m % 12 + 1, 1) - dt.timedelta(days=1))
        o(f"INSERT IGNORE INTO periodo_autoevaluacion (anio, mes, fecha_apertura, fecha_cierre) VALUES ({a},{m},'{a}-{m:02d}-01 00:00:00','{fin} 23:59:59');")
    o("-- figuras renombradas en el Excel (solo si todavía tienen el nombre viejo)")
    for viejo, nuevo in RENOMBRADAS.items():
        o(f"UPDATE figura SET nombre = {q(nuevo)} WHERE nombre = {q(viejo)} AND NOT EXISTS (SELECT 1 FROM (SELECT id_figura FROM figura WHERE nombre = {q(nuevo)}) x);")
    o("CREATE TEMPORARY TABLE tmp_resp (fecha DATETIME, nombre VARCHAR(80), figura VARCHAR(80), color VARCHAR(10), fuente VARCHAR(15), valida TINYINT, nuevo TINYINT DEFAULT 0, aplica TINYINT DEFAULT 0, id_miembro INT, id_figura INT, anterior VARCHAR(10));")
    for i in range(0, len(filas), 200):
        o("INSERT INTO tmp_resp (fecha, nombre, figura, color, fuente, valida) VALUES\n  " + ",\n  ".join(
            f"('{f[0]}',{q(f[1])},{q(f[2])},'{f[3]}','{f[4]}',{int(f[5])})" for f in filas[i:i + 200]) + ";")
    o("""
UPDATE tmp_resp t JOIN miembro m ON m.nombre = t.nombre SET t.id_miembro = m.id_miembro;
UPDATE tmp_resp t JOIN figura f ON f.nombre = t.figura SET t.id_figura = f.id_figura;
-- nombres o figuras que no existen en la BD (se avisan y no se cargan)
SELECT CONCAT('NO ENCONTRADO: ', nombre, ' / ', figura) AS aviso FROM tmp_resp WHERE id_miembro IS NULL OR id_figura IS NULL;

-- solo las filas que la BD todavía no tiene
UPDATE tmp_resp t SET t.nuevo = 1
 WHERE t.id_miembro IS NOT NULL AND t.id_figura IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM historial_evaluacion h WHERE h.id_miembro = t.id_miembro AND h.id_figura = t.id_figura
                   AND h.fuente = t.fuente AND h.fecha = t.fecha);
""")
    # Se procesan en orden de fecha, una por una (cada respuesta parte del color que dejó la anterior)
    o("-- se aplican en orden de fecha")
    for f in filas:
        cond = (f"t.fecha = '{f[0]}' AND t.nombre = {q(f[1])} AND t.figura = {q(f[2])} AND t.fuente = '{f[4]}'")
        o(f"UPDATE tmp_resp t LEFT JOIN evaluacion e ON e.id_miembro = t.id_miembro AND e.id_figura = t.id_figura SET t.anterior = e.color, "
          f"t.aplica = (t.valida = 1 AND (t.fuente = 'MENSUAL' OR e.color IS NULL OR (e.color + 0) < FIELD(t.color,'ROJO','AMARILLO','VERDE','AZUL')) "
          f"AND NOT EXISTS (SELECT 1 FROM historial_evaluacion w WHERE w.id_miembro = t.id_miembro AND w.id_figura = t.id_figura AND w.aplicado = 1 "
          f"AND w.fuente IN ('MENSUAL','ENSAYO','CORRECCION') AND w.fecha > t.fecha + INTERVAL 5 HOUR)) WHERE t.nuevo = 1 AND {cond};")
        o(f"INSERT INTO evaluacion (id_miembro, id_figura, color, fecha_actualizacion) SELECT t.id_miembro, t.id_figura, t.color, t.fecha FROM tmp_resp t "
          f"WHERE t.nuevo = 1 AND t.aplica = 1 AND {cond} ON DUPLICATE KEY UPDATE color = VALUES(color), fecha_actualizacion = VALUES(fecha_actualizacion);")
    o("""
INSERT INTO historial_evaluacion (id_miembro, id_figura, color_anterior, color_nuevo, fuente, aplicado, id_periodo, registrado_por, motivo, fecha)
SELECT t.id_miembro, t.id_figura, t.anterior, t.color, t.fuente, t.aplica,
       (SELECT p.id_periodo FROM periodo_autoevaluacion p WHERE t.fuente = 'MENSUAL' AND p.anio = YEAR(t.fecha) AND p.mes = MONTH(t.fecha)),
       t.id_miembro, 'Respuesta del Form (Excel)', t.fecha
  FROM tmp_resp t WHERE t.nuevo = 1 ORDER BY t.fecha;

SELECT t.nombre AS miembro, t.fuente, DATE_FORMAT(MIN(t.fecha), '%d/%m %H:%i') AS fecha, COUNT(*) AS respuestas,
       SUM(t.aplica = 1 AND NOT (t.anterior <=> t.color)) AS colores_cambiados
  FROM tmp_resp t WHERE t.nuevo = 1 GROUP BY t.nombre, t.fuente ORDER BY MIN(t.fecha);
DROP TEMPORARY TABLE tmp_resp;
COMMIT;""")


if __name__ == "__main__":
    main(sys.argv[1])
