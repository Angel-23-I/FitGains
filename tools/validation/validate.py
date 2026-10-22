"""Fase 1: compara proveedores de IA con fotos de comidas cuyo peso conoces.

Uso:
  1. Pon las fotos en tools/validation/photos/
  2. Rellena tools/validation/ground_truth.csv (file,real_grams,real_kcal)
  3. Crea tools/validation/.env con GEMINI_API_KEY y/o ANTHROPIC_API_KEY
  4. pip install -r tools/validation/requirements.txt
  5. python tools/validation/validate.py [--providers gemini,claude]

Escribe results.csv con lo estimado por cada proveedor y muestra el error medio.
Las kcal aquí son las del propio modelo (sin USDA), para medir solo la visión.
"""
import argparse
import base64
import csv
import json
import os
import statistics
import time
from pathlib import Path

import anthropic
import requests
from dotenv import load_dotenv
from pydantic import BaseModel

HERE = Path(__file__).parent
load_dotenv(HERE / ".env")

PROMPT = """Eres un nutricionista que analiza fotos de comida.
Identifica cada alimento visible por separado y estima los gramos de cada porción
usando como referencia el plato, los cubiertos o las manos.
Da también las kcal totales de cada alimento para esa porción.
confidence va de 0 a 1. Si no hay comida, devuelve items vacío."""


class Item(BaseModel):
    name: str
    grams: float
    kcal: float
    confidence: float


class Detection(BaseModel):
    items: list[Item]


def claude(image_b64: str, mime: str) -> Detection:
    client = anthropic.Anthropic()
    response = client.messages.parse(
        model=os.getenv("CLAUDE_MODEL", "claude-opus-5-5"),
        max_tokens=16000,
        output_config={"effort": "low"},
        messages=[{
            "role": "user",
            "content": [
                {"type": "image", "source": {"type": "base64", "media_type": mime, "data": image_b64}},
                {"type": "text", "text": PROMPT},
            ],
        }],
        output_format=Detection,
    )
    if response.parsed_output is None:
        raise RuntimeError(f"sin resultado ({response.stop_reason})")
    return response.parsed_output


def gemini(image_b64: str, mime: str) -> Detection:
    model = os.getenv("GEMINI_MODEL", "gemini-2.5-flash")
    num = {"type": "NUMBER"}
    schema = {
        "type": "OBJECT",
        "properties": {"items": {"type": "ARRAY", "items": {
            "type": "OBJECT",
            "properties": {"name": {"type": "STRING"}, "grams": num, "kcal": num, "confidence": num},
            "required": ["name", "grams", "kcal", "confidence"],
        }}},
        "required": ["items"],
    }
    res = requests.post(
        f"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent",
        headers={"x-goog-api-key": os.environ["GEMINI_API_KEY"]},
        json={
            "contents": [{"parts": [{"inline_data": {"mime_type": mime, "data": image_b64}}, {"text": PROMPT}]}],
            "generationConfig": {"responseMimeType": "application/json", "responseSchema": schema},
        },
        timeout=120,
    )
    res.raise_for_status()
    text = res.json()["candidates"][0]["content"]["parts"][0]["text"]
    return Detection.model_validate(json.loads(text))


PROVIDERS = {"gemini": gemini, "claude": claude}


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--providers", default="gemini,claude")
    args = parser.parse_args()
    providers = [p.strip() for p in args.providers.split(",") if p.strip()]

    with open(HERE / "ground_truth.csv", newline="", encoding="utf-8") as f:
        truth = [r for r in csv.DictReader(f) if r["file"] and not r["file"].startswith("#")]

    rows, errors = [], {p: {"grams": [], "kcal": []} for p in providers}
    for t in truth:
        path = HERE / "photos" / t["file"]
        if not path.exists():
            print(f"[omitida] {t['file']} no existe")
            continue
        mime = "image/png" if path.suffix.lower() == ".png" else "image/jpeg"
        b64 = base64.b64encode(path.read_bytes()).decode()
        for p in providers:
            start = time.time()
            try:
                det = PROVIDERS[p](b64, mime)
            except Exception as e:  # noqa: BLE001 - se registra y se sigue con la siguiente foto
                print(f"[error] {p} {t['file']}: {e}")
                continue
            grams = sum(i.grams for i in det.items)
            kcal = sum(i.kcal for i in det.items)
            real_g, real_k = float(t["real_grams"]), float(t["real_kcal"])
            errors[p]["grams"].append(abs(grams - real_g) / real_g * 100)
            errors[p]["kcal"].append(abs(kcal - real_k) / real_k * 100)
            rows.append({
                "file": t["file"], "provider": p, "seconds": round(time.time() - start, 1),
                "est_grams": round(grams), "real_grams": real_g,
                "est_kcal": round(kcal), "real_kcal": real_k,
                "items": "; ".join(f"{i.name} {i.grams:.0f}g" for i in det.items),
            })
            print(f"{p:7} {t['file']:25} {kcal:6.0f} kcal (real {real_k:.0f})")

    if rows:
        with open(HERE / "results.csv", "w", newline="", encoding="utf-8") as f:
            w = csv.DictWriter(f, fieldnames=list(rows[0]))
            w.writeheader()
            w.writerows(rows)

    print("\nError medio absoluto (%)")
    for p, e in errors.items():
        if e["kcal"]:
            print(f"  {p:7} gramos {statistics.mean(e['grams']):5.1f} %   kcal {statistics.mean(e['kcal']):5.1f} %"
                  f"   ({len(e['kcal'])} fotos)")


if __name__ == "__main__":
    main()
