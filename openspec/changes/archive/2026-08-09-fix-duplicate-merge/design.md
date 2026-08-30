## Context

See proposal.md — Why. Current state: `pushEdits()` (SyncCoordinator.kt:40-65) clears `pending` for any push acknowledgement including `duplicate`; the server replies with `existing_id` in `PushResultDto` but the app ignores it. On the next `pull()`, the local record (new id not on server, no longer pending) is dropped by `finalizeDeletion()` (SyncCoordinator.kt:107-112), silently deleting the user's record and photos.

The server's `save_client` returns `(status="duplicate", existing_id)` and does NOT persist the rejected record's fields or photos (database.py). So the app must perform the merge, using `existing_id` and the `latest` server values from a pull.

## Goals / Non-Goals

**Goals:**
- Eliminate the silent deletion path for `duplicate` acknowledgements.
- Preserve locally entered fields and photos by associating them to the retained (existing) record.
- Keep the server as the conflict owner: after merge the record is re-pushed and re-pulled as a normal pending sync.
- Surface a user-visible message that the merge happened.

**Non-Goals:**
- Server-side auto-merge (server behavior is unchanged; it already dedupes on normalized name and returns `existing_id`).
- Interactive duplicate-resolution UI (choosing which fields win) — out of scope; a deterministic merge + notification suffices.
- Field-level conflict UI between two diverged records.

## Decisions

**D1 — Merge on the client using data already available after the push.** After a `duplicate` ack, `pushEdits` will, in the same transaction, re-attribute the local record to `existing_id` (copy client row id → `existing_id`, keep `pending=true`, keep fields/photos), then clear pending for the *old* id. The `pull()` that follows fetches the canonical record's server values; because the local copy of `existing_id` is marked pending, `pull()` skips overwriting it (SyncCoordinator.kt:102) and its data re-pushes on the next sync.
- *Alternative considered:* server-side merge on push — rejected; it changes server behavior and risks diverging data copies across devices.
- *Alternative considered:* adopt `existing_id` instantly without a follow-up push — rejected; newly-pushed field values (notas/contrato/fotos) would be lost unless re-pushed.

**D2 — Rename the local record id to the sever's `existing_id`.** Because the offline-first model keys raster by record id, the cleanest mapping is: keep the local record but swap its `id` and `nome` (server-normalized name if returned) to the canonical, mark pending, and mark `current old id` as nothing (the row simply adopts the new id after a delete+insert with the same PK change). Photos keep their ids and get re-associated to the new client id; those photos carry their locally `pending` state so they re-push next cycle.
- *Alternative:* keep two rows and copy fields one by one — rejected as more error-prone under `updated_at` LWW.

**D3 — Reuse `pushEdits`'s existing ack transaction.** `pushEdits` already receives per-record statuses; add a branch: on `status == "duplicate" && existingId != null`, run the re-attribution (D2) instead of `clienteDao.update(pending=false)`. Everything else (accepted/stale) unchanged.
- `stale` is unaffected: server keeps its record; the local values were older and pulling wins is LWW by design.

**D4 — User-visible duplicate merge.** Add a `DuplicateMerged(name)` variant to `SyncOutcome` (now `data class`-ifiable or documented), so `SyncManager` can expose it through `_fault`/`_status` or a one-time swarm `_lastResult` read by `SettingsViewModel`, and `SyncStatusBar`/`SettingsScreen` show "O registo «X» foi fundido com um registo existente." Persisted as a toast-level one-shot, not sticky.

## Risks / Trade-offs

- **Field-merge ambiguity**: user-entered `contrato`/`notas` on the duplicate conflicts with canonical values → server LWW decides on next push (local wins if its `updated_at` is newer). Mitigation: set the merged record's `updated_at` to max of both, so the user's newer edits win, and document the LWW outcome. [Risk] Duplicate's `updated_at` older than canonical → local edits silently lose. Mitigation: after merge, bump `updated_at` to `now` for the re-pushed record.
- **Id collision on re-attach** : `existing_id` equal to another local record id (both exist locally) → D1 must handle insert-or-merge clauses (reserve the old id first, upsert canonical). Mitigation: perform delete-old + insert-new within one Room transaction keyed on `existing_id`; this is the same row being replaced, and Room handles `INSERT OR REPLACE`.
- **Server may return `duplicate` with a name-normalized but diacritically different raw name** → the app merges to `existing_id` even if visual names differ (`Maria Santos` vs `maria santos`). Acceptable; server already considers them identical.
- **Partial failure mid-sync** : if pull fails after a merge re-attach, the local record with old id stays pending → next sync re-pushes → server again returns `duplicate` + same `existing_id` → merge re-runs idempotently. No data loss.

## Migration Plan

- App-only change; update `SyncCoordinator` + `SyncMapper` (id mapping) + `SyncOutcome`/`SyncManager`/UI one-shot message.
- Server untouched; no data migration. Old devices continue failing as today until updated; documented in release notes.

## Open Questions

- What exact user-facing message text should the UI show for the merge? (deferred to apply-time UI copy).
- Should the canonical record's normalized `nome` come from the server (pull) or be kept as the user typed it? Current choice: keep the user's entered name until the next pull, to avoid surprising renames mid-sync.