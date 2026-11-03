# Proposal

## Why

The repository root still holds files that belong to one component: the Android
changelog and a server deploy helper. Keeping them at the root blurs component
boundaries, makes the layout inconsistent with the documented monorepo, and
leaves readers guessing which component owns each file.

## What Changes

- Move `CHANGELOG.md` to `elisdb-android/CHANGELOG.md`.
- Move `scripts/deploy-server.sh` to `elisdb-server/scripts/deploy-server.sh`
  and make it resolve the repository root from its new location.
- Remove the empty root `photos/` directory and the root `scripts/` directory
  once it is empty; keep `elisdb-server/photos/`.
- Update every reference in workflows, documentation, skills and scripts so the
  Android release, server deployment and developer instructions keep working.
- State the resulting layout as a new `repository-layout` capability so future
  changes keep component files inside their component.

## Capabilities

### New Capabilities

- `repository-layout`: how the monorepo is organized by component, what may
  live at the root, and where component-owned files and helpers live.

### Modified Capabilities

- Nenhuma.

## Impact

- `CHANGELOG.md` → `elisdb-android/CHANGELOG.md`
- `scripts/deploy-server.sh` → `elisdb-server/scripts/deploy-server.sh`
- `.github/workflows/android-release.yml`
- `.opencode/skills/github-actions/SKILL.md`
- `.opencode/skills/android-release/SKILL.md`
- `elisdb-server/docs/execucao.md`
- `README.md`, `AGENTS.md` and other tracked references found by search.
- No application, server or database behavior changes.
