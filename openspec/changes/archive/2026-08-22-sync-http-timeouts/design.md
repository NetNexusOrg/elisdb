## Context

`ApiClient.create` constrói o `HttpClient(Android)` só com `ContentNegotiation`, `expectSuccess = false` e um `defaultRequest` (User-Agent). Sem o plugin `HttpTimeout`, o Ktor não impõe limite: ligações, envio e resposta podem ficar pendentes indefinidamente. O `SyncManager.sync()` marca `IN_PROGRESS` no início e só publica `SUCCESS`/`ERROR` depois de `coordinator.sync()` regressar — logo, qualquer pedido preso deixa o estado em `IN_PROGRESS`.

## Goals / Non-Goals

**Goals:**
- Um pedido que não responde num prazo limitado falha com erro de rede, permitindo ao sync transitar para `ERROR` (`SyncFault.NETWORK`) em vez de pendurar.

**Non-Goals:**
- Não alterar a lógica de sincronização, o single-flight nem o mapeamento de erros.
- Não adicionar cancelamento manual do sync nem indicadores de progresso por foto.

## Decisions

- **Usar o plugin `HttpTimeout` do Ktor.** Configurar `connectTimeoutMillis`, `requestTimeoutMillis` e `socketTimeoutMillis` com valores razoáveis (ex. 30s de pedido, 15s de ligação/socket). Alternativa considerada: `withTimeout` à volta do `coordinator.sync()` inteiro — rejeitada porque o timeout por pedido dá feedback mais cedo e não aborta sincronizações legítimas e longas (muitos downloads de fotos) quando cada pedido individual responde a tempo.
- **Manter `expectSuccess = false`** e o tratamento de 401 existente; o timeout só afeta pedidos que não respondem.

## Risks / Trade-offs

- [Downloads de fotos grandes/legítimos poderem exceder o timeout] → Usar `requestTimeout` generoso para o corpo (o plugin aplica um limite total por pedido) e, se necessário, um valor maior para `downloadPhoto`. Registar nos testes que fotos normais terminam bem dentro do prazo.
- [Timeouts demasiado curtos em redes lentas] → Escolher valores intermédios (30s) e, se o utilizador relatar falsos erros, aumentar.
- [Falsos `TransientError`] → O `SyncManager` já mapeia `TransientError` para `ERROR`/`NETWORK` com mensagem acionável, coerente com o comportamento pretendido.

## Migration Plan

Sem migração. Rollback trivial (remover `install(HttpTimeout)`).

## Open Questions

Nenhuma.
