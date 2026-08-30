## Why

When the user opens the Settings screen with no sync activity and no pending changes, the sync status bar shows **only the sync button** — no icon and no message (the `syncMessage` function returns `null` for the IDLE state). On a physical device this reads as "there is no synchronization information at all", which is confusing: the user sees an isolated button and cannot tell whether sync is configured, running, or simply idle. Only after tapping the button does it show "Sincronizado".

The screen is expected to "show the current sync status ... for each state"; the IDLE state currently shows nothing.

## What Changes

- The settings sync status bar shows an informative message in the IDLE state (e.g. "Sem sincronizações pendentes" / "Tudo sincronizado") instead of nothing, so the user always sees the synchronization state.
- The existing pending/error/success/in-progress states keep their current messages and the fixed sync-action position.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `client-settings`: the sync status row always shows a message, including an informative idle state.

## Impact

- **App**: `SyncStatusBar`/`syncMessage` — add an IDLE message (no pending uploads/downloads, no success/error).
- **Behavior**: opening Settings always shows a sync message; the IDLE state is no longer a bare button.
- **Tests**: update `SyncStatusBarTest.idle_mostraBotaoSemMensagem` to expect the new idle message; the sync action position tests remain valid.
