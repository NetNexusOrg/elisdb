## Context

See proposal.md. Current problems verified in code:

- `SyncManager.syncNow()` (SyncManager.kt:53-85) guards only its own path (`_status == IN_PROGRESS`) and builds a coordinator via `coordinatorProvider.coordinator()`.
- `SyncWorker.doWork()` (SyncManager.kt:93-114) builds its own `SyncCoordinator` from `app.container` and returns a WorkManager `Result` without touching `_status`/`_fault`.

No shared lock and no shared status publisher connect the two.

## Goals / Non-Goals

**Goals:**
- One coordinator instance owned by `SyncManager`.
- `syncNow()` and the worker call the same `SyncManager.sync()` entry, serialized by the same mutex.
- A single source of truth for `SyncStatus`/`SyncFault`, updated on every path (manual + periodic + failure).
- WorkManager still gets the correct `Result` mapping (success / retry / failure) for scheduling/backoff.

**Non-Goals:**
- Changing the sync algorithm or the server protocol.
- Adding re-run/queue management beyond "run now" (no coalescing window, no pending-run queue) — a queued trigger while IN_PROGRESS is acceptable as documented.
- Persisting status across process death (status is session-scoped).

## Decisions

**D1 — `SyncManager` owns the coordinator and a `Mutex`, and exposes `suspend fun sync(): SyncOutcome`.** `syncNow()` becomes a thin wrapper that sets `_status = IN_PROGRESS`, awaits `mutex.withLock { coordinator.sync() }`, maps outcome→`SyncStatus`/`SyncFault`, and returns `SyncOutcome` unchanged for the caller.
- *Alternative:* keep coordinator creation in each caller and only share a mutex — rejected because the coordinator holds no local mutable state beyond the DAO/API, so sharing one instance is safe and simplifies DI.

**D2 — `SyncWorker` delegates to `SyncManager.sync()` and maps the returned `SyncOutcome` to WorkManager `Result`.** Mapping stays as today (Success→`Result.success`, Auth/Config→`Result.failure`, Transient→`Result.retry`) — only the entry point changes. The worker gets the app's `SyncManager` from the container.
- *Alternative:* worker uses `syncNow()` — rejected because `syncNow()` is fire-and-forget (`viewModelScope.launch`); the worker must await completion to return a correct `Result`.

**D3 — Join semantics.** A manual trigger arriving while a periodic run is in-flight calls `mutex.withLock { ... }` and blocks until the in-flight run finishes but does NOT start a second push. This is the "join" mode. We accept blocking the UI coroutine briefly because sync is already running.
- *Alternative:* return early with the current status without waiting — would leave the user believing a sync ran when it only echoed the last status.

## Risks / Trade-offs

- **Worker and foreground share a queue**: a long pull blocks a manual trigger via the mutex → mitigated by join semantics and short syncs; acceptable.
- **Two processes/instances**: out of scope; one process on app.
- **Deadlock risk with Room**: `sync()` calls `withTransaction` inside coordinator; mutex is held across it, which is fine as long as no other coroutine needs the same mutex inside the transaction. Documented.
- **`SyncStatus` transient**: if the app dies mid-sync, status resets to IDLE on boot; WorkManager reschedules, and next run updates status. Accepted.

## Migration Plan

- Pure app refactor inside `sync` package; no schema/DB changes. Steps: introduce `SyncManager.sync()` + mutex; refactor `syncNow()` and `SyncWorker`; update tests. Rollback = revert the merge.
- Update `openspec/specs/client-sync-state` and `client-sync` delta after archive.

## Open Questions

- Should a duplicate trigger while IN_PROGRESS be surfaced as "already syncing" in the UI, or silently join? Choose silent join per D3; UI copy can be added in apply if needed.