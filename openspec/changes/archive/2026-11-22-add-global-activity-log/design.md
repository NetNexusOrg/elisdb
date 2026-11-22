# Design

## Context

Ver `proposal.md` — Why. Factos que moldam o desenho:

- A auditoria já é gravada em `client_audit` (sem FK, por isso sobrevive à
  eliminação do cliente) e já alimenta o log por cliente.
- `client_history_page` pagina por `id` ascendente com `after_id`; o novo ecrã
  precisa do sentido inverso.
- `AuditEntryOut` não inclui o cliente; o log por cliente sabe-o pelo contexto.
- `ClientLogScreen` tem os cartões, o detalhe de campos/fotos e os estados de
  erro/vazio como funções privadas.
- A lista de clientes já usa um `DropdownMenu` com `Novo`, `Configuração` e
  `Sobre`; `Navigation.kt` regista cada destino por tipo.
- `FotoDao` só consulta fotos por cliente; os cartões do log precisam de
  miniaturas por ids de fotos de várias entradas.

## Goals / Non-Goals

**Goals:**

- Ver todas as alterações confirmadas numa única página, mais recentes primeiro.
- Reutilizar a auditoria e os componentes visuais existentes, sem duplicar
  regras de apresentação.
- Paginação estável mesmo com novas entradas a chegar ao topo.

**Non-Goals:**

- Registar alterações locais ainda não sincronizadas.
- Filtrar o log global por cliente, autor, ação ou intervalo de datas.
- Alterar o endpoint e o ecrã de histórico por cliente.
- Alterar o schema da base de dados.

## Decisions

### `GET /api/history` com cursor `before_id`

O histórico global é o mesmo `client_audit`, ordenado por `id DESC`. A paginação
usa `before_id` (último id da página) e não um offset:

- novas entradas inserem-se no topo e não deslocam as páginas já lidas;
- o índice da chave primária serve a ordenação e o filtro `id < ?`;
- não há repetições nem saltos entre páginas.

`limit` tem omissão 50 e máximo 200, como o histórico por cliente. A resposta é
`{entries, next_before_id}`; `next_before_id` só vem quando existem entradas
mais antigas. O endpoint exige autenticação, como os restantes.

### Nome do cliente resolvido por página

A query junta `clientes` para obter o nome atual. Para entradas cujo cliente já
foi eliminado, o nome não está na linha atual; nesse caso o servidor procura,
numa única consulta adicional limitada aos ids da página, a entrada `delete`
mais recente de cada cliente e lê `details.deleted_client.nome`. Se ainda assim
não houver nome, usa o identificador do cliente.

Assim, todas as entradas antigas de um cliente eliminado mostram o nome que ele
tinha, não só a entrada de eliminação. O custo é uma consulta por página, não
uma por entrada.

### Modelo de resposta separado para não mexer no log por cliente

`AuditEntryOut` mantém-se intacto. É criado `ActivityEntryOut` (com
`cliente_id` e `cliente_nome`) e `ActivityPageOut`. No Android, o DTO
`AuditEntryDto` ganha `clienteId`/`clienteNome` opcionais com omissão `null`; o
endpoint por cliente não os envia e o ecrã existente ignora-os. Isto evita dois
DTOs quase iguais e mantém a compatibilidade do contrato antigo.

### Componentes de log partilhados, ecrãs com ordem própria

É extraído `ui/detalhe/LogComponents.kt` com `LogEntryCard`, `ChangeDetails`,
`FieldChangeRow`, `PhotoChanges`, `LogEmptyState`, `LogErrorState`,
`actionIcon`, `actionLabel` e os formatadores. O cartão aceita um cabeçalho de
cliente opcional e um callback de navegação; o log por cliente passa ambos a
nulo e o comportamento mantém-se.

Cada ecrã mantém a sua `LazyColumn` e o seu agrupamento por dia, porque o log
por cliente é ascendente e o global é descendente. Duplicar só a lista é mais
simples do que generalizar a ordenação num componente comum.

### Miniaturas por página, não por entrada

`ActivityLogViewModel` reúne os ids de fotos adicionadas nas entradas carregadas
e faz uma consulta `FotoDao.getUrisByIds(ids)` por página, construindo um mapa
`id -> uri` reutilizado pelos cartões. Fotos que já não existem localmente
aparecem apenas como contagem, como no log por cliente.

### Navegação para o cliente só quando existe

O servidor devolve `cliente_nome` mesmo para eliminados, mas o cartão só é
clicável se o cliente existir na base local. O ViewModel consulta os ids
existentes da página (`ClienteDao`) e expõe um conjunto de ids navegáveis; a
ação `delete` também nunca navega. O nome mostrado vem da entrada, pelo que um
cliente eliminado continua legível.

### Menu e navegação

`ActivityLog` é um `data object` em `NavigationKeys.kt` e ganha um `entry` em
`Navigation.kt`, com o ViewModel criado por `container.withApiClient` para as
chamadas autenticadas. O item **Logs** usa `Icons.Default.History` e fica antes
de `Configuração`, junto das restantes ações da lista.

## Risks / Trade-offs

- [Retenção da auditoria apaga entradas antigas] → a página mostra o que existe;
  não é inventado histórico local.
- [Cliente renomeado] → o cabeçalho usa o nome atual; a entrada de edição
  continua a mostrar o nome antigo e o novo nos detalhes, como já acontece.
- [Servidor antigo sem `/api/history`] → o ecrã mostra o estado de erro com
  retry, tal como o log por cliente quando o endpoint falha.
- [Chave sem nome] → o cartão apresenta "Utilizador desconhecido".
- [Entradas de um cliente eliminado sem snapshot] → cai no identificador do
  cliente; caso raro e apenas para dados anteriores à auditoria de eliminação.
- [Página com muitas fotos] → a consulta de URIs é feita por página e limitada
  aos ids carregados, não ao histórico completo.

## Migration Plan

1. Servidor primeiro: o endpoint é apenas aditivo e não altera tabelas nem
   respostas existentes.
2. App depois: sem migração Room; se o servidor ainda não tiver o endpoint, o
   ecrã falha de forma controlada.
3. Rollback: remover o item de menu e o endpoint não afeta os dados nem o log
   por cliente.
