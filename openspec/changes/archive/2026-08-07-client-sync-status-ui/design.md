## Context

See `proposal.md` — the sync engine is implemented (`SyncManager`, `SyncApi`, `SyncCoordinator`) but nothing in the UI consumes `ClienteRepository.syncStatus`/`syncNow()`. `ClientListViewModel` currently builds only `ClientListUiState` from `clientesComFotos` + query, and `Navigation.kt` constructs it with just `container.repository`.

`SyncStatus` is an enum (`IDLE`, `IN_PROGRESS`, `SUCCESS`, `ERROR`) and already observable. The gap is purely on the UI/ViewModel wiring.

## Goals / Non-Goals

- **Goals**: show sync status in the client list and expose a manual sync trigger; surface config faults (missing base URL / signing key) as actionable, distinct messages.
- **Non-Goals**: introduction of new sync scheduling policies, retry logic, or conflict resolution; changes outside the app UI/ViewModel surface. The sync engine behaviour is unchanged.

## Approach

1. **ViewModel state**: extend `ClientListUiState.Success` (or add a parallel `syncStatus` field on the sealed state) so the screen can render the current `SyncStatus`. In `ClientListViewModel`, `combine(repository.clientesComFotos, repository.syncStatus, query)` and expose a `fun manualSync()` that calls `repository.syncNow()` in `viewModelScope` (guarding against concurrent runs via a flag or via the existing `IN_PROGRESS` status).

2. **Config-fault signalling**: the repository/sync layer must surface whether a failure is due to `SERVER_BASE_URL`/key being unset. Implementation note: `ApiClientProvider` already throws when the key seed is missing; map that into `SyncOutcome` classification (add a fault kind) rather than surfacing as a transient network error.

3. **UI**: on `ClientListScreen`, add a top-bar or FAB-row sync control: a status chip/text (idle / in-progress / last success / error) and a refresh icon that calls `manualSync()` and is disabled while `IN_PROGRESS`.

## Alternatives Considered

- Driving the sync purely from WorkManager with a passive status: rejected — the requirement is an explicit user-triggerable sync and direct feedback.
- Novel `UiState` hierarchy (Loading/Success/Error with errors): `SyncStatus` already covers the needed states; reuse it to avoid duplication.