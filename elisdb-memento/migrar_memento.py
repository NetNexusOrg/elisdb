#!/usr/bin/env python3
"""Migração do export Memento (ELIS.csv) para o formato de dados do elisdb.

Standalone — não depende da app Android nem do servidor elisdb-server.
Lê o CSV, valida coordenadas, resolve conflitos (TIPO A/B) e gera um ficheiro
SQLite (.db) com o schema alvo do servidor (clientes; sem fotos). Não escreve
na base de produção.

Os timestamps são escritos em **segundos** Unix (`updated_at`, `created_at`,
`delivery_ts`), o mesmo relógio do servidor. Validação:

    ../elisdb-server/.venv/bin/python -m pytest -q test_migrar_memento.py
"""

import argparse
import csv
import re
import sqlite3
import sys
import uuid
from dataclasses import dataclass, field
from datetime import datetime, timezone

# ---------------------------------------------------------------------------
# Parsing de coordenadas
# ---------------------------------------------------------------------------


def extract_numbers(coord: str) -> list[str]:
    """Extrai todos os números (possivelmente decimais/negativos) do campo."""
    return re.findall(r"-?\d+(?:\.\d+)?", coord or "")


def classify(coords: str) -> str:
    """Classifica um campo Coordenadas: 'vazio' | 'ok' | 'tipo_a' | 'erro'."""
    n = extract_numbers(coords)
    if len(n) == 0:
        return "vazio"
    if len(n) == 2:
        return "ok"
    if len(n) > 2 and len(n) % 2 == 0:
        return "tipo_a"
    return "erro"


def split_pairs(coords: str) -> list[tuple[str, str]]:
    """Divide os números em pares (lat, lon)."""
    n = extract_numbers(coords)
    return [(n[i], n[i + 1]) for i in range(0, len(n) - 1, 2)]


# ---------------------------------------------------------------------------
# Modelos de resultados
# ---------------------------------------------------------------------------


@dataclass
class CasoTipoA:
    nome: str
    pares: list[tuple[str, str]]
    escolhido: tuple[str, str]
    notas_extra: str


@dataclass
class CasoTipoB:
    nome: str
    linhas: int
    pares_validos: list[tuple[str, str]]
    escolhido: tuple[str, str] | None
    notas_originais: list[str]


@dataclass
class Relatorio:
    linhas_processadas: int = 0
    linhas_ignoradas: int = 0
    coords_vazias: int = 0
    avisos: list[str] = field(default_factory=list)
    erros_parsing: list[tuple[str, str]] = field(default_factory=list)
    tipo_a: list[CasoTipoA] = field(default_factory=list)
    tipo_b: list[CasoTipoB] = field(default_factory=list)


# ---------------------------------------------------------------------------
# Geração do ficheiro SQLite
# ---------------------------------------------------------------------------

# Schema alvo do servidor para clientes. O export do Memento não representa
# eliminações, por isso não há tabela `tombstones`: o servidor cria os
# tombstones quando um cliente é apagado depois da importação.
SCHEMA = """
CREATE TABLE clientes (
    id TEXT PRIMARY KEY,
    nome TEXT UNIQUE COLLATE NOCASE,
    contrato TEXT,
    latitude REAL,
    longitude REAL,
    notas TEXT,
    updated_at INTEGER NOT NULL,
    created_at INTEGER NOT NULL DEFAULT 0,
    delivery_ts INTEGER NOT NULL DEFAULT 0,
    ativo INTEGER NOT NULL DEFAULT 1
);
"""


def gerar_db(caminho: str, clientes: list[dict]) -> None:
    import os

    if os.path.exists(caminho):
        os.remove(caminho)
    conn = sqlite3.connect(caminho)
    conn.executescript(SCHEMA)
    conn.executemany(
        "INSERT INTO clientes (id, nome, contrato, latitude, longitude, notas, "
        "updated_at, created_at, delivery_ts, ativo) "
        "VALUES (:id, :nome, :contrato, :latitude, :longitude, :notas, "
        ":updated_at, :created_at, :delivery_ts, :ativo)",
        clientes,
    )
    conn.commit()
    conn.close()


# ---------------------------------------------------------------------------
# Migração
# ---------------------------------------------------------------------------


def migrar(rows: list[dict], timestamp_s: int) -> tuple[list[dict], Relatorio]:
    """Converte as linhas do CSV em registos prontos para o schema do servidor.

    ``timestamp_s`` é um instante Unix em **segundos** (o mesmo relógio que o
    servidor usa em ``updated_at``/``created_at``/``delivery_ts``).
    """
    rel = Relatorio()
    rel.linhas_processadas = len(rows)
    # Contagem total de campos Coordenadas vazios no input (25 no export atual),
    # independentemente de como a linha é depois processada/agrupada.
    rel.coords_vazias = sum(1 for r in rows if classify(r.get("Coordenadas") or "") == "vazio")

    # Agrupar por nome normalizado (trim/upper) para detetar TIPO B.
    grupos: dict[str, list[dict]] = {}
    for r in rows:
        nome_raw = (r.get("Nome") or "").strip()
        if not nome_raw:
            rel.linhas_ignoradas += 1
            continue
        chave = nome_raw.upper()
        grupos.setdefault(chave, []).append(r)

    clientes: list[dict] = []
    for chave, linhas in grupos.items():
        nome = linhas[0]["Nome"].strip().upper()
        if len(linhas) > 1:
            # TIPO B: várias linhas com o mesmo nome normalizado.
            pares_validos: list[tuple[str, str]] = []
            notas_originais: list[str] = []
            for ln in linhas:
                c = classify(ln.get("Coordenadas") or "")
                if c == "ok":
                    pares_validos.append(split_pairs(ln["Coordenadas"])[0])
                elif c == "tipo_a":
                    pares_validos.extend(split_pairs(ln["Coordenadas"]))
                elif c == "erro":
                    rel.erros_parsing.append((nome, ln.get("Coordenadas") or ""))
                notas_originais.append((ln.get("Notas") or "").strip())
            escolhido = pares_validos[0] if pares_validos else None
            rel.tipo_b.append(
                CasoTipoB(nome, len(linhas), pares_validos, escolhido, notas_originais)
            )
            contrato = linhas[0]["PDL"].strip() or None
            notas = "\n\n".join(n for n in notas_originais if n) or None
            clientes.append(
                {
                    "id": str(uuid.uuid4()),
                    "nome": nome,
                    "contrato": contrato,
                    "latitude": float(escolhido[0]) if escolhido else None,
                    "longitude": float(escolhido[1]) if escolhido else None,
                    "notas": notas,
                    "updated_at": timestamp_s,
                    "created_at": timestamp_s,
                    "delivery_ts": timestamp_s,
                    "ativo": 1,
                }
            )
            continue

        ln = linhas[0]
        coords = ln.get("Coordenadas") or ""
        c = classify(coords)
        lat = lon = None
        if c == "ok":
            lat, lon = (float(x) for x in split_pairs(coords)[0])
        elif c == "tipo_a":
            pares = split_pairs(coords)
            escolhido = pares[0]
            lat, lon = (float(x) for x in escolhido)
            restantes = " | ".join(f"{p[0]},{p[1]}" for p in pares[1:])
            rel.tipo_a.append(CasoTipoA(nome, pares, escolhido, restantes))
        elif c == "erro":
            rel.erros_parsing.append((nome, coords))
        # c == "vazio": latitude/longitude ficam NULL (não é erro); já contado no input

        contrato = (ln.get("PDL") or "").strip() or None
        notas = (ln.get("Notas") or "").strip() or None
        if c == "tipo_a":
            extras = f"Coordenadas alternativas: {rel.tipo_a[-1].notas_extra}"
            notas = "\n\n".join(n for n in [notas, extras] if n)
        clientes.append(
            {
                "id": str(uuid.uuid4()),
                "nome": nome,
                "contrato": contrato,
                "latitude": lat,
                "longitude": lon,
                "notas": notas,
                "updated_at": timestamp_s,
                "created_at": timestamp_s,
                "delivery_ts": timestamp_s,
                "ativo": 1,
            }
        )

    return clientes, rel


# ---------------------------------------------------------------------------
# Relatório
# ---------------------------------------------------------------------------


def imprimir_relatorio(rel: Relatorio) -> None:
    print("=" * 60)
    print("RELATÓRIO DA MIGRAÇÃO")
    print("=" * 60)
    print(f"Linhas processadas : {rel.linhas_processadas}")
    print(f"Linhas ignoradas   : {rel.linhas_ignoradas}")
    print(f"Coordenadas vazias : {rel.coords_vazias}")
    print(f"Avisos             : {len(rel.avisos)}")
    print(f"Erros de parsing   : {len(rel.erros_parsing)}")
    print(f"TIPO A resolvidos  : {len(rel.tipo_a)}")
    print(f"TIPO B resolvidos  : {len(rel.tipo_b)}")
    print()
    if rel.erros_parsing:
        print("Erros de parsing:")
        for nome, coords in rel.erros_parsing:
            print(f"  - {nome}: {coords!r}")
    if rel.tipo_a:
        print("TIPO A (múltiplos pares no mesmo campo):")
        for caso in rel.tipo_a:
            print(f"  - {caso.nome}")
            print(f"      pares    : {', '.join(f'{a},{b}' for a, b in caso.pares)}")
            print(f"      escolhido: {caso.escolhido[0]},{caso.escolhido[1]}")
            print(f"      notas    : {caso.notas_extra}")
    if rel.tipo_b:
        print("TIPO B (nomes repetidos):")
        for caso in rel.tipo_b:
            print(f"  - {caso.nome} (x{ caso.linhas })")
            if caso.pares_validos:
                print(f"      pares    : {', '.join(f'{a},{b}' for a, b in caso.pares_validos)}")
                print(f"      escolhido: {caso.escolhido[0]},{caso.escolhido[1]}")
            else:
                print("      sem coordenadas válidas")


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------


def main() -> int:
    ap = argparse.ArgumentParser(description="Migra ELIS.csv para o formato elisdb")
    ap.add_argument("--input", default="ELIS.csv", help="Ficheiro CSV de entrada")
    ap.add_argument("--output", default="memento-import.db", help="Ficheiro SQLite de saída")
    ap.add_argument(
        "--timestamp",
        type=int,
        default=None,
        help="Epoch em segundos para created/updated/delivery (default: agora)",
    )
    args = ap.parse_args()

    try:
        with open(args.input, newline="", encoding="utf-8-sig") as fh:
            rows = list(csv.DictReader(fh))
    except FileNotFoundError:
        print(f"Erro: ficheiro de entrada não encontrado: {args.input}", file=sys.stderr)
        return 1

    ts = args.timestamp or int(datetime.now(timezone.utc).timestamp())
    clientes, rel = migrar(rows, ts)
    gerar_db(args.output, clientes)

    imprimir_relatorio(rel)
    print()
    print(f"Escritos {len(clientes)} clientes em {args.output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
