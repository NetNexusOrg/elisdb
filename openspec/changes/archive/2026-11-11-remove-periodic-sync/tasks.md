# Tasks — remove-periodic-sync

## 1. Remove the setting and the periodic schedule

- [x] 1.1 Remove the "Automático (15 min)" row and switch from `ConfigScreen`.
- [x] 1.2 Remove `periodicSyncEnabled` / `onPeriodicSyncChanged` / `setPeriodicSync` from `SyncViewModel` and its `ConfigDestination` wiring.
- [x] 1.3 Replace `PeriodicSyncScheduler`/`PeriodicSyncSettings` with a retry-only `SyncRetryScheduler`.
- [x] 1.4 Cancel the legacy `elisdb-periodic-sync` job at startup and on sync-state reset.
- [x] 1.5 Keep `PeriodicSyncWorker` for transient retries and document its new role.

## 2. Tests and docs

- [x] 2.1 Remove the periodic toggle test from `SyncViewModelTest`.
- [x] 2.2 Remove the periodic preference test and cleanup from `SettingsStoreTest`.
- [x] 2.3 Keep `PeriodicSyncWorkerTest` covering the retry mapping.
- [x] 2.4 Update `elisdb-android/docs/uso.md` with the remaining sync triggers and retry behavior.

## 3. Verification and archive

- [x] 3.1 Run `:app:testDebugUnitTest`, `:app:lintDebug` and `:app:compileDebugAndroidTestKotlin`.
- [x] 3.2 Install on the emulator and confirm the toggle is gone and manual sync still runs.
- [x] 3.3 Validate and archive the change.

## Workflow

- Commit after user confirmation (never commit or push automatically).
