# Design — add-server-tui

## Forma

`start.py` é um script autónomo (stdlib apenas) que corre no host, na raiz de
`elisdb-server/`. Lê o mesmo `.env` que o `compose.yaml` e resolve os caminhos a
partir de `ELISDB_DATA_DIR`, para que o modo local e o Docker usem exatamente os
mesmos ficheiros.

```
start.py
  ├─ env.py        leitura/escrita do .env (comentários preservados)
  ├─ status.py     snapshot do estado (partilhado com o servidor)
  └─ actions.py    start/stop local e Docker, utilizadores
```

Para manter o âmbito pequeno, `env.py` e `actions.py` ficam como funções dentro
de `start.py`; só `status.py` vai para o pacote `elisdb/` porque também é útil
fora da TUI (testes, futura API).

## Modos

| Modo | Como arranca | Como verifica |
|---|---|---|
| Docker | `docker compose up -d` | `docker compose ps` + `/health` |
| Local | `python -m uvicorn elisdb.main:app` (subprocesso) | pid em `.run/server.pid` + `/health` |

Regras:

- Nunca os dois: se o contentor estiver a correr, o modo local recusa; se o pid
  local estiver vivo, o Docker recusa.
- O modo local injeta no subprocesso as variáveis do `.env`, com os caminhos
  traduzidos (`/app/data` → `./elisdb-data`), para paridade com o contentor.
- Parar é sempre explícito e confirmado; nunca `docker compose down -v`.

## Estado

`elisdb/status.py` expõe `collect(data_dir, db_path, configs_dir, docker=True)`
e devolve um dicionário serializável:

```python
{
  "mode": "docker" | "local" | "stopped",
  "data_dir": "...", "owner": "1001:1001", "writable": True,
  "sizes": {"db": 462848, "photos": ..., "backups": ..., "apk": ...},
  "counts": {"clients": 527, "photos": 197, "tombstones": 21},
  "keys": {"active": 7, "total": 14},
  "port": 8000, "health": "ok" | "unreachable",
}
```

Contagens por SQL direto em modo read-only (`mode=ro`), para não bloquear
escritas nem migrar nada.

## Utilizadores

`keys.py` ganha:

- `list --json`: lista id, comentário, criada, revogada, `provisioned`.
- `revoke --id`: revoga **uma** chave (`revoked_at`), preservando a linha.

A TUI usa estes comandos via `docker compose exec` (modo Docker) ou chamada
direta (modo local), e mostra o resultado agrupado por utilizador. Nunca
sobrescreve `<nome>.config.json`; se existir, sugere `<nome>-2.config.json`.

## Configuração

Leitura: `KEY=valor` por linha; comentários e linhas desconhecidas preservados.
Escrita: valida tudo primeiro, grava em `tmp` no mesmo diretório, `os.replace`,
`chmod 600`, e guarda `.env.bak-<timestamp>` (0600).

Validações mínimas: inteiros positivo/≥0 conforme o campo, URL absoluta
`http(s)`, lista de utilizadores com nomes seguros, `PUID/PGID` inteiros,
`ELISDB_DATA_DIR` criável. Se algo falhar, não escreve nada.

Aplicar: mostra o diff e pergunta; se sim, reinicia o modo ativo e mostra o
resultado. Se o arranque falhar, oferece repor o `.env.bak`.

## Alternativas rejeitadas

- **Textual/rich**: dependência grande para um menu numerado; stdlib chega.
- **TUI dentro do contentor**: não vê os binds do host nem pode arrancar/parar o
  próprio contentor; teria de falar com o daemon.
- **Web UI**: âmbito muito maior; não é necessário para operação local.
