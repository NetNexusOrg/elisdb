## Why

Hoje cada cliente só pode ter uma tag. A equipa precisa de classificar o mesmo
cliente em várias dimensões (ex.: tipo de serviço e rota), sem criar registos
duplicados nem perder a filtragem já existente.

## What Changes

- O campo `tag` passa a conter um **conjunto de tags separadas por `;`**
  (ex.: `PREMIUM;GOLD;ROTA 21`), mantendo-se um único campo de texto na base de
  dados e na API.
- Normalização no servidor e na app: `trim`, maiúsculas, sem duplicados,
  ordenação alfabética.
- Limites: máximo 10 tags por cliente, 100 caracteres por tag e 500 no total.
- Formulário: campo para escrever e adicionar tags, com chips removíveis.
- Lista: todas as tags do cliente, em badges que passam para a linha seguinte.
- Detalhe: todas as tags em chips.
- Filtros da lista e do mapa: escolher uma tag mostra os clientes que a
  contenham; `Todas as tags` limpa.
- Histórico: alterações ao conjunto de tags aparecem como `Tags`.
- Sem migração de base de dados (a coluna `tag TEXT` já existe).
- Mantém-se a compatibilidade: uma tag única continua a ser um conjunto de uma
  tag; apps antigas mostram a string inteira sem quebrar.

## Capabilities

### New Capabilities

<!-- Nenhuma - esta alteração modifica apenas comportamento existente. -->

### Modified Capabilities

- `client-data`: o registo do cliente passa a suportar um conjunto de tags
  normalizado no campo `tag`.
- `client-screens`: o formulário, a lista, o detalhe, o histórico e os filtros
  passam a lidar com várias tags.
- `server-sync`: o servidor normaliza e valida o conjunto de tags no push/pull.
- `client-map`: o filtro por tag passa a considerar todas as tags do cliente.

## Impact

- Servidor: `app/models.py` (limite e validação/normalização de `tag`) e testes.
- Android: `ClienteDraft`, `ClienteRepositoryImpl`, `ClientFormViewModel`/Screen,
  `ClientListViewModel`/Screen, `ClientMapViewModel`/Screen,
  `ClientDetailScreen`, `ClientLogScreen` e testes.
- Sem tabelas novas, sem migração Room/SQLite e sem alterações ao contrato de
  sincronização (o campo `tag` continua a ser texto).
- Versões: servidor 1.5.0 primeiro; app 1.12.0 depois.
