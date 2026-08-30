## 1. Settings sync state

- [x] 1.1 Extend `SettingsUiState` with `syncStatus` and `syncFault` fields and collect them from `SyncManager.status`/`fault` flows in `SettingsViewModel`.
- [x] 1.2 Add `manualSync()` to `SettingsViewModel` that calls `syncManager.syncNow()`, guarded against re-entry while a sync is in progress.

## 2. Move the sync status bar to settings

- [x] 2.1 Move the `SyncStatusBar` and `syncMessage` composables (with their icon/message rendering for IN_PROGRESS, SUCCESS, ERROR/CONFIG/AUTH/NETWORK) from `ClientListScreen.kt` into the settings package.
- [x] 2.2 Render the sync status bar in `SettingsScreen` wired to `SettingsUiState.syncStatus`/`syncFault` and the `manualSync()` action.

## 3. Remove sync UI from the client list

- [x] 3.1 Remove the `SyncStatusBar` usage from `ClientListScreen`; keep the settings action accessible (now in the top bar after the `client-list-topbar` change).
- [x] 3.2 Drop `syncStatus`, `syncFault` from `ClientListUiState.Success` and remove `manualSync()`/the `syncRunning` guard and the sync-flow combination from `ClientListViewModel`.

## 4. Tests

- [x] 4.1 Update `ClientListScreenTest`/`ClientListViewModelTest` to drop sync-status/sync-button assertions (sync state is no longer on the list).
- [x] 4.2 Add settings tests covering sync status rendering (in-progress, success, error messages) and that manual sync is triggerable and not duplicated while running.

## 5. Build and verification

- [x] 5.1 Build the debug app, run the unit and instrumented test suites, and confirm no regressions from moving the sync status bar.