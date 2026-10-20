# Tasks

## 1. Components
- [x] 1.1 Add `ui/components/StatTile.kt` (value + label tile, optional muted value colour)
- [x] 1.2 Rework `SyncStatusBar`: tinted icon container, `titleMedium` title, optional `detail` line; pending title "Alterações por sincronizar"
- [x] 1.3 Remove the check-for-updates action (`check_action`) and the `checking`/`onCheckClick` plumbing; the state is refreshed when the screen opens

## 2. Screen
- [x] 2.1 `ConfigScreen`: summary card (bar + notice banner + last-sync caption + primary control), data card (two stat tiles + divider + pending rows), server card (icon-led URL/key/user rows + import/remove actions)
- [x] 2.2 Primary control shows "Importar configuração" when not configured and opens the file picker on the same screen
- [x] 2.3 Muted pending rows when the count reads `0`; muted "—" when the server total is unknown
- [x] 2.4 `Navigation`/`NavigationKeys`: single `Config` destination fed by `SyncViewModel` + `ServerConfigViewModel`; menu has one "Configuração" entry
- [x] 2.5 Delete `SyncScreen.kt` and `ServerConfigScreen.kt`; drop `hasKey` from `SyncUiState` (the server view model owns the configured state)

## 3. Tests
- [x] 3.1 Update `SyncStatusBarTest` for the new pending title and the detail line
- [x] 3.2 Merge `SyncScreenTest` and `ServerConfigScreenTest` into `ConfigScreenTest` (labels/caption, data totals, pending detail line, server details, import/remove, invalid file)
- [x] 3.3 Update `ClientListScreenTest` (single menu entry) and `SyncViewModelTest` (constructor)
- [x] 3.4 Run unit tests, lint and the instrumented screen tests on the emulator

## 4. Validation
- [x] 4.1 Verify on the emulator against the live server (synced state, pending state, server card, import flow)
- [x] 4.2 `openspec validate rework-config-screen --strict`
