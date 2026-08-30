## R4 — Created date preserved

- [x] 4.1 Server: add `created_at` column (default `updated_at` on existing rows), set on insert, never changed on update; include in `ClienteOutDto`.
- [x] 4.2 App: extend `Cliente`/DAO/Room schema with `createdAt`; add Room migration.
- [x] 4.3 `SyncMapper`/`SyncApi`: map `created_at` on pull; `applyServerItem` sets `dataCriacao = server.createdAt`.
- [x] 4.4 Fallback path: on records without `created_at`, preserve local `dataCriacao` instead of `updatedAt`.
- [x] 4.5 Test: client created, edited on server, re-pulled — `dataCriacao` still equals the original creation date.

## R5 — Clock-skew-aware auth messaging

- [x] 5.1 App: `AuthException` gains a `cause` (StaleClock | InvalidKey) parsed from the 401 `detail` ("stale or future timestamp" → StaleClock; else InvalidKey).
- [x] 5.2 `SyncManager`/`SyncFault`: add `SyncFault.CLOCK`; map `StaleClock` → CLOCK with a dedicated message; keep existing mapping for InvalidKey.
- [x] 5.3 `SyncStatusBar`/`SettingsScreen`: add message for CLOCK ("verifique a hora do relógio do dispositivo/servidor").
- [x] 5.4 Update `docs/uso.md` troubleshooting to explain the new clock message.
- [x] 5.5 Unit test: fake ApiClient returns 401 with "stale or future timestamp" → SyncManager sets CLOCK fault, not AUTH.

## R6 — Observability on client list

- [x] 6.1 `ClientListViewModel`: expose `syncStatus`/`syncFault` (from `SyncManager`) and a `manualSync()` trigger.
- [x] 6.2 `ClientListScreen`: render a top-bar sync status + manual sync button, reusing `syncMessage`/`SyncStatusBar` components.
- [x] 6.3 Confirm `ClientListScreen` passes through the nav change in `Navigation.kt` if needed; verify with Compose preview/debug build.
- [x] 6.4 Update `docs/uso.md` first-sync and daily-usage sections to describe the list topbar (status + button).
- [x] 6.5 Compose/unit tests for the new topbar states (IDLE/SUCCESS/ERROR+CONFIG/CLOCK).

## R7 — Incremental pull

- [x] 7.1 Server: add `since` query param to `GET /api/pull`; filter `updated_at > since`; optional page-limit `limit` + `next_since` returned.
- [x] 7.2 Server tests: no-`since` returns full dataset; `since` returns only newer records; pagination when `limit` exceeded.
- [x] 7.3 App: persist `lastPullWatermark` (max `updated_at` seen) in Room settings; bump after each pull.
- [x] 7.4 App: `api.pull(since)` sends the watermark; first-ever pull sends none (full). Wire through `SyncCoordinator.pull()`.
- [x] 7.5 **Gate**: do not enable `since` monotonic until `sync-incremental-photos` (R3) lands (confirm with a TODO in code + flag).
- [x] 7.6 Tests: delta pull applies authoritative records with no pending local edits; non-pending deletes handled correctly.
- [x] 7.7 Update `docs/` + `server-sync` spec at archive: pull is incremental and backward-compatible.

## R8 — Photo hygiene

- [x] 8.1 Push: when a foto row's file is missing, mark the row `pending_delete` (or delete), log a warning — instead of silently `continue`.
- [x] 8.2 Sweep: after each successful pull and on app start, delete files under the photos dir with no matching `uriLocal` in the DB (bounded batch).
- [x] 8.3 Guard the sweep against concurrent writes (run after sync completes; skip while a pull/materialize is in progress).
- [x] 8.4 Tests: row-with-missing-file gets cleaned on next push; orphaned file on disk removed by sweep; existing referenced files untouched.

## Cross-cutting

- [x] C.1 Run `./gradlew testDebugUnitTest` (app) and server test suite after each group; keep green.
- [x] C.2 Confirm no real secret/key appears anywhere in this change.
- [x] C.3 Update `docs/uso.md` + README for the combined behavior (topbar, clock message, delta pull).