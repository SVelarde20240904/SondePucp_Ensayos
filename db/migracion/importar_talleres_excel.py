"""
Carga el historial del team Talleres desde su Excel (CRONOGRAMAS_2026-2.xlsx):
  - DICTADO DE NIVELES  -> talleres del ciclo y quién fue tallerista (TP/TB/TI/TR), apoyo (AP) o maestro (MT)
  - REGISTRO PASOS      -> pasos enseñados por nivel en cada taller (y el catálogo de pasos por nivel)
  - FEEDBACK RAM        -> feedback entre compañeros de dictado
  - PLANTILLA FLYER     -> cronograma del taller más reciente (bloques por nivel, talleristas, maestro, música)

Uso:  python importar_talleres_excel.py CRONOGRAMAS_2026-2.xlsx > actualizacion_talleres.sql
Es idempotente: se puede cargar dos veces sin duplicar (talleres por fecha; personas, pasos y feedback por contenido).
Requiere: pip install openpyxl
"""
import sys, datetime as dt, unicodedata, re
import openpyxl

NIVEL_CODIGO = {"TP": "PRINCIPIANTE", "TB": "BASICO", "TI": "INTRO_RUEDA", "TR": "RUEDA_BASICA"}
NIVEL_TITULO = {"principiantes": "PRINCIPIANTE", "principiante": "PRINCIPIANTE", "basico": "BASICO", "intro a la rueda": "INTRO_RUEDA",
                "intro rueda": "INTRO_RUEDA", "rueda basica": "RUEDA_BASICA", "nivel principiante": "PRINCIPIANTE", "nivel basico": "BASICO"}
NO_SON_PASOS = {"filtro", "secuencia", "musica"}
# nombres del Excel -> nombre en el sistema (los que no coinciden solos)
ALIAS = {"alessia": "Alessia Leon", "alex": "Alexander Gil", "alvaro": "Alvaro Camposano", "andre": "André La Hoz", "angela": "Angela Yanac",
         "angelita": "Angela Yanac", "ariana mercedes": "Ariana David", "cielo": "Cielo Camayo", "daniela lisseth": "Daniela Llano",
         "daniela": "Daniela Llano", "deyvid": "Deyvid Usco", "erich vercelli": "Erick Vercelli", "esteban": "Esteban Gutiérrez",
         "enmanuel (ilo)": "Enmanuel Vega", "ilo": "Enmanuel Vega", "jose perez": "José Pérez", "mafer alvarez": "Maria Fernanda Alvarez",
         "maricielo": "Maricielo Mamani", "mauricio": "Mauricio Halanoca", "silvia del valle": "Silvia Valle", "silvia": "Silvia Valle",
         "haniel": "Haniel Ghiggo", "antony": "Antony Palacios", "anto": "Antony Palacios", "diana": "Diana Levano", "sergio": "Sergio Velarde",
         "claudia": "Claudia Alonso", "clau": "Claudia Alonso", "pedro": "Pedro Chero", "abigail": "Abigail Delgado", "natalia": "Natalia Vidal",
         "geraldine": "Xiomara Ames", "pierina": "Pierina Perales", "christian": "Christian Lescano"}


def norm(s):
    s = unicodedata.normalize("NFKD", str(s)).encode("ascii", "ignore").decode()
    return " ".join(s.lower().replace("\n", " ").split())


def q(v):
    return "NULL" if v is None else "'" + str(v).strip().replace("\\", "\\\\").replace("'", "''") + "'"


def miembro(nombre):
    n = norm(nombre)
    return ALIAS.get(n, " ".join(w.capitalize() if w.islower() else w for w in str(nombre).strip().split()))


def mid(nombre):
    return f"(SELECT id_miembro FROM miembro WHERE nombre={q(miembro(nombre))})"


def paso_txt(v):
    if v is None: return None
    if isinstance(v, float): v = int(v) if v == int(v) else v
    s = " ".join(str(v).split())
    return s or None


def hora_txt(v):
    if v is None: return None
    if isinstance(v, dt.time): return f"{v.hour}:{v.minute:02d}"
    if isinstance(v, dt.datetime): return f"{v.hour}:{v.minute:02d}"
    return str(v).strip()


def dur_txt(v):
    if v is None: return None
    if isinstance(v, (int, float)): return f"{int(v)}'"
    return str(v).strip().replace("''", "'")


def ciclo(d): return f"{d.year}-{1 if d.month <= 7 else 2}"


def main(xlsx):
    wb = openpyxl.load_workbook(xlsx, data_only=True)
    out = []
    w = out.append
    w("-- =====================================================================")
    w(f"--  Historial del team Talleres (generado de {xlsx.replace(chr(92), '/').split('/')[-1]} · {dt.date.today()})")
    w("--  Idempotente: si se carga dos veces no duplica nada.")
    w("-- =====================================================================")
    w("SET NAMES utf8mb4;")
    w("START TRANSACTION;")

    # ---------------- DICTADO DE NIVELES ----------------
    ws = wb["DICTADO DE NIVELES"]
    fechas = [c.value.date() if isinstance(c.value, dt.datetime) else None for c in ws[1]][1:]
    filas = []
    for row in ws.iter_rows(min_row=2, values_only=True):
        if not row[0] or norm(row[0]) == "leyenda": break
        filas.append(row)
    # pasos por fecha (misma posición de columna)
    rp = wb["REGISTRO PASOS"]
    pasos = {}   # fecha -> nivel -> [(seccion, nombre)]
    nivel, seccion = None, 1
    for r in range(1, rp.max_row + 1):
        cab = rp.cell(r, 1).value
        if cab and norm(cab) in NIVEL_TITULO: nivel, seccion = NIVEL_TITULO[norm(cab)], 1; continue
        if cab and norm(cab).startswith("musica"): seccion += 1; continue
        if not nivel: continue
        for i, f in enumerate(fechas):
            v = paso_txt(rp.cell(r, i + 2).value)
            if f and v and norm(v) not in NO_SON_PASOS:
                pasos.setdefault(f, {}).setdefault(nivel, []).append((seccion, v))
    # un taller por fecha que tenga dictado o pasos
    con_datos = [f for i, f in enumerate(fechas) if f and (any(str(r[i + 1] or "").strip().lower() not in ("", "x") for r in filas) or f in pasos)]
    numero = {f: k + 1 for k, f in enumerate(sorted(con_datos))}
    codigos = {}   # (fecha, nombre sistema) -> código
    w("\n-- Talleres del ciclo (uno por fecha)")
    for f in sorted(con_datos):
        w(f"INSERT IGNORE INTO taller (ciclo, numero, fecha, hora_inicio, hora_fin, estado, final_fecha, notas) VALUES "
          f"('{ciclo(f)}', {numero[f]}, '{f}', '19:30:00', '21:05:00', 'REALIZADO', '{f} 21:05:00', 'Cargado del Excel del team (DICTADO DE NIVELES)');")
    w("\n-- Dictado (TP/TB/TI/TR = tallerista del nivel · AP = apoyo · MT = maestro de taller)")
    sin = set()
    for row in filas:
        nom = miembro(row[0])
        for i, f in enumerate(fechas):
            if f not in numero: continue
            cod = str(row[i + 1] or "").strip().upper()
            if cod in ("", "X"): continue
            codigos[(f, nom)] = cod
            if cod in NIVEL_CODIGO: fun, niv = "TALLERISTA", NIVEL_CODIGO[cod]
            elif cod == "AP": fun, niv = "APOYO", None
            elif cod == "MT": fun, niv = "MAESTRO", None
            else: sin.add(cod); continue
            w(f"INSERT INTO taller_persona (id_taller, id_miembro, funcion, nivel, planeado) SELECT t.id_taller, m.id_miembro, '{fun}', {q(niv)}, 1 "
              f"FROM taller t JOIN miembro m ON m.nombre={q(nom)} WHERE t.fecha='{f}' AND NOT EXISTS (SELECT 1 FROM taller_persona p WHERE p.id_taller=t.id_taller "
              f"AND p.id_miembro=m.id_miembro AND p.funcion='{fun}' AND p.nivel <=> {q(niv)});")
    if sin: w("-- códigos que no se entendieron: " + ", ".join(sorted(sin)))

    # ---------------- PASOS ----------------
    w("\n-- Catálogo de pasos por nivel y pasos enseñados en cada taller")
    for f in sorted(pasos):
        if f not in numero: continue
        for niv, lista in pasos[f].items():
            for orden, (sec, nombre) in enumerate(lista, 1):
                w(f"INSERT IGNORE INTO paso_taller (nombre, nivel) VALUES ({q(nombre)}, '{niv}');")
                w(f"INSERT IGNORE INTO taller_paso (id_taller, nivel, id_paso, orden, seccion, planificado, ensenado) SELECT t.id_taller, '{niv}', p.id_paso, {orden}, {sec}, 1, 1 "
                  f"FROM taller t JOIN paso_taller p ON p.nivel='{niv}' AND p.nombre={q(nombre)} WHERE t.fecha='{f}';")

    # ---------------- FEEDBACK RAM ----------------
    fb = wb["FEEDBACK RAM"]
    w("\n-- Feedback entre compañeros de dictado")
    n_fb = 0
    for r in range(1, fb.max_row + 1):
        # fila de destinatarios: nombres en las columnas 2, 5, 8, 11
        if not any(fb.cell(r, c).value for c in (2, 5, 8, 11)) or fb.cell(r, 1).value is not None: continue
        if not all(isinstance(fb.cell(r, c).value, str) and len(fb.cell(r, c).value) < 25 for c in (2, 5, 8, 11) if fb.cell(r, c).value): continue
        destinos = {c: fb.cell(r, c).value for c in (2, 5, 8, 11) if fb.cell(r, c).value}
        rr = r + 1
        while rr <= fb.max_row and not (fb.cell(rr, 2).value and isinstance(fb.cell(rr, 2).value, str) and len(fb.cell(rr, 2).value) < 25 and fb.cell(rr, 1).value is None):
            for c, para in destinos.items():
                txt, fec = fb.cell(rr, c).value, fb.cell(rr, c - 1).value
                if not txt or not isinstance(fec, dt.datetime): continue
                m = re.match(r"\s*(?:De\s+)?([^:]{2,25}):\s*(.*)", str(txt), re.S)
                if not m: continue
                de, cuerpo = m.group(1).strip(), m.group(2).strip()
                f = fec.date()
                tipo = "APOYO" if codigos.get((f, miembro(de))) == "AP" else "COMPANERO"
                w(f"INSERT INTO taller_feedback (id_taller, de_miembro, para_miembro, tipo, texto, fecha) SELECT t.id_taller, {mid(de)}, {mid(para)}, '{tipo}', {q(cuerpo)}, '{f} 23:00:00' "
                  f"FROM taller t WHERE t.fecha='{f}' AND {mid(de)} IS NOT NULL AND {mid(para)} IS NOT NULL AND NOT EXISTS (SELECT 1 FROM taller_feedback x WHERE x.id_taller=t.id_taller "
                  f"AND x.de_miembro={mid(de)} AND x.para_miembro={mid(para)} AND LEFT(x.texto,120)=LEFT({q(cuerpo)},120));")
                w(f"INSERT IGNORE INTO taller_respuesta (id_taller, id_miembro, fecha) SELECT t.id_taller, {mid(de)}, '{f} 23:00:00' FROM taller t WHERE t.fecha='{f}' AND {mid(de)} IS NOT NULL;")
                n_fb += 1
            rr += 1
    w(f"-- {n_fb} feedbacks")

    # ---------------- PLANTILLA FLYER (taller más reciente) ----------------
    pf = wb["PLANTILLA FLYER"]
    fecha_fl, titulo_fl = None, None
    for row in pf.iter_rows(min_row=1, max_row=6):
        for c in row:
            if isinstance(c.value, str) and "TALLER SDP" in c.value.upper(): titulo_fl = c.value
            if isinstance(c.value, dt.datetime): fecha_fl = c.value.date()
    if fecha_fl:
        num = re.match(r"\s*(\d+)", titulo_fl or "")
        w(f"\n-- Cronograma del {titulo_fl or 'taller'} ({fecha_fl})")
        w(f"INSERT IGNORE INTO taller (ciclo, numero, fecha, hora_inicio, hora_fin, estado, notas) VALUES ('{ciclo(fecha_fl)}', {num.group(1) if num else 'NULL'}, "
          f"'{fecha_fl}', '19:30:00', '21:05:00', 'PROGRAMADO', 'Cronograma cargado de la PLANTILLA FLYER');")
        cols = {"PRINCIPIANTE": 2, "BASICO": 6, "INTRO_RUEDA": 10, "RUEDA_BASICA": 14}   # B, F, J, N (hora, actividad, duración)
        for niv, c0 in cols.items():
            bloques = []
            for r in range(6, pf.max_row + 1):
                h, a, d = pf.cell(r, c0).value, pf.cell(r, c0 + 1).value, pf.cell(r, c0 + 2).value
                if a is None: continue
                if str(h).strip().upper() == "HORA": bloques.append((None, "— " + " ".join(str(a).split()) + " —", None)); continue
                bloques.append((hora_txt(h), " ".join(str(a).replace("\n", " · ").split()), dur_txt(d)))
            w(f"-- {niv}: {len(bloques)} bloques (solo si el taller aún no tiene cronograma)")
            for o, (h, a, d) in enumerate(bloques, 1):
                w(f"INSERT INTO taller_bloque (id_taller, nivel, orden, hora, actividad, duracion) SELECT t.id_taller, '{niv}', {o}, {q(h)}, {q(a[:300])}, {q(d)} FROM taller t "
                  f"WHERE t.fecha='{fecha_fl}' AND NOT EXISTS (SELECT 1 FROM taller_bloque b WHERE b.id_taller=t.id_taller AND b.nivel='{niv}' AND b.orden={o});")
        # distribución por niveles (columnas R:S)
        actual, otros = None, []
        for r in range(5, 20):
            nv, nom = pf.cell(r, 18).value, pf.cell(r, 19).value
            if isinstance(nv, str):
                t = norm(nv)
                if t.startswith("maestr"): otros.append(("MAESTRO", nv.split(":", 1)[1].strip()))
                elif t.startswith("encargado de musica"): w(f"UPDATE taller SET musica_texto={q(nv.split(':', 1)[1].strip())} WHERE fecha='{fecha_fl}' AND musica_texto IS NULL;")
                elif "/" in t: actual = None; w(f"-- {nv}: talleristas sin nivel definido en el Excel -> asignarlos en 'Editar cronograma'")
                else: actual = NIVEL_TITULO.get(t, NIVEL_TITULO.get("nivel " + t))
            if nom and actual:
                w(f"INSERT INTO taller_persona (id_taller, id_miembro, funcion, nivel, planeado) SELECT t.id_taller, m.id_miembro, 'TALLERISTA', '{actual}', 1 FROM taller t "
                  f"JOIN miembro m ON m.nombre={q(miembro(nom))} WHERE t.fecha='{fecha_fl}' AND NOT EXISTS (SELECT 1 FROM taller_persona p WHERE p.id_taller=t.id_taller "
                  f"AND p.id_miembro=m.id_miembro AND p.funcion='TALLERISTA');")
            elif nom: w(f"--    {nom}")
        for fun, nom in otros:
            w(f"INSERT INTO taller_persona (id_taller, id_miembro, funcion, planeado) SELECT t.id_taller, m.id_miembro, '{fun}', 1 FROM taller t JOIN miembro m ON m.nombre={q(miembro(nom))} "
              f"WHERE t.fecha='{fecha_fl}' AND NOT EXISTS (SELECT 1 FROM taller_persona p WHERE p.id_taller=t.id_taller AND p.id_miembro=m.id_miembro AND p.funcion='{fun}');")

    # nombres que no existen en la BD
    w("\n-- Aviso: personas del Excel que no se encontraron en la BD (no se cargaron)")
    nombres = sorted({miembro(r[0]) for r in filas})
    w("SELECT CONCAT('NO ENCONTRADO: ', x.n) AS aviso FROM (" + " UNION ".join(f"SELECT {q(n)} AS n" for n in nombres) + ") x WHERE x.n NOT IN (SELECT nombre FROM miembro);")
    w("SELECT CONCAT(COUNT(*), ' talleres') AS resumen FROM taller UNION ALL SELECT CONCAT(COUNT(*), ' personas en dictado') FROM taller_persona "
      "UNION ALL SELECT CONCAT(COUNT(*), ' pasos en el catálogo') FROM paso_taller UNION ALL SELECT CONCAT(COUNT(*), ' pasos registrados en talleres') FROM taller_paso "
      "UNION ALL SELECT CONCAT(COUNT(*), ' feedbacks') FROM taller_feedback;")
    w("COMMIT;")
    print("\n".join(out))


if __name__ == "__main__":
    main(sys.argv[1])
