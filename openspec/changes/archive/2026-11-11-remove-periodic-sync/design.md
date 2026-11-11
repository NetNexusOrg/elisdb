# Design — remove-periodic-sync

## Decisions

### Remove the toggle and its state from the settings layer

The "Automático (15 min)" row and the `Switch` are removed from `ConfigScreen`.
`SyncUiState.periodicSyncEnabled`, the `periodicSyncEnabled` /
`onPeriodicSyncChanged` constructor parameters and `setPeriodicSync()` are removed
from `SyncViewModel`, and `ConfigDestination` stops wiring them.

### Replace the periodic scheduler with a retry-only scheduler

`PeriodicSyncScheduler` and `PeriodicSyncSettings` are deleted.
`SyncRetryScheduler` keeps only the transient-failure path:
`scheduleRetry()`, `cancelRetry()`, `cancelPending()` and
`cancelLegacyPeriodicSync()`. It uses the same unique work name
(`elisdb-sync-retry`), network constraint, exponential backoff and
`ExistingWorkPolicy.KEEP` as before.

### Cancel the legacy periodic job on upgrade

`ElisdbApp` calls `cancelLegacyPeriodicSync()` at startup, and
`resetSyncState()` calls `cancelPending()`. The unique work name
`elisdb-periodic-sync` is kept as a constant only for this cleanup, so a device
that had the option enabled stops the 15-minute schedule after upgrading.

### Keep `PeriodicSyncWorker`

The worker is still the one-off retry executor and already maps transient errors
to `Result.retry()` and auth/clock/config errors to `Result.failure()`. The class
name is kept to avoid breaking a retry that is already enqueued when the app is
updated; its KDoc now describes the retry role.

### Keep the resilience contract

`client-resilience` is unchanged: transient network/server failures still retry
with backoff, auth/config/validation failures do not, and a manual sync resets the
backoff.

## Risks

- Users who relied on the 15-minute schedule lose automatic background updates.
  The launch/save/delete/manual triggers and the transient retry remain, and the
  docs state the new behavior explicitly.
- A legacy periodic job must be cancelled even when the app is started offline;
  the cancellation is unconditional at startup and does not depend on network.

## Out of scope

Server, API, database schema, version bump and `CHANGELOG.md`.
