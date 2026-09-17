## Context

See `proposal.md` for motivation. The current Android app has one settings screen
(`ui/settings/SettingsScreen.kt`) driven by a single `SettingsViewModel`, reached
from the client-list dropdown via one `Settings` navigation key. Navigation 3
(`androidx.navigation3`) maps each back-stack key to an entry that owns its own
ViewModel instance via the `viewModel {}` factory, so a screen reached from two
places does not naturally share one shared ViewModel instance.

The sync UI is already a self-contained composable (`SyncStatusBar`) with its own
tests; the server-config UI is a `Card` plus an import button and two private
helpers (`ServerInfoRow`, `maskedKey`).

## Goals / Non-Goals

**Goals:**
- Focused, independently navigable screens: synchronization, server, and about.
- Reuse `SyncStatusBar` and the server-info presentation without behavior changes.
- Keep `SettingsStore`, `SyncManager`, and `AppContainer` untouched.
- No user-visible regression in sync status/pending behavior or server config
  import/remove/identity behavior.

**Non-Goals:**
- Changing sync engine behavior, scheduling, or the settings persistence model.
- Adding new server endpoints or data.
- Redesigning the visual style beyond moving existing content into its own screen.

## Decisions

- **Two ViewModels (`SyncViewModel`, `ServerConfigViewModel`) instead of one shared
  `SettingsViewModel`.** Navigation 3 gives each entry its own ViewModel store, so
  sharing a single instance would require awkward hoisting. Splitting also removes
  the state conflation (sync state vs. configuration state) that motivated the
  change. Alternative considered: keep `SettingsViewModel` and pass one instance to
  both entries — rejected because it fights the per-entry ViewModel scoping and
  keeps the mixed responsibility.
- **Keep both screens in the existing `ui/settings/` package rather than new
  packages.** Minimal churn; `ui/settings/` already owns `SyncStatusBar`.
- **`SyncViewModel` depends on `SettingsStore` + `SyncManager`** (needs `hasKey`
  for the "not configured" state and the pending/status flows). `ServerConfigViewModel`
  depends on `SettingsStore` + `SyncManager` + `fetchKeyIdentity` (import still
  triggers an immediate sync and identity lookup, preserving current behavior).
- **Navigation keys `Sync` and `Server`** are plain `@Serializable data object`s,
  matching the existing flat style. `Settings` is removed.
- **About is a static screen with no ViewModel.** Its content is fixed (app name,
  version from `BuildConfig.VERSION_NAME`, description, developer name and email),
  so it takes only an `onBack` callback and is placed in its own `ui/about/` package.
  A new `About` navigation key is added, matching the flat key style.
- **Client count is read via a dedicated DAO query** (`ClienteDao.observeCount()`,
  excluding pending deletions) rather than deriving it from the full client list, so
  the sync screen does not load all rows just to show a number.
- **Two distinct sync-screen actions with different roles.** The status-row action
  is a *check for updates* (`SyncViewModel.checkForUpdates` → `SyncManager.refreshPendingDownloads`),
  which queries the server (`/api/latest`) and refreshes the pending indicators
  without transferring data; the full-width control performs the real transfer
  (`SyncViewModel.manualSync` → `SyncManager.syncNow`). This removes the earlier
  duplication where two controls triggered the identical sync. The check exposes a
  `checking` flag (spinner + disabled) and is disabled when no server is configured.

## Risks / Trade-offs

- [Two ViewModels each collect `SyncManager` flows] → Acceptable; collectors use
  `viewModelScope` and are cancelled when the entry leaves the back stack. Only one
  of the two screens is on top at a time.
- [Duplicated `fetchKeyIdentity` wiring in `Navigation.kt`] → Contained to one
  entry; `SyncViewModel` does not need identity.
- [Tests currently bind to the combined screen] → Split the screen and ViewModel
  tests alongside the production changes so behavior stays covered.
- [Stale references in `docs/`] → Grep for "Definições" and update after the code
  change; add the About screen to the documented navigation flows.
- [Hard-coded developer contact goes stale] → Centralise the About strings in the
  composable so they are trivial to update.
