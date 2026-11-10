---
name: github-workflow
description: GitHub operations for the ElisDB repository (gh CLI, SSH, PRs, issues, releases, visibility). Use when creating PRs, issues, releases, or inspecting the remote repo.
---

# GitHub Workflow

## Repository (verified)

- Owner/repo: `NetNexusOrg/elisdb`. **PRIVATE** (must stay private).
- Remote `origin` is SSH: `git@github.com:NetNexusOrg/elisdb.git`. Branch `main`.
- Authenticated via `gh` (account `NetNexusOrg`).

## Using `gh`

- Prefer `gh` for GitHub tasks: `gh pr create`, `gh issue create`, `gh release create`, `gh run list`, `gh repo view`.
- Check status/context before acting: `gh auth status`, `gh repo view --json visibility`, `gh pr list`.
- Inspect a PR's diff across all its commits, not just the latest, before merging.

## Pull requests / issues

- Write PR titles/descriptions that explain why and what; reference related issues/OpenSpec changes.
- Keep PRs small and reviewable.

## Releases

- Releases are created automatically by CI from `v*` tags (see `github-actions` / `android-ci.yml`). Prefer tags over manual release creation.
- Do not delete a release or its assets without confirmation.

## Safety rules

- **Never make the repository public.**
- **Never delete repositories, branches, or releases without explicit confirmation.**
- Never push secrets, tokens, or keys via GitHub.
- Use SSH (not HTTPS) for `origin`; do not switch the protocol.

## Verification

- After pushing: confirm the code is on GitHub and the repo is still private:
  `gh repo view NetNexusOrg/elisdb --json visibility`.
