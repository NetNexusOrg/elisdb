## Why

O servidor ElisDB corre hoje apenas com Python num ambiente virtual local (`.venv` + uvicorn). Não há forma de o distribuir e executar como um contentor, o que dificulta o deployment em servidores, CI e ambientes de teste. Adicionar um `Dockerfile` e um exemplo `compose.yaml` permite correr o servidor em Docker com persistência (base de dados SQLite e fotos) na mesma convenção dos env vars já suportados (`ELISDB_DB`, `ELISDB_PHOTOS`).

## What Changes

- **Novo `Dockerfile`** em `elisdb-server/` (build multi-stage Python, a app em `/app`, expõe a porta 8000).
- **Novo `compose.yaml`** em `elisdb-server/` (serviço `elisdb-server`, volume para os dados, porta 8000 exposta no host, healthcheck via `/health`).
- **Suporte a dados persistentes**: a base de dados (SQLite) e as fotos vivem num volume montado; o compose define `ELISDB_DB` e `ELISDB_PHOTOS` como caminhos dentro desse volume.
- **Healthcheck** com base `/health` do servidor (sem autenticação).
- **Docs**: secção "Docker" no `README.md` e/ou `docs/execucao.md` com os comandos `docker build`/`docker compose up`.
- Sem alteração ao código do servidor nem à API; sem dependências novas na app.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

Nenhuma — mudança de packaging/deployment (tooling). O comportamento externo (API, autenticação, persistência) não muda; os env vars são os mesmos. A change declara `skip_specs: true`.

## Impact

- `elisdb-server/Dockerfile` (novo).
- `elisdb-server/compose.yaml` (novo).
- `elisdb-server/.dockerignore` (novo, para excluir `.venv`, `__pycache__`, `*.db`, `photos/`).
- `elisdb-server/README.md` e/ou `docs/execucao.md` (secção Docker).
- Imagem base: python slim oficial; dependências de `requirements.txt`.