## Why

When the server configuration is not loaded, the settings sync status row shows the long message "Servidor não configurado. Abre as Definições para configurar.". The user finds this verbose — inside the Settings screen itself, telling the user to "open Settings to configure" is redundant. A short "Não configurado" is enough.

## What Changes

- The sync status row's configuration-fault message changes from "Servidor não configurado. Abre as Definições para configurar." to "Não configurado".

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `client-settings`: the missing-server-configuration sync message reads "Não configurado".

## Impact

- **App**: `SyncStatusBar`/`syncMessage` — the `SyncFault.CONFIG` message changes to "Não configurado".
- **Behavior**: with no server config, the settings sync status shows "Não configurado".
- **Tests**: update `SyncStatusBarTest.errorConfig_mostraMensagemConfiguracao` to expect "Não configurado".
