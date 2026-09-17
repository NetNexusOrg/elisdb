## 1. Navigation

- [x] 1.1 Replace the `Settings` navigation key with `Sync` and `Server` in `NavigationKeys.kt`; verify the file compiles (`./gradlew :app:compileDebugKotlin`).
- [x] 1.2 Update `ClientListScreen` callbacks to `onSyncClick`/`onServerClick` and wire `backStack.add(Sync)` / `backStack.add(Server)` in `Navigation.kt`; remove the `entry<Settings>` block and add `entry<Sync>` and `entry<Server>`, each building its own ViewModel. Verify it compiles.
- [x] 1.3 Add the `About` navigation key and its `entry<About>` (static screen, `onBack` only), and wire the client-list `onAboutClick` callback in `Navigation.kt`; verify it compiles.

## 2. Home menu

- [x] 2.1 Replace the "Definições" dropdown item in `ClientListScreen.kt` with "Sincronização" and "Servidor" items and update the menu test's expectations.
- [x] 2.2 Add the "Sobre" dropdown item and update the menu test to assert it.

## 3. ViewModels

- [x] 3.1 Add `SyncViewModel` (sync status/fault/notice/pending/hasKey + `manualSync`, `refreshPendingState`) and verify its unit tests pass.
- [x] 3.2 Add `ServerConfigViewModel` (baseUrl/signingKey/hasKey/error/keyIdentity + `importKey`, `clearConfig`) and verify its tests pass.
- [x] 3.3 Delete `SettingsViewModel.kt` and confirm no references remain (`rg SettingsViewModel`).
- [x] 3.4 Add `checkForUpdates()` (with a `checking` flag) to `SyncViewModel`, distinct from `manualSync()`, and verify its tests pass.

## 4. Screens

- [x] 4.1 Add `SyncScreen.kt` (top bar "Sincronização", `SyncStatusBar`, sync notice) and verify it renders in its Compose test.
- [x] 4.2 Add `ServerConfigScreen.kt` (top bar "Servidor", server-config card, import/remove) and verify it renders in its Compose test.
- [x] 4.3 Delete `SettingsScreen.kt` and confirm no references remain (`rg SettingsScreen`).
- [x] 4.4 Show the total number of stored clients (excluding pending deletions) on the synchronization screen; add a DAO count query and verify the count appears in the Compose test.
- [x] 4.5 Add `ui/about/AboutScreen.kt` (top bar "Sobre", app name/version/description and developer name/email) and verify it renders in its Compose test.
- [x] 4.6 Show the time of the last successful sync on the synchronization screen, persisting it through a new `LastSyncStore` wired in `AppContainer`, and verify in tests.
- [x] 4.7 Show explicit pending-upload (with count) and pending-download rows on the synchronization screen and verify in the Compose test.
- [x] 4.8 Add a prominent full-width manual sync control (disabled while syncing) and verify the Compose test.
- [x] 4.9 Make the About contact email actionable (open an external email composer) without crashing if no email app is available.
- [x] 4.10 Make the status-row action a "check for updates" action (server query, no transfer, spinner while checking, disabled without config) and keep the full-width control as the sole data-transfer action; verify in the Compose tests.

## 5. Tests

- [x] 5.1 Update `ClientListScreenTest` to assert "Sincronização", "Servidor" and "Sobre" in the menu.
- [x] 5.2 Split the settings screen test into `SyncScreenTest` and `ServerConfigScreenTest`; confirm behavior coverage for sync status/trigger and import/remove/invalid.
- [x] 5.3 Split the settings ViewModel tests into `SyncViewModelTest` and `ServerConfigViewModelTest`; confirm they pass.
- [x] 5.4 Add `AboutScreenTest` asserting the app name, version, description and developer email are shown.
- [x] 5.5 Add `SyncManager` tests for the pending-upload count and the persisted last-successful-sync timestamp.

## 6. Verification

- [x] 6.1 Run `./gradlew :app:lintDebug` and `./gradlew :app:testDebugUnitTest` from `elisdb-android/` and confirm both pass.
- [x] 6.2 Run `./gradlew :app:connectedDebugAndroidTest` (device/emulator) and confirm the instrumented suite passes. _(103 tests, 0 failures on `medium_phone` AVD / API 36)_
- [x] 6.3 Grep `*/docs/` for "Definições" and update any stale references; document the About screen in the navigation flows; run `openspec validate rework-home-menu-and-pages`.
