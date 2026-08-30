## Context

The app and server currently have single terse READMEs (see proposal.md — Why). This is a docs-only change: both repos get a `docs/` folder with detailed Portuguese guides and their READMEs become indices. There is no code, spec, or behavior change, so the design is about documentation structure, coverage, and consistency.

## Goals / Non-Goals

Goals:
- Establish a consistent, discoverable docs structure in both repos (`README` → index → guides).
- Every guide answers, in order, the four operator questions: *instalar*, *correr*, *configurar*, *usar*.
- All prose in Portuguese; commands, identifiers and values stay in English/code.
- No information loss: every existing build/version/config detail moves or is linked coherently.

Non-Goals:
- No changes to app/server source, build scripts, env var names or behavior.
- No sweep-by-agent rewriting of historical internal docs; scope is the two repos' public docs.
- Docs do not go into vendored/translated internationalization.

## Decisions

- **`docs/` per repo with README as index.** Alternative: a single top-level shared `docs/`. Rejected — the repos are independent and deployed/tagged separately; keeping docs next to their repo keeps versions in sync and review simple (`README` shortest-path).
- **Four ordered files per repo, one per question: `instalacao.md`, `execucao.md`, `configuracao.md`, `uso.md`.** Alternative: a single long guide. Rejected — a reader only wants one answer (e.g. "how do I run it again?"), so small, purpose-named files are faster to navigate and link to precisely.
- **`docs/index.md` (in Portuguese) inside each `docs/`** describing the folder and linking the four guides; `README.md` becomes a short landing page that sells the repo, links `docs/index`, and summarizes install/run in 3 commands each. Alternative: put everything in README. Rejected: README would exceed a screen and duplicate itself.
- **Content takes an operator-path "from zero":** server docs run first (provision keys), app docs consume them (`ELISDB_PRIVATE_KEY`, server URL). Documented as a numbered end-to-end "first run" flow in the app `uso.md`.
- **Keep exact existing technical facts 1:1** (versioning single source, fail-fast signing, R8 scope, BuildConfig env mapping, endpoint table, dedupe/conflict rules) — ported into the right guide and cross-linked, never reworded so meaning drifts.

## Risks / Trade-offs

- **Docs drift from real behavior (stale commands, wrong env names).** → Author at the repo root of each repo running the exact commands while writing; add "if this fails" fixes.
- **Cross-repo links break if the server repo isn't cloned at `../elisdb-server`.** → Use `<caminho>../elisdb-server</caminho>`-style relative references matched to the repo layout hints, phrased as "no repositório do servidor".

## Migration Plan

- Docs in both repos are replaced in one commit; nothing to roll back safely beyond reverting that commit. No deployment/migration suffix.

## Open Questions

- None.