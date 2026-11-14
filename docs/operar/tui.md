# Gestão do servidor (TUI)

`start.py` é um menu de terminal só com a biblioteca padrão, para operar o
servidor sem decorar comandos de Docker nem editar ficheiros à mão.

```bash
cd elisdb-server
python start.py
```

```
1) Status   2) Iniciar   3) Parar   4) Utilizadores   5) Configuração   0) Sair
```

## Status

Modo ativo (`docker`, `local` ou `parado`), diretório de dados, dono e permissão
de escrita, tamanho de `elisdb.db`/`photos/`/`backups/`/`apk/`, contagens de
clientes, fotos e tombstones, chaves ativas, backups (quantos e o mais recente) e
o `/health`. É só leitura — abre a base de dados em modo `read-only`.

## Iniciar

- **Local** — `uvicorn elisdb.main:app` em `127.0.0.1:8000` como o teu
  utilizador, com o mesmo `ELISDB_DATA_DIR` do Docker. Log em `.run/server.log`,
  PID em `.run/server.pid`.
- **Docker** — `docker compose up -d --build`.

O modo local precisa das dependências Python. Ordem de escolha do interpreter:
`ELISDB_PYTHON` → `.venv/bin/python` → o Python que correu o `start.py`.

```bash
uv venv .venv && uv pip install -r requirements.txt   # recomendado
# alternativa: python3 -m venv .venv && .venv/bin/pip install -r requirements.txt
```

Em produção usa Docker — não é preciso instalar nada no host. Os dois modos são
exclusivos: com um ativo, o outro é recusado. Antes de arrancar, `elisdb-data/` e
`configs/` têm de ser graváveis pelo teu uid/gid (o contentor corre com
`PUID:PGID`).

## Utilizadores

Lista agrupada por utilizador (chaves ativas/total, e se existe
`configs/<nome>.config.json`), adiciona (emite chave + ficheiro `0600`), revoga
por utilizador ou por ID, e mostra o detalhe.

- Revogar é *soft*: a linha fica com `revoked_at`, preservando a auditoria.
  Apagar a linha exige confirmação escrita.
- Nunca sobrescreve um `.config.json` existente; sugere outro nome.
- Recusa revogar a última chave ativa.
- A seed privada nunca é impressa.

## Configuração

Edita grupos de variáveis do `.env` ou o ficheiro inteiro no `$EDITOR`. Valida
antes de escrever (inteiros, booleanos, URL, nível de log), guarda
`.env.bak-<timestamp>`, mostra o diff e pergunta **“aplicar agora?”**; se
aceitares, reinicia o modo que estava ativo e, em caso de falha, oferece repor o
backup.

`PUID`/`PGID` diferentes do dono de `elisdb-data/`/`configs/` são recusados: a TUI
nunca corre `sudo` nem `chown`.

## Flags

```bash
python start.py --status
python start.py --list-users [--json]
python start.py --add-user maria --server-url https://elisdb.exemplo.pt
python start.py --revoke maria --yes
```

## Limites

Não faz deploy (usa `docs/operar/deploy.md`), não altera o esquema, não corre
`docker compose down -v` e nunca apaga dados.

## Docker via snap

Em Docker instalado por snap, `security_opt: no-new-privileges` com entrypoint em
script pode falhar com `exec /usr/local/bin/entrypoint.sh: operation not
permitted`. É limitação do confinamento do snap, não do compose (em Docker normal
funciona). Para testar localmente, usa um override **só de teste**:

```yaml
# /tmp/compose.snap.yaml
services:
  elisdb-server:
    security_opt: !reset []
```

```bash
COMPOSE_FILE=compose.yaml:/tmp/compose.snap.yaml python start.py
```

Não commites o override.
