## Why

Numa instalação limpa, tocar em "sincronizar" deixa a app presa em "a sincronizar" durante minutos. A causa é que o `HttpClient` (Ktor) é criado **sem qualquer timeout**: uma ligação parada/lenta — por exemplo o download de uma foto durante o pull inicial completo — nunca falha, por isso `SyncCoordinator.sync()` nunca regressa e o estado fica em `IN_PROGRESS` indefinidamente. Como os dados vão sendo gravados à medida que o pull processa cada cliente, o utilizador vê dados na app mas o estado continua "a sincronizar".

## What Changes

- Configurar timeouts (ligação/pedido/socket) no `HttpClient` do `ApiClient`, para que pedidos que não respondem dentro de um prazo falhem com erro de rede.
- Com isso, uma falha de rede/paragem deixa de "pendurar" o sync: o `SyncCoordinator` passa a mapear a exceção para `SyncOutcome.TransientError` e o estado transita para `ERROR` (em vez de ficar preso em `IN_PROGRESS`).

## Capabilities

### New Capabilities

- Nenhuma.

### Modified Capabilities

- Nenhuma. A especificação `client-sync` já exige que o estado transite para sucesso/erro; a mudança apenas faz o erro acontecer em vez de pendurar. Declarada com `skip_specs: true`.

## Impact

- `app/src/main/java/com/example/elisdb/remote/ApiClient.kt`: adicionar `install(HttpTimeout)` (e possivelmente `expectSuccess`/config já existente) com limites razoáveis.
- `gradle/libs.versions.toml`: sem alteração (o plugin `HttpTimeout` vem no `ktor-client-core` já usado).
- Testes: adicionar teste unitário que confirme que um pedido que não responde falha (ex. com `MockEngine`), se praticável.
