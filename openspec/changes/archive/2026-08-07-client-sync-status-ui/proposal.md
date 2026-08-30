## Why

The sync engine (push/pull/delete, ed25519 authentication, `syncStatus`) is fully implemented and `ClienteRepository.syncNow()`/`syncStatus` are exposed, yet nothing in the UI consumes them: users have no way to trigger a sync manually and no feedback on whether data is in sync, syncing, or failed. For a release-ready app the user must be able to start a sync and see its outcome.

## What Changes

- Add a persistent sync status indicator on the client list screen showing `IDLE`, `IN_PROGRESS`, `SUCCESS`, `ERROR`.
- Add a manual sync trigger (refresh action) on the client list screen that calls `syncNow()`.
- Surface the latest `SyncStatus` from `repository.syncStatus` in `ClientListViewModel` as part of its UI state.
- Provide clear error feedback when a sync fails (e.g. auth or transient network error), so the user knows why and what to do.
- Distinguish "no server configured" from silent failure so a user with a missing/incorrect signing key or base URL sees an actionable message.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `client-sync`: adds a concrete UI requirement that the sync status be observable in the list screen and a manual sync be triggerable from it.
- `client-screens`: adds the sync status bar / refresh control requirement to the client list screen.

## Impact

- `app/src/main/java/com/example/elisdb/ui/cliente/ClientListViewModel.kt` — merge `repository.syncStatus` into `ClientListUiState`.
- `app/src/main/java/com/example/elisdb/ui/cliente/ClientListScreen.kt` — render status indicator and refresh action.
- `app/src/main/java/com/example/elisdb/ui/cliente/ClientListViewModel.kt` (factory wiring) and `Navigation.kt` — pass through the sync trigger; no API change needed since `ClienteRepository` already exposes `syncNow()`/`syncStatus`.
- `SyncStatus` enum and `SyncManager.status` reused as-is; error messaging may need richer detail mapping.