## 1. Cliente partilhado no AppContainer

- [x] 1.1 `AppContainer` constrói um único `ApiClient` (e `HttpClient`) de longa duração e expõe-no via propriedade/provider
- [x] 1.2 `coordinatorProvider` (`AppContainer.kt:61,65-78`) passa a usar a instância partilhada em vez de `createApiClient()`
- [x] 1.3 `fetchLatestUpdatedAt` (`AppContainer.kt:82-88`) usa a instância partilhada
- [x] 1.4 `fetchKeyIdentity` e `fetchClientHistory` (`Navigation.kt:62-68,98-100`) usam a instância partilhada

## 2. Reatividade a mudanças de configuração

- [x] 2.1 Quando o `baseUrl`/key nas definições muda, o `AppContainer` reconstrói o `ApiClient` partilhado (e fecha o anterior) para refletir a nova configuração

## 3. Verificação

- [x] 3.1 Correr `./gradlew :app:assembleDebug` e testes unitários da app
- [x] 3.2 Confirmar (via inspeção/log) que syncs repetidos reutilizam o mesmo `HttpClient` e não acumulam clients
