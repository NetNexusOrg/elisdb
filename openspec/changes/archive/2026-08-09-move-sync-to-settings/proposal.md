## Why

The sync state (icon, status message, and manual trigger) lives today on the client list screen, while server configuration — the values a sync depends on — is edited on the settings screen. Sync information and its configuration should live on the same page so the user sees the connection status exactly where it is configured.

## What Changes

- **Move the sync status bar out of the client list screen.** The client list screen SHALL no longer show the sync icon, status message, sync errors, or the manual sync trigger; it keeps only the client list, search and navigation (settings, add client).
- **Add the sync status bar to the settings screen.** The settings screen SHALL show the sync status (idle/in-progress/success/error), the corresponding message for errors (missing config, auth, network), and the manual sync trigger button — collocated with the server URL and signing key fields.
- The missing-configuration prompt SHALL remain reachable from the client list (settings action) but the sync-spec show a genuine error there; the sync icon/warning is shown in settings.
- No change to the sync engine, status model, or repository: only where status is rendered.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `client-screens`: the client list screen loses the sync status message, sync error display, "no sync activity when idle" behavior, and the manual sync trigger requirement.
- `client-settings`: the settings screen gains the sync status information (icon + message + errors) and the manual sync trigger.

## Impact

- `app/src/main/java/com/example/elisdb/ui/cliente/ClientListScreen.kt` — remove `SyncStatusBar`/`syncMessage` usage from the list screen; keep the settings action.
- `app/src/main/java/com/example/elisdb/ui/settings/SettingsScreen.kt` — add the sync status bar (icon, message, manual trigger), reused or reimplemented from the current list bar.
- `app/src/main/java/com/example/elisdb/ui/settings/SettingsViewModel.kt` — expose sync status/fault (from the shared `SyncManager` `status`/`fault` flows) and a manual sync action; the client list ViewModel no longer needs these.
- `app/src/main/java/com/example/elisdb/ui/cliente/ClientListViewModel.kt` / `ClientListUiState` — drop the sync fields if no longer used by the list.
- Tests: UI tests for list and settings sync rendering updated; ViewModel tests for settings sync status added.