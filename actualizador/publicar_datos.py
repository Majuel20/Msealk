"""Extrae en temporal; valida antes de reemplazar los archivos publicados."""
from __future__ import annotations

import argparse
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile

REPOSITORIO = Path(__file__).resolve().parents[1]


def validar_json(path: Path, permitir_vacio: bool = False) -> dict:
    contenido = json.loads(path.read_text(encoding="utf-8-sig"))
    if not isinstance(contenido, dict) or not isinstance(contenido.get("cortes"), list):
        raise ValueError("El JSON no contiene la lista de cortes esperada.")
    cortes = contenido["cortes"]
    if contenido.get("total") != len(cortes):
        raise ValueError("El total no coincide con la cantidad de cortes.")
    metadata = contenido.get("metadata")
    if not isinstance(metadata, dict) or not metadata.get("fecha_ejecucion"):
        raise ValueError("Falta la fecha de extracción.")
    if not metadata.get("metodo_ids") or int(metadata.get("ids_detectados", 0)) <= 0:
        raise ValueError("No se confirmó la detección de avisos en SEAL; se conservan los datos publicados.")
    if int(metadata.get("eventos_totales_extraidos", 0)) <= 0:
        raise ValueError("No se pudo extraer ningún aviso; se conservan los datos publicados.")
    if not cortes and not permitir_vacio:
        raise ValueError("Resultado vacío: revisar SEAL y ejecutar manualmente con permitir_vacio si corresponde.")
    for corte in cortes:
        if not isinstance(corte, dict) or not str(corte.get("id", "")).strip() or not corte.get("fecha"):
            raise ValueError("Hay un corte sin identificador o fecha.")
    return contenido


def publicar(origen: Path, destino: Path, permitir_vacio: bool = False) -> int:
    contenido = validar_json(origen / "cortes_seal_latest.json", permitir_vacio)
    archivos = {}
    # Leer todos antes de modificar cualquier archivo publicado.
    for extension in ("json", "csv", "txt", "xlsx"):
        source = origen / f"cortes_seal_latest.{extension}"
        if not source.is_file() or not source.stat().st_size:
            raise ValueError(f"Falta el resultado {source.name}; no se publicará una extracción incompleta.")
        archivos[f"cortes_programados.{extension}"] = source.read_bytes()
    destino.mkdir(parents=True, exist_ok=True)
    for nombre, contenido_bytes in archivos.items():
        with tempfile.NamedTemporaryFile(dir=destino, prefix=".pendiente-", delete=False) as temporal:
            temporal.write(contenido_bytes)
            temporal_path = Path(temporal.name)
        try:
            os.replace(temporal_path, destino / nombre)
        finally:
            temporal_path.unlink(missing_ok=True)
    return len(contenido["cortes"])


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--validar", type=Path, help="Solo valida un JSON; no extrae ni publica.")
    parser.add_argument("--permitir-vacio", action="store_true")
    parser.add_argument("--destino", type=Path, default=REPOSITORIO / "datos")
    args = parser.parse_args()
    if args.validar:
        data = validar_json(args.validar, args.permitir_vacio)
        print(f"JSON válido: {len(data['cortes'])} cortes.")
        return
    with tempfile.TemporaryDirectory(prefix="msealk-extraccion-") as temporal:
        try:
            subprocess.run([
                sys.executable, str(Path(__file__).with_name("extraer_cortes.py")),
                "--output-dir", temporal, "--dias-atras", "1", "--dias-adelante", "30", "--no-html",
            ], check=True, cwd=REPOSITORIO)
            cantidad = publicar(Path(temporal), args.destino.resolve(), args.permitir_vacio)
        except Exception:
            # Mostrar el diagnóstico antes de que TemporaryDirectory lo elimine.
            print("\nDIAGNÓSTICO DE SEAL: se conservan los datos publicados.", flush=True)
            for registro in sorted(Path(temporal).glob("extraccion_*.log")):
                print(registro.read_text(encoding="utf-8", errors="replace"), flush=True)
            raise
        print(f"Publicación preparada: {cantidad} cortes en {args.destino}.")


if __name__ == "__main__":
    main()
