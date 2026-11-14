---
name: documentation
description: How to maintain technical documentation in the ElisDB repo (root docs/ tree, READMEs, Portuguese, keeping docs in sync with behavior). Use when behavior changes and docs must follow.
---

# Documentation

## Where docs live (verified)

Single tree at `docs/`, one page per purpose:

- `docs/index.md` — the only map: decide here where something belongs.
- `docs/usar/` — end users: `instalar-servidor.md`, `ligar-telemovel.md`, `app.md`.
- `docs/operar/` — operators: `servidor.md`, `configuracao.md`, `tui.md`,
  `backup.md`, `deploy.md`, `publicar-app.md`, `nginx.md`, `seguranca.md`,
  `problemas.md`.
- `docs/referencia/` — wire protocol: `api.md`, `sincronizacao.md`.
- `docs/desenvolver/` — build/sign the app: `app.md`, `assinatura.md`.
- `docs/historia/` — frozen runbooks and reviews; never linked as current guidance.
- Component `README.md` files are short pointers into `docs/`.
- Specs are the source of truth for behavior (`openspec/specs/`); docs describe
  usage/ops for humans.

## Language & style

- Portuguese (PT-PT), direct and practical. One document = one purpose and one audience.
- One source per topic. "Ligar um telemóvel" lives only in `docs/usar/ligar-telemovel.md`;
  other pages link to it instead of restating it.
- Tutorial ≠ reference: env vars, endpoints and the sync contract belong in
  `docs/operar/configuracao.md` and `docs/referencia/`.
- Budget: user guides ≤120 lines, reference ≤150, nothing above 200 (CHANGELOGs excluded).

## Rules

- Update docs **only when behavior changes**; do not rewrite docs speculatively.
- Every default, path, flag and version you write must be checked against the code.
- Do not create new pages unless no existing page owns the topic; prefer editing.
- Keep `docs/index.md` accurate when a page is added, moved or removed.
- If a change affects operators (env vars, Docker, deploy), update the specific
  page under `docs/operar/` — never a catch-all guide.
- After any docs edit run `python3 scripts/check-docs.py` (also runs in Server CI).
