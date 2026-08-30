## 1. Config-fault classification in the sync layer

- [x] 1.1 Map the missing-key / missing-server-base-url condition in the sync layer (`SyncCoordinator`/`SyncApi`/`ApiClientProvider`) to a distinct sync outcome/fault kind (e.g. `ConfigFault`) instead of a transient network error.
- [x] 1.2 Propagate the fault kind through `SyncManager.syncNow()` so callers can distinguish config faults from network errors.

## 2. ViewModel wiring

- [x] 2.1 Extend `ClientListViewModel` to combine `repository.clientesComFotos`, `repository.syncStatus`, and the query into `ClientListUiState`.
- [x] 2.2 Add `manualSync()` to `ClientListViewModel` calling `repository.syncNow()` in `viewModelScope`, with a guard that prevents concurrent runs while status is `IN_PROGRESS`.

## 3. UI: status indicator and manual trigger

- [x] 3.1 Add a sync status indicator (idle / in-progress / last success / error) to `ClientListScreen`, including the actionable message for config faults.
- [x] 3.2 Add a manual sync action (e.g. top-bar refresh icon) that calls `manualSync()` and is disabled while a sync is in progress.

## 4. Verification

- [x] 4.1 Confirm the existing sync tests still pass and add/update tests covering the config-fault outcome mapping.
- [x] 4.2 Run the app and verify the status indicator and manual sync trigger behave as specified (idle → in-progress → success/error, and the empty-config message).