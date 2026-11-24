#!/usr/bin/env python3
"""Valida e integra UNA entrega ZIP en el repositorio (directorio actual).

Formato del ZIP:
  MANIFEST.json   (obligatorio)
  files/...       (archivos nuevos o modificados, con su ruta dentro del repo)

MANIFEST.json:
  {
    "entrega": 2,                 # entero, siempre ultima+1 (la base es 1)
    "version": "0.2.0",           # x.y.z, siempre mayor que la anterior
    "tipo": "incremental",        # "base" | "incremental"
    "titulo": "Texto corto",
    "changelog": {"anadido": [], "cambiado": [], "corregido": []},
    "eliminar": ["ruta/archivo"], # opcional, eliminaciones autorizadas
    "renombrar": [{"de": "a", "a": "b"}]  # opcional
  }
Sale con codigo != 0 ante cualquier problema; no deja nada commiteado.
"""
import datetime
import fnmatch
import hashlib
import json
import os
import re
import shutil
import stat
import subprocess
import sys
import zipfile
from pathlib import PurePosixPath

LEDGER = "deliveries/ledger.json"
MAX_FILES = 20000
MAX_TOTAL = 400 * 1024 * 1024
FORBIDDEN_PREFIXES = (".git/", ".github/", "deliveries/")
PROTECTED_DELETE = (".github", "ci", "deliveries", "LICENSE.md", "LICENSE")
FORBIDDEN_NAMES = ("*.jks", "*.keystore", "*.p12", "*.pfx", "*.pem", "*.key",
                   "keystore.properties", "local.properties", ".env", ".env.*")
SEMVER = re.compile(r"^\d+\.\d+\.\d+$")
AUTO_START, AUTO_END = "<!-- AUTO:START -->", "<!-- AUTO:END -->"


def fail(msg):
    print(f"::error::{msg}")
    sys.exit(1)


def safe_rel(path, what):
    if not path or "\x00" in path or "\\" in path or path.startswith("/") or re.match(r"^[A-Za-z]:", path):
        fail(f"Ruta peligrosa en {what}: {path!r}")
    parts = PurePosixPath(path).parts
    if ".." in parts or "." in parts:
        fail(f"Ruta peligrosa en {what}: {path!r}")
    return "/".join(parts)


def check_name(rel, what):
    if any(rel == p.rstrip("/") or rel.startswith(p) for p in FORBIDDEN_PREFIXES):
        fail(f"{what} toca una zona protegida ({rel}). Los workflows de .github/ se editan a mano en GitHub "
             "(el token de Actions no puede modificarlos) y deliveries/ lo gestiona el sistema.")
    base = rel.rsplit("/", 1)[-1]
    if any(fnmatch.fnmatch(base, pat) for pat in FORBIDDEN_NAMES):
        fail(f"{what} contiene un archivo de credenciales/local no permitido: {rel}")


def sha256_file(p):
    h = hashlib.sha256()
    with open(p, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def read_json(path, default):
    if not os.path.exists(path):
        return default
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def vtuple(v):
    return tuple(int(x) for x in v.split("."))


def set_output(k, v):
    line = f"{k}={v}"
    out = os.environ.get("GITHUB_OUTPUT")
    if out:
        with open(out, "a", encoding="utf-8") as f:
            f.write(line + "\n")
    print("  salida:", line)


def bullets(items):
    return "\n".join(f"- {i}" for i in items) if items else ""


def main():
    if len(sys.argv) != 2:
        fail("Uso: apply_delivery.py <entrega.zip>")
    zpath = sys.argv[1]
    if not os.path.isfile(zpath):
        fail(f"No existe {zpath}")
    zsha = sha256_file(zpath)

    # ---- 1. ZIP valido
    try:
        zf = zipfile.ZipFile(zpath)
    except zipfile.BadZipFile:
        fail("El ZIP esta corrupto o no es un ZIP valido.")
    bad = zf.testzip()
    if bad:
        fail(f"ZIP corrupto: fallo CRC en {bad}")
    infos = zf.infolist()
    if len(infos) > MAX_FILES:
        fail("El ZIP tiene demasiados archivos.")
    if sum(i.file_size for i in infos) > MAX_TOTAL:
        fail("El ZIP descomprimido supera el limite de tamaño.")

    # ---- 2. Manifiesto
    if "MANIFEST.json" not in zf.namelist():
        fail("Falta MANIFEST.json en la raiz del ZIP.")
    try:
        m = json.loads(zf.read("MANIFEST.json").decode("utf-8"))
    except Exception as e:
        fail(f"MANIFEST.json no es JSON valido: {e}")
    for k in ("entrega", "version", "tipo", "titulo"):
        if k not in m:
            fail(f"MANIFEST.json: falta el campo '{k}'")
    entrega, version, tipo, titulo = m["entrega"], str(m["version"]), m["tipo"], str(m["titulo"]).strip()
    if not isinstance(entrega, int) or entrega < 1:
        fail("MANIFEST.json: 'entrega' debe ser un entero >= 1")
    if not SEMVER.match(version):
        fail("MANIFEST.json: 'version' debe tener formato x.y.z")
    if tipo not in ("base", "incremental"):
        fail("MANIFEST.json: 'tipo' debe ser 'base' o 'incremental'")
    if not titulo or len(titulo) > 120 or "\n" in titulo:
        fail("MANIFEST.json: 'titulo' vacio, multilinea o demasiado largo")
    cl = m.get("changelog", {}) or {}
    anadido, cambiado, corregido = (list(cl.get(k, []) or []) for k in ("anadido", "cambiado", "corregido"))
    if not (anadido or cambiado or corregido):
        fail("MANIFEST.json: 'changelog' no puede estar vacio (no se publican versiones sin cambios descritos).")
    eliminar = [safe_rel(p, "eliminar") for p in (m.get("eliminar") or [])]
    renombrar = [(safe_rel(r["de"], "renombrar.de"), safe_rel(r["a"], "renombrar.a")) for r in (m.get("renombrar") or [])]

    # ---- 3. Duplicados y orden (libro de registro)
    ledger = read_json(LEDGER, [])
    if any(e.get("sha256") == zsha for e in ledger):
        fail("Esta entrega (mismo contenido) ya fue procesada antes.")
    if any(e.get("entrega") == entrega for e in ledger):
        fail(f"Ya existe una entrega {entrega:03d} procesada. Cada entrega lleva un numero nuevo.")
    if tipo == "base":
        if ledger or entrega != 1:
            fail("La entrega 'base' solo puede ser la numero 1 en un repositorio sin entregas previas.")
    else:
        if not ledger:
            fail("Aun no hay entrega base procesada. Sube primero la entrega 001 (tipo base).")
        last = max(ledger, key=lambda e: e["entrega"])
        if entrega != last["entrega"] + 1:
            fail(f"Orden incorrecto: la ultima entrega fue la {last['entrega']:03d}; esta debe ser la {last['entrega'] + 1:03d}.")
        if vtuple(version) <= vtuple(last["version"]):
            fail(f"La version {version} debe ser mayor que la anterior ({last['version']}).")
    tag = f"v{version}"
    tags = subprocess.run(["git", "tag", "-l", tag], capture_output=True, text=True).stdout.split()
    if tag in tags:
        fail(f"La etiqueta {tag} ya existe; no se reutilizan versiones.")

    # ---- 4. Validar contenido
    payload = {}
    for i in infos:
        n = i.filename
        if n == "MANIFEST.json" or n.endswith("/"):
            continue
        mode = (i.external_attr >> 16) & 0xFFFF
        if stat.S_ISLNK(mode):
            fail(f"Enlace simbolico no permitido en el ZIP: {n}")
        if n.startswith("files/"):
            rel = safe_rel(n[len("files/"):], "archivo del ZIP")
            check_name(rel, "La entrega")
            if rel == "CHANGELOG.md":
                fail("No incluyas CHANGELOG.md: lo genera el sistema a partir del campo 'changelog' del manifiesto.")
            payload[rel] = i
        elif n.startswith("__MACOSX/") or n.endswith(".DS_Store"):
            continue
        elif n in ("LEEME_ENTREGA.md",):
            continue
        else:
            fail(f"Archivo inesperado en el ZIP: {n} (solo MANIFEST.json, LEEME_ENTREGA.md y files/...)")
    if not payload and not eliminar and not renombrar:
        fail("La entrega no contiene ningun cambio.")
    for p in eliminar + [x for r in renombrar for x in r]:
        check_name(p, "El manifiesto")
    for p in eliminar:
        if p in PROTECTED_DELETE or any(p.startswith(x + "/") for x in PROTECTED_DELETE):
            fail(f"No se permite eliminar zona protegida: {p}")
        if not os.path.exists(p):
            fail(f"'eliminar' apunta a algo que no existe: {p}")
    for de, a in renombrar:
        if not os.path.exists(de):
            fail(f"'renombrar.de' no existe: {de}")
        if os.path.exists(a) or a in payload:
            fail(f"'renombrar.a' ya existe: {a}")
    clash = set(eliminar) & set(payload)
    if clash:
        fail(f"Conflicto: se elimina y se añade a la vez: {sorted(clash)[:3]}")

    # ---- 5. Aplicar (solo en el arbol de trabajo; no se commitea hasta compilar bien)
    nuevos, modificados, borrados = [], [], []
    for de, a in renombrar:
        os.makedirs(os.path.dirname(a) or ".", exist_ok=True)
        shutil.move(de, a)
        borrados.append(f"{de} -> {a}")
    for p in eliminar:
        if os.path.isdir(p):
            shutil.rmtree(p)
        else:
            os.remove(p)
        borrados.append(p)
    root = os.path.realpath(".")
    for rel, info in sorted(payload.items()):
        dest = os.path.realpath(rel)
        if not dest.startswith(root + os.sep):
            fail(f"Ruta fuera del repositorio: {rel}")
        data = zf.read(info)
        existed = os.path.isfile(dest)
        if existed:
            with open(dest, "rb") as f:
                if f.read() == data:
                    continue
        elif os.path.isdir(dest):
            fail(f"Se intenta escribir un archivo donde hay una carpeta: {rel}")
        os.makedirs(os.path.dirname(dest), exist_ok=True)
        with open(dest, "wb") as f:
            f.write(data)
        if rel in ("gradlew", "ci/apply_delivery.py") or rel.endswith(".sh"):
            os.chmod(dest, 0o755)
        (modificados if existed else nuevos).append(rel)

    # ---- 6. Configuracion de la app (viene de la base)
    app = read_json("ci/app.json", None)
    if not app:
        fail("Falta ci/app.json (nombre de la app, tarea Gradle y carpeta de APK). Lo trae la entrega base.")
    for k in ("app_name", "gradle_task", "apk_dir"):
        if not app.get(k):
            fail(f"ci/app.json: falta '{k}'")

    # ---- 7. Documentacion: CHANGELOG, PROJECT_STATE, registro
    hoy = datetime.date.today().isoformat()
    ahora = datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%d %H:%M UTC")
    secciones = [("Añadido", anadido), ("Cambiado", cambiado), ("Corregido", corregido)]
    entry = f"## [{version}] - {hoy} (entrega {entrega:03d}) — {titulo}\n\n"
    for nombre, items in secciones:
        if items:
            entry += f"### {nombre}\n{bullets(items)}\n\n"
    cur = open("CHANGELOG.md", encoding="utf-8").read() if os.path.exists("CHANGELOG.md") else ""
    head = "# Changelog\n\n"
    body = cur[len(head):] if cur.startswith(head) else cur
    with open("CHANGELOG.md", "w", encoding="utf-8") as f:
        f.write(head + entry + body)

    block = (f"{AUTO_START}\n- Última entrega: {entrega:03d} — {titulo}\n- Última versión: {version} (`{tag}`)\n"
             f"- Integrada: {ahora}\n{AUTO_END}")
    st = open("PROJECT_STATE.md", encoding="utf-8").read() if os.path.exists("PROJECT_STATE.md") else "# Estado del proyecto\n"
    if AUTO_START in st and AUTO_END in st:
        st = re.sub(re.escape(AUTO_START) + r".*?" + re.escape(AUTO_END), lambda _: block, st, flags=re.S)
    else:
        st = st.rstrip() + "\n\n" + block + "\n"
    with open("PROJECT_STATE.md", "w", encoding="utf-8") as f:
        f.write(st)

    os.makedirs("deliveries/processed", exist_ok=True)
    os.makedirs("deliveries/inbox", exist_ok=True)
    dest_zip = f"deliveries/processed/{os.path.basename(zpath)}"
    if os.path.exists(dest_zip):
        dest_zip = f"deliveries/processed/{entrega:03d}-{zsha[:8]}.zip"
    shutil.move(zpath, dest_zip)
    open("deliveries/inbox/.gitkeep", "a").close()
    ledger.append({"entrega": entrega, "version": version, "tag": tag, "tipo": tipo, "titulo": titulo,
                   "sha256": zsha, "fecha": ahora, "archivo": dest_zip})
    with open(LEDGER, "w", encoding="utf-8") as f:
        json.dump(ledger, f, ensure_ascii=False, indent=2)
        f.write("\n")

    # ---- 8. Notas de la Release
    def lista(titulo_, arr):
        if not arr:
            return ""
        extra = f"\n- … y {len(arr) - 40} más" if len(arr) > 40 else ""
        return f"\n<details><summary>{titulo_} ({len(arr)})</summary>\n\n" + bullets([f"`{x}`" for x in arr[:40]]) + extra + "\n\n</details>\n"

    notes = f"# {app['app_name']} {version}\n\n- **Versión:** {version} (entrega {entrega:03d})\n- **Fecha de compilación:** {ahora}\n\n"
    for nombre, items in (("Funciones añadidas", anadido), ("Cambios", cambiado), ("Correcciones", corregido)):
        if items:
            notes += f"## {nombre}\n{bullets(items)}\n\n"
    if tipo == "base":
        notes += f"## Archivos de esta entrega\nBase inicial: {len(nuevos)} archivos.\n"
    else:
        notes += "## Archivos de esta entrega\n" + lista("Nuevos", nuevos) + lista("Modificados", modificados) + lista("Eliminados/renombrados", borrados)
    notes += "\n## Instalación\nDescarga la APK adecuada (arm64-v8a para casi todos los móviles actuales; `universal` si dudas). Están firmadas con la misma clave en todas las versiones; verifica con `SHA256SUMS.txt`.\n"
    tmp = os.environ.get("RUNNER_TEMP", "/tmp")
    with open(os.path.join(tmp, "release_notes.md"), "w", encoding="utf-8") as f:
        f.write(notes)

    print(f"Entrega {entrega:03d} v{version} integrada: {len(nuevos)} nuevos, {len(modificados)} modificados, {len(borrados)} eliminados.")
    set_output("version", version)
    set_output("version_code", entrega)
    set_output("tag", tag)
    set_output("titulo", titulo)
    set_output("release_name", f"{app['app_name']} {version}")
    set_output("gradle_task", app["gradle_task"])
    set_output("apk_dir", app["apk_dir"])


if __name__ == "__main__":
    main()
