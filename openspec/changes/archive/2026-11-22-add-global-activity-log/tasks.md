# Tasks

## 1. Servidor: consulta e endpoint globais

- [x] 1.1 Adicionar `activity_page(limit, before_id)` em `elisdb-server/elisdb/database.py`: entradas por `id DESC` com junção a `clientes`, nome atual e, para clientes eliminados, o nome do snapshot `delete` mais recente numa consulta adicional limitada à página; devolver `(entries, next_before_id)`; verificar com testes de base que a ordem, o cursor e o nome de um cliente eliminado estão corretos (`python -m pytest tests/test_database.py -q`).
- [x] 1.2 Adicionar `ActivityEntryOut` (com `cliente_id` e `cliente_nome`) e `ActivityPageOut` em `elisdb-server/elisdb/models.py`; verificar que o endpoint por cliente continua a usar `AuditEntryOut` sem campos novos (`python -m pytest tests/test_api.py -q`).
- [x] 1.3 Expor `GET /api/history` autenticado em `elisdb-server/elisdb/main.py`, com `limit` (1..200, omissão 50) e `before_id`, devolvendo a página e `next_before_id`; verificar com testes de ordem descendente, paginação sem repetições, última página sem cursor, limite inválido e pedido sem autenticação (`python -m pytest tests/test_api.py -q`).
- [x] 1.4 Cobrir a compatibilidade do histórico por cliente e entradas sem detalhes no histórico global; verificar que as respostas antigas não mudaram (`python -m pytest tests/test_sync_integrity.py tests/test_api.py -q`).

## 2. Android: rede e consultas locais

- [x] 2.1 Acrescentar `clienteId`/`clienteNome` opcionais a `AuditEntryDto` e criar `ActivityPageDto` em `remote/RemoteDto.kt`; verificar com teste de serialização que uma resposta sem os campos novos continua a descodificar e que uma resposta global os preenche.
- [x] 2.2 Adicionar `fetchActivityPage(limit, beforeId, timestamp)` a `SyncApi` e implementá-lo em `ApiClient` com pedido assinado a `/api/history`; verificar com teste de `ApiClient`/`MockEngine` que o caminho, os parâmetros e a autenticação estão corretos.
- [x] 2.3 Adicionar `FotoDao.getUrisByIds(ids)` e `ClienteDao.getExistingIds(ids)`; verificar com teste de base em memória que só devolvem registos existentes e que uma lista vazia não falha.

## 3. Android: componentes partilhados e ecrã global

- [x] 3.1 Extrair de `ui/detalhe/ClientLogScreen.kt` para `ui/detalhe/LogComponents.kt` o cartão de entrada, o detalhe de campos/fotos e os estados de vazio/erro, com cabeçalho de cliente opcional; verificar que os testes existentes de `ClientLogScreen` continuam verdes e que o ecrã por cliente não muda visualmente.
- [x] 3.2 Criar `ui/detalhe/ActivityLogViewModel.kt` com carregamento inicial, `carregarMais`, cursor `beforeId`, estados de carregamento/erro/vazio, mapa de miniaturas por página e conjunto de clientes navegáveis; verificar com testes unitários de primeira página, página seguinte sem repetições, fim da lista, erro com retry e lista vazia.
- [x] 3.3 Criar `ui/detalhe/ActivityLogScreen.kt` com agrupamento por dia descendente, cartão com cliente/ação/autor/hora/detalhes, miniaturas e ação `Carregar mais`; verificar com teste Compose que a ordem, o agrupamento, o cliente eliminado não clicável e o botão de carregar mais se comportam como especificado.
- [x] 3.4 Adicionar `ActivityLog` a `NavigationKeys.kt`, registar o destino em `Navigation.kt` e o item **Logs** (`Icons.Default.History`) no menu da lista antes de `Configuração`; verificar que o item abre o ecrã e que a navegação para o detalhe de um cliente existente funciona no emulador.

## 4. Verificação integrada

- [x] 4.1 Correr os testes unitários e o lint Android (`./gradlew :app:testDebugUnitTest :app:lintDebug`) e confirmar que não há regressões.
- [x] 4.2 Correr a suite completa do servidor (`.venv/bin/python -m pytest -q`) e confirmar que continua verde.
- [x] 4.3 Validar no emulador: abrir **Logs** pelo menu, confirmar ordem descendente e agrupamento, carregar mais, abrir um cliente existente e confirmar que uma entrada de cliente eliminado não navega.
- [x] 4.4 Validar a change com `openspec validate add-global-activity-log --strict` e confirmar que todos os deltas fecham.

## Workflow follow-up

- Acrescentar a entrada em **Novidades** do `elisdb-android/CHANGELOG.md` na próxima versão, ao preparar a release.
- Arquivar a change depois da revisão e dos testes aprovados.
