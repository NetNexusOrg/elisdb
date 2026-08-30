## 1. ViewModel: import parsing and state

- [x] 1.1 Add fields to `SettingsUiState` for key-present indicator and import/clear state (e.g. `hasKey`, masked display derived from `signingKey`).
- [x] 1.2 Remove `onSigningKeyChange` free-text handler; stop binding the signing-key text field to editable state.
- [x] 1.3 Add `importKey(Uri)` (or text-based `importKey(content: String)`) that extracts the seed from file text — bare base64 or `python -m app.keys issue` output — and validates via `Signer.fromBase64`.
- [x] 1.4 On invalid file content: set the invalid-key message, leave stored key unchanged.
- [x] 1.5 Add `clearKey()` that persists a blank signing key via `settingsStore.save` and updates displayed state.
- [x] 1.6 On valid import: persist the extracted seed, keep masked display consistent, and confirm the import.

## 2. Screen: file picker and masked display

- [x] 2.1 In `SettingsScreen.kt`, replace the signing-key `OutlinedTextField` with a masked key indicator (fixed mask, never the raw value) and a clear action when a key is set.
- [x] 2.2 Add an "Importar chave" button wired to `rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument())`.
- [x] 2.3 Read the picked `Uri` content via `contentResolver` and pass the text to `viewModel.importKey(...)`.
- [x] 2.4 Show the existing invalid-key error through the supporting-text/message slot.

## 3. Tests

- [x] 3.1 Android test: importing a bare seed persists it and updates state.
- [x] 3.2 Android test: importing full `python -m app.keys issue` output extracts the seed.
- [x] 3.3 Android test: invalid file content is refused and stored key unchanged.
- [x] 3.4 Android test: clear key persists blank and sync stays blocked locally.

## 4. Verification

- [x] 4.1 Update any `SettingsViewModelTest` construction sites for the new signature.
- [x] 4.2 Run `./gradlew testDebugUnitTest` and `compileDebugAndroidTestKotlin`; confirm green.