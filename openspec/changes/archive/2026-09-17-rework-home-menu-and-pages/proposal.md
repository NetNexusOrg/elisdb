## Why

The home menu currently offers a single "Definições" entry that opens one screen
mixing two unrelated concerns: synchronization status/trigger and server-access
configuration. Users looking for either task must parse a combined page, and the
single `SettingsViewModel` conflates sync state with configuration state. Splitting
them gives each task a dedicated, focused screen. The menu is also the natural home
for a dedicated "Sobre" (About) page with app and developer information, which does
not belong to any existing screen.

## What Changes

- The home dropdown menu SHALL replace the single "Definições" action with
  "Sincronização", "Servidor" and "Sobre" actions.
- A dedicated **Sync** screen SHALL show the sync status row (icon + message) with a
  fixed-position "check for updates" action (a server query that does not transfer
  data) and a prominent full-width manual sync control (the actual data transfer),
  the one-shot sync notice, the time of the last successful sync, the total number
  of stored clients, and explicit pending-upload (with count) and pending-download
  rows.
- A dedicated **Server** screen SHALL show the read-only server configuration
  (base URL, masked signing key, associated user), the import-configuration
  action, and the remove-configuration action.
- A dedicated **About** screen SHALL show information about the app (name, version,
  short description) and about the developer (name and an actionable contact email).
- The old combined settings screen and its route SHALL be removed.
- `SettingsViewModel` SHALL be split into `SyncViewModel` and
  `ServerConfigViewModel`; `SyncStatusBar` and the server-info row helper are reused.
- **BREAKING** (internal/UI): the `Settings` navigation key and `SettingsScreen`
  composable are removed; navigation now uses `Sync`, `Server` and `About` keys.

## Capabilities

### New Capabilities

- `client-about`: a dedicated About screen showing app information (name, version,
  description) and developer information (name, contact email), reachable from the
  home menu.

### Modified Capabilities

- `client-screens`: the client-list top-bar menu requirement changes — instead of a
  single "settings action" it now hosts a sync action, a server action, and an
  about action.
- `client-settings`: scope is reduced to the server-access screen only (server
  configuration view, import, remove, key encrypted at rest). The sync-status,
  manual-trigger, fixed-position, and pending-state requirements are removed from
  this capability.
- `client-sync`: gains the user-facing requirements for the dedicated sync screen —
  status presentation, total client count, pending state, fixed-position manual
  trigger, and the screen it appears on.

## Impact

- Android navigation: `NavigationKeys.kt` (`Settings` → `Sync`, `Server`, `About`),
  `Navigation.kt` (entries and client-list callbacks).
- Android UI: `ui/cliente/ClientListScreen.kt` (dropdown), new
  `ui/settings/SyncScreen.kt` and `ui/settings/ServerConfigScreen.kt`, new
  `ui/about/AboutScreen.kt`; `ui/settings/SettingsScreen.kt` removed.
  `ui/settings/SyncStatusBar.kt` reused.
- Android data: `ClienteDao` gains a client-count query flow.
- Android sync: `SyncManager` gains a pending-upload count and a last-successful-sync
  timestamp (persisted through a new `LastSyncStore`); `AppContainer` wires it.
- Android ViewModels: new `SyncViewModel` and `ServerConfigViewModel`;
  `SettingsViewModel` removed. `AboutScreen` is static (reads `BuildConfig`), no
  ViewModel. `SettingsStore` unchanged.
- Tests: client-list menu test updated; settings screen/ViewModel tests split into
  sync and server counterparts; About screen test added. `SyncStatusBarTest`
  unchanged.
