# Tasks: Client edit log

## 1. Servidor — tabela e registo de auditoria

- [x] 1.1 Em `app/database.py`, adicionar tabela `client_audit` ao SCHEMA (`id`, `cliente_id`, `key_id`, `username`, `action`, `ts`) com índice em `cliente_id`; adicionar migração idempotente para bases existentes
- [x] 1.2 Em `save_client`, aceitar parâmetro opcional `actor: dict | None` e registar uma entrada de auditoria (`create` no insert, `edit` no update) apenas em persistências aceites (não em `stale`/`duplicate` recusados)
- [x] 1.3 Adicionar `client_history(cliente_id)` que devolve as entradas ordenadas por `ts`/`id`

## 2. Servidor — endpoint

- [x] 2.1 Em `app/main.py`, no `push`, passar `request.app.state.authenticated_key` como `actor` ao `save_client`
- [x] 2.2 Adicionar `GET /api/clients/{id}/history` (autenticado) devolvendo a lista de entradas; lista vazia para cliente desconhecido
- [x] 2.3 Em `app/models.py`, adicionar `AuditEntryOut` (`action`, `username`, `key_id`, `ts`)

## 3. Servidor — testes

- [x] 3.1 Em `test_main.py`, cenários: create regista ator; update regista ator; chave sem nome regista `username` nulo; `stale`/`duplicate` recusado não regista; endpoint devolve histórico ordenado; cliente desconhecido devolve lista vazia

## 4. App — DTOs e fetch

- [x] 4.1 Em `remote/RemoteDto.kt`, adicionar `AuditEntryDto` (`action`, `username`, `keyId`, `ts`)
- [x] 4.2 Em `sync/SyncApi.kt`/`remote/ApiClient.kt`, adicionar `fetchClientHistory(id): List<AuditEntryDto>`

## 5. App — UI do Log

- [x] 5.1 Em `ui/detalhe/ClientDetailViewModel.kt`, adicionar estado `log`/`logLoading`/`logError` e `loadLog()` que chama `fetchClientHistory`
- [x] 5.2 Em `ClientDetailScreen.kt`, adicionar item "Log" ao menu (top bar) que abre um diálogo/folha a listar create + edits (ator + data formatada); falha mostra mensagem não bloqueante; histórico vazio indica que não há histórico

## 6. App — testes

- [x] 6.1 Adicionar testes do `ClientDetailViewModel` para loadLog (sucesso, falha não bloqueante)
- [x] 6.2 Adicionar teste de ecrã (instrumentação) para o botão Log abrir a vista com as entradas
- [x] 6.3 Atualizar `ApiClientTest` para `fetchClientHistory`

## 7. Verificação

- [x] 7.1 Correr `testDebugUnitTest` e a instrumentação; confirmar que passam
- [x] 7.2 Verificação end-to-end no emulador: criar e editar um cliente, abrir o Log e confirmar que mostra o utilizador e a hora do create e dos edits
