---
name: documentation
description: How to maintain technical documentation in the ElisDB repo (READMEs, docs/ folders, Portuguese, keeping docs in sync with behavior). Use when behavior changes and docs must follow.
---

# Documentation

## Where docs live (verified)

- Root `README.md` — project overview and component index.
- `elisdb-android/README.md` + `elisdb-android/docs/` (index, instalacao, execucao, configuracao, uso, reviews/).
- `elisdb-server/README.md` + `elisdb-server/docs/` (index, instalacao, execucao, configuracao, uso, nginx-proxy-manager).
- Specs are the source of truth for behavior (`openspec/specs/`); docs describe usage/ops for humans.

## Language & style

- Documentation is written in Portuguese; keep it consistent with existing tone (direct, practical).
- Update the relevant `docs/` page when a change genuinely alters documented behavior, commands,
  config, or setup steps.

## Rules

- Update docs **only when behavior changes**; do not rewrite docs speculatively.
- Keep the root `README.md` index accurate when components change.
- Do not create new documentation files unless they serve a real need; prefer editing existing pages.
- Reflect any command/flag/env-var change in the corresponding doc (e.g. a new endpoint → `api-backend` spec + server docs).
- If a change affects operators (env vars, Docker, provisioning), update `docs/execucao.md` / `docs/configuracao.md`.
