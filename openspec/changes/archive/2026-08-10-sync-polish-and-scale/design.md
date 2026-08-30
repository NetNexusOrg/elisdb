## Context

Umbrella change bundling five independent medium-rigor improvements from the sync-system review (R4–R8). See proposal for what/why and each capability spec for the contract.

Current state relevant to each:

- **R4 (`dataCriacao`)**: `applyServerItem` sets `dataCriacao = server.updatedAt` (SyncCoordinator.kt:126-127). Server schema has only `updated_at`.
- **R5 (auth messaging)**: ApiClient throws `AuthException` on any 401 (`ApiClient.kt:114`); SyncManager maps it to AUTH → UI "chave não registada" (`SyncStatusBar.kt:72`).
- **R6 (list observability)**: `SyncStatusBar` only rendered in SettingsScreen.kt:58; ClientListScreen has no sync affordance.
- **R7 (incremental pull)**: `api.pull()` body: `$baseUrl/api/pull` (ApiClient.kt:~85-90) returns everything.
- **R8 (photo hygiene)**: `SyncMapper` silently `continue`s on missing photo files (`SyncMapper.kt:19`); no orphan sweep.

The five sub-changes share no code and can land independently; tasks are grouped per R#. `docs/uso.md` currently asserts behaviors for R6/R5 that the implementation does not reflect — docs are corrected in the same change.

## Decisions

**D1 — `dataCriacao` preserved (R4):** Add `created_at` on the server (`created_at INTEGER NOT NULL DEFAULT updated_at` for legacy rows, set on insert, unchanged on update) and transport it in `ClienteOutDto`. App assigns `dataCriacao = server.createdAt`. For already-installed devices, first pull after upgrade uses server `created_at` fallback to `updated_at`.
- *Alternative:* heuristic (preserve local when only updatedAt changed) — rejected: unreliable for multi-device history.

**D2 — Auth causes (R5):** `ApiClient` will throw `AuthException(cause = StaleClock | InvalidKey)` by parsing the server's 401 `detail` ("stale or future timestamp" → Stale; else InvalidKey). SyncManager maps `Stale → SyncFault.CLOCK` (new enum) with its own message; `InvalidKey` keeps the existing message.
- *Alternative:* expose raw detail string — rejected to keep UI enum-driven.

**D3 — TopBar on list (R6):** add a `SyncStatusBar`-style element (read-only status + manual sync icon) into ClientListScreen's top bar, reusing the existing `syncStatus`/`syncFault` flow via ClientListViewModel. `docs/uso.md` refreshed to: match implemented reality (status + manual button on the list).
- *Alternative:* docs-only correction — rejected; the spec `client-sync-state` requires observability, and the review flagged the gap as worth fixing.

**D4 — Incremental pull (R7):** add `?since=timestamp` param to `GET /api/pull`; server filters `updated_at > since` (exclusive on equality), with a page limit (e.g. 500) and a `next_since` cursor only if a further page exists; app stores the last `updated_at` watermark in Room settings and sends it on pull; first pull omits `since` (full). `client-sync` pull already keeps non-pending local records, so delta application is compatible.
- *Alternative:* pagination by offset — more complex; since-timestamp matches LWW semantics. Chosen.
- Note: R7 depends on R3 (unchanged-photo skipping) to avoid re-downloading base64 for unchanged records; R3 landed in `sync-incremental-photos`. Do not enable `since` until R3's compare is in place, else delta pulls still rewrite photos.

**D5 — Photo hygiene (R8):** records:
- During push, a row whose file is missing is skipped (today); change: mark row `pending_delete` and report a warning, so it gets cleaned next sync instead of lingering.
- Occasional sweep (after each pull or on app start): enumerate the photos dir; delete files with no matching `uriLocal` in the DB. Runs in background coroutine, bounded (only files under our photos root).
- *Alternative:* full automation on every sync — reduced to infrequent sweep to avoid startup cost.

## Risks / Trade-offs

- **R4**: Room schema change + app migration; multi-device LWW on `createdAt` (stale device could push a smaller createdAt) → frozen as immutable after first insert; conflicts only via new `updatedAt` edits.
- **R5**: server `401 detail` strings are stable (`main.py:34/39/42/48`) but not versioned → parse with fallback to generic.
- **R7**: delta pull with photo-bearing records returns full base64 unless R3 skips → explicitly gated on R3 landing first; watermark stored as server `updated_at` (aligned to LWW) to avoid device-clock jumps.
- **R8**: sweep could race a concurrent real write → we run it only after a sync completes and before manual photo capture (activity-scope mutex) — documented.
- Doc assertions corrected in the same change to avoid permanent mismatch.

## Migration Plan

Four groups, each independently revertible:

1. **R4**: server migration (`ALTER`/schema bump) + DTO + app Room migration (add `createdAt` column) + pull mapping (revert: keep `dataCriacao=updatedAt`, column unused).
2. **R5**: app-only enum + message change (revert: restore old mapping).
3. **R6**: app UI topbar + docs (revert: drop topbar).
4. **R7**: server endpoint `since` + app watermark (revert: restore full pull; keep config). Gated on R3.
5. **R8**: app sweep + push mark (revert: no-op).

## Open Questions

- **R4**: Should `dataCriacao` fall back to the local first-seen date on a fresh sync when the server lacks `created_at`? Awaiting archive-time decision; default is server `created_at`.
- **R7**: default page size & whether to expose `next_since` — resolved during implementation; no spec impact.