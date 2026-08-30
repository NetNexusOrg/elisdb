## Why

When the Settings screen shows that there are no pending synchronizations, the sync status row currently reads "Sem sincronizações pendentes". The user finds this wording redundant — when there is nothing pending, the natural message is simply "Sincronizado". This makes the idle state consistent with the success state and shorter.

## What Changes

- When there are no pending synchronizations (no pending uploads, no pending downloads, and no in-progress/failure), the settings sync status row shows "Sincronizado" instead of "Sem sincronizações pendentes".

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `client-settings`: the idle sync state message reads "Sincronizado" instead of "Sem sincronizações pendentes".

## Impact

- **App**: `SyncStatusBar`/`syncMessage` — the idle fallback message changes from "Sem sincronizações pendentes" to "Sincronizado".
- **Behavior**: opening Settings with nothing to sync shows "Sincronizado".
- **Tests**: update `SyncStatusBarTest` idle assertion to expect "Sincronizado".
