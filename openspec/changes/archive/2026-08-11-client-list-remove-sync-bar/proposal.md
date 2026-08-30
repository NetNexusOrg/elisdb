## Why

A lista de clientes é a ecrã principal do utilizador. Depois de remover o texto de
"Servidor não configurado" já feito, a barra de sincronização (símbolo de
sincronizar + estado) continua a ocupar espaço e a duplicar informação que pertence
às Definições. A observabilidade da sincronização deve viver apenas nas Definições;
a lista fica dedicada aos clientes.

## What Changes

- Remover a `SyncStatusBar` (símbolo e mensagens de sincronização) da ecrã de lista
  de clientes.
- Remover o estado de sincronização (`syncStatus`/`syncFault`) de `ClientListUiState`
  e a função `manualSync` do `ClientListViewModel` (dead code após a remoção da UI).
- Atualizar os testes que dependiam da barra na lista (`ClientListScreenTest`) e do
  estado de sync no ViewModel (`ClientListViewModelTest`).
- A sincronização e o seu estado continuam disponíveis nas Definições; o motor de
  sincronização (`SyncManager`) não muda.

## Capabilities

### New Capabilities
- nenhuma.

### Modified Capabilities
- `sync-observability`: o requisito "Sync observable from the client list" é
  **removido** — o estado de sincronização e o gatilho manual deixam de ser expostos
  na lista de clientes, passando a viver apenas nas Definições.

## Impact

- App Android: `ClientListScreen.kt`, `ClientListViewModel.kt`, e testes
  `ClientListViewModelTest.kt` / `ClientListScreenTest.kt`.
- `SyncStatusBar.kt` mantém-se (usada nas Definições).
- Sem alterações ao motor de sincronização, API remota ou servidor.