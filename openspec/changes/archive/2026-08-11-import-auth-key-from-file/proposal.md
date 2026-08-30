## Why

Configuring the authentication key by pasting a base64 seed into a text field is error-prone (accidental characters, copy/paste mistakes) and exposes the full secret on screen. The server issues keys via a CLI command whose output can be handed off directly, so the app should accept that artifact as a file instead.

## What Changes

- **BREAKING** Remove the manual signing-key text field from the settings screen ("Chave de assinatura").
- Add an "Importar chave" button that opens the Android file picker and reads the selected file.
- The imported file may be a bare base64 ed25519 seed **or** the full `python -m app.keys issue <nome>` terminal output; the seed is extracted in both cases.
- Show the current key state as a masked, non-editable value with a button to clear it (preserving the existing "blank key" behavior).
- Invalid file content is refused with a message and the previously stored key is left unchanged.

## Capabilities

### New Capabilities
- none

### Modified Capabilities
- `client-settings`: "Server connection settings screen" — the signing key seed is no longer viewable/editable as free text; it is imported from a file, shown masked, and can be cleared.
- `client-key-import`: "Import a server-issued key" — import source changes from pasted base64 to a chosen file, and the accepted formats are widened to tolerate the full `issue` command output.

## Impact

- `app/src/main/java/com/example/elisdb/ui/settings/SettingsScreen.kt` — remove signing-key text field; add masked key display, import button, and clear action; wire file-picker result.
- `app/src/main/java/com/example/elisdb/ui/settings/SettingsViewModel.kt` — drop `onSigningKeyChange` free-text path; add file-import parsing/validation and clear action on the state.
- `app/src/main/java/com/example/elisdb/ui/settings/SettingsUiState.kt` (if split) — key display/import-state fields.
- New file-picker integration (Activity Result API / `OpenDocument`) in the settings package or via the ViewModel.
- Existing seed persistence (`SettingsStore`) and `Signer.fromBase64` validation are reused unchanged.
- Android tests for `SettingsViewModel` updated to the import/clear interaction.