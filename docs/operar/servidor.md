# Servidor: correr, parar, ver estado

## Subir e parar

Na pasta `elisdb-server/`:

```bash
docker compose up -d      # subir (constrói a imagem se necessário)
docker compose ps         # estado e saúde
docker compose logs -f    # acompanhar logs
docker compose stop       # parar sem apagar dados
docker compose down       # remover o contentor (os dados ficam)
```

O serviço publica a porta 8000. Por omissão só em `127.0.0.1`; para a rede local
define `ELISDB_BIND_ADDRESS` (ex.: `0.0.0.0`) no `.env`.

Se a porta estiver ocupada, muda o mapeamento no `compose.yaml`.

## Saúde

```bash
curl http://localhost:8000/health
# {"status":"ok","version":"1.5.0","api_version":2,"instance_id":"..."}
curl http://localhost:8000/ready
```

- `/health` — o processo responde.
- `/ready` — a base de dados está acessível.
- `/api/time` — público, usado pela app para medir o desvio de relógio.

Os restantes `/api/*` exigem chave; sem ela devolvem `401`. Isso é normal.

## Dados e permissões

| Caminho no contentor | No host |
|---|---|
| `/app/data/elisdb.db` | `elisdb-data/elisdb.db` |
| `/app/data/photos/` | `elisdb-data/photos/` |
| `/app/data/backups/` | `elisdb-data/backups/` |
| `/app/data/apk/latest/` | `elisdb-data/apk/latest/` |
| `/app/data/configs/` | `configs/` |

São **bind mounts**: vês e copias os ficheiros sem `docker cp`, e sobrevivem a
reinícios e rebuilds.

O contentor corre como `PUID:PGID` (omissão `1000:1000`) e **não** altera donos.
Se os diretórios não forem graváveis por esse uid, o arranque falha com uma
mensagem a indicar o `chown` a fazer.

## Modo local (sem Docker)

A TUI (`python start.py`) consegue correr o servidor localmente com
`uvicorn elisdb.main:app` e guardar o pid em `.run/server.pid`. Os dois modos são
**exclusivos** e partilham os mesmos `elisdb-data/` e `configs/`.

O modo local usa o Python do host ou `.venv/bin/python` (override:
`ELISDB_PYTHON`) e precisa das dependências instaladas:

```bash
uv venv .venv && uv pip install -r requirements.txt
```

Em produção usa Docker: o modo local existe para desenvolvimento e diagnóstico.

## Atualizar o servidor

Ver [deploy](deploy.md). O código publicado tem de ser um commit que o Server CI
já testou.

## Produção

- **HTTPS é do operador.** O servidor fala HTTP simples; põe um proxy TLS à
  frente ([Nginx Proxy Manager](nginx.md)) e aponta a app para `https://`.
- **Um único worker.** O cache anti-replay e o rate limit vivem em memória; o
  arranque falha se `WEB_CONCURRENCY`/`UVICORN_WORKERS` for maior que 1.
- **Versões fixas.** As dependências estão presas em `requirements.txt` para
  imagens reprodutíveis.
- **Backups automáticos** estão ligados por omissão (diário, retenção 3). Ver
  [backups](backup.md).
