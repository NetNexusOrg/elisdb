## 1. Implementação

- [x] 1.1 Em `ClientListScreen.kt`: remover a chamada `SyncStatusBar(...)` do `topBar` e os parâmetros `syncStatus`, `syncFault` e `onSyncClick` dos dois overloads (incluindo a derivação no overload público).
- [x] 1.2 Em `ClientListViewModel.kt`: remover `syncStatus`/`syncFault` de `ClientListUiState.Success`, o `combine` `syncState`, e `manualSync()`; o `uiState` passa a combinar apenas `clientesComFotos` e a query.
- [x] 1.3 Confirmar com `grep` que `SyncStatusBar`, `manualSync` e os campos de sync não ficam usados indevidamente fora das Definições.

## 2. Testes

- [x] 2.1 `ClientListScreenTest.kt`: remover os testes da barra e simplificar `setListContent`/`FakeListRepository` (a interface mantém os membros, sem alimentar a UI).
- [x] 2.2 `ClientListViewModelTest.kt`: remover o teste do estado `syncStatus`/`syncFault` e o de `manualSync_acionaRepositorio`.
- [x] 2.3 Verificar `testDebugUnitTest` + `compileDebugAndroidTestKotlin` e correr o instrumentado `ClientListScreenTest` no emulador.

## 3. Verificação manual

- [x] 3.1 Compilar e instalar no emulador.
- [x] 3.2 Confirmar que a lista de clientes não mostra símbolo nem estado de sincronização.
- [x] 3.3 Confirmar que as Definições continuam a mostrar a barra de sincronização.