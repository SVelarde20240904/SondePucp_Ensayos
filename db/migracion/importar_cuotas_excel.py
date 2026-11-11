"""Historial de cuotas 2026 (hoja 'Cuota 2026' de CAJA_2026.xlsx) + encargados de cobro (Hoja 2) -> SQL idempotente."""
import sys, re, datetime as dt, openpyxl
# nombre en el Excel -> (id_miembro, palabra que debe aparecer en su nombre/nombre completo/apodo)
M = {"Álvaro Camposano": (8, "Camposano"), "Angela Karina Yánac León": (10, "Yanac"), "Ximena Robles": (57, "Robles"),
     "Gabriela Silvana Arrieta Prado": (29, "Arrieta"), "Alex Beteta": (7, "Beteta"), "Alexander Gil": (6, "Gil"), "Pedro Chero": (50, "Pedro"),
     "Deyvid Usco Rutti": (23, "Usco"), "Diana Levano": (24, "Levano"), "Natalia Vidal": (48, "Vidal"), "Silvia Valle Gomez": (56, "Valle"),
     "Adrian López": (2, "Lopez"), "Maria Fernanda Alvarez": (40, "Alvarez Guerra"), "Antony Palacios": (11, "Palacios"),
     "Gabriela sanchez": (30, "Gabriela Belen"), "Maricielo": (42, "Maricielo"), "Sergio": (55, "Velarde"), "Pierina": (51, "Pierina"),
     "Abigail": (1, "Abigail"), "Jose": (35, "Pérez"), "Ilo": (25, "Enmanuel"), "Mayte": (46, "Mayte"), "María Isabel Olaya Padilla": (39, "Isabel"),
     "Ari": (13, "Ariana"), "Haniel": (31, "Haniel"), "Claudia": (20, "Claudia"), "Brayan Chero": (15, "Brian"), "Ale": (3, "Cuenca"),
     "Oscar Erick Vercelli Luis": (26, "Vercelli"), "Ivan Felices": (33, "Felices"), "Joseph": (36, "Josefh"), "Alessia": (5, "Alessia"),
     "Arturo": (14, "Arturo"), "Cielo": (19, "Cielo"), "Esteban": (27, "Esteban"), "Heidy": (32, "Heidy"), "Andre": (9, "André"),
     "Mimi": (47, "Miranda"), "Daniela": (22, "Llano"), "Paola": (49, "Paola"), "Mauricio": (45, "Mauricio"),
     "Sebastián Flores Villavicencio": (54, "Sebastian"), "Joaquín García": (17, "Joaquin"), "Mateo Pisconte": (44, "Pisconte"),
     "Marko Romaní": (43, "Romani"), "Rodrigo Tay": (52, "Tay"), "Aránzazu Arigita": (12, "Arigita"), "María Espinoza": (41, "Espinoza"),
     "Kamila Meneses": (37, "Kamila"), "Alessandra Novoa": (4, "Novoa"), "Estrella Pequeño": (28, "Estrella"), "Daniela Álvarez": (21, "Alvarez Lucas"),
     "Xiomara Ames": (58, "Ames"), "Christian Lescano": (18, "Lescano"), "Jorge Hualpa": (34, "Hualpa"), "Camila Carrasco": (16, "Reymundo"),
     "Sahily Flores": (53, "Sahily"), "Luciana Torres": (38, "Torres")}
COBRA = {"Adrian": (2, "Lopez"), "Alex": (6, "Gil"), "Daniela": (22, "Llano"), "Diana": (24, "Levano"), "Gaby": (30, "Gabriela Belen")}
NIUS_ADELANTO = {"Camila Carrasco"}   # pagó en la columna de octubre, pero los nius pagan desde noviembre -> se toma como noviembre

def q(s): return "NULL" if s is None else "'" + str(s).replace("'", "''") + "'"
def dec(v): return "NULL" if v is None else f"{float(v):.2f}"

wb = openpyxl.load_workbook(sys.argv[1], data_only=True)
ws = wb["Cuota 2026"]
filas, avisos = [], []
for r in ws.iter_rows(min_row=2, values_only=True):
    nombre = (r[1] or "").strip()
    if not nombre: continue
    if nombre not in M: avisos.append(nombre); continue
    idm, pal = M[nombre]
    celdas = list(r[2:15])                       # ene..dic 2026 + ene 2027
    anual = isinstance(celdas[0], (int, float)) and celdas[0] == 66
    if anual: celdas = [0] * 12 + [celdas[12]]   # pagaron los 12 meses (en el Excel junio quedó en blanco)
    for i, v in enumerate(celdas):
        if v is None or (isinstance(v, str) and not v.strip()): continue
        anio, mes = (2026, i + 1) if i < 12 else (2027, 1)
        if nombre in NIUS_ADELANTO and (anio, mes) == (2026, 10): mes, nota_extra = 11, " (lo pagó en octubre)"
        else: nota_extra = ""
        estado, fijo, pagado, nota = "PAGADA", None, None, None
        if isinstance(v, (int, float)):
            if anual: pagado, nota = 5.5, "Pago anual S/ 66 (Excel)"
            elif v == 0: pagado, nota = None, "Pagado (Excel)"
            else:
                pagado = v
                nota = "Pagó S/ %s ese mes, cubre varios meses (Excel)" % (int(v) if v == int(v) else v) if v > 9 else "Excel"
        else:
            t = v.strip().lower()
            if t == "pagado": nota = "Pagado (Excel)"
            elif t.startswith("pendiente"):
                m = re.search(r"s/\s*([\d.,]+)", t)
                monto = float(m.group(1).replace(",", ".")) if m else 9.0
                estado, fijo = "PENDIENTE", monto
                nota = "Deuda que venía del Excel" if monto > 9 else ("Saldo pendiente (Excel)" if monto < 6 else None)
            else: avisos.append(f"{nombre} {anio}-{mes}: '{v}'"); continue
        filas.append((idm, pal, anio, mes, estado, fijo, pagado, (nota + nota_extra) if nota else (nota_extra.strip(" ()") or None)))

cob = []
for r in wb["Hoja 2"].iter_rows(min_row=2, values_only=True):
    if not r[0] or not r[1]: continue
    n, c = r[0].strip(), r[1].strip()
    if n in M and c in COBRA: cob.append((M[n][0], M[n][1], COBRA[c][0], COBRA[c][1]))
    else: avisos.append(f"cobrador: {n} -> {c}")

o = print
o("-- Historial de cuotas 2026 y encargados de cobro (CAJA_2026.xlsx). Idempotente: no pisa lo que ya se pagó o se tocó desde la web.")
o("SET NAMES utf8mb4;\nSTART TRANSACTION;")
o("CREATE TEMPORARY TABLE tmp_cu (id INT, palabra VARCHAR(40), anio SMALLINT, mes TINYINT, estado VARCHAR(10), fijo DECIMAL(7,2), pagado DECIMAL(7,2), nota VARCHAR(200));")
o("INSERT INTO tmp_cu VALUES\n  " + ",\n  ".join(f"({a},{q(b)},{c},{d},{q(e)},{dec(f)},{dec(g)},{q(h)})" for a, b, c, d, e, f, g, h in filas) + ";")
o("CREATE TEMPORARY TABLE tmp_co (id INT, palabra VARCHAR(40), cob INT, palabra_cob VARCHAR(40));")
o("INSERT INTO tmp_co VALUES\n  " + ",\n  ".join(f"({a},{q(b)},{c},{q(d)})" for a, b, c, d in cob) + ";")
V = "CONCAT(m.nombre,' ',IFNULL(m.nombre_completo,''),' ',IFNULL(m.apodo,'')) LIKE CONCAT('%', {p}, '%')"
o(f"""SELECT DISTINCT CONCAT('NO COINCIDE (no se toca): id ', t.id, ' / ', t.palabra) AS aviso FROM tmp_cu t LEFT JOIN miembro m ON m.id_miembro = t.id
 WHERE m.id_miembro IS NULL OR NOT ({V.format(p='t.palabra')});
DELETE t FROM tmp_cu t LEFT JOIN miembro m ON m.id_miembro = t.id WHERE m.id_miembro IS NULL OR NOT ({V.format(p='t.palabra')});
-- inserta o actualiza solo cuotas intactas (pendientes, sin pago, sin abono). estado va al final: MySQL evalúa en orden
INSERT INTO cuota (id_miembro, anio, mes, estado, monto_fijo, monto_pagado, nota)
SELECT id, anio, mes, estado, fijo, pagado, nota FROM tmp_cu
ON DUPLICATE KEY UPDATE
  monto_fijo   = IF(cuota.estado = 'PENDIENTE' AND cuota.id_pago IS NULL AND cuota.abonado = 0, VALUES(monto_fijo), cuota.monto_fijo),
  monto_pagado = IF(cuota.estado = 'PENDIENTE' AND cuota.id_pago IS NULL AND cuota.abonado = 0, VALUES(monto_pagado), cuota.monto_pagado),
  nota         = IF(cuota.estado = 'PENDIENTE' AND cuota.id_pago IS NULL AND cuota.abonado = 0, VALUES(nota), cuota.nota),
  estado       = IF(cuota.estado = 'PENDIENTE' AND cuota.id_pago IS NULL AND cuota.abonado = 0, VALUES(estado), cuota.estado);
-- encargado de cobro (solo si no tiene uno)
SELECT CONCAT('NO COINCIDE cobrador: id ', t.id, ' / ', t.palabra) AS aviso FROM tmp_co t
  LEFT JOIN miembro m ON m.id_miembro = t.id LEFT JOIN miembro c ON c.id_miembro = t.cob
 WHERE m.id_miembro IS NULL OR c.id_miembro IS NULL OR NOT ({V.format(p='t.palabra')})
    OR NOT (CONCAT(c.nombre,' ',IFNULL(c.nombre_completo,'')) LIKE CONCAT('%', t.palabra_cob, '%'));
UPDATE miembro m JOIN tmp_co t ON t.id = m.id_miembro JOIN miembro c ON c.id_miembro = t.cob
   SET m.id_cobrador = t.cob
 WHERE m.id_cobrador IS NULL AND {V.format(p='t.palabra')} AND CONCAT(c.nombre,' ',IFNULL(c.nombre_completo,'')) LIKE CONCAT('%', t.palabra_cob, '%');
-- resumen
SELECT m.nombre, co.nombre AS cobra,
       SUM(c.estado = 'PAGADA') AS pagadas, SUM(c.estado = 'PENDIENTE' AND c.anio = 2026 AND c.mes < 11) AS debe_meses,
       SUM(IF(c.estado = 'PENDIENTE' AND (c.anio < 2026 OR c.mes < 11), c.monto_fijo - c.abonado, 0)) AS debe_soles
  FROM miembro m LEFT JOIN cuota c ON c.id_miembro = m.id_miembro LEFT JOIN miembro co ON co.id_miembro = m.id_cobrador
 WHERE m.id_miembro IN (SELECT id FROM tmp_cu UNION SELECT id FROM tmp_co) GROUP BY m.id_miembro ORDER BY debe_soles DESC, m.nombre;
DROP TEMPORARY TABLE tmp_cu; DROP TEMPORARY TABLE tmp_co;
COMMIT;""")
for a in avisos: print("-- AVISO:", a, file=sys.stderr)
print(f"-- {len(filas)} cuotas, {len(cob)} encargados", file=sys.stderr)
