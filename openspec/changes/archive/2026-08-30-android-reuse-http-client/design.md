## Context

`ApiClient.create()` (`remote/ApiClient.kt:57-83`) constrói um `HttpClient(Android)` novo. Hoje um novo `ApiClient` é criado em cada sync (`AppContainer.createApiClient()`, `AppContainer.kt:61,65-78`), em `fetchLatestUpdatedAt` (`AppContainer.kt:82-88`), `fetchKeyIdentity` e `fetchClientHistory` (`Navigation.kt:62-68,98-100`). Nada chama `close()`, logo acumula-se connection pool + dispatcher por operação.

## Goals / Non-Goals

**Goals:**
- Uma única instância de `HttpClient`/`ApiClient` partilhada pela app.
- Os pontos de criação atuais passam a usar a instância partilhada.

**Non-Goals:**
- Não alterar timeouts/headers/config do cliente (mantém-se o comportamento de rede).
- Não alterar a assinatura ed25519 dos pedidos.

## Decisions

**D1: `ApiClient` de longa duração no `AppContainer`**
`AppContainer` constrói um `ApiClient` (e o `HttpClient` subjacente) uma única vez e expõe via propriedade/`provider`. `coordinatorProvider`, `fetchLatestUpdatedAt`, `fetchKeyIdentity`, `fetchClientHistory` usam essa mesma instância.
- *Alternativa*: `object` singleton global — rejeitado, foge à injeção de dependência já usada em `AppContainer` e dificulta testes.

**D2: Ciclo de vida**
O `HttpClient` vive para toda a app; fechar no encerramento do processo (ou simplesmente deixar o processo terminar e libertar). Não fechar entre syncs.

## Risks / Trade-offs
- [Partilhar um client pode misturar configs de URL diferentes] → o `ApiClient` já recebe `baseUrl` no `create`; a instância partilhada deve ser (re)construída quando o `baseUrl`/key mudam (nas definições), pelo que o `AppContainer` deve expor um `ApiClient` reativo ao `SettingsStore`.

## Migration Plan
- Sem migração de dados. Apenas lifecycle do cliente.

## Open Questions
- Nenhuma que altere specs, abordagem ou tarefas.
