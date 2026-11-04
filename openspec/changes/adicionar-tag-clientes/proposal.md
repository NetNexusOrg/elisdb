## Why

Os clientes não têm hoje nenhuma forma de os agrupar por categoria de negócio
(por exemplo, tipo de serviço ou zona). Adicionar uma etiqueta livre ao registo
do cliente e permitir filtrar a lista por essa etiqueta resolve o agrupamento
sem obrigar a uma alteração de schema futura.

## What Changes

- Adicionar um campo opcional `tag` (texto livre, uma etiqueta por cliente) ao
  registo do cliente, persistido localmente e no servidor.
- Tornar a tag editável no formulário de criação/edição.
- Mostrar a tag no ecrã de detalhe e como etiqueta na lista de clientes.
- Permitir filtrar a lista por tag: o menu do ícone dentro da caixa de pesquisa
  lista as tags existentes e mostra apenas os clientes com a tag escolhida.
- Transportar a tag no push/pull de sincronização e registá-la no histórico de
  alterações.
- A pesquisa livre da lista mantém-se inalterada (nome, contrato e notas).
- Campo aditivo e compatível: clientes e servidores antigos continuam a
  funcionar (campo ausente lido como vazio).

## Capabilities

### New Capabilities

<!-- Nenhuma - esta alteração modifica apenas comportamento existente. -->

### Modified Capabilities

- `client-data`: o schema do registo do cliente passa a incluir uma tag opcional,
  e as operações de criação/edição persistem-na.
- `client-screens`: o formulário ganha o campo Tag, o detalhe mostra a tag, a
  lista mostra-a como etiqueta e o menu de filtros permite filtrar por tag.
- `server-sync`: o registo transportado e persistido no servidor passa a incluir
  a tag, incluindo o respetivo limite de tamanho.
- `client-sync`: o mapeamento de push e de pull passa a transportar a tag.
- `client-map`: o mapa de clientes ganha um filtro por tag, independente do
  filtro da lista.

## Impact

- Servidor: `app/models.py`, `app/database.py` (schema, migração, escrita,
  leitura e auditoria) e testes `pytest`.
- Android: entidade Room e migração 10→11, `ClienteDraft`, DTOs remotos,
  `SyncMapper`/`SyncCoordinator`, ViewModels e ecrãs de formulário, detalhe e
  lista, e testes unitários/instrumentados.
- Sem dependências novas e sem alterações destrutivas de base de dados.
