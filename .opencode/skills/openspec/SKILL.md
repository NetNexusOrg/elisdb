---
name: openspec
description: Project rules for using OpenSpec in the ElisDB repo (when to propose a change, spec/design/tasks workflow, keeping specs in sync). Use when planning or documenting a feature or significant change. The mechanical CLI steps are handled by the openspec-* skills.
---

# OpenSpec (this project)

This skill defines *when and how* to use OpenSpec here. The step-by-step CLI mechanics
(`openspec new change`, `status`, `instructions`, `validate`, `archive`) live in the
`openspec-*` skills — delegate to them for the exact commands.

## Layout (verified)

- Specs (single source of truth): `openspec/specs/<capability>/spec.md`.
- Change proposals (in-flight + archived): `openspec/changes/<name>/` and `openspec/changes/archive/`.
- Schema: `spec-driven` (`openspec/config.yaml`). Existing capabilities include `client-*`,
  `server-*`, `sync-*`, `app-build`, `release-pipeline`, etc.

## When to create a change

- Any feature, behavior change, or architectural decision → start with an OpenSpec change
  (proposal → specs delta → design → tasks).
- Trivial fixes (typo, one-line bug, cosmetic) → do NOT create an OpenSpec; keep it a direct
  change. Do not add ceremony where it adds no value.

## Rules

1. Analyze the project before proposing (read existing specs to avoid contradicting them).
2. Write clear requirements using SHALL/MUST with WHEN/THEN scenarios (4-hashtag `#### Scenario:`).
3. New capability → `## ADDED Requirements`; changed behavior → `## MODIFIED Requirements`
   (full updated block). Do not invent requirements to satisfy validation.
4. Write `design.md` only when the change is non-trivial; skip for simple changes.
5. Implement incrementally via `tasks.md`; mark checkboxes `- [x]` as you go (use `openspec-apply-change`).
6. Validate before/after: `openspec validate <change>`.
7. Keep specs synced with real behavior — after implementation, `openspec-sync-specs` then archive.
8. Update specs when behavior changes; a stale spec is worse than none.

## Capability naming

- kebab-case; align new capabilities with existing ones (e.g. `client-sync`, `server-auth`).
- Check `openspec/specs/` before adding a new capability to avoid duplicates.
