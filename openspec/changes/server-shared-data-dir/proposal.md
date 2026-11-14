# Proposal

## Why

Hoje o servidor tem três layouts de dados diferentes: o `compose.yaml` do repo usa
um volume nomeado, a produção usa um bind mount em `./elisdb-data`, e uma execução
local sem variáveis escreve em `elisdb-server/elisdb.db` + `photos/`. Além disso:

- `configs/` vive **dentro** do diretório de dados e os ficheiros criados por
  `docker compose exec` ficam `root:root`, obrigando a `docker cp`/`chown`.
- O contentor arranca como root só para `chown -R` de ~600 MB e depois desce para
  `PUID` com `setpriv`.
- `ELISDB_USERS` provisiona e **revoga** chaves automaticamente no arranque, um
  comportamento implícito e destrutivo que ninguém pediu.
- O `Dockerfile` faz `COPY . .` e o `.dockerignore` não exclui `configs/`, `.env`
  nem `elisdb-data/`, pelo que segredos locais podem entrar na imagem.
- Não há um ficheiro único de configuração: os valores estão inline no
  `compose.yaml` da VPS e alguns `docker run` dependem de defaults do código.

## What Changes

- Mover `Dockerfile` e `docker-entrypoint.sh` para `docker/`.
- Compose com bind mounts explícitos: `${ELISDB_DATA_DIR:-./elisdb-data}:/app/data`
  e `./configs:/app/data/configs`; `user: "${PUID:-1000}:${PGID:-1000}"`.
- Entrypoint sem root, sem `chown`, sem `setpriv`: `mkdir -p` + `exec "$@"`.
- Remover `ELISDB_USERS` e `elisdb/provision.py`; chaves só por ação explícita do
  operador (CLI agora, TUI na fase 3).
- `.env` (0600, ignorado) + `.env.example` versionado; `compose.yaml` só interpola.
- `Dockerfile` com `COPY` explícito e `.dockerignore` a excluir `configs/`,
  `elisdb-data/`, `.env*`, `start.py`, `scripts/`, `tests/`, `.git/`.
- `.gitignore` do servidor ganha `configs/`, `elisdb-data/`, `.env`, `.run/`.
- Documentar a migração da VPS (backup, `.env`, `up -d`, verificação, rollback).

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `server-deployment`: bind mounts partilhados e sem `chown`; configuração vinda
  de um único `.env`; imagem sem dados/segredos; defaults que não criam contas
  (sem provisionamento automático); deploy que exclui `elisdb-data/` e `configs/`.
- `server-key-provisioning`: removido o provisionamento por `ELISDB_USERS`;
  cenários de ficheiros privados e de caminhos passam a falar de emissão manual.

## Impact

- `compose.yaml`, `docker/Dockerfile`, `docker/entrypoint.sh`, `.dockerignore`,
  `.gitignore`, `.env.example`, `scripts/deploy-server.sh`, `elisdb/main.py`
  (arranque), `elisdb/keys.py` (validação de URL), docs.
- Remoção de `elisdb/provision.py` e `tests/test_provision.py`; ajustes em
  `tests/test_deployment.py`.
- Produção: uma janela de deploy com backup obrigatório; sem migração de dados.
