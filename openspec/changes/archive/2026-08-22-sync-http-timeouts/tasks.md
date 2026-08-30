## 1. Adicionar timeouts HTTP

- [x] 1.1 Em `ApiClient.create`, adicionar `install(HttpTimeout)` com `connectTimeoutMillis`/`requestTimeoutMillis`/`socketTimeoutMillis` (ex. 15s/30s/15s)
- [x] 1.2 Confirmar que um pedido que não responde lança exceção mapeada para `SyncOutcome.TransientError` (já coberto por `SyncCoordinator.sync()`)

## 2. Verificação

- [x] 2.1 Correr `./gradlew assembleDebug lintDebug testDebugUnitTest` e confirmar build verde
- [x] 2.2 Adicionar teste unitário (se praticável com `MockEngine`) que confirme que o timeout produz falha e não um pedido pendente
- [x] 2.3 Smoke-test no emulador contra um servidor acessível: sync termina em sucesso/erro e nunca fica preso em "a sincronizar"
