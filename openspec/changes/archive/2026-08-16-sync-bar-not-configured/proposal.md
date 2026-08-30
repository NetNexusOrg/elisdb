## Why

When no server configuration is loaded, the Settings sync status bar shows "Sincronizado" (the idle fallback), which is misleading: the device is not synchronized, it simply has no server configured. The bar only switches to "Não configurado" after the user tries to sync and it fails with a configuration fault. The message should reflect the real state immediately.

## What Changes

- The settings sync status bar shows "Não configurado" as soon as no server configuration is loaded, without requiring a sync attempt.
- When a configuration is present, the existing messages (pending/success/error/idle "Sincronizado") are unchanged.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `client-settings`: the sync status row shows "Não configurado" whenever no server configuration is loaded, independent of the last sync attempt.

## Impact

- **App**: `SettingsViewModel` already exposes `hasKey` (or `baseUrl`); pass it to `SyncStatusBar`/`syncMessage` so the no-config state renders "Não configurado" before any sync.
- **Behavior**: with no config, the bar immediately reads "Não configurado"; with config, behavior is unchanged.
- **Tests**: update `SyncStatusBarTest` (idle-with-no-config shows "Não configurado"; idle-with-config shows "Sincronizado").
