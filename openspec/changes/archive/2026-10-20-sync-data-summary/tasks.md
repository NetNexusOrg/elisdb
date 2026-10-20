# Tasks: Sync data summary

## 1. Servidor — contagem de clientes

- [x] 1.1 Em `app/database.py`, adicionar `client_count()` que devolve `COUNT(*)` dos clientes (tombstones não contam)
- [x] 1.2 Em `app/main.py`, incluir `client_count` na resposta de `GET /api/latest` (aditivo, compatível)
- [x] 1.3 Em `tests/test_api.py`, cobrir: contagem devolvida com `latest_updated_at`; aumenta com push; diminui após delete

## 2. App — buscar e expor a contagem do servidor

- [x] 2.1 Em `sync/SyncApi.kt`, adicionar `ServerInfo(latestUpdatedAt, clientCount)` e `fetchServerInfo()`
- [x] 2.2 Em `remote/ApiClient.kt`, implementar `fetchServerInfo()` lendo `client_count` de `/api/latest`
- [x] 2.3 Em `sync/SyncManager.kt`, substituir `fetchLatestUpdatedAt` por `fetchServerInfo` e expor `serverClienteCount: StateFlow<Int?>`
- [x] 2.4 Em `AppContainer.kt`, ligar `fetchServerInfo` ao `ApiClient`
- [x] 2.5 Em `ui/settings/SyncViewModel.kt`, expor `serverClienteCount` em `SyncUiState`

## 3. UI — cartão "Dados"

- [x] 3.1 Em `ui/settings/SyncScreen.kt`, mostrar "Clientes no servidor" (ou `—`) e renomear a linha local para "Clientes na app"

## 4. Testes

- [x] 4.1 Atualizar fakes e testes unitários do `SyncManager` para `ServerInfo`
- [x] 4.2 Atualizar `SyncViewModelTest` (instrumentação) e adicionar cenário do total do servidor
- [x] 4.3 Atualizar `SyncScreenTest` para as novas linhas (servidor e app)
- [x] 4.4 Correr `pytest`, `testDebugUnitTest`, `lintDebug` e os testes instrumentados

## 5. Verificação

- [x] 5.1 Verificar no emulador contra o servidor: 480 no servidor e 480 na app; sem configuração mostra `—`
- [x] 5.2 `openspec validate sync-data-summary`
