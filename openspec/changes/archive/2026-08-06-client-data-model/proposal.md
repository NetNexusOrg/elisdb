## Why

The app's local data model uses auto-incremented `Long` identifiers and has no per-record modification timestamp or sync state, so it cannot interoperate with the server, whose records carry stable TEXT identifiers and resolve conflicts by last-write-wins on `updated_at`. Making the local model "sync ready" (stable shared ids, modification timestamps, and pending-change markers) is the foundation for offline-first syncing.

## What Changes

- **BREAKING:** Client and photo identifiers change from local autoincrement `Long` to stable server-compatible identifiers (UUID text) that are shared between device and server.
- Client records gain an `updated_at` modification timestamp used by the server for last-write-wins conflict resolution.
- Client and photo records gain a "pending/dirty" marker indicating unsynced local changes so the sync engine can push exactly what changed offline.
- Deletion becomes a soft, pending operation (tombstone) so an offline delete can be transmitted later, instead of being dropped immediately.
- Photo records gain a server-compatible fotoid and the ordering/content-type metadata required by the push/pull contract.

## Capabilities

### New Capabilities
- `client-sync-state`: capabilities for tracking which records have unsynced local changes (dirty markers and deletion pending) in support of offline-first sync.

### Modified Capabilities
- `client-data`: the client record and photo schemas change to carry server-compatible identifiers, a modification timestamp, pending-change metadata, and soft-delete markers, without changing how screens read/list them.

## Impact

- `app/src/main/java/com/example/elisdb/data/local/Cliente.kt` — id type, add `updatedAt` + pending/delete flag.
- `app/src/main/java/com/example/elisdb/data/local/Foto.kt` — id type, add mime/ordem metadata.
- `app/src/main/java/com/example/elisdb/data/local/{ClienteDao,FotoDao}.kt` — DAO queries for dirty records and pendings.
- `app/src/main/java/com/example/elisdb/data/local/AppDatabase.kt` — schema/migration (Room version bump).
- `app/src/main/java/com/example/elisdb/data/ClienteRepositoryImpl.kt` and call sites that assume `Long` ids.
- The UI layer must be updated to read the new id type where it browses/navigates by client id.