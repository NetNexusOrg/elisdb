## Why

On the Settings screen, the "Remover chave" action clears only the signing key but leaves the server URL configured (`SettingsViewModel.clearKey()` calls `settingsStore.save(baseUrl, "")`). The user expects a single action that removes the whole server configuration, otherwise the screen keeps showing a configured URL while the device can no longer sync.

## What Changes

- Replace the "Remover chave" button with a "Remover configuração" action that clears the entire server configuration: base URL, signing key, and the resolved associated user state.
- Removing the configuration leaves the Settings screen showing "Não configurado" for the URL and "Não configurada" for the key, and hides the associated-user row and the removal action.
- A subsequent sync reports a configuration error (no server configured).

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `client-settings`: the "clear a stored signing key" action becomes a "remove server configuration" action that clears URL, key, and associated user together.

## Impact

- **App**:
  - `ui/settings/SettingsScreen.kt`: button text "Remover chave" → "Remover configuração" (and update the `onClick` handler).
  - `ui/settings/SettingsViewModel.kt`: replace `clearKey()` with a `clearConfig()` action that saves an empty URL and key and resets the UI state (`baseUrl`, `signingKey`, `hasKey`, `keyIdentity`).
  - `settings/SettingsStore.kt`: no interface change strictly required (a full clear is `save("", "")`), but a dedicated clear helper may be added for clarity.
- **Behavior**: after removal, sync fails with `SyncFault.CONFIG` ("Servidor não configurado…") — consistent with the blank-key behavior today.
- **Tests**: update `clearKey_persistsBlankAndKeyHidden` (androidTest `SettingsViewModelTest`) to assert the URL is also cleared; add a unit test for the new `clearConfig()`; cover the button label/behavior in a settings screen test if feasible.
- **Specs**: the `client-settings` scenario "Clear a stored signing key" is replaced by a "Remove server configuration" scenario.