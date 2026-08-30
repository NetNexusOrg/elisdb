## Context

`SettingsViewModel` mantém `_uiState` como `MutableStateFlow<SettingsUiState>`. O `init` lança vários collectors no `viewModelScope` (que usa `Dispatchers.Main.immediate`), e `importKey`/`clearConfig`/`lookupKeyIdentity` mutam o estado. Nos testes instrumentados o método corre no thread de instrumentação enquanto os collectors correm no main thread, pelo que as escritas `_uiState.value = _uiState.value.copy(...)` (read-modify-write) e o snapshot `val state = _uiState.value` em `importKey` permitem corridas de perda de atualização.

## Goals / Non-Goals

**Goals:**
- Eliminar as corridas de perda de atualização, tornando cada mutação de `_uiState` atómica.

**Non-Goals:**
- Não alterar a estrutura de `SettingsUiState` nem o comportamento observável de importação/configuração.
- Não alterar a semântica de `SyncManager`.

## Decisions

- **Usar `_uiState.update { ... }` em vez de `_uiState.value = ...`.** `MutableStateFlow.update` faz um loop CAS, tornando cada mutação atómica relativamente a outras chamadas `update`. Alternativa considerada: um `Mutex` à volta das mutações — rejeitada por ser mais invasiva e desnecessária quando `update` resolve o problema.
- **Remover o snapshot em `importKey`.** Em vez de `val state = _uiState.value` seguido de `state.copy(...)`, ler `current` dentro do lambda de `update`, de modo que as duas escritas de `importKey` (reset do erro e aplicação do resultado) partam sempre do estado mais recente.

## Risks / Trade-offs

- [Escritas fora de `update` que permaneçam] → Converter todos os pontos de escrita de `_uiState` (collectors, `importKey`, `clearConfig`, `lookupKeyIdentity`) para `update`, para não deixar caminhos não-atómicos.
- [Ordem de eventos entre collectors e import] → `update` garante atomicidade por escrita, mas não impõe ordem global; isso é aceitável porque cada campo mantém a sua própria consistência.

## Migration Plan

Sem migração. Rollback trivial (reverter para `_uiState.value = ...`).

## Open Questions

Nenhuma.
