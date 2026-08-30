## Context

A barra de sincronização é partilhada entre Definições e lista de clientes. A lista
é o único consumidor de `syncStatus`/`syncFault` promovidos pelo `ClientListViewModel`
(através de `ClienteRepository`); as Definições consomem o `SyncManager` diretamente.
Motivação em `proposal.md` — Why.

## Goals / Non-Goals

**Goals:**
- A lista de clientes não apresenta símbolo nem estado de sincronização.
- Limpar o estado e o `manualSync` mortos do `ClientListViewModel`.
- Sincronização e respetivo estado permanecem nas Definições.

**Non-Goals:**
- Não alterar `SyncManager`/WorkManager, o motor de sincronização nem a API remota.
- Não alterar `SyncStatusBar.kt` (usada nas Definições).
- Não modificar `ClienteRepository`/`ClienteRepositoryImpl` (a interface oferece
  `syncStatus`/`syncFault`/`syncNow` que continuam a servir Definições e o motor).

## Decisions

**D1 — Remover a `SyncStatusBar` e os parâmetros de sync da `ClientListScreen`.**
Os dois overloads (`ClientListScreen(viewModel, ...)` e o interno) perdem os
parâmetros `syncStatus`, `syncFault` e `onSyncClick`; o `SyncStatusBar(...)` do
`topBar` é eliminado, ficando apenas o `TopAppBar`.
Alternativa considerada: manter a barra apenas oculta — rejeitada, deixa dead code.

**D2 — Simplificar o `ClientListViewModel` e `ClientListUiState`.**
Remover `syncStatus`/`syncFault` de `Success`, o `combine(syncState)`, e
`manualSync()`. O `uiState` combina apenas `repository.clientesComFotos` com a query.
Alternativa considerada: manter o estado "para o futuro" — rejeitada, contradiz o
non-goal de código limpo.

**D3 — Atualizar os testes.**
- `ClientListScreenTest`: remover os testes da barra (`topbarErroClock`, `topbarErroAuth`,
  `topbarErroConfig`, `topbarSincronizado`, `botaoSincronizarDisparaSync`) e os
  parâmetros `syncStatusFlow`/`syncFaultFlow`/`onSync` do helper. O `FakeListRepository`
  continua a implementar a interface (campos exigidos por `ClienteRepository`).
- `ClientListViewModelTest`: remover o teste do estado `syncStatus`/`syncFault` e o de
  `manualSync_acionaRepositorio`.

**D4 — Confirmar antes de apagar** que `manualSync`, `SyncStatus`/`SyncFault` no
ViewModel da lista e a barra não são utilizados noutro sítio (grep durante a implementação).

## Risks / Trade-offs

- [Alguém confiava no estado de sync na lista] → Mitigação: Definições continuam a
  expor estado e gatilho; comportamento do motor inalterado.
- [Testes a dependerem da interface não usados] → Os fakes mantêm os membros exigidos
  pela interface; apenas deixam de alimentar a UI da lista.