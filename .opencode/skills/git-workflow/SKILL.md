---
name: git-workflow
description: Git conventions and safety rules for the ElisDB repo (branches, commits, tags, rollback). Use when committing, branching, tagging, reviewing diffs, or undoing work.
---

# Git Workflow

## Branches

- Default branch is `main`. Work on a short-lived feature/fix branch (`feat/…`, `fix/…`) for anything non-trivial; commit directly to `main` only for tiny fixes.
- Keep branches small and merge frequently to avoid drift.

## Commits

- Conventional Commits: `feat:`, `fix:`, `docs:`, `ci:`, `chore:`, `refactor:`, `test:`.
- Prefer small, semantically clear commits; one logical change per commit.
- **Never run `git commit` or `git push` without explicit user confirmation — always ask first.**
- Before committing: inspect `git status`, `git diff`, and `git diff --cached`; stage only intended files.

## Secrets before commit

- Always check that no secret/data file is staged: `git status` must not list
  `*.config.json`, `*.db`, `*.jks`, `*.keystore`, `keystore.properties`, `.env`,
  `ELIS.csv`, or `memento-import.db`. Run `git check-ignore <file>` if unsure.
- If a secret is found staged, unstage it and confirm with the user; never commit it.

## Diff inspection / merge

- Review your own diff before committing; explain non-obvious changes.
- Prefer `git merge` over `git rebase` on shared branches; rebase only local unpushed work.

## Tags

- Release tags are SemVer `vX.Y.Z` and trigger CI (see `android-release`). Tag only when a release is intended.

## Rollback / undo

- Prefer `git revert` for pushed commits; `git reset` only for local unpushed work.
- **Never force push** (`git push --force`, `-f`, `--force-with-lease`) **without explicit user confirmation.**
- Never delete branches/tags (`git branch -D`, `git push --delete`, `git push origin :branch`) without confirmation.

## Rules

- Do not amend pushed commits. If a commit fails (e.g. a hook), fix and create a new commit.
- Do not change git config (user.name/email, hooks, etc.) unless asked.
