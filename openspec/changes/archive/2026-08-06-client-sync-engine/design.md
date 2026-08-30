## Context

This change coordinates offline-first sync. The server is the single authority for conflicts (last-write-wins on `updated_at`). It depends on `client-data-model` (pending markers, stable ids, `updatedAt`) and `client-server-api` (transport, DTOs, auth). See proposal.md - Why and the `server-sync`/`server-auth` specs.

## Goals / Non-Goals

**Goals:**
- Offline-first: local writes are always possible; they sync when online.
- Push only pending data and pull the authoritative dataset.
- The app never decides conflicts (no local LWW); it reflects server results.
- Idempotent, retryable, status observable.

**Non-Goals:**
- Local conflict resolution / merging.
- Implementing the transport (that's `client-server-api`).
- Offline queueing storage design beyond the pending markers (model change).

## Decisions

**1. A `SyncCoordinator` orchestrates via a `SyncManager` that uses WorkManager's retry-friendly work.**
Rationale: WorkManager gives constraints (network), backoff, and periodic scheduling naturally; keeps the UI layer above it.

**2. Push-before-pull ordering.**
Send pending changes first (so the server has freshest state), then pull. Rationale: the pull then reflects confirmed writes.

**3. Apply pull by server-authority:**
- Records with no pending local edits → overwrite with server values.
- Records that the push reported as `stale`/removed → drop/overwrite from pull.
- Pending records not yet acked → stored locally until push acks (not overwritten by an in-flight pull).

**4. Local photo lifecycle on pull:** decode `dataB64` into app-private files; on server-confirmed delete, remove the local file.

## Risks / Trade-offs

- **[Risk] Overwriting a device's unsynced edit with a pull** → pull only overwrites non-pending records; pending are re-pushed.
- **[Risk] base64 + full-dataset pull is heavy** → acceptable at current scale; could add delta-pull later (out of scope).
- **[Risk] Conflict between two devices' pending edits** → server resolves via LWW; the app reflects the returned status. That is the intended behavior (server owns conflicts).