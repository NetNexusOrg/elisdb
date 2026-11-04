#!/usr/bin/env python3
"""Importa os clientes do export Memento para o servidor ElisDB.

O comando é intencionalmente simples: ``./elisdb-memento/importar``. Não tem
opções de automação; lê os caminhos fixos abaixo, mostra o que vai fazer e pede
confirmação cliente a cliente antes de enviar seja o que for.

Fluxo:
  1. garante a chave ed25519 dedicada (cria-a via Docker na primeira execução);
  2. lê ``ELIS.csv`` e normaliza os nomes como o servidor;
  3. compara com ``GET /api/clients`` e ignora quem já existe;
  4. mostra um resumo, escreve o relatório e pergunta o que for preciso;
  5. confirma um cliente de cada vez;
  6. envia só os confirmados em lotes de 200 para ``POST /api/push``.

Nunca imprime nem guarda a private seed no relatório. O config e o relatório
contêm dados sensíveis e são ignorados pelo Git.
"""

from __future__ import annotations

import base64
import csv
import difflib
import hashlib
import json
import os
import re
import shutil
import subprocess
import sys
import unicodedata
import uuid
from dataclasses import dataclass, field
from datetime import datetime
from pathlib import Path
from typing import Any
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PrivateKey
from cryptography.hazmat.primitives.serialization import Encoding, PublicFormat

from migrar_memento import (
    CasoTipoA,
    CasoTipoB,
    Relatorio,
    classify,
    ler_csv_com_cabecalho,
    migrar,
    normalize_nome,
    split_pairs,
)

BASE_DIR = Path(__file__).resolve().parent
REPO_ROOT = BASE_DIR.parent
CSV_PATH = BASE_DIR / "ELIS.csv"
CONFIG_PATH = BASE_DIR / "memento-import.config.json"
REPORT_PATH = BASE_DIR / "memento-import-report.txt"
EXCLUDE_PATH = BASE_DIR / "memento-import-exclude.txt"

DEFAULT_SERVER_URL = "http://127.0.0.1:8000"
BATCH_SIZE = 200
HTTP_TIMEOUT_SECONDS = 30
DOCKER_TIMEOUT_SECONDS = 120

# Safeguard do CSV: o export tem de trazer estas colunas e os valores têm de
# caber nos limites do servidor (app/models.py).
CAMPOS_OBRIGATORIOS = ("Nome", "PDL", "Coordenadas", "Notas")
# `Rota` é usada como tag quando existe; `Foto` e `Plantas` são ignoradas.
CAMPOS_OPCIONAIS = ("Rota", "Foto", "Plantas")
CAMPOS_CONHECIDOS = set(CAMPOS_OBRIGATORIOS) | set(CAMPOS_OPCIONAIS)
MAX_NOME = 200
MAX_TEXTO = 100_000
MAX_TAG = 100
MAX_ERROS_VALIDACAO = 50

# Headers usados pelo servidor (app/auth.py).
TIMESTAMP_HEADER = "X-Elisdb-Timestamp"
SIGNATURE_HEADER = "X-Elisdb-Signature"
KEY_HEADER = "X-Elisdb-Key"


class ApiError(Exception):
    """Resposta HTTP de erro do servidor."""

    def __init__(self, status: int, detail: str):
        super().__init__(f"HTTP {status}: {detail}")
        self.status = status
        self.detail = detail


class NetworkError(Exception):
    """Falha de rede/timeout ao falar com o servidor."""


@dataclass
class Config:
    server_url: str
    private_seed: str
    user_name: str = ""


@dataclass
class ClienteNovo:
    cliente: dict[str, Any]
    origem: str = "normal"  # normal | tipo_a | tipo_b
    caso_tipo_a: CasoTipoA | None = None
    caso_tipo_b: CasoTipoB | None = None
    existente_semelhante: dict[str, Any] | None = None
    decisao: str = "pendente"


@dataclass
class ValidacaoCsv:
    """Resultado do safeguard aplicado ao export antes de falar com o servidor."""

    total: int = 0
    com_nome: int = 0
    nomes_unicos: int = 0
    sem_coordenadas: int = 0
    tipo_a: int = 0
    erros_coordenadas: int = 0
    problemas_coordenadas: list[str] = field(default_factory=list)
    erros: list[str] = field(default_factory=list)
    avisos: list[str] = field(default_factory=list)
    erros_truncados: bool = False
    problemas_truncados: bool = False

    @property
    def ok(self) -> bool:
        return not self.erros


@dataclass
class Analise:
    rel: Relatorio
    novos: list[ClienteNovo]
    existentes: list[dict[str, Any]]
    excluidos: list[tuple[dict[str, Any], str]]
    total_linhas: int
    total_servidor: int

    @property
    def possiveis_duplicados(self) -> int:
        return sum(1 for item in self.novos if item.existente_semelhante is not None)

    @property
    def sem_coordenadas(self) -> int:
        return sum(
            1
            for item in self.novos
            if item.cliente.get("latitude") is None
            or item.cliente.get("longitude") is None
        )


# ---------------------------------------------------------------------------
# Configuração e chave dedicada
# ---------------------------------------------------------------------------


def _chmod_600(caminho: Path) -> bool:
    """Garante que o config só é legível pelo dono."""
    try:
        os.chmod(caminho, 0o600)
        return (caminho.stat().st_mode & 0o077) == 0
    except OSError:
        return False


def _sem_segredo(texto: str) -> str:
    """Remove linhas que possam conter a private seed de saídas de erro."""
    return "\n".join(
        linha
        for linha in texto.splitlines()
        if "private seed" not in linha.lower()
    ).strip()


def _ajuda_config() -> None:
    print()
    print("Configuração necessária (uma vez):")
    print(
        "  docker compose exec elisdb-server python -m app.keys issue "
        "memento-import \\"
    )
    print(
        f"    --server-url {DEFAULT_SERVER_URL} "
        "--output /app/data/memento-import.config.json"
    )
    print(
        "  docker compose cp elisdb-server:/app/data/memento-import.config.json "
        "elisdb-memento/memento-import.config.json"
    )
    print("  chmod 600 elisdb-memento/memento-import.config.json")
    print()
    print(
        "Se o servidor for remoto, usa o URL remoto no --server-url e copia o "
        "ficheiro para o caminho acima."
    )


def _docker_compose() -> list[str]:
    return ["docker", "compose", "-f", "elisdb-server/compose.yaml"]


def criar_config(caminho: Path = CONFIG_PATH) -> bool:
    """Cria uma chave dedicada no servidor local e copia o config para o host."""
    if shutil.which("docker") is None:
        print("Não encontrei o Docker para criar a chave automaticamente.")
        _ajuda_config()
        return False

    print("Não encontrei o config; vou criar uma chave dedicada 'memento-import'.")
    issue = _docker_compose() + [
        "exec",
        "-T",
        "elisdb-server",
        "python",
        "-m",
        "app.keys",
        "issue",
        "memento-import",
        "--server-url",
        DEFAULT_SERVER_URL,
        "--output",
        "/app/data/memento-import.config.json",
    ]
    try:
        resultado = subprocess.run(
            issue,
            cwd=REPO_ROOT,
            capture_output=True,
            text=True,
            timeout=DOCKER_TIMEOUT_SECONDS,
        )
    except (OSError, subprocess.TimeoutExpired) as exc:
        print(f"Não consegui executar o Docker: {exc}")
        _ajuda_config()
        return False

    if resultado.returncode != 0:
        print("Não consegui criar a chave automaticamente.")
        detalhe = _sem_segredo(resultado.stderr or "")
        if detalhe:
            print(f"Detalhe: {detalhe}")
        _ajuda_config()
        return False

    # O comando acima imprime a private seed no stdout. NÃO a mostramos; o
    # segredo é lido diretamente do ficheiro criado no volume.
    cat = _docker_compose() + [
        "exec",
        "-T",
        "elisdb-server",
        "cat",
        "/app/data/memento-import.config.json",
    ]
    try:
        copia = subprocess.run(
            cat,
            cwd=REPO_ROOT,
            capture_output=True,
            timeout=DOCKER_TIMEOUT_SECONDS,
        )
    except (OSError, subprocess.TimeoutExpired) as exc:
        print(f"Não consegui ler o config criado no servidor: {exc}")
        _ajuda_config()
        return False

    if copia.returncode != 0 or not copia.stdout:
        print("Não consegui copiar o config criado no servidor.")
        _ajuda_config()
        return False

    try:
        fd = os.open(caminho, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o600)
        with os.fdopen(fd, "wb") as fh:
            fh.write(copia.stdout)
    except OSError as exc:
        print(f"Não consegui escrever {caminho}: {exc}")
        return False

    if not _chmod_600(caminho):
        print(
            f"Não consegui garantir permissões 600 em {caminho}. "
            "Corrige as permissões e tenta outra vez."
        )
        return False

    print(f"Chave dedicada criada. Config guardado em {caminho} (permissões 600).")
    return True


def carregar_config(caminho: Path = CONFIG_PATH) -> Config | None:
    """Lê e valida o config; cria-o na primeira execução se possível."""
    if not caminho.exists():
        if not criar_config(caminho):
            return None

    if not _chmod_600(caminho):
        print(
            f"O config {caminho} não está legível apenas pelo dono. "
            "Corrige as permissões (chmod 600) e tenta outra vez."
        )
        return None
    try:
        dados = json.loads(caminho.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        print(f"Config inválido em {caminho}: {exc}")
        print("Apaga o ficheiro e corre o comando outra vez para gerar uma chave nova.")
        return None

    server_url = str(dados.get("server_url") or "").strip().rstrip("/")
    private_seed = str(dados.get("private_seed") or "").strip()
    user_name = str(dados.get("user_name") or "").strip()
    if not server_url or not private_seed:
        print(f"Config incompleto em {caminho} (falta server_url ou private_seed).")
        return None
    if not re.match(r"^https?://", server_url):
        server_url = "http://" + server_url

    try:
        seed = base64.b64decode(private_seed, validate=True)
        if len(seed) != 32:
            raise ValueError("tamanho inválido")
    except (ValueError, base64.binascii.Error):
        print(f"Config inválido em {caminho}: private_seed não é uma seed ed25519.")
        return None

    return Config(server_url=server_url, private_seed=private_seed, user_name=user_name)


# ---------------------------------------------------------------------------
# HTTP assinado (ed25519), igual ao formato do servidor
# ---------------------------------------------------------------------------


def _build_message(method: str, path: str, body: bytes, timestamp: int) -> bytes:
    digest = hashlib.sha256(body).hexdigest()
    return f"{method}\n{path}\n{digest}\n{timestamp}".encode("utf-8")


def _auth_headers(
    config: Config, method: str, path: str, body: bytes, timestamp: int
) -> dict[str, str]:
    private = Ed25519PrivateKey.from_private_bytes(
        base64.b64decode(config.private_seed)
    )
    public_b64 = base64.b64encode(
        private.public_key().public_bytes(Encoding.Raw, PublicFormat.Raw)
    ).decode("ascii")
    mensagem = _build_message(method, path, body, timestamp)
    assinatura = base64.b64encode(private.sign(mensagem)).decode("ascii")
    return {
        TIMESTAMP_HEADER: str(timestamp),
        SIGNATURE_HEADER: assinatura,
        KEY_HEADER: public_b64,
    }


def _open_json(
    url: str, method: str, body: bytes, headers: dict[str, str]
) -> Any:
    request = Request(url, data=body or None, method=method, headers=headers)
    try:
        with urlopen(request, timeout=HTTP_TIMEOUT_SECONDS) as resposta:
            raw = resposta.read()
            return json.loads(raw.decode("utf-8")) if raw else None
    except HTTPError as exc:
        detalhe = exc.read().decode("utf-8", "replace")
        try:
            parsed = json.loads(detalhe)
            detalhe = str(parsed.get("detail", detalhe))
        except (ValueError, AttributeError):
            pass
        raise ApiError(exc.code, detalhe) from None
    except (URLError, TimeoutError, OSError) as exc:
        motivo = getattr(exc, "reason", exc)
        raise NetworkError(str(motivo)) from None


def obter_hora_servidor(server_url: str) -> int:
    """Obtém a hora do servidor; é ela que assina os pedidos autenticados."""
    try:
        dados = _open_json(
            f"{server_url.rstrip('/')}/api/time", "GET", b"", {}
        )
    except ApiError as exc:
        raise NetworkError(f"não consegui obter /api/time ({exc})") from None
    try:
        return int(dados["time"])
    except (KeyError, TypeError, ValueError):
        raise NetworkError("resposta inválida de /api/time") from None


def listar_clientes(config: Config, timestamp: int) -> list[dict[str, Any]]:
    dados = _pedido_assinado(config, "GET", "/api/clients", None, timestamp)
    if not isinstance(dados, list):
        raise NetworkError("resposta inválida de /api/clients")
    return dados


def _com_nonce(path: str) -> str:
    """Torna cada assinatura única, mesmo com o mesmo timestamp.

    O servidor rejeita uma assinatura já vista (anti-replay). Sem um nonce, dois
    pedidos GET iguais no mesmo segundo teriam exatamente a mesma assinatura, o
    que acontece facilmente ao repetir a importação. O nonce vai no query
    string, que faz parte da mensagem assinada; os endpoints ignoram parâmetros
    extra.
    """
    separador = "&" if "?" in path else "?"
    return f"{path}{separador}nonce={uuid.uuid4().hex}"


def _pedido_assinado(
    config: Config,
    method: str,
    path: str,
    payload: dict[str, Any] | None,
    timestamp: int,
) -> Any:
    body = (
        b""
        if payload is None
        else json.dumps(payload, ensure_ascii=False, separators=(",", ":")).encode(
            "utf-8"
        )
    )
    signed_path = _com_nonce(path)
    headers = _auth_headers(config, method, signed_path, body, timestamp)
    if body:
        headers["Content-Type"] = "application/json"
    return _open_json(
        f"{config.server_url.rstrip('/')}{signed_path}", method, body, headers
    )


def _push_batch(config: Config, records: list[dict[str, Any]]) -> list[dict[str, Any]]:
    timestamp = obter_hora_servidor(config.server_url)
    dados = _pedido_assinado(
        config, "POST", "/api/push", {"records": records}, timestamp
    )
    if not isinstance(dados, list):
        raise NetworkError("resposta inválida de /api/push")
    return dados


# ---------------------------------------------------------------------------
# Leitura, comparação e possíveis duplicados
# ---------------------------------------------------------------------------


def _erro_validacao(validacao: ValidacaoCsv, mensagem: str) -> None:
    if len(validacao.erros) < MAX_ERROS_VALIDACAO:
        validacao.erros.append(mensagem)
    else:
        validacao.erros_truncados = True


def _problema_coordenada(validacao: ValidacaoCsv, mensagem: str) -> None:
    if len(validacao.problemas_coordenadas) < MAX_ERROS_VALIDACAO:
        validacao.problemas_coordenadas.append(mensagem)
    else:
        validacao.problemas_truncados = True


def _aviso_validacao(validacao: ValidacaoCsv, mensagem: str) -> None:
    if len(validacao.avisos) < MAX_ERROS_VALIDACAO:
        validacao.avisos.append(mensagem)


def validar_csv(rows: list[dict], cabecalho: list[str]) -> ValidacaoCsv:
    """Verifica se o export é compatível com o que o importador espera.

    Este safeguard corre antes de qualquer pedido ao servidor. Valida o
    cabeçalho, os limites de tamanho dos campos do servidor e as coordenadas.
    Linhas com erros de coordenadas são reportadas, mas não impedem a
    importação das restantes (o ``migrar`` já as salta). Problemas estruturais
    (colunas em falta, CSV vazio, nome demasiado longo, etc.) abortam tudo.
    """
    validacao = ValidacaoCsv(total=len(rows))

    if not cabecalho:
        _erro_validacao(validacao, "O CSV não tem cabeçalho.")
        return validacao

    em_falta = [campo for campo in CAMPOS_OBRIGATORIOS if campo not in cabecalho]
    if em_falta:
        _erro_validacao(
            validacao,
            "Colunas obrigatórias em falta: " + ", ".join(em_falta),
        )

    desconhecidas = [campo for campo in cabecalho if campo not in CAMPOS_CONHECIDOS]
    if desconhecidas:
        _aviso_validacao(
            validacao,
            "Colunas desconhecidas (ignoradas): " + ", ".join(desconhecidas),
        )

    vistos: set[str] = set()
    for numero, row in enumerate(rows, start=2):
        if None in row:
            _erro_validacao(
                validacao,
                f"Linha {numero}: tem mais campos do que o cabeçalho.",
            )
            continue

        nome = str(row.get("Nome") or "").strip()
        if not nome:
            continue

        nome_normalizado = normalize_nome(nome)
        if len(nome_normalizado) > MAX_NOME:
            _erro_validacao(
                validacao,
                f"Linha {numero}: Nome com {len(nome_normalizado)} caracteres "
                f"(máximo {MAX_NOME}).",
            )

        contrato = str(row.get("PDL") or "").strip()
        if len(contrato) > MAX_TEXTO:
            _erro_validacao(
                validacao,
                f"Linha {numero}: PDL/contrato com {len(contrato)} caracteres "
                f"(máximo {MAX_TEXTO}).",
            )

        notas = str(row.get("Notas") or "").strip()
        if len(notas) > MAX_TEXTO:
            _erro_validacao(
                validacao,
                f"Linha {numero}: Notas com {len(notas)} caracteres "
                f"(máximo {MAX_TEXTO}).",
            )

        rota = str(row.get("Rota") or "").strip()
        if len(rota) > MAX_TAG:
            _erro_validacao(
                validacao,
                f"Linha {numero}: Rota/tag com {len(rota)} caracteres "
                f"(máximo {MAX_TAG}).",
            )

        validacao.com_nome += 1
        if nome_normalizado in vistos:
            _aviso_validacao(
                validacao,
                f"Linha {numero}: nome repetido será colapsado: {nome}",
            )
        else:
            vistos.add(nome_normalizado)

        coords = str(row.get("Coordenadas") or "").strip()
        tipo = classify(coords)
        if tipo == "vazio":
            validacao.sem_coordenadas += 1
        elif tipo == "tipo_a":
            validacao.tipo_a += 1
            problemas: list[str] = []
            for indice, par in enumerate(split_pairs(coords), start=1):
                try:
                    latitude = float(par[0])
                    longitude = float(par[1])
                except ValueError:
                    problemas.append(f"par {indice}: coordenadas não numéricas")
                    continue
                if not -90 <= latitude <= 90:
                    problemas.append(f"par {indice}: latitude fora de -90..90")
                elif not -180 <= longitude <= 180:
                    problemas.append(f"par {indice}: longitude fora de -180..180")
            if problemas:
                validacao.erros_coordenadas += 1
                for problema in problemas:
                    _problema_coordenada(
                        validacao,
                        f"Linha {numero}: {problema}",
                    )
        elif tipo == "erro":
            validacao.erros_coordenadas += 1
            _problema_coordenada(
                validacao,
                f"Linha {numero}: coordenadas inválidas: {coords!r}",
            )
        else:
            pares = split_pairs(coords)
            problema: str | None = None
            if len(pares) != 1:
                problema = f"Linha {numero}: coordenadas inesperadas: {coords!r}"
            else:
                try:
                    latitude = float(pares[0][0])
                    longitude = float(pares[0][1])
                except ValueError:
                    problema = (
                        f"Linha {numero}: coordenadas não numéricas: {coords!r}"
                    )
                else:
                    if not -90 <= latitude <= 90:
                        problema = (
                            f"Linha {numero}: latitude fora de -90..90: {latitude}"
                        )
                    elif not -180 <= longitude <= 180:
                        problema = (
                            f"Linha {numero}: longitude fora de -180..180: {longitude}"
                        )
            if problema is not None:
                validacao.erros_coordenadas += 1
                _problema_coordenada(validacao, problema)

    validacao.nomes_unicos = len(vistos)
    if validacao.total == 0:
        _erro_validacao(validacao, "O CSV não tem linhas de dados.")
    if validacao.com_nome == 0:
        _erro_validacao(
            validacao,
            "O CSV não tem nenhuma linha com Nome preenchido.",
        )
    return validacao


def _imprimir_validacao(validacao: ValidacaoCsv) -> None:
    print()
    print("Verificação do CSV:")
    print(f"  linhas ................: {validacao.total}")
    print(f"  linhas com nome .......: {validacao.com_nome}")
    print(f"  nomes únicos ..........: {validacao.nomes_unicos}")
    print(f"  sem coordenadas .......: {validacao.sem_coordenadas}")
    print(f"  TIPO A ................: {validacao.tipo_a}")
    print(f"  coordenadas inválidas .: {validacao.erros_coordenadas}")
    if validacao.avisos:
        print(f"  avisos ................: {len(validacao.avisos)}")
        for aviso in validacao.avisos:
            print(f"    - {aviso}")
    if validacao.problemas_coordenadas:
        print("  problemas de coordenadas:")
        for problema in validacao.problemas_coordenadas:
            print(f"    - {problema}")
        if validacao.problemas_truncados:
            print(f"    - ... e mais (limite {MAX_ERROS_VALIDACAO}).")
    if validacao.erros:
        print(f"  ERROS .................: {len(validacao.erros)}")
        for erro in validacao.erros:
            print(f"    - {erro}")
        if validacao.erros_truncados:
            print(f"    - ... e mais erros (limite {MAX_ERROS_VALIDACAO}).")


def ler_exclusoes(caminho: Path = EXCLUDE_PATH) -> set[str]:
    if not caminho.exists():
        return set()
    nomes: set[str] = set()
    try:
        for linha in caminho.read_text(encoding="utf-8").splitlines():
            linha = linha.strip()
            if not linha or linha.startswith("#"):
                continue
            nomes.add(normalize_nome(linha))
    except OSError as exc:
        print(f"Aviso: não consegui ler {caminho}: {exc}")
    return nomes


def _nome_loose(nome: str) -> str:
    """Nome sem acentos/pontuação, para detetar renomeações pequenas."""
    decomposto = unicodedata.normalize("NFKD", nome)
    sem_acentos = "".join(c for c in decomposto if not unicodedata.combining(c))
    return re.sub(r"[^A-Z0-9]+", " ", sem_acentos.upper()).strip()


def _semelhanca(a: str, b: str) -> float:
    return difflib.SequenceMatcher(None, a, b).ratio()


def _possivel_duplicado(
    cliente: dict[str, Any], existentes: list[dict[str, Any]]
) -> dict[str, Any] | None:
    nome = normalize_nome(str(cliente.get("nome") or ""))
    nome_loose = _nome_loose(nome)
    contrato = str(cliente.get("contrato") or "").strip()
    melhor: dict[str, Any] | None = None
    melhor_score = 0.0

    for existente in existentes:
        existente_nome = normalize_nome(str(existente.get("nome") or ""))
        if nome_loose == _nome_loose(existente_nome):
            return existente
        existente_contrato = str(existente.get("contrato") or "").strip()
        if not contrato or not existente_contrato or contrato != existente_contrato:
            continue
        score = _semelhanca(nome, existente_nome)
        if score >= 0.92 and score > melhor_score:
            melhor = existente
            melhor_score = score
    return melhor


def _coordenadas_validas(cliente: dict[str, Any]) -> bool:
    """Confirma os limites do servidor para coordenadas já convertidas."""
    latitude = cliente.get("latitude")
    longitude = cliente.get("longitude")
    if latitude is None or longitude is None:
        return True
    return -90 <= latitude <= 90 and -180 <= longitude <= 180


def analisar(
    rows: list[dict[str, Any]],
    timestamp: int,
    existentes: list[dict[str, Any]],
    exclusoes: set[str],
) -> Analise:
    clientes, rel = migrar(rows, timestamp)
    por_nome = {normalize_nome(str(c["nome"])): c for c in existentes}
    tipo_a = {normalize_nome(caso.nome): caso for caso in rel.tipo_a}
    tipo_b = {normalize_nome(caso.nome): caso for caso in rel.tipo_b}

    novos: list[ClienteNovo] = []
    existentes_no_csv: list[dict[str, Any]] = []
    excluidos: list[tuple[dict[str, Any], str]] = []

    for cliente in clientes:
        nome = normalize_nome(str(cliente.get("nome") or ""))
        if not _coordenadas_validas(cliente):
            excluidos.append((cliente, "coordenadas fora dos limites"))
            continue
        if nome in exclusoes:
            excluidos.append((cliente, "excluído pelo ficheiro memento-import-exclude.txt"))
            continue
        if nome in por_nome:
            existentes_no_csv.append(cliente)
            continue
        item = ClienteNovo(
            cliente=cliente,
            origem="tipo_a" if nome in tipo_a else "tipo_b" if nome in tipo_b else "normal",
            caso_tipo_a=tipo_a.get(nome),
            caso_tipo_b=tipo_b.get(nome),
        )
        item.existente_semelhante = _possivel_duplicado(cliente, existentes)
        novos.append(item)

    return Analise(
        rel=rel,
        novos=novos,
        existentes=existentes_no_csv,
        excluidos=excluidos,
        total_linhas=len(rows),
        total_servidor=len(existentes),
    )


# ---------------------------------------------------------------------------
# Interação
# ---------------------------------------------------------------------------


def _ask_yes_no(prompt: str, default: bool = False) -> bool:
    sufixo = "[S/n]" if default else "[s/N]"
    while True:
        raw = input(f"{prompt} {sufixo} ").strip().lower()
        if raw == "":
            return default
        if raw in ("s", "sim", "y", "yes"):
            return True
        if raw in ("n", "nao", "não", "no"):
            return False
        print("Responde s (sim) ou n (não).")


def _ask_index(total: int, default: int = 1) -> int:
    while True:
        raw = input(f"Coordenadas a usar (1-{total}) [default: {default}]: ").strip()
        if raw == "":
            return default
        if raw.isdigit() and 1 <= int(raw) <= total:
            return int(raw)
        print(f"Escolhe um número entre 1 e {total}.")


def _fmt_coords(cliente: dict[str, Any]) -> str:
    lat = cliente.get("latitude")
    lon = cliente.get("longitude")
    if lat is None or lon is None:
        return "sem coordenadas"
    return f"{lat}, {lon}"


def _fmt_notas(cliente: dict[str, Any], limite: int = 220) -> str:
    notas = str(cliente.get("notas") or "").replace("\n", " ").strip()
    if not notas:
        return "—"
    if len(notas) > limite:
        return notas[: limite - 3] + "..."
    return notas


def _fmt_contrato(cliente: dict[str, Any]) -> str:
    return str(cliente.get("contrato") or "—")


def _notas_tipo_a(caso: CasoTipoA, escolhido_idx: int) -> str | None:
    restantes = [
        par for i, par in enumerate(caso.pares) if i != escolhido_idx
    ]
    alternativas = " | ".join(f"{lat},{lon}" for lat, lon in restantes)
    partes = [
        caso.notas_originais,
        f"Coordenadas alternativas: {alternativas}" if alternativas else "",
    ]
    return "\n\n".join(p for p in partes if p) or None


def revisar_tipos_a_b(analise: Analise) -> None:
    casos = list(analise.rel.tipo_a) + list(analise.rel.tipo_b)
    if not casos:
        return

    if not _ask_yes_no(
        f"Rever TIPO A/B um a um? ({len(casos)} caso(s))", default=False
    ):
        return

    por_nome = {
        normalize_nome(str(item.cliente["nome"])): item for item in analise.novos
    }

    for caso in analise.rel.tipo_a:
        item = por_nome.get(normalize_nome(caso.nome))
        print()
        print(f"TIPO A — {caso.nome}")
        print(f"  pares: {', '.join(f'{a},{b}' for a, b in caso.pares)}")
        if item is None:
            print("  Já existe ou foi excluído; não é revisto.")
            continue
        idx = _ask_index(len(caso.pares), default=1)
        escolhido = caso.pares[idx - 1]
        item.cliente["latitude"] = float(escolhido[0])
        item.cliente["longitude"] = float(escolhido[1])
        item.cliente["notas"] = _notas_tipo_a(caso, idx - 1)

    for caso in analise.rel.tipo_b:
        item = por_nome.get(normalize_nome(caso.nome))
        print()
        print(f"TIPO B — {caso.nome} (x{caso.linhas})")
        contratos = sorted({c for c in caso.contratos if c})
        if len(contratos) > 1:
            print(f"  Aviso: PDL/contrato diferentes: {', '.join(contratos)}")
            print("  Mantém-se o contrato da primeira linha.")
        rotas = sorted({r for r in caso.rotas if r})
        if len(rotas) > 1:
            print(f"  Aviso: Rotas/tags diferentes: {', '.join(rotas)}")
            print("  Mantém-se a tag da primeira linha.")
        if not caso.pares_validos:
            print("  Sem coordenadas válidas; fica sem localização.")
            continue
        print(
            "  pares: "
            + ", ".join(f"{a},{b}" for a, b in caso.pares_validos)
        )
        if item is None:
            print("  Já existe ou foi excluído; não é revisto.")
            continue
        idx = _ask_index(len(caso.pares_validos), default=1)
        escolhido = caso.pares_validos[idx - 1]
        item.cliente["latitude"] = float(escolhido[0])
        item.cliente["longitude"] = float(escolhido[1])


def _print_cliente(item: ClienteNovo, indice: int, total: int) -> None:
    cliente = item.cliente
    print()
    print(f"[{indice}/{total}] {cliente.get('nome')}")
    print(f"  PDL (contrato): {_fmt_contrato(cliente)}")
    print(f"  Tag (Rota): {cliente.get('tag') or '—'}")
    print(f"  Coordenadas: {_fmt_coords(cliente)}")
    print(f"  Notas: {_fmt_notas(cliente)}")
    if item.origem != "normal":
        print(f"  Caso: {item.origem.upper().replace('_', ' ')}")
    if item.existente_semelhante is not None:
        existente = item.existente_semelhante
        print(
            "  Aviso: possível duplicado/renomeação de "
            f"{existente.get('nome')} (PDL {_fmt_contrato(existente)})."
        )


def _print_existente(existente: dict[str, Any]) -> None:
    print()
    print("Cliente existente no servidor:")
    print(f"  id: {existente.get('id')}")
    print(f"  nome: {existente.get('nome')}")
    print(f"  PDL (contrato): {_fmt_contrato(existente)}")
    print(f"  coordenadas: {_fmt_coords(existente)}")
    print(f"  notas: {_fmt_notas(existente, limite=1000)}")


def _print_detalhes(item: ClienteNovo) -> None:
    cliente = item.cliente
    print()
    print("Detalhes:")
    print(f"  nome: {cliente.get('nome')}")
    print(f"  PDL (contrato): {_fmt_contrato(cliente)}")
    print(f"  tag: {cliente.get('tag') or '—'}")
    print(f"  coordenadas: {_fmt_coords(cliente)}")
    print(f"  notas: {_fmt_notas(cliente, limite=100000)}")
    if item.existente_semelhante is not None:
        _print_existente(item.existente_semelhante)


def revisar_duvidosos(analise: Analise) -> None:
    duvidosos = [item for item in analise.novos if item.existente_semelhante is not None]
    if not duvidosos:
        return

    if not _ask_yes_no(
        f"Encontrei {len(duvidosos)} possível(is) duplicado(s)/renomeação(ões). "
        "Rever um a um?",
        default=False,
    ):
        for item in duvidosos:
            item.decisao = "saltado_duplicado"
        print(f"  {len(duvidosos)} cliente(s) saltados por possível duplicado.")
        return

    for indice, item in enumerate(duvidosos, start=1):
        while True:
            _print_cliente(item, indice, len(duvidosos))
            raw = input("Incluir na revisão? [s/N] (v=ver existente) ").strip().lower()
            if raw in ("", "n", "nao", "não"):
                item.decisao = "saltado_duplicado"
                break
            if raw in ("s", "sim", "y", "yes"):
                item.decisao = "incluido"
                break
            if raw == "v":
                if item.existente_semelhante is not None:
                    _print_existente(item.existente_semelhante)
                continue
            print("Opção inválida. Usa s, n ou v.")


def confirmar_um_a_um(itens: list[ClienteNovo]) -> list[ClienteNovo] | None:
    """Confirma cliente a cliente. Devolve None se o utilizador abortar."""
    confirmados: list[ClienteNovo] = []
    total = len(itens)
    for i, item in enumerate(itens, start=1):
        while True:
            _print_cliente(item, i, total)
            raw = input(
                "Importar? [s/N] (v=ver, f=finalizar, q=abortar) "
            ).strip().lower()
            if raw in ("", "n", "nao", "não", "no"):
                item.decisao = "saltado"
                break
            if raw in ("s", "sim", "y", "yes"):
                item.decisao = "confirmado"
                confirmados.append(item)
                break
            if raw == "v":
                _print_detalhes(item)
                continue
            if raw == "f":
                item.decisao = "nao_revisto"
                for restante in itens[i:]:
                    restante.decisao = "nao_revisto"
                print("Revisão terminada; envio dos clientes já confirmados.")
                return confirmados
            if raw == "q":
                for confirmado in confirmados:
                    confirmado.decisao = "abortado"
                item.decisao = "abortado"
                for restante in itens[i:]:
                    restante.decisao = "abortado"
                return None
            print("Opção inválida. Usa s, n, v, f ou q.")
    return confirmados


# ---------------------------------------------------------------------------
# Envio e relatório
# ---------------------------------------------------------------------------


def _cliente_payload(cliente: dict[str, Any]) -> dict[str, Any]:
    return {
        "id": cliente["id"],
        "nome": cliente["nome"],
        "contrato": cliente.get("contrato"),
        "tag": cliente.get("tag"),
        "latitude": cliente.get("latitude"),
        "longitude": cliente.get("longitude"),
        "notas": cliente.get("notas"),
        "ativo": bool(cliente.get("ativo", 1)),
        "updated_at": int(cliente["updated_at"]),
    }


def enviar(confirmados: list[ClienteNovo], config: Config) -> int:
    """Envia em lotes; devolve o número de clientes que não ficaram aceites."""
    erros = 0
    total_lotes = (len(confirmados) + BATCH_SIZE - 1) // BATCH_SIZE
    for numero_lote, inicio in enumerate(
        range(0, len(confirmados), BATCH_SIZE), start=1
    ):
        lote = confirmados[inicio : inicio + BATCH_SIZE]
        records = [
            {"client": _cliente_payload(item.cliente), "fotos": []} for item in lote
        ]
        while True:
            try:
                resultados = _push_batch(config, records)
                break
            except NetworkError as exc:
                if _ask_yes_no(
                    f"Erro de rede no lote {numero_lote}/{total_lotes}: {exc}. "
                    "Tentar novamente?",
                    default=True,
                ):
                    continue
                for item in lote:
                    item.decisao = "erro"
                erros += len(lote)
                print("Envio interrompido por erro de rede.")
                return erros
            except ApiError as exc:
                for item in lote:
                    item.decisao = "erro"
                erros += len(lote)
                print(f"Erro no lote {numero_lote}/{total_lotes}: {exc}")
                if exc.status == 401:
                    print(
                        "A chave não foi aceite. Confirma o config e, se "
                        "necessário, apaga-o e corre o comando outra vez."
                    )
                return erros

        por_id = {
            str(resultado.get("id")): resultado
            for resultado in resultados
            if isinstance(resultado, dict)
        }
        aceites = 0
        duplicados = 0
        for item in lote:
            resultado = por_id.get(str(item.cliente["id"]))
            if resultado is None:
                item.decisao = "erro"
                erros += 1
                continue
            status = str(resultado.get("status") or "")
            if status == "accepted":
                item.decisao = "importado"
                aceites += 1
            elif status == "duplicate":
                item.decisao = "duplicado"
                duplicados += 1
            elif status == "stale":
                item.decisao = "stale"
                erros += 1
            else:
                item.decisao = "erro"
                erros += 1
        print(
            f"  Lote {numero_lote}/{total_lotes}: {len(lote)} cliente(s); "
            f"{aceites} aceite(s), {duplicados} duplicado(s)."
        )
    return erros


def _linha_cliente(cliente: dict[str, Any]) -> str:
    return (
        f"{cliente.get('nome')} | PDL {_fmt_contrato(cliente)} | "
        f"{_fmt_coords(cliente)}"
    )


def escrever_relatorio(
    caminho: Path,
    analise: Analise,
    final: bool = False,
    abortado: bool = False,
) -> None:
    linhas: list[str] = []
    linhas.append("Relatório de importação Memento -> ElisDB")
    linhas.append(f"Data: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    linhas.append("")
    linhas.append(f"Linhas no CSV: {analise.total_linhas}")
    linhas.append(f"Clientes no servidor: {analise.total_servidor}")
    linhas.append(f"Novos clientes: {len(analise.novos)}")
    linhas.append(f"Já existentes (ignorados): {len(analise.existentes)}")
    linhas.append(f"Excluídos: {len(analise.excluidos)}")
    linhas.append(f"Nomes vazios ignorados: {analise.rel.linhas_ignoradas}")
    linhas.append(f"Erros de parsing: {len(analise.rel.erros_parsing)}")
    linhas.append(f"Sem coordenadas: {analise.sem_coordenadas}")
    linhas.append(f"TIPO A: {len(analise.rel.tipo_a)}")
    linhas.append(f"TIPO B: {len(analise.rel.tipo_b)}")
    linhas.append(f"Possíveis duplicados/renomeações: {analise.possiveis_duplicados}")
    if abortado:
        linhas.append("Estado: ABORTADO pelo utilizador; nada foi enviado.")
    elif final:
        linhas.append("Estado: revisão terminada; resultado abaixo.")
    else:
        linhas.append("Estado: pré-visualização; nada foi enviado ainda.")
    linhas.append("")

    linhas.append("Novos clientes:")
    if not analise.novos:
        linhas.append("  (nenhum)")
    for item in analise.novos:
        linhas.append(f"  - {_linha_cliente(item.cliente)} | {item.decisao}")
    linhas.append("")

    linhas.append("Já existentes no servidor (ignorados, nunca alterados):")
    if not analise.existentes:
        linhas.append("  (nenhum)")
    for cliente in analise.existentes:
        linhas.append(f"  - {_linha_cliente(cliente)}")
    linhas.append("")

    linhas.append("Excluídos:")
    if not analise.excluidos:
        linhas.append("  (nenhum)")
    for cliente, motivo in analise.excluidos:
        linhas.append(f"  - {_linha_cliente(cliente)} | {motivo}")
    linhas.append("")

    linhas.append("Erros de parsing:")
    if not analise.rel.erros_parsing:
        linhas.append("  (nenhum)")
    for nome, coords in analise.rel.erros_parsing:
        linhas.append(f"  - {nome}: {coords!r}")
    linhas.append("")

    linhas.append("TIPO A:")
    if not analise.rel.tipo_a:
        linhas.append("  (nenhum)")
    for caso in analise.rel.tipo_a:
        pares = ", ".join(f"{a},{b}" for a, b in caso.pares)
        linhas.append(f"  - {caso.nome}: pares [{pares}]")
    linhas.append("")

    linhas.append("TIPO B:")
    if not analise.rel.tipo_b:
        linhas.append("  (nenhum)")
    for caso in analise.rel.tipo_b:
        pares = ", ".join(f"{a},{b}" for a, b in caso.pares_validos)
        contratos = ", ".join(c for c in caso.contratos if c) or "—"
        linhas.append(
            f"  - {caso.nome} (x{caso.linhas}): pares [{pares}] | "
            f"PDL [{contratos}]"
        )
    linhas.append("")

    linhas.append("Resultado do envio:")
    enviados = [item for item in analise.novos if item.decisao != "pendente"]
    if not enviados:
        linhas.append("  (nada enviado)")
    for item in enviados:
        linhas.append(f"  - {item.cliente.get('nome')}: {item.decisao}")
    linhas.append("")

    try:
        caminho.write_text("\n".join(linhas) + "\n", encoding="utf-8")
    except OSError as exc:
        print(f"Aviso: não consegui escrever o relatório {caminho}: {exc}")


def _imprimir_resumo(analise: Analise) -> None:
    print()
    print("Resumo:")
    print(f"  linhas no CSV ........: {analise.total_linhas}")
    print(f"  clientes no servidor .: {analise.total_servidor}")
    print(f"  novos ................: {len(analise.novos)}")
    print(f"  já existentes ........: {len(analise.existentes)} (ignorados)")
    print(f"  excluídos ............: {len(analise.excluidos)}")
    print(f"  nomes vazios .........: {analise.rel.linhas_ignoradas} (ignorados)")
    print(f"  erros de parsing .....: {len(analise.rel.erros_parsing)} (saltados)")
    print(f"  sem coordenadas ......: {analise.sem_coordenadas}")
    print(f"  TIPO A / TIPO B ......: {len(analise.rel.tipo_a)} / {len(analise.rel.tipo_b)}")
    print(f"  possíveis duplicados .: {analise.possiveis_duplicados}")


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------


def main(
    csv_path: Path = CSV_PATH,
    config_path: Path = CONFIG_PATH,
    report_path: Path = REPORT_PATH,
    exclude_path: Path = EXCLUDE_PATH,
) -> int:
    print("ElisDB — importação de clientes do Memento")
    print("=" * 52)

    if not csv_path.exists():
        print(f"Erro: não encontrei o CSV em {csv_path}.")
        print("Exporta a base de dados do Memento e guarda o ficheiro nesse caminho.")
        return 1

    try:
        cabecalho, rows = ler_csv_com_cabecalho(str(csv_path))
    except (OSError, ValueError, csv.Error) as exc:
        print(f"Erro ao ler {csv_path}: {exc}")
        return 1

    validacao = validar_csv(rows, cabecalho)
    print(f"[1/8] CSV: {validacao.total} linhas lidas.")
    _imprimir_validacao(validacao)
    if not validacao.ok:
        print()
        print("Safeguard: o CSV não é compatível com a ElisDB.")
        print("Corrige os erros acima e volta a correr o comando. Nada foi enviado.")
        return 1

    if validacao.problemas_coordenadas:
        print(
            "Aviso: há problemas de coordenadas; as linhas afetadas podem ser "
            "saltadas, mas as restantes podem ser importadas."
        )

    config = carregar_config(config_path)
    if config is None:
        return 1

    try:
        timestamp = obter_hora_servidor(config.server_url)
        existentes = listar_clientes(config, timestamp)
    except NetworkError as exc:
        print(f"Erro de rede: {exc}")
        print("Confirma que o servidor está a correr e que o URL do config está certo.")
        return 1
    except ApiError as exc:
        print(f"Erro do servidor ao listar clientes: {exc}")
        if exc.status == 401:
            print(
                "A chave não foi aceite. Confirma o config e, se necessário, "
                "apaga-o e corre o comando outra vez."
            )
        return 1
    print(f"[2/8] Servidor: {len(existentes)} clientes existentes.")

    exclusoes = ler_exclusoes(exclude_path)
    analise = analisar(rows, timestamp, existentes, exclusoes)
    print("[3/8] Comparação feita.")
    _imprimir_resumo(analise)
    escrever_relatorio(report_path, analise, final=False)
    print(f"[4/8] Relatório de pré-visualização: {report_path}")

    if not analise.novos:
        print("Nada a importar: todos os clientes do CSV já existem ou foram excluídos.")
        escrever_relatorio(report_path, analise, final=True)
        return 0

    print("[5/8] Revisão de casos especiais.")
    revisar_tipos_a_b(analise)
    revisar_duvidosos(analise)

    confirmaveis = [
        item
        for item in analise.novos
        if item.decisao in ("pendente", "incluido")
    ]
    if not confirmaveis:
        print("Nenhum cliente por confirmar. Nada foi enviado.")
        escrever_relatorio(report_path, analise, final=True)
        return 0

    print(f"[6/8] Confirmação 1 a 1: {len(confirmaveis)} cliente(s).")
    confirmados = confirmar_um_a_um(confirmaveis)
    if confirmados is None:
        print("Importação abortada. Nada foi enviado.")
        escrever_relatorio(report_path, analise, final=True, abortado=True)
        return 0
    if not confirmados:
        print("Nenhum cliente confirmado. Nada foi enviado.")
        escrever_relatorio(report_path, analise, final=True)
        return 0

    print(
        f"[7/8] A enviar {len(confirmados)} cliente(s) em lotes de {BATCH_SIZE}..."
    )
    erros = enviar(confirmados, config)
    escrever_relatorio(report_path, analise, final=True)
    print("[8/8] Relatório final escrito.")

    importados = sum(1 for item in analise.novos if item.decisao == "importado")
    duplicados = sum(1 for item in analise.novos if item.decisao == "duplicado")
    saltados = sum(
        1 for item in analise.novos if item.decisao in ("saltado", "saltado_duplicado")
    )
    nao_revistos = sum(1 for item in analise.novos if item.decisao == "nao_revisto")
    confirmados_nao_enviados = sum(
        1 for item in analise.novos if item.decisao == "confirmado"
    )
    print()
    print("Concluído.")
    print(f"  importados ....: {importados}")
    print(f"  duplicados ....: {duplicados}")
    print(f"  saltados ......: {saltados}")
    print(f"  não revistos ..: {nao_revistos}")
    print(f"  confirmados não enviados: {confirmados_nao_enviados}")
    print(f"  erros .........: {erros}")
    print(f"  relatório .....: {report_path}")
    print("Sincroniza os telemóveis para receberem os novos clientes.")
    return 0 if erros == 0 else 1


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (KeyboardInterrupt, EOFError):
        print("\nCancelado. Nada foi enviado.")
        raise SystemExit(130)
