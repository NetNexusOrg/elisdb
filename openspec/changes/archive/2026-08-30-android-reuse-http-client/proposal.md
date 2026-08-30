## Why

Cada sincronização constrói um `HttpClient` Ktor novo e nunca o fecha. `ApiClient.create()` (`remote/ApiClient.kt:57-83`) cria um `HttpClient(Android)` fresco, e um novo `ApiClient` é construído em cada sync: `SyncManager.startRun` → `coordinatorProvider.coordinator()` → `AppContainer.createApiClient()` (`AppContainer.kt:61,65-78`), além de `fetchLatestUpdatedAt` (`AppContainer.kt:82-88`), `fetchKeyIdentity` e `fetchClientHistory` (`Navigation.kt:62-68,98-100`).

Como nada chama `client.close()`, repetidas sincronizações e fetches de histórico/identidade fogram connection pools e dispatchers de coroutines. Sob sync frequente isto pode esgotar threads/descritores e degradar a app ou o dispositivo.

## What Changes

- Manter um único `ApiClient`/`HttpClient` de longa duração no `AppContainer` (ciclo de vida do processo da app), em vez de criar um por operação.
- Os pontos que hoje chamam `createApiClient()` (sync, `fetchLatestUpdatedAt`, `fetchKeyIdentity`, `fetchClientHistory`) passam a usar a instância partilhada.
- O `HttpClient` é fechado apenas no encerramento do processo (ou simplesmente mantido para a lifetime do processo, o que é aceitável para uma app single-process).

## Capabilities

### New Capabilities

### Modified Capabilities
- `client-sync`: nova requirement "HTTP client lifecycle" — o app SHALL reutilizar um único `HttpClient` de longa duração para todos os pedidos ao servidor e SHALL NOT criar um novo cliente por sincronização.

## Impact
- Android: `remote/ApiClient.kt`, `AppContainer.kt`, `Navigation.kt`, `SyncManager.kt` (usar instância partilhada).
- Sem alteração de comportamento de rede (mesmos timeouts/headers); apenas lifecycle do cliente.
