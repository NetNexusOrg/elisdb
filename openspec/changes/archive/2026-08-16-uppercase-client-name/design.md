## Context

Client names are stored and displayed exactly as typed (see proposal.md — Why). Today the only name normalizations are `.trim()` in the app repository write path and trim+collapse-whitespace (`normalize_nome`) on the server; case is never transformed. Duplicate detection already ignores case via SQLite `COLLATE NOCASE`, and the app's list ordering uses `COLLATE NOCASE` too, so the change is purely about the stored/presented casing, not matching semantics.

## Goals / Non-Goals

**Goals:**
- Client names are stored and presented in uppercase on the app (list, detail, form) and on the server, regardless of input casing.
- Offline-first correctness: names are uppercase even before the first sync.
- Converges existing mixed-case records to uppercase over normal use.

**Non-Goals:**
- Changing duplicate detection or search semantics (already case-insensitive).
- Migrating existing stored records in bulk.
- Changing the whitespace-collapsing behavior (`normalize_nome`) beyond adding case folding.

## Decisions

**D1 — Uppercase at the storage boundary on both sides, plus live in the form.**
- App form: `ClientFormViewModel.onNomeChange` uppercases as the user types (immediate feedback, per client-screens spec).
- App repository: `ClienteRepositoryImpl.save()` uppercases (and trims) the name for both create and edit — the single funnel for local writes, keeping the stored value uppercase even before sync.
- App pull: `SyncCoordinator.applyServerItem()` uppercases the name when storing a pulled record, so names received from a legacy server also become uppercase locally.
- Server: `normalize_nome()` in `models.py` uppercases after whitespace collapse; `save_client()` then stores the uppercase form, and `pull` returns it.
- *Rationale*: server-side uppercasing makes the server the canonical consistent source across all devices; app-side uppercasing guarantees correctness for offline-created/edited records and provides the live UX. 
- *Alternative considered*: app-only uppercasing — rejected because records created by other devices or pre-existing mixed-case server data would still come back mixed-case.
- *Alternative considered*: display-only uppercasing (never store) — rejected because it does not meet "guardada" (stored) and would diverge between devices.

**D2 — Locale-aware case folding.**
- Kotlin: `uppercase()` (default locale; handles Portuguese accented characters).
- Python: `str.upper()` (Unicode-aware).
- *Rationale*: PT/EN are the supported languages; both APIs handle accented uppercase (Á, É, Í, Ó, Ú, Ã, Ç) correctly.

**D3 — No DB migration and no schema change.**
- `nome` stays a plain text column; the unique index remains `COLLATE NOCASE`. Uppercasing only changes the stored value's case, which never affects the uniqueness/matching constraint.
- *Rationale*: a case-folding index would be redundant with `COLLATE NOCASE` and would need a migration for no benefit.

**D4 — Existing mixed-case records converge without a bulk migration.**
- A record is uppercased on the next touch: edit on the app (`save()`), push to the server (`save_client()`), or pull (`applyServerItem()`).
- *Rationale*: a bulk rewrite of server data would touch many rows and photo paths for zero functional gain; convergence over normal use is sufficient.

**D5 — Duplicate merge is unaffected.**
- `mergeDuplicate` keeps the local name and re-pushes it; since the local name is already uppercase at save time and the server uppercases on push, the merged name is uppercase with no extra logic.
- Search (`normalizeForSearch` lowercases for matching) and list ordering (`COLLATE NOCASE`) keep working with uppercase-stored names unchanged.

## Risks / Trade-offs

- [Pre-existing server records stay mixed-case until re-pushed] → Convergence via D4; the app's `applyServerItem` uppercases them locally on pull, so users see uppercase even before the server converges.
- [Locale-dependent casing (e.g. German ß, Turkish i)] → Not a concern for the supported PT/EN scope; default-locale `uppercase()`/`upper()` is acceptable.
- [Double normalization writes (uppercase is idempotent)] → No risk: uppercasing an already-uppercase name is a no-op, so re-pushes/pulls are stable.

## Migration Plan

1. Deploy the server change first (uppercases on push/pull); existing data is unaffected until touched.
2. Ship the app update (form, repository, applyServerItem). No DB migration required.
3. Rollback: revert the server `normalize_nome` and the app uppercasing; names revert to the previously stored casing, no data loss.

## Open Questions

None.