# Tasks — Servidor em Docker

## 1. `.dockerignore`

- [x] 1.1 Criar `elisdb-server/.dockerignore` que exclua `.venv/`, `__pycache__/`, `*.pyc`, `*.db`, `*.sqlite*`, `photos/`, `data/`, `.pytest_cache/`, `docs/` e `README.md`.

## 2. `Dockerfile`

- [x] 2.1 Criar `elisdb-server/Dockerfile` (multi-stage): base `python:3.14-slim`; instala dependências de `requirements.txt`; copia a app para `/app`; `WORKDIR /app`; `ENV ELISDB_DB=data/elisdb.db` e `ENV ELISDB_PHOTOS=data/photos`; `EXPOSE 8000`; utilizador não-root `appuser`; `CMD` com uvicorn a ouvir em `0.0.0.0:8000`.
- [ ] 2.2 Confirmar que `docker build` termina sem erros e que o `CMD` inicia o uvicorn em `0.0.0.0:8000`.

## 3. `compose.yaml`

- [x] 3.1 Criar `elisdb-server/compose.yaml`: serviço `elisdb-server` com `build: .`; mapeia `8000:8000`; volume nomeado `elisdb-data` montado em `/app/data`; `ELISDB_DB=/app/data/elisdb.db` e `ELISDB_PHOTOS=/app/data/photos`; healthcheck via `python -c` a `http://127.0.0.1:8000/health`.
- [ ] 3.2 Confirmar que `docker compose config` valida o ficheiro sem erros e que o serviço passa no healthcheck depois de subir.

## 4. Documentação

- [x] 4.1 Adicionar a `elisdb-server/docs/execucao.md` uma secção "Docker" com build, `compose up`, verificação de `/health`, logs e paragem.
- [x] 4.2 Na secção Docker: nota de persistência no volume `elisdb-data` e configuração da allowlist dentro do contentor (`docker compose exec ... app.keys issue`).

## 5. Confirmação

- [ ] 5.1 Todos os critérios de conclusão cumpridos; a change fica pronta para arquivo.