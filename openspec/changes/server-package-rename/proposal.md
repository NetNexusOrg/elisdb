# Proposal

## Why

O pacote Python do servidor chama-se `app/`, um nome genérico que colide com o
diretório de trabalho do contentor (`/app`) e com o vocabulário da app Android.
O nome não diz nada sobre o domínio e torna confuso distinguir "o pacote do
servidor" de "a aplicação". Esta mudança prepara a reorganização seguinte
(`server-shared-data-dir` e `add-server-tui`), que passa a referenciar o pacote
pelo nome do projeto.

## What Changes

- Renomear o pacote `elisdb-server/app/` para `elisdb-server/elisdb/`. Os imports
  internos são relativos e não mudam.
- Atualizar todas as referências externas: testes (`from app.` → `from elisdb.`),
  `Dockerfile` (`uvicorn elisdb.main:app`), `docker-entrypoint.sh`
  (`python -m elisdb.provision`), `scripts/deploy-server.sh`
  (`python -m elisdb.backup`), `scripts/sync_smoke.py`, CI
  (`.github/workflows/server-ci.yml`) e documentação/README/skills.
- Apagar `elisdb-server/uv.lock`, um stub inválido de 52 B sem dependências
  resolvidas.
- **Não** altera comportamento, API, esquema, autenticação, provisionamento nem
  o layout de dados. `/app` (WORKDIR do contentor) e `/app/data` mantêm-se.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `server-auth`: a referência ao comando de gestão de chaves passa de
  `python -m app.keys` para `python -m elisdb.keys`.

## Impact

- Código: `elisdb-server/app/**` (renomeado), `elisdb-server/tests/**`,
  `scripts/`, `Dockerfile`, `docker-entrypoint.sh`.
- CI: passo de import em `server-ci.yml`.
- Docs: `elisdb-server/README.md`, `docs/*.md`, `README.md` raiz,
  `.opencode/skills/project-context/`.
- Sem migração de dados, sem alteração de contrato com a app Android.
