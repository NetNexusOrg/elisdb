#!/usr/bin/env python3
"""Testes do importador Memento -> ElisDB (sem rede e sem Docker reais)."""

import base64
import os
import subprocess
import sys
from pathlib import Path

import pytest

sys.path.insert(0, os.path.dirname(__file__))

import importar_memento as imp  # noqa: E402


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


def _cliente(nome="Ana", contrato=None, lat=37.1, lon=-8.2, notas=None, tag=None):
    return {
        "id": f"id-{nome}",
        "nome": nome,
        "contrato": contrato,
        "tag": tag,
        "latitude": lat,
        "longitude": lon,
        "notas": notas,
        "ativo": 1,
        "updated_at": 1000,
        "created_at": 1000,
        "delivery_ts": 1000,
    }


def test_analisar_ignora_existentes_e_excluidos():
    rows = [
        _row(nome="Ana", pdl="1"),
        _row(nome="Maria", pdl="2"),
        _row(nome="Zé", pdl="3", coord=""),
    ]
    existentes = [_cliente(nome="MARIA", contrato="2")]
    analise = imp.analisar(rows, 1000, existentes, {"ZÉ"})

    assert [item.cliente["nome"] for item in analise.novos] == ["ANA"]
    assert [c["nome"] for c in analise.existentes] == ["MARIA"]
    assert [c["nome"] for c, _ in analise.excluidos] == ["ZÉ"]
    assert analise.sem_coordenadas == 0


def test_analisar_conta_sem_coordenadas():
    analise = imp.analisar([_row(nome="Ana", coord="")], 1000, [], set())
    assert len(analise.novos) == 1
    assert analise.sem_coordenadas == 1
    assert analise.novos[0].cliente["latitude"] is None


def test_analisar_exclui_coordenadas_fora_dos_limites():
    analise = imp.analisar([_row(nome="Fora", coord="91,-8.2")], 1000, [], set())
    assert analise.novos == []
    assert len(analise.excluidos) == 1
    assert analise.excluidos[0][1] == "coordenadas fora dos limites"


def test_possivel_duplicado_ignora_acentos():
    existente = _cliente(nome="MARIA JOÃO", contrato="123")
    novo = _cliente(nome="MARIA JOAO", contrato="999")
    assert imp._possivel_duplicado(novo, [existente]) is existente


def test_possivel_duplicado_por_semelhanca_e_contrato():
    existente = _cliente(
        nome="RESTAURANTE AROMA BOM SABOR", contrato="129020"
    )
    novo = _cliente(nome="RESTAURANTE AROMA BOM SABOR II", contrato="129020")
    assert imp._possivel_duplicado(novo, [existente]) is existente


def test_pedido_assinado_usa_nonce(monkeypatch):
    config = imp.Config("http://x", base64.b64encode(b"x" * 32).decode())
    chamadas = []

    def fake_open(url, method, body, headers):
        chamadas.append((url, method, body, headers))
        return []

    monkeypatch.setattr(imp, "_open_json", fake_open)
    imp._pedido_assinado(config, "GET", "/api/clients", None, 1000)
    imp._pedido_assinado(config, "GET", "/api/clients", None, 1000)

    assert chamadas[0][0] != chamadas[1][0]
    assert "nonce=" in chamadas[0][0]
    assert (
        chamadas[0][3][imp.SIGNATURE_HEADER]
        != chamadas[1][3][imp.SIGNATURE_HEADER]
    )


def test_possivel_duplicado_nao_dispara_com_contrato_diferente():
    existente = _cliente(
        nome="RESTAURANTE AROMA BOM SABOR", contrato="129020"
    )
    novo = _cliente(nome="RESTAURANTE AROMA BOM SABOR II", contrato="999999")
    assert imp._possivel_duplicado(novo, [existente]) is None


def test_confirmar_um_a_um_s_n_f(monkeypatch):
    itens = [imp.ClienteNovo(_cliente(nome=n)) for n in ("ANA", "BIA", "CAIO")]
    respostas = iter(["s", "n", "f"])
    monkeypatch.setattr("builtins.input", lambda _prompt: next(respostas))

    confirmados = imp.confirmar_um_a_um(itens)

    assert confirmados == [itens[0]]
    assert itens[0].decisao == "confirmado"
    assert itens[1].decisao == "saltado"
    assert itens[2].decisao == "nao_revisto"


def test_confirmar_um_a_um_v_e_depois_s(monkeypatch):
    itens = [imp.ClienteNovo(_cliente(nome="ANA"))]
    respostas = iter(["v", "s"])
    monkeypatch.setattr("builtins.input", lambda _prompt: next(respostas))

    confirmados = imp.confirmar_um_a_um(itens)

    assert confirmados == itens
    assert itens[0].decisao == "confirmado"


def test_confirmar_um_a_um_abortar(monkeypatch):
    itens = [imp.ClienteNovo(_cliente(nome=n)) for n in ("ANA", "BIA")]
    monkeypatch.setattr("builtins.input", lambda _prompt: "q")

    confirmados = imp.confirmar_um_a_um(itens)

    assert confirmados is None
    assert [item.decisao for item in itens] == ["abortado", "abortado"]


def test_confirmar_um_a_um_abortar_depois_de_confirmar(monkeypatch):
    itens = [imp.ClienteNovo(_cliente(nome=n)) for n in ("ANA", "BIA")]
    respostas = iter(["s", "q"])
    monkeypatch.setattr("builtins.input", lambda _prompt: next(respostas))

    confirmados = imp.confirmar_um_a_um(itens)

    assert confirmados is None
    assert [item.decisao for item in itens] == ["abortado", "abortado"]


def test_enviar_lotes_de_200(monkeypatch):
    itens = [imp.ClienteNovo(_cliente(nome=f"CLIENTE {i}")) for i in range(201)]
    lotes = []

    def fake_push(_config, records):
        lotes.append(len(records))
        return [
            {"id": record["client"]["id"], "status": "accepted"}
            for record in records
        ]

    monkeypatch.setattr(imp, "_push_batch", fake_push)
    erros = imp.enviar(itens, imp.Config("http://x", "seed"))

    assert lotes == [200, 1]
    assert erros == 0
    assert all(item.decisao == "importado" for item in itens)


def test_enviar_duplicate_nao_e_erro(monkeypatch):
    itens = [imp.ClienteNovo(_cliente(nome="ANA"))]
    monkeypatch.setattr(
        imp,
        "_push_batch",
        lambda _config, records: [
            {"id": records[0]["client"]["id"], "status": "duplicate"}
        ],
    )

    assert imp.enviar(itens, imp.Config("http://x", "seed")) == 0
    assert itens[0].decisao == "duplicado"


def test_enviar_error_e_erro(monkeypatch):
    itens = [imp.ClienteNovo(_cliente(nome="ANA"))]
    monkeypatch.setattr(
        imp,
        "_push_batch",
        lambda _config, records: [
            {"id": records[0]["client"]["id"], "status": "error"}
        ],
    )

    assert imp.enviar(itens, imp.Config("http://x", "seed")) == 1
    assert itens[0].decisao == "erro"


def test_criar_config_com_docker_mockado(monkeypatch, tmp_path):
    destino = tmp_path / "memento-import.config.json"
    config = {
        "server_url": "http://127.0.0.1:8000",
        "private_seed": base64.b64encode(b"x" * 32).decode(),
        "user_name": "memento-import",
    }

    monkeypatch.setattr(imp.shutil, "which", lambda _cmd: "/usr/bin/docker")

    def fake_run(cmd, **kwargs):
        if "cat" in cmd:
            return subprocess.CompletedProcess(
                cmd, 0, stdout=__import__("json").dumps(config).encode(), stderr=b""
            )
        return subprocess.CompletedProcess(
            cmd, 0, stdout="private seed: nao-deve-aparecer", stderr=""
        )

    monkeypatch.setattr(imp.subprocess, "run", fake_run)

    assert imp.criar_config(destino) is True
    assert destino.exists()
    assert destino.stat().st_mode & 0o777 == 0o600
    assert "nao-deve-aparecer" not in destino.read_text()


def test_main_nao_envia_quando_utilizador_diz_n(monkeypatch, tmp_path):
    csv_path = tmp_path / "ELIS.csv"
    csv_path.write_text(
        "Rota,PDL,Nome,Coordenadas,Notas,Foto,Plantas\n"
        '1,10,Ana,"37.1,-8.2",,,\n',
        encoding="utf-8-sig",
    )
    config_path = tmp_path / "config.json"
    config_path.write_text(
        __import__("json").dumps(
            {
                "server_url": "http://127.0.0.1:8000",
                "private_seed": base64.b64encode(b"x" * 32).decode(),
                "user_name": "memento-import",
            }
        ),
        encoding="utf-8",
    )
    report_path = tmp_path / "report.txt"

    monkeypatch.setattr(imp, "obter_hora_servidor", lambda _url: 1000)
    monkeypatch.setattr(imp, "listar_clientes", lambda _config, _ts: [])
    monkeypatch.setattr("builtins.input", lambda _prompt: "n")

    def nao_deve_enviar(*_args, **_kwargs):
        raise AssertionError("não devia enviar")

    monkeypatch.setattr(imp, "_push_batch", nao_deve_enviar)

    assert imp.main(csv_path, config_path, report_path) == 0
    texto = report_path.read_text().lower()
    assert "saltado" in texto
    assert "ana: saltado" in texto


def test_main_envia_confirmado(monkeypatch, tmp_path):
    csv_path = tmp_path / "ELIS.csv"
    csv_path.write_text(
        "Rota,PDL,Nome,Coordenadas,Notas,Foto,Plantas\n"
        '1,10,Ana,"37.1,-8.2",,,\n',
        encoding="utf-8-sig",
    )
    config_path = tmp_path / "config.json"
    config_path.write_text(
        __import__("json").dumps(
            {
                "server_url": "http://127.0.0.1:8000",
                "private_seed": base64.b64encode(b"x" * 32).decode(),
                "user_name": "memento-import",
            }
        ),
        encoding="utf-8",
    )
    report_path = tmp_path / "report.txt"

    monkeypatch.setattr(imp, "obter_hora_servidor", lambda _url: 1000)
    monkeypatch.setattr(imp, "listar_clientes", lambda _config, _ts: [])
    monkeypatch.setattr("builtins.input", lambda _prompt: "s")
    monkeypatch.setattr(
        imp,
        "_push_batch",
        lambda _config, records: [
            {"id": records[0]["client"]["id"], "status": "accepted"}
        ],
    )

    assert imp.main(csv_path, config_path, report_path) == 0
    texto = report_path.read_text()
    assert "importado" in texto
    assert "Concluído" not in texto  # o relatório não inclui o stdout


def test_ler_exclusoes(tmp_path):
    caminho = tmp_path / "exclude.txt"
    caminho.write_text("# comentário\n\nana   maria\n", encoding="utf-8")
    assert imp.ler_exclusoes(caminho) == {"ANA MARIA"}
    assert imp.ler_exclusoes(tmp_path / "nao-existe.txt") == set()


CABECALHO_REAL = ["Rota", "PDL", "Nome", "Coordenadas", "Notas", "Foto", "Plantas"]


def test_validar_csv_export_valido():
    rows = [
        _row(nome="Ana", pdl="1"),
        _row(nome="Bruno", coord=""),
        _row(nome="Casa", coord="37.1,-8.2, 37.0,-8.1"),
    ]
    validacao = imp.validar_csv(rows, CABECALHO_REAL)
    assert validacao.ok
    assert validacao.total == 3
    assert validacao.com_nome == 3
    assert validacao.nomes_unicos == 3
    assert validacao.sem_coordenadas == 1
    assert validacao.tipo_a == 1
    assert validacao.erros_coordenadas == 0


def test_validar_csv_colunas_obrigatorias_em_falta():
    validacao = imp.validar_csv([_row()], ["Nome", "Coordenadas"])
    assert not validacao.ok
    assert any("PDL" in erro for erro in validacao.erros)
    assert any("Notas" in erro for erro in validacao.erros)


def test_validar_csv_coordenadas_invalidas():
    validacao = imp.validar_csv([_row(coord="37.1,-8.2,37.0")], CABECALHO_REAL)
    assert validacao.ok  # erro de uma linha não aborta o CSV inteiro
    assert validacao.erros_coordenadas == 1
    assert any(
        "coordenadas inválidas" in problema
        for problema in validacao.problemas_coordenadas
    )


def test_validar_csv_latitude_fora_do_intervalo():
    validacao = imp.validar_csv([_row(coord="91,-8.2")], CABECALHO_REAL)
    assert validacao.ok
    assert any(
        "latitude fora" in problema
        for problema in validacao.problemas_coordenadas
    )


def test_validar_csv_tipo_a_valida_todos_os_pares():
    validacao = imp.validar_csv(
        [_row(coord="91,-8.2, 37.0,-8.1")], CABECALHO_REAL
    )
    assert validacao.ok
    assert validacao.tipo_a == 1
    assert any(
        "par 1: latitude fora" in problema
        for problema in validacao.problemas_coordenadas
    )


def test_validar_csv_nome_demasiado_longo():
    validacao = imp.validar_csv([_row(nome="X" * 201)], CABECALHO_REAL)
    assert not validacao.ok
    assert any("Nome com 201" in erro for erro in validacao.erros)


def test_validar_csv_rota_demasiado_longa():
    validacao = imp.validar_csv([_row(rota="X" * 101)], CABECALHO_REAL)
    assert not validacao.ok
    assert any("Rota/tag com 101" in erro for erro in validacao.erros)


def test_validar_csv_rota_e_opcional():
    row = _row()
    row.pop("Rota")
    validacao = imp.validar_csv([row], CABECALHO_REAL)
    assert validacao.ok


def test_cliente_payload_inclui_tag():
    payload = imp._cliente_payload(_cliente(nome="ANA", tag="Rota A"))
    assert payload["tag"] == "Rota A"


def test_print_cliente_mostra_tag(capsys):
    imp._print_cliente(imp.ClienteNovo(_cliente(nome="ANA", tag="Rota A")), 1, 1)
    assert "Tag (Rota): Rota A" in capsys.readouterr().out


def test_validar_csv_vazio():
    validacao = imp.validar_csv([], CABECALHO_REAL)
    assert not validacao.ok
    assert any("não tem linhas" in erro for erro in validacao.erros)


def test_main_aborta_csv_incompativel_antes_do_servidor(monkeypatch, tmp_path):
    csv_path = tmp_path / "ELIS.csv"
    csv_path.write_text("Nome\nAna\n", encoding="utf-8-sig")
    config_path = tmp_path / "config.json"
    report_path = tmp_path / "report.txt"

    def nao_deve_ser_chamado(*_args, **_kwargs):
        raise AssertionError("o servidor/config não devia ser contactado")

    monkeypatch.setattr(imp, "carregar_config", nao_deve_ser_chamado)
    monkeypatch.setattr(imp, "obter_hora_servidor", nao_deve_ser_chamado)
    monkeypatch.setattr(imp, "listar_clientes", nao_deve_ser_chamado)

    assert imp.main(csv_path, config_path, report_path) == 1
    assert not report_path.exists()
