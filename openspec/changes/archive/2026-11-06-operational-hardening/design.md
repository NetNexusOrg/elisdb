# Design

## Context

Ver `proposal.md` — Why. A importação guarda o URL antes de o validar; o `ApiClient` é cacheado sem sincronização; não há worker periódico nem backoff; o pull do servidor abre uma ligação por registo; e o `compose.yaml` traz defaults inseguros. O P2 assume que o P0 já introduziu lotes e pull seguro, mas não depende dele para a validação de URL.

## Goals / Non-Goals

**Goals:**

- Validar a configuração antes de alterar estado e nunca deixar o dispositivo num estado parcial.
- Tornar o cliente HTTP seguro sob concorrência e mudança de configuração.
- Adicionar sync periódico opcional, trigger após delete e backoff.
- Reduzir o custo do pull no servidor e persistir hashes.
- Tornar o deploy Docker seguro por omissão.

**Non-Goals:**

- Não mudar o modelo de autenticação nem o formato dos registos.
- Não introduzir múltiplos workers do servidor.
- Não substituir o WorkManager por um serviço sempre ativo.
- Não implementar backup (P3).

## Decisions

### 1. Validação antes de persistir

`ServerConfigViewModel.importKey` valida o URL com `ServerUrlPolicy` e a seed com `Signer` antes de chamar `settingsStore.save`. Só depois de ambas as validações passarem é que pede ao `SyncManager` para resetar o estado e arrancar o sync. Um erro mantém a configuração e o estado atuais.

**Alternativas:** validar no `ApiClientProvider` (tarde demais, o estado já foi limpo); guardar e mostrar erro depois (má UX e risco de perda de watermark). A validação na fronteira é simples e testável.

### 2. Cliente HTTP com referência contada

O `AppContainer` mantém um `AtomicReference<ClientHolder>`, onde `ClientHolder` tem o `ApiClient` e um contador de pedidos ativos. Quem usa o cliente incrementa/decrementa o contador; ao mudar a configuração, a referência é trocada e o cliente antigo é fechado apenas quando o contador chega a zero. Pedidos novos usam sempre a referência atual.

**Alternativas:** `@Synchronized` e fechar o antigo de imediato (fecha um cliente em uso); nunca fechar (fuga de pools); um cliente por pedido (viola o requisito de cliente único). A referência contada cumpre o requisito existente e o novo.

### 3. Sync periódico opt-in com WorkManager

Um `PeriodicWorkRequest` (mínimo 15 min, configurável) corre o mesmo `SyncManager.sync()`; o worker é agendado apenas quando o utilizador ativa a opção nas settings. Save e delete enfileiram um `OneTimeWorkRequest` expedited, para além do trigger imediato em processo. Falhas transitórias usam `BackoffPolicy.EXPONENTIAL` com jitter; erros de auth/config não são repetidos. O single-flight existente continua a serializar.

**Alternativas:** timer em processo (morre com a app); `AlarmManager` (mais permissões e complexidade); sync contínuo (bateria). WorkManager é a API adequada e já está no projeto.

### 4. Pull do servidor em lote

`pull_all` lê a página de clientes numa única ligação e carrega as fotos com uma query `WHERE cliente_id IN (...)`, montando os registos em memória. O hash em falta é calculado e gravado na mesma transação/commit; os leitores seguintes reutilizam-no.

**Alternativas:** manter N+1 (simples mas cresce com a página); cache de hashes em memória (perde-se no restart). A query em lote é direta e mantém o contrato.

### 5. Deploy seguro por omissão

`compose.yaml` deixa `ELISDB_USERS` e `ELISDB_SERVER_URL` vazios/comentados; o entrypoint valida que, se há users, há URL válida e não placeholder; o bind por omissão é `127.0.0.1:8000`. A documentação exige TLS/reverse proxy para exposição pública. O container continua a correr como utilizador não-root via `setpriv`.

**Alternativas:** manter defaults e avisar (um deploy ingénuo cria contas); exigir sempre variáveis (quebra o arranque de desenvolvimento). Defaults vazios + validação é o equilíbrio.

## Risks / Trade-offs

- [WorkManager tem latência mínima de 15 min] → o trigger imediato em processo cobre save/delete; o worker é uma rede de segurança.
- [Contador de referências pode atrasar o fecho] → o cliente antigo é fechado quando o último pedido termina; monitorizar pedidos longos (downloads de APK/fotos).
- [Validação de URL mais restrita pode rejeitar configurações antigas] → mensagem clara e possibilidade de usar HTTPS; documentar a migração.
- [Query em lote pode ter parâmetros SQLite elevados] → dividir em sub-lotes de ids (por exemplo, 500) mantendo o custo linear.
- [Bind loopback quebra deployments existentes] → variável explícita `ELISDB_BIND_ADDRESS`; documentar no changelog.
- [Backoff pode atrasar a recuperação] → sync manual ignora o backoff; limite máximo curto.

## Migration Plan

1. Android: validação de import, cliente com referência contada, worker opcional e trigger de delete.
2. Servidor: pull em lote e hash commitado; sem migração de dados.
3. Deploy: atualizar `compose.yaml`/documentação; operadores existentes definem explicitamente bind/URL/users.
4. Rollback: o servidor e o app antigos continuam a funcionar; o bind local e os defaults vazios exigem ação do operador.
