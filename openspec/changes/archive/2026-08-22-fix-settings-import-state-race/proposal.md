## Why

O teste instrumentado `SettingsViewModelTest.import_invalidSeedInConfigRefused` falha de forma intermitente: `importKey` captura um snapshot (`state`) no início e escreve-o de volta no caminho de erro, enquanto os collectors do `viewModelScope` fazem `_uiState.value = _uiState.value.copy(...)` (read-modify-write não-atómico). Numa corrida, um collector que leu o valor intermédio (com `signingKeyError` ainda a null) escreve por cima do erro acabado de definir, deixando `signingKeyError` a null.

## What Changes

- Tornar atómicas as mutações de `_uiState` usando `_uiState.update { ... }` em `importKey`, `clearConfig`, `lookupKeyIdentity` e nos collectors, eliminando a perda de atualizações em corrida.

## Capabilities

### New Capabilities

- Nenhuma.

### Modified Capabilities

- Nenhuma. Correção interna de concorrência (a especificação `client-settings` já cobre o comportamento de importação). Declarada com `skip_specs: true`.

## Impact

- `app/src/main/java/com/example/elisdb/ui/settings/SettingsViewModel.kt`: substituir `_uiState.value = ...` por `_uiState.update { ... }` em todos os pontos.
- Testes: `SettingsViewModelTest` (instrumentado) deixa de ser intermitente; nenhum novo teste necessário.
