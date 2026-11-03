# Tasks

## 1. OpenSpec

- [x] 1.1 Write the proposal, `repository-layout` spec delta, design and tasks; verify `openspec validate reorganize-monorepo --strict` passes.
- [x] 1.2 Confirm `openspec list --specs` shows no existing `repository-layout` spec before creating it.

## 2. Move component files

- [x] 2.1 Move the changelog with `git mv CHANGELOG.md elisdb-android/CHANGELOG.md`; verify `git status` shows a rename and the file is gone from the root.
- [x] 2.2 Move the deploy helper with `git mv scripts/deploy-server.sh elisdb-server/scripts/deploy-server.sh`; verify `git status` shows a rename.
- [x] 2.3 Remove the empty root `photos/` and `scripts/` directories with `rmdir`; verify they are gone and `elisdb-server/photos/` remains.
- [x] 2.4 Update the deploy helper to resolve the repository root from its new location; verify `bash -n elisdb-server/scripts/deploy-server.sh` passes.

## 3. Update references

- [x] 3.1 Update `.github/workflows/android-release.yml` to read `elisdb-android/CHANGELOG.md`; verify both workflows parse with PyYAML.
- [x] 3.2 Update `.opencode/skills/github-actions/SKILL.md` and `.opencode/skills/android-release/SKILL.md` with the new changelog and deploy helper paths; verify no old path remains in the skills.
- [x] 3.3 Update `elisdb-server/docs/execucao.md` with `elisdb-android/CHANGELOG.md` and `./elisdb-server/scripts/deploy-server.sh`; verify the documented commands are valid from the repository root.
- [x] 3.4 Update `README.md` and `AGENTS.md` (and any other tracked file found) so the documented layout matches the repository; verify a repo-wide search finds no stale root-level references.

## 4. Verification

- [x] 4.1 Run `git diff --check`; verify no whitespace errors.
- [x] 4.2 Run `./gradlew :app:compileDebugKotlin` in `elisdb-android`; verify it succeeds.
- [x] 4.3 Verify `git status` contains no secrets or data, and that build/runtime artifacts remain ignored.

## Workflow follow-up

- Sync the `repository-layout` delta into `openspec/specs/repository-layout/spec.md`.
- Archive the change to `openspec/changes/archive/2026-11-03-reorganize-monorepo`.
- Commit and push `main` after implementation and after archiving.
