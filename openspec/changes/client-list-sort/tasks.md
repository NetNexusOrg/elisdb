# Tasks

## 1. Lista de clientes

- [x] 1.1 `ClientListViewModel`: `ClientSortOrder` (`NAME`, `RECENT`), estado `ordem`, `onOrdemChange` e ordenação depois do filtro/pesquisa.
- [x] 1.2 `ClientListScreen`: opções de ordenação no menu do ícone de filtro, com separador e marca de seleção.
- [x] 1.3 Testes unitários da ordenação (alfabética por predefinição, recentes primeiro, desempate).
- [x] 1.4 Teste instrumentado do menu de ordenação.
- [x] 1.5 Ordenação por **última edição** (`updatedAt` descendente, desempate por criação e nome) e respetiva opção no menu.

## 2. Verificação e documentação

- [x] 2.1 `:app:testDebugUnitTest`, `:app:lintDebug` e `:app:connectedDebugAndroidTest` verdes.
- [x] 2.2 `elisdb-android/docs/uso.md` e `CHANGELOG.md`.
- [x] 2.3 `openspec validate client-list-sort --strict` e marcar as caixas.
