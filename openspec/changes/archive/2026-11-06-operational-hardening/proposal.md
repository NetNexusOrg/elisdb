# Proposal

## Why

Depois dos bloqueadores, restam problemas operacionais que tornam o sistema frágil no dia-a-dia: a importação de configuração guarda URLs inválidos e limpa o estado antes de validar; o cliente HTTP partilhado não é seguro sob concorrência; não há sync periódico nem retry com backoff; o pull do servidor abre uma ligação SQLite por registo e não persiste hashes de fotos; e o `compose.yaml` traz contas e URL HTTP de exemplo que um deploy ingénuo aceita. Estes pontos aumentam o suporte, o consumo de recursos e o risco operacional.

## What Changes

- A importação valida o URL com a mesma política do cliente (`ServerUrlPolicy`) antes de guardar; um URL inválido não altera a configuração nem limpa o estado de sync.
- O ciclo de vida do cliente HTTP partilhado passa a ser thread-safe: nunca fecha um cliente em uso e não cria clientes duplicados sob chamadas concorrentes.
- O sync passa a ter um worker periódico opcional (WorkManager) com backoff exponencial e um trigger imediato após criar/editar/apagar, mantendo o sync manual e o single-flight.
- O servidor lê uma página de pull com uma única ligação e carrega as fotos em lote, em vez de N+1 ligações por registo.
- O hash de conteúdo das fotos é persistido uma vez e reutilizado; deixa de ser recalculado em cada leitura por falta de commit.
- O provisionamento confina caminhos de configuração (em conjunto com a change de segurança) e o `compose.yaml` deixa de ter contas e URL de exemplo por omissão, falhando cedo quando a configuração está incompleta.
- O deploy por omissão fica limitado a loopback e documenta TLS/reverse proxy; o operador tem de optar explicitamente por expor a rede.

## Capabilities

### New Capabilities

- `server-deployment`: configuração segura por omissão para o deploy Docker (sem contas/URL de exemplo, bind local, validação de ambiente e orientação TLS).

### Modified Capabilities

- `client-key-import`: validação completa do URL antes de guardar e de reiniciar o estado.
- `client-settings`: importação inválida não altera a configuração existente.
- `client-sync`: trigger de sync após delete, worker periódico opcional e ciclo de vida thread-safe do cliente HTTP.
- `client-resilience`: retry com backoff e comportamento previsível quando a rede falha.
- `server-sync`: leitura de pull em lote com custo de ligações limitado.
- `server-photo-storage`: hash de conteúdo persistido e reutilizado.

## Impact

- Android: `ServerConfigViewModel`, `AppContainer`, `ApiClientProvider`, `SyncManager`, `ClientDetailViewModel`, novo worker, `ClienteRepositoryImpl` e testes.
- Servidor: `app/database.py`, `app/main.py`, `compose.yaml`, `docker-entrypoint.sh`, documentação e testes.
- Operação: deploy existente tem de definir explicitamente `ELISDB_USERS`/`ELISDB_SERVER_URL` e optar por bind público; sem migração de dados.
- API: sem breaking change; o pull mantém o contrato e melhora o custo.
