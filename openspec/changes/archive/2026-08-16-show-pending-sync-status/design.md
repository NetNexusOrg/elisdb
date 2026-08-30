## Context

The app marks local changes as `pending` in the database (`ClienteDao.getPending()` / `getPendingDeletions()`), and the sync engine already uses those markers to decide what to push. However, the UI only ever observes `SyncManager.status` (a `SyncStatus` enum: IDLE, IN_PROGRESS, SUCCESS, ERROR), which reflects the *last sync attempt* and persists globally. After a local save the user opens settings and sees the stale "Sincronizado" from an earlier sync, even though the new record is still pending. Conversely, when another device has pushed changes, the server holds records newer than this device's persisted pull watermark (`RoomPullWatermark`), yet the app never checks and cannot tell the user there is work to receive. There is no observable "pending" state (in either direction) anywhere in the UI, despite `client-sync-state` requiring one.

## Goals / Non-Goals

**Goals:**
- The settings sync status row shows "sincronização por fazer" whenever pending uploads exist, and shows "há atualizações para receber" whenever the server has newer data than the local pull watermark.
- Both states derive from authoritative sources (the pending DAO markers; the server's latest timestamp vs the persisted watermark), so they are always accurate.

**Non-Goals:**
- Changing the sync engine's push/pull logic (it already clears pending on ack and advances the watermark on pull).
- Auto-triggering a sync on save (out of scope; the fix is about *displaying* pending state, not scheduling).

## Decisions

**D1 — Derive a pending-uploads signal from the DAO, combined with sync status.**
- A pending-uploads boolean comes from observing `ClienteDao` (pending or deleted-pending rows exist). It is combined with `SyncStatus` so the row shows:
  - pending → "Sincronização por fazer" (new state, only when there is work and the last result is not a fresh success that just acknowledged it)
  - otherwise the existing states (idle/in-progress/success/error)
- *Rationale*: the DAO is the single source of truth for "has unsynced work", exactly what the sync engine uses; no new bookkeeping.
- *Alternative considered*: infer pending from `SyncStatus` transitions — rejected because a status alone cannot tell whether the last sync acknowledged *everything*.

**D2 — Combine at the `SyncManager` level.**
- `SyncManager` observes the DAO pending queries and the pull watermark, and exposes the current "has pending work" alongside `SyncStatus` (e.g. derived `hasPendingUploads`/`hasPendingDownloads` StateFlows, or a folded combined state). `SettingsViewModel` observes them and passes the combined state to the UI.
- *Rationale*: keeps the UI dumb and puts the decision next to the sync engine which owns the pending semantics.

**D3 — The pending state yields to a fresh success.**
- After a sync that just acknowledged everything and retrieved all newer server data, both pending signals are false, so the row shows "Sincronizado". The pending states appear again as soon as a new local change is saved or the server gains newer data.
- *Rationale*: matches the spec ("status clears once all pending records are acknowledged") and avoids flickering between pending and success on the same tick.

**D4 — UI rendering.**
- `SyncStatusBar`/`syncMessage` gains the pending cases (e.g. cloud-upload icon + "Sincronização por fazer"; cloud-download icon + "Há atualizações para receber"), reusing the same layout so the fixed sync-action position (already covered) is preserved.

**D5 — Server exposes a lightweight latest-timestamp endpoint (Opção A).**
- New `GET /api/latest` (authenticated) returning `{"latest_updated_at": <max(clientes.updated_at, tombstones.deleted_at)>}`. The app fetches it when the settings screen needs the status (cheap, one request) and compares it with the persisted pull watermark: `latest > watermark` ⇒ pending downloads.
- *Rationale*: an endpoint is the only way to detect work to receive *before* a pull; the alternative (embedding the value in the pull envelope) is only known after the pull already happened. The value uses the same epoch-seconds scale as the watermark.
- *Alternative considered*: embed `latest_updated_at` in the pull envelope — rejected because it cannot indicate pending downloads before the user syncs.

**D6 — Files touched.**
- Server: `app/main.py` (`GET /api/latest`), `app/database.py` (query the max over clientes + tombstones), tests.
- App: `SyncApi`/`ApiClient` (`fetchLatestUpdatedAt`), `SyncManager` (pending upload/download derivation), `SettingsViewModel` (observe), `SyncStatusBar`/`syncMessage` (render), `SettingsUiState` (carry pending flags), `AppContainer` (inject `ClienteDao`/watermark into `SyncManager`). No DB schema change.

## Risks / Trade-offs

- [Pending flicker during a sync] → While IN_PROGRESS the row shows the in-progress state (existing behavior); the pending states apply to idle states with outstanding work.
- [Extra request for the latest timestamp] → One cheap authenticated request when the settings status is needed; acceptable.
- [Observation churn on large datasets] → DAO flows already feed the list screens; reusing them is cheap.

## Migration Plan

1. Deploy the server change first (adds `GET /api/latest`; additive, no wire-format break).
2. Ship the app update (pending-state derivation + rendering). No DB migration.

## Open Questions

None.

