#!/usr/bin/env python3
"""Testes do script de migração Memento → elisdb (standalone)."""

import os
import sqlite3
import sys

sys.path.insert(0, os.path.dirname(__file__))

from migrar_memento import (  # noqa: E402
    classify,
    extract_numbers,
    ler_csv_com_cabecalho,
    migrar,
    normalize_nome,
    split_pairs,
)


def test_extract_numbers():
    assert extract_numbers("37.086,-8.221") == ["37.086", "-8.221"]
    assert extract_numbers("37.086,-8.221, 37.085,-8.221") == [
        "37.086",
        "-8.221",
        "37.085",
        "-8.221",
    ]
    assert extract_numbers("") == []
    assert extract_numbers("abc") == []


def test_classify():
    assert classify("37.1,-8.2") == "ok"
    assert classify("37.1,-8.2, 37.0,-8.1") == "tipo_a"
    assert classify("37.1,-8.2, 37.0") == "erro"  # ímpar
    assert classify("") == "vazio"
    assert classify("nada aqui") == "vazio"


def test_split_pairs():
    assert split_pairs("37.1,-8.2") == [("37.1", "-8.2")]
    assert split_pairs("37.1,-8.2, 37.0,-8.1") == [("37.1", "-8.2"), ("37.0", "-8.1")]


def test_normalize_nome_matches_server():
    assert normalize_nome("  ana   maria  ") == "ANA MARIA"
    assert normalize_nome("Ana\tMaria") == "ANA MARIA"


def test_migrar_colapsa_espacos_internos():
    rows = [
        _row(nome="Ana   Maria", coord="37.1,-8.2"),
        _row(nome="ana maria", coord="37.1,-8.2"),
    ]
    clientes, rel = migrar(rows, 1000)
    assert len(clientes) == 1
    assert clientes[0]["nome"] == "ANA MARIA"
    assert len(rel.tipo_b) == 1


def _row(nome="Ana", pdl="", coord="37.1,-8.2", notas="", rota="1"):
    return {
        "Rota": rota,
        "PDL": pdl,
        "Nome": nome,
        "Coordenadas": coord,
        "Notas": notas,
        "Foto": "",
        "Plantas": "",
    }


def test_migrar_linha_normal():
    clientes, rel = migrar([_row(nome="Ana", pdl="CT-1", coord="37.1,-8.2", notas="x")], 1000)
    assert len(clientes) == 1
    c = clientes[0]
    assert c["nome"] == "ANA"
    assert c["contrato"] == "CT-1"
    assert c["tag"] == "1"
    assert c["latitude"] == 37.1
    assert c["longitude"] == -8.2
    assert c["notas"] == "x"
    assert c["created_at"] == 1000 == c["updated_at"]
    assert len(rel.tipo_a) == 0
    assert len(rel.tipo_b) == 0


def test_migrar_rota_como_tag_preserva_valor():
    rows = [_row(nome="Ana", rota="  Rota A  ")]
    clientes, _ = migrar(rows, 1000)
    assert clientes[0]["tag"] == "Rota A"


def test_migrar_rota_vazia_fica_null():
    rows = [_row(nome="Ana", rota="   ")]
    clientes, _ = migrar(rows, 1000)
    assert clientes[0]["tag"] is None


def test_migrar_tipo_b_usa_rota_da_primeira_linha():
    rows = [
        _row(nome="VERANDA", rota="21"),
        _row(nome="Veranda", rota="22"),
    ]
    clientes, rel = migrar(rows, 1000)
    assert len(clientes) == 1
    assert clientes[0]["tag"] == "21"
    assert rel.tipo_b[0].rotas == ["21", "22"]


def test_migrar_tipo_a_primeira_mais_notas():
    rows = [_row(nome="CASA", coord="37.1,-8.2, 37.0,-8.1")]
    clientes, rel = migrar(rows, 1000)
    assert len(clientes) == 1
    c = clientes[0]
    assert c["latitude"] == 37.1
    assert c["longitude"] == -8.2
    assert "37.0,-8.1" in (c["notas"] or "")
    assert len(rel.tipo_a) == 1
    assert rel.tipo_a[0].escolhido == ("37.1", "-8.2")


def test_migrar_tipo_b_colapsa_para_um():
    rows = [
        _row(nome="VERANDA", coord="37.2,-7.5", notas="Monte Rei"),
        _row(nome="Veranda", coord="37.2,-7.5", notas="outra"),
    ]
    clientes, rel = migrar(rows, 1000)
    assert len(clientes) == 1
    assert clientes[0]["nome"] == "VERANDA"
    assert len(rel.tipo_b) == 1
    assert rel.tipo_b[0].linhas == 2


def test_migrar_nome_vazio_ignorado():
    rows = [_row(nome="", coord="37.1,-8.2")]
    clientes, rel = migrar(rows, 1000)
    assert len(clientes) == 0
    assert rel.linhas_ignoradas == 1


def test_migrar_coords_vazias_null():
    rows = [_row(nome="Ana", coord="")]
    clientes, rel = migrar(rows, 1000)
    assert len(clientes) == 1
    assert clientes[0]["latitude"] is None
    assert clientes[0]["longitude"] is None
    assert rel.coords_vazias == 1
    assert len(rel.erros_parsing) == 0


def test_migrar_erro_parsing_nao_cria_cliente():
    rows = [_row(nome="Ana", coord="37.1,-8.2,37.0")]
    clientes, rel = migrar(rows, 1000)
    assert len(clientes) == 0
    assert len(rel.erros_parsing) == 1


def test_migrar_tipo_b_com_erro_mantem_linha_valida():
    rows = [
        _row(nome="CLUB HOUSE", coord="37.1,-8.2,37.0"),
        _row(nome="club house", coord="37.2,-8.3"),
    ]
    clientes, rel = migrar(rows, 1000)
    assert len(clientes) == 1
    assert clientes[0]["latitude"] == 37.2
    assert len(rel.erros_parsing) == 1
    assert len(rel.tipo_b) == 1


def test_migrar_output_sem_duplicados_nocase(tmp_path):
    rows = [
        _row(nome="Maria", coord="37.1,-8.2"),
        _row(nome="maria ", coord="37.1,-8.2"),
    ]
    clientes, rel = migrar(rows, 1000)
    assert len(clientes) == 1
    assert len(rel.tipo_b) == 1


def test_output_db_schema(tmp_path):
    from migrar_memento import gerar_db

    clientes, _ = migrar([_row(nome="Ana", coord="37.1,-8.2")], 1000)
    db = str(tmp_path / "out.db")
    gerar_db(db, clientes)
    conn = sqlite3.connect(db)
    cols = [r[1] for r in conn.execute("PRAGMA table_info(clientes)")]
    assert cols == [
        "id",
        "nome",
        "contrato",
        "tag",
        "latitude",
        "longitude",
        "notas",
        "updated_at",
        "created_at",
        "delivery_ts",
        "ativo",
    ]
    conn.close()


def test_output_uses_seconds_and_current_fields(tmp_path):
    """O schema gerado tem de casar com o relógio e os campos do servidor."""
    from migrar_memento import gerar_db

    # Um instante real em segundos (2025-09-16), não milissegundos.
    timestamp = 1_758_000_000
    clientes, _ = migrar([_row(nome="Ana", coord="37.1,-8.2")], timestamp)
    db = str(tmp_path / "out.db")
    gerar_db(db, clientes)

    conn = sqlite3.connect(db)
    row = conn.execute(
        "SELECT updated_at, created_at, delivery_ts, ativo FROM clientes"
    ).fetchone()
    conn.close()

    assert row == (timestamp, timestamp, timestamp, 1)
    # Milissegundos seriam ~1000x maiores; o servidor espera segundos.
    assert row[0] < 100_000_000_000


def test_ler_csv_com_cabecalho(tmp_path):
    csv_path = tmp_path / "ELIS.csv"
    csv_path.write_text(
        "Rota,PDL,Nome,Coordenadas,Notas,Foto,Plantas\n"
        '1,10,Ana,"37.1,-8.2",,, \n',
        encoding="utf-8-sig",
    )
    cabecalho, rows = ler_csv_com_cabecalho(str(csv_path))
    assert cabecalho == ["Rota", "PDL", "Nome", "Coordenadas", "Notas", "Foto", "Plantas"]
    assert len(rows) == 1
    assert rows[0]["Nome"] == "Ana"
    assert rows[0]["PDL"] == "10"
