## Why

Manual sync (`syncNow()`) and the periodic WorkManager `SyncWorker` currently run independently, each building its own `SyncCoordinator`, so two syncs can execute concurrently (double push/pull, photo churn races). Worse, the periodic path never updates the observable `SyncStatus`/`SyncFault`, so automatic background failures are invisible to the user. Found as review findings R2 (concurrency, `SyncManager.kt:55` + `:93-114`) and R4.1 (worker never writes status).

## What Changes

- A single `SyncManager` own all sync executions; both the manual trigger and the periodic worker go through the same coordinator instance.
- A mutex/single-flight guard ensures only one sync runs at a time across manual and periodic triggers; a concurrent request either joins the in-flight run or is queued, never duplicated.
- The periodic worker updates the same observable `SyncStatus`/`SyncFault` so background failures are surfaced to the UI like manual ones.
- WorkManager backoff/retry still maps outcomes to `Result` (success/retry/failure) as today.

## Capabilities

### New Capabilities

- `sync-single-flight`: how the app serializes concurrent sync triggers into a single in-flight run and keeps retries independent.

### Modified Capabilities

- `client-sync`: the "Sync status exposure" requirement changes because status now reflects periodic runs too.
- `client-sync-state`: the state contract extends to "sync status is observable for both manual and automatic syncs".

## Impact

- `app/src/main/java/com/example/elisdb/sync/SyncManager.kt` — move coordinator construction here, add mutex, expose status for worker path.
- `app/src/main/java/com/example/elisdb/sync/SyncCoordinator.kt` — no behavior change; reused instance.
- `SyncWorker` — call `SyncManager.sync()` in shared scope instead of local `SyncCoordinator`.
- `SettingsViewModel`/`SyncStatusBar` — unchanged rendering; now receives periodic results.
- Tests: SyncManager/worker concurrency unit tests; existing UI tests unaffected.