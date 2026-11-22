# Proposal

## Why

O histórico de alterações existe, mas só é consultável cliente a cliente, no
detalhe de cada registo. Não há forma de ver, num único sítio e por ordem
cronológica, quem criou, alterou ou eliminou clientes — o que dificulta auditar
o trabalho da equipa e perceber o que mudou recentemente.

## What Changes

- Nova opção **Logs** no menu **Mais opções** da lista de clientes.
- Nova página global de alterações, com as entradas mais recentes primeiro,
  agrupadas por dia e paginadas.
- Cada entrada mostra o cliente, a ação (criou/editou/eliminou), o autor, a hora
  e o detalhe do que mudou (campos e fotos), reutilizando o cartão do log por
  cliente.
- Tocar no nome de um cliente existente abre o respetivo detalhe; clientes
  eliminados aparecem identificados pelo nome guardado no snapshot da eliminação
  e não são clicáveis.
- Novo endpoint autenticado `GET /api/history` para o histórico global, com
  paginação por cursor (`limit`, `before_id`, `next_before_id`).
- Sem alterações de schema, migrações ou dependências; a auditoria existente é
  reutilizada.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `client-screens`: nova entrada **Logs** no menu da lista, nova página global
  com estados de carregamento/erro/vazio, agrupamento por dia, paginação e
  navegação para o cliente quando ainda existe.
- `server-sync`: novo endpoint de histórico global, paginado do mais recente
  para o mais antigo, incluindo o cliente e o nome de clientes eliminados.

## Impact

- Servidor: `elisdb/main.py` (endpoint), `elisdb/database.py` (consulta
  paginada), `elisdb/models.py` (modelos de resposta) e testes `pytest`.
- Android: `remote/RemoteDto.kt`, `remote/ApiClient.kt`, `sync/SyncApi.kt`,
  `data/local/FotoDao.kt`, novo `ui/detalhe/ActivityLogScreen.kt` e
  `ActivityLogViewModel.kt`, extração de componentes de `ClientLogScreen.kt`,
  `Navigation.kt`, `NavigationKeys.kt` e menu de `ClientListScreen.kt`.
- Sem alterações ao protocolo de sync nem ao formato dos dados sincronizados.
