## Why

The Settings screen shows textual elements that the user wants removed:

1. Below the "Importar configuração do servidor" button there is a paragraph: "Importe o ficheiro de configuração emitido pelo servidor (python -m app.keys issue <nome> --server-url <url>). O URL do servidor e a chave ficam configurados em simultâneo." The user finds it unnecessary clutter.
2. After importing a config file, the screen shows a confirmation message "Configuração importada e guardada." The user wants it gone.
3. After removing the server configuration, the screen shows a confirmation message "Configuração removida." The user wants it gone too.

## What Changes

- Remove the import-instructions paragraph from the Settings screen.
- Remove the "Configuração importada e guardada." success message after a config import.
- Remove the "Configuração removida." confirmation message after clearing the server configuration.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `client-settings`: the Settings screen no longer shows the server-config import instructions paragraph, nor the "Configuração importada e guardada" and "Configuração removida" confirmations.

## Impact

- **App**: `SettingsScreen.kt` (remove the instructions `Text`), `SettingsViewModel.kt` (stop setting `message = "Configuração importada e guardada."` on import and `message = "Configuração removida."` on clear).
- **Behavior**: importing or removing a config still applies/clears and triggers sync, but shows no confirmation text; the instructions paragraph is gone.
- **Tests**: update the test that asserts on "Configuração removida." and ensure no test asserts on the removed texts.
