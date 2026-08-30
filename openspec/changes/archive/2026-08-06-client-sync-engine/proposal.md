## Why

The app must work fully offline and reconcile with the server when connectivity returns, pushing local changes and pulling the authoritative server state. The server owns conflict resolution (last-write-wins on `updated_at`), so the app must only push and pull — never resolve conflicts locally. A sync coordinator is needed to serialize offline changes, retry when offline, and apply the server's verdict to the local store.

## What Changes

- Add an offline-first sync service that, when connectivity is available, pushes all pending/dirty client+photo changes via the server API and pulls the full server state.
- **BREAKING (data-surface):** the app treats the server as the single authority: values returned by a `pull` overwrite local records that have no pending local edits; a local record that a push reports as `stale` is replaced by the pulled server record.
- Pending operations (new/edited/deleted records) are queued locally (via `client-sync-state`) and only cleared once the server acknowledges them.
- Photo changes ride along with their client: base64 on push, decoded to app-private files on pull, and removed when the server deletes them.
- Idempotent: repeated pushes/pulls of the same state converge; network errors are retried later without data loss.
- Sync runs automatically on connectivity changes and app start, exposing a sync status (idle/in-progress/error) to the UI.

## Capabilities

### New Capabilities
- `client-sync`: the end-to-end offline-first synchronization capability: pushing pending local records, pulling remote authoritative state, applying server results to local storage, and surfacing sync status. (This change builds on the API layer introduced by client-server-api.)

### Modified Capabilities
- `client-data`: read/observe semantics gain the notion that pulled server records replace stale local copies, and that a locally-deleted client stays hidden but its deletion may still be pending transmission.

## Impact

- New `.../data/sync/` package — `SyncCoordinator`, `SyncWorker` (WorkManager), `SyncStatus`.
- `app/src/main/java/com/example/elisdb/data/ClienteRepositoryImpl.kt` — derive pending snapshot and observe authoritative data merged with server pulls.
- `app/src/main/java/com/example/elisdb/data/ClienteRepository.kt` — expose `syncNow()`, `syncStatus` flow.
- `app/src/main/java/com/example/elisdb/ElisdbApp.kt` — start/stop the coordinator; periodic work.
- UI — sync status indicator; refresh-on-sync so lists update after pull.
- AndroidManifest manifests-workchain permission.
- Depends on `client-server-api` (transport) and `client-data-model` (sync state).