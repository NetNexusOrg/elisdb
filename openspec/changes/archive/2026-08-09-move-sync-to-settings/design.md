## Context

See proposal.md — Why. Today the sync status bar (icon, status message, sync errors, and the manual trigger) renders on the client list screen via `SyncStatusBar` in `ClientListScreen.kt`, fed by `ClientListUiState.Success.syncStatus`/`syncFault`. Those come from `ClienteRepository.syncStatus`/`syncFault` flows combined in `ClientListViewModel`. The settings screen (`SettingsScreen.kt` + `SettingsViewModel`) already receives the shared `SyncManager` (see Navigation.kt), which exposes the same status and fault as `status`/`fault` StateFlows. This change relocates the sync status bar to settings and drops it from the list.

## Goals / Non-Goals

- **Goals**: sync status + messages + manual trigger appear on the settings screen; the client list no longer shows any sync UI; sync engine, status model, repository, and `SyncManager` unchanged.
- **Non-Goals**: changing the sync engine, the `SyncStatus`/`SyncFault` model, or the repository's sync flows; adding new statuses or messages; reworking the settings form fields.

## Decisions

**1. Source the settings sync status from `SyncManager.status`/`fault` flows, not the repository.**

`SyncManager` is already injected into `SettingsViewModel` (Navigation.kt) and is the authoritative source of sync state for the app (it drives periodic + manual sync and `SyncWorker`). `SettingsViewModel` collects `syncManager.status` + `syncManager.fault` into its `SettingsUiState` alongside the form fields, and exposes `manualSync()` that calls `syncManager.syncNow()`.

- Rationale: one shared source of truth; the settings page is exactly where sync is triggered and configured, so it should observe the manager that owns the schedule.
- Alternative considered: route the repository's `syncStatus`/`syncFault` into settings. Rejected — the repository flows were introduced to power the list screen's status bar and would leave the list ViewModel as a redundant middleman; `SyncManager` is simpler and already injected.

**2. Reuse the existing `SyncStatusBar` composable, relocated.**

Move `SyncStatusBar`/`syncMessage` (plus the `Sync`/`CloudDone`/`CloudOff` icons and `CircularProgressIndicator` rendering) from `ClientListScreen.kt` into the settings package. The `syncMessage(state)` mapping (IN_PROGRESS → "A sincronizar…", SUCCESS → "Sincronizado", ERROR→CONFIG/AUTH/NETWORK messages, IDLE → null) stays identical.

- Rationale: the messages and state mapping are already spec-compliant and covered by tests; relocating (not rewriting) minimizes behavioral drift.
- Alternative considered: duplicating the composable in both screens. Rejected — no screen shows it anymore but settings, so a single copy lives in settings. The client list ViewModel drops its now-unused `syncStatus`/`syncFault` fields and `manualSync()`.

**3. Remove the sync UI from the client list and simplify its ViewModel.**

`ClientListUiState.Success` drops `syncStatus`/`syncFault`; `ClientListViewModel` combines only `clientesComFotos` + `query` and removes `manualSync()` and the `syncRunning` guard. The list keeps the settings action in the search field's trailing icon so the user can still reach the new sync location.

- Rationale: keeps the list ViewModel minimal and avoids exposing sync state nobody renders.
- Note: `ClienteRepository.syncStatus`/`syncFault`/`syncNow()` stay on the interface (used elsewhere / by `SyncManager` path), so no repository change is needed.

## Risks / Trade-offs

- [Sync status is now only visible on the settings screen] → The list keeps a prominent settings action; the settings screen is one tap away. Accepted per the requirement.
- [CONFIG fault previously rendered a "Definições" shortcut on the list; after the move the message and the settings screen are the same page] → The CONFIG message ("Servidor não configurado...") now shows on the settings screen where the user can immediately fix it; no cross-screen navigation needed.
- [Removing fields from `ClientListUiState` touches existing list tests] → Update the affected UI/ViewModel tests to drop sync assertions; add settings-side tests for the moved behavior.

## Migration Plan

1. Add sync status/fault + `manualSync()` to `SettingsUiState`/`SettingsViewModel` (observe `syncManager.status`/`fault`; call `syncManager.syncNow()`).
2. Move `SyncStatusBar`/`syncMessage` into the settings package and render it in `SettingsScreen`.
3. Remove `SyncStatusBar` from `ClientListScreen` and strip `syncStatus`/`syncFault`/`manualSync()` from `ClientListUiState`/`ClientListViewModel`.
4. Update tests; build and run the suites.

Rollback: revert the ViewModel/screen changes; the shared `SyncManager` and repository are untouched, so behavior degrades gracefully to the previous layout.

## Open Questions

None.