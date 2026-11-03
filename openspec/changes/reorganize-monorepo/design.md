# Design

## Context

See `proposal.md` — Why. The repository is a monorepo with four components
(`elisdb-android/`, `elisdb-server/`, `elisdb-telegram-bot/`, `elisdb-memento/`)
plus root-level OpenSpec and OpenCode tooling. Two component-owned files still
live at the root and are referenced by CI, docs and skills.

## Goals / Non-Goals

**Goals:**

- Give every component a single folder for its files and helpers.
- Keep the root limited to cross-cutting files.
- Preserve the release workflow, deploy guard and developer commands after the
  moves.

**Non-Goals:**

- Changing application, server, database or sync behavior.
- Reorganizing files that already live inside a component folder.
- Moving root tooling (`openspec/`, `.opencode/`, `.github/`) or the root
  documentation files.

## Decisions

1. **Move files with `git mv`** so history follows them: `CHANGELOG.md` to
   `elisdb-android/CHANGELOG.md`, and `scripts/deploy-server.sh` to
   `elisdb-server/scripts/deploy-server.sh`.
2. **Resolve the repo root in the deploy helper from the script location**
   (`$(dirname "$0")/../..`). The helper performs Git commands and rsyncs
   `elisdb-server/`, so resolving the repository root is correct from any
   working directory. The alternative — keeping the old `..` resolution — would
   treat `elisdb-server/` as the root and break every Git check.
3. **Reference moved paths explicitly in CI and docs.** The release step runs
   from the repository root, so it reads `elisdb-android/CHANGELOG.md`. The
   server docs invoke the helper as `./elisdb-server/scripts/deploy-server.sh`
   from the repository root.
4. **Delete only empty root directories.** The root `photos/` is empty and
   untracked; `scripts/` is removed after the move. `elisdb-server/photos/` stays.
5. **Record the layout as a new `repository-layout` capability**, because the
   structure is a project-wide contract that future changes must preserve.

## Risks / Trade-offs

- **Missed reference after a move** → Search the whole tracked tree for the old
  paths, then run `git diff --check`, YAML parsing, `bash -n` and the Android
  compile check.
- **Release notes stop being found** → The workflow points at the new changelog
  path and the release step still fails loudly when the tag section is missing.
- **Deploy helper breaks from the new location** → The helper resolves the
  repository root from its own path and is syntax-checked with `bash -n`.
