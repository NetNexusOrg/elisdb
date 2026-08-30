## 1. Mutação atómica de estado

- [x] 1.1 Em `importKey`, substituir o snapshot `val state = _uiState.value` + `state.copy(...)` por `_uiState.update { ... }`, lendo `current` dentro do lambda
- [x] 1.2 Converter `clearConfig` para `_uiState.update { ... }`
- [x] 1.3 Converter `lookupKeyIdentity` para `_uiState.update { ... }`
- [x] 1.4 Converter os collectors de `syncManager` (status, fault, lastResult, hasPendingUploads, hasPendingDownloads) para `_uiState.update { ... }`

## 2. Verificação

- [x] 2.1 Correr `./gradlew assembleDebug lintDebug testDebugUnitTest` e confirmar build verde
- [x] 2.2 Correr `./gradlew connectedDebugAndroidTest` e confirmar que `SettingsViewModelTest.import_invalidSeedInConfigRefused` passa (repetir para confirmar que não é intermitente)
