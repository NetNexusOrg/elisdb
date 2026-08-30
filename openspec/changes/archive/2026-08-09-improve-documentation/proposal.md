## Why

Both READMEs are terse and developer-centric: they assume prior knowledge, mix build/internal details with usage, and give no step-by-step path from zero to a working app+server setup. New users (and the operator) cannot reliably install, run, configure and use the platform from the docs alone, and there is no guidance for day-to-day operation.

## What Changes

- Replace each terse README with an **index** that links to a dedicated `docs/` folder (one per repo, `elisdb/docs/` in the app repo and `elisdb-server/docs/` in the server repo).
- Write detailed, ordered guides in `docs/` for both repos, all in **Portuguese**:
  - Installation (dependencies, setup, prerequisites)
  - Running (dev run, release build, expected ports/endpoints)
  - Configuration (environment variables, keystore, server base URL, signing keys)
  - Usage (how an operator uses the server CLI + how a user uses the app)
  - Server **installation, run, configure** cover `ELISDB_DB`, `ELISDB_PHOTOS`, the `app.keys` CLI (issue/add/list/remove), and the API endpoints.
  - App **installation and use** cover device sign-up key pairing with the server allowlist, first-run sync, and the app "Definições" (runtime URL/key that reads the same env knobs).
- Keep every existing build/version/config detail already documented (single-source versioning, fail-fast signing, R8, BuildConfig env mapping) but move or link it coherently so nothing is lost.
- Fix any inaccuracies discovered while writing (e.g., outdated flags, missing steps).

## Constraints

- Documentation only — no changes to app or server source, build scripts, or behavior.
- All new prose is in Portuguese; code/commands/keys remain in English.
- Do not commit secrets or real keys in examples (use `<...>` placeholders).

## Impact

- `README.md` (app and server) — become concise indices.
- New `docs/` folders in both repos with guides.
- No code, no specs — `skip_specs` change.