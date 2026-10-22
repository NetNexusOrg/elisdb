# Proposal

## Why

The synchronization screen says the same thing twice — the status row reports "Sincronização disponível (enviar e receber)" while the data rows repeat the same information numerically — and the most important signal (is my data in sync?) is rendered in small, muted text next to an unlabeled icon. The numbers that would answer "quanto há no servidor, quanto há na app, falta sincronizar?" are presented as four identical rows, so they cannot be compared at a glance. Finally, when no server is configured, the primary button still offers a sync that is guaranteed to fail, with no way forward from the screen.

The synchronization screen and the server screen are also two halves of the same question: the sync state depends on the server configuration, and configuring the server lives one screen away. The menu offers two entries ("Sincronização" and "Servidor") for one task, and after importing a configuration the user has to navigate back to check whether the sync is running.

## What Changes

- The status block becomes a single, prominent summary: a tinted state icon, a short state title ("Sincronizado", "Alterações por sincronizar", "A sincronizar…", "Não configurado", or the failure message), and a one-line detail with the concrete pending work ("3 por enviar · novidades por receber"). The pending state is refreshed when the screen opens, so the separate check-for-updates action is removed and the screen keeps a single sync action.
- The last-successful-sync time moves under the state title as a caption ("Última sincronização: …" / "Ainda sem sincronizações"), instead of being a separate data row.
- The "Dados" card leads with the two client totals side by side ("Na app" / "No servidor"), followed by the pending-changes rows. Both pending rows are muted when they read `0`.
- The synchronization screen and the server screen become a single **Configuração** screen with three cards in the same visual language (status, data, server). The menu has one entry, "Configuração".
- The server card shows the URL, the masked key and the associated user with icon-led detail rows, followed by the import action and, when configured, the remove action. Importing a configuration stays on the same screen and starts a sync, so the status card reflects it immediately.
- When no server configuration is loaded, the summary states that synchronization is not available and the import action is offered only once, in the server card.
- The post-sync notice is shown as a highlighted banner instead of a bare colored line.
- No data-transfer changes: the check performed on open remains a read-only server query, and syncing stays manual.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `client-sync`: the synchronization-screen requirements are restated around a single **configuration screen** with a prominent status summary (state title plus pending detail), a single manual sync action (the check-for-updates action and its fixed position are removed — the state is refreshed on open), a last-sync caption, a comparable server/device client total display, the server configuration details on the same screen, and an import action when no server is configured.

## Impact

- **App**: new `ui/settings/ConfigScreen.kt` (status, data and server cards; import/remove actions), `ui/settings/SyncScreen.kt` and `ui/settings/ServerConfigScreen.kt` removed, `ui/settings/SyncStatusBar.kt` (prominent title, optional detail line, state-tinted icon, shorter pending message, check action removed), `ui/settings/SyncViewModel.kt` (drop `checkForUpdates`/`checking`/`hasKey`; keep the refresh on open), `ui/settings/ServerConfigViewModel.kt` unchanged, new `ui/components/StatTile.kt`, `ui/components/InfoCard.kt` (optional title), `Navigation.kt` + `NavigationKeys.kt` (single `Config` destination fed by both view models), `ui/cliente/ClientListScreen.kt` (one "Configuração" menu entry).
- **Specs**: `openspec/specs/client-sync/spec.md` — "Dedicated synchronization screen" replaced by "Configuration screen", "Check for updates action" and "Check action stays in a fixed position" removed, "Server and device client totals shown" reworded without the check action.
- **Tests**: instrumented `ConfigScreenTest` (replaces `SyncScreenTest` + `ServerConfigScreenTest`), `SyncStatusBarTest`, `ClientListScreenTest` menu assertions, `SyncViewModelTest` constructor.
- **Docs**: `elisdb-android/docs/uso.md`, `configuracao.md`, `index.md` — menu and screen names.
- **Not affected**: `SyncManager`, server API, sync semantics, data storage.
