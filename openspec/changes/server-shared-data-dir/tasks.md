# Tasks — server-shared-data-dir

## 1. Layout

- [x] 1.1 `git mv elisdb-server/Dockerfile elisdb-server/docker/Dockerfile`
- [x] 1.2 `git mv elisdb-server/docker-entrypoint.sh elisdb-server/docker/entrypoint.sh`
- [x] 1.3 `compose.yaml`: `build: {context: ., dockerfile: docker/Dockerfile}`

## 2. Compose e permissões

- [x] 2.1 `user: "${PUID:-1000}:${PGID:-1000}"`
- [x] 2.2 Bind mounts `"${ELISDB_DATA_DIR:-./elisdb-data}:/app/data"` e `"./configs:/app/data/configs"`
- [x] 2.3 Env do contentor com caminhos fixos (`/app/data/...`) e tunables por interpolação
- [x] 2.4 Remover `ELISDB_USERS` e o volume nomeado `elisdb-data`

## 3. Entrypoint sem root

- [x] 3.1 Remover `chown -R` e `setpriv`
- [x] 3.2 `mkdir -p` dos diretórios e verificação de escrita por `PUID:PGID` com erro acionável
- [x] 3.3 `exec "$@"` (sem Python de provisionamento)

## 4. Remoção do provisionamento automático

- [x] 4.1 Apagar `elisdb/provision.py` e `tests/test_provision.py`
- [x] 4.2 Remover referências a `ELISDB_USERS` (compose, docs, CI, testes)
- [x] 4.3 Mover `validate_server_url` para `elisdb/keys.py`; exigir `ELISDB_SERVER_URL` apenas ao emitir
- [x] 4.4 Ajustar `tests/test_deployment.py` (sem asserções de provisionamento/ordem `provision → exec`)

## 5. Imagem

- [x] 5.1 `COPY` explícito (pacote `elisdb`, `VERSION`, `entrypoint.sh`)
- [x] 5.2 `.dockerignore` com `configs/`, `elisdb-data/`, `.env*`, `start.py`, `scripts/`, `tests/`, `.git/`
- [x] 5.3 `ENV PYTHONDONTWRITEBYTECODE=1`

## 6. Configuração

- [x] 6.1 `.env.example` com todas as chaves e defaults seguros
- [x] 6.2 `.gitignore` do servidor: `configs/`, `elisdb-data/`, `.env`, `.run/`

## 7. Scripts e documentação

- [x] 7.1 `scripts/deploy-server.sh`: excluir `elisdb-data/`, `configs/`; manter backup pré-deploy
- [x] 7.2 Atualizar `docs/{configuracao,execucao,backup,seguranca,nginx-proxy-manager}.md`

## 8. Verificação

- [x] 8.1 `.venv/bin/python -m pytest -q` verde
- [x] 8.2 `docker build` e confirmar que a imagem não contém `.env`/`configs/`
- [ ] 8.3 Paridade manual: criar cliente/foto em local e em Docker sobre o mesmo `elisdb-data`, sem `chown`
- [x] 8.4 `openspec validate server-shared-data-dir`

## 9. Migração da VPS (requer autorização)

- [ ] 9.1 Backup verificado + cópia off-VPS de backup e `configs/`
- [ ] 9.2 Parar contentor, `cp -a elisdb-data/configs/. configs/`, `chown -R 1001:1001 configs elisdb-data`
- [ ] 9.3 Criar `.env`, trocar `compose.yaml` host-local, `docker compose config`, `up -d --build`
- [ ] 9.4 Verificar `/health`, 527 clientes, 197 fotos, 21 tombstones, 7 chaves ativas, uma foto e `stat` 1001
- [ ] 9.5 Remover o diretório legado `app/` da VPS (com confirmação)
