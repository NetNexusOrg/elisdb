# Tasks

## 1. Validação de configuração

- [x] 1.1 Validar o URL com `ServerUrlPolicy` antes de guardar no `ServerConfigViewModel`; verificar com testes de HTTP público, HTTP privado, HTTPS e esquema inválido.
- [x] 1.2 Garantir que uma importação rejeitada não altera URL/key/user name nem limpa watermark/hashes; verificar com testes de estado e de configuração existente.
- [x] 1.3 Mostrar uma mensagem acionável e manter o ecrã utilizável após rejeição; verificar com teste de ViewModel/Compose.
- [x] 1.4 Atualizar `client-key-import`/settings docs com a política de URL.

## 2. Cliente HTTP concorrente

- [x] 2.1 Implementar um `ClientHolder` com contador de referências e troca atómica no `AppContainer`; verificar com teste de chamadas concorrentes e de troca de configuração.
- [x] 2.2 Garantir que um cliente antigo só é fechado quando o último pedido termina; verificar com teste de pedido lento + troca de settings.
- [x] 2.3 Garantir que pedidos novos nunca recebem um cliente fechado; verificar com teste de stress.
- [x] 2.4 Documentar o ciclo de vida do cliente.

## 3. Sync periódico, delete e backoff

- [x] 3.1 Adicionar o trigger de sync após confirmar a deleção; verificar com teste de `ClientDetailViewModel` e de `SyncManager`.
- [x] 3.2 Criar o worker periódico opt-in com WorkManager e single-flight; verificar com teste de agendamento e de execução.
- [x] 3.3 Implementar backoff exponencial com jitter e não repetir erros de auth/config; verificar com testes de retry e de manual override.
- [x] 3.4 Adicionar a opção de ativar/desativar o sync periódico nas settings e persistir a escolha; verificar com teste de UI/prefs.
- [x] 3.5 Documentar bateria, latência mínima e comportamento offline.

## 4. Performance do pull e hash

- [x] 4.1 Refatorar `pull_all` para ler a página numa única ligação e carregar fotos em lote; verificar com teste de 1000 registos e contagem de conexões/queries.
- [x] 4.2 Persistir o hash em falta no mesmo commit e reutilizá-lo; verificar com teste de duas leituras e contagem de cálculos.
- [x] 4.3 Garantir sub-lotes de ids para não exceder parâmetros SQLite; verificar com teste de página máxima.
- [x] 4.4 Documentar o custo do pull e os limites.

## 5. Deploy seguro

- [x] 5.1 Remover `ELISDB_USERS`/`ELISDB_SERVER_URL` por omissão do `compose.yaml` e tornar o bind predefinido em loopback; verificar com `docker compose config` e arranque local.
- [x] 5.2 Validar no entrypoint users/URL e rejeitar placeholders; verificar com testes de arranque e de mensagem.
- [x] 5.3 Documentar TLS/reverse proxy e o opt-in para bind público; verificar que os comandos da documentação funcionam.
- [x] 5.4 Adicionar teste/check de CI que falhe se o compose voltar a ter contas por omissão.

## 6. Verificação integrada

- [x] 6.1 Correr `pytest -q` no servidor e garantir pull/hash/deploy tests.
- [x] 6.2 Correr os testes Android de settings, sync e cliente HTTP.
- [x] 6.3 Validar a change com `openspec validate operational-hardening`.

## Workflow follow-up

- Arquivar a change depois da implementação e da revisão.
