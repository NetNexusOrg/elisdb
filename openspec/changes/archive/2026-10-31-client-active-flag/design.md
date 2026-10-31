# Design

## Context

Ver `proposal.md` — Why. O projeto já trata o cliente como um conjunto de campos
opcionais sincronizados por LWW (`nome`, `contrato`, `notas`, `latitude`,
`longitude`); a BD Android está na versão 9 e o servidor aplica migrações
idempotentes em `_apply_schema`. O histórico guarda alterações por campo em
`details.fields` e as fotos em `details.photos`.

## Goals / Non-Goals

**Goals:**

- Representar ativo/inativo como um booleano com default ativo, sem perder dados.
- Propagar o estado pelo sync existente (push/pull, LWW, duplicados) sem lógica
  nova no coordenador.
- Tornar o estado visível e editável com o mínimo de UI.

**Non-Goals:**

- Filtrar ou esconder clientes inativos da lista.
- Impedir edições, navegação ou sincronização de clientes inativos.
- Toggle direto na lista (fica para uma iteração futura).
- Estados intermédios (suspenso, arquivado) ou datas de desativação.

## Decisions

### Booleano `ativo` na tabela `clientes`

Coluna `ativo INTEGER NOT NULL DEFAULT 1` no servidor e `ativo: Boolean = true` na
entidade Room. Alternativas: enum textual (`estado`) — rejeitada por admitir
valores inválidos e exigir validação em todos os pontos; tabela separada — sem
justificação para um único campo. O default `1` garante que registos antigos e
payloads antigos ficam ativos sem backfill.

### Nome do campo: `ativo`

Os campos de domínio já são portugueses (`nome`, `contrato`, `notas`, `fotos`), por
isso o wire e a BD usam `ativo` em vez de `active`, mantendo o contrato coerente.

### O estado entra no LWW como qualquer campo

O `save` do repositório atualiza `updated_at` e incrementa `revision`, por isso o
coordenador não precisa de código novo: o push já reenvia o cliente alterado e o
pull aplica o registo mais recente. Não se cria endpoint nem flag de sync.

### Migrações

- Android: `MIGRATION_9_10` com `ALTER TABLE clientes ADD COLUMN ativo INTEGER NOT
  NULL DEFAULT 1`; schema exportado `10.json`.
- Servidor: `_migrate_clientes_ativo(conn)` com verificação de `PRAGMA table_info`
  antes do `ALTER` e chamada em `_apply_schema`; o `SCHEMA` passa a incluir a
  coluna para instalações novas.

### Auditoria

Alterações de estado entram em `details.fields` com chave `ativo` e valor
`"Ativo"`/`"Inativo"`, e o cliente Android mostra “Ativou o cliente” / “Desativou o
cliente”. Alternativa: evento próprio no histórico — rejeitada por duplicar o
formato existente (`fields`) sem ganho.

### UI

- Formulário: `Switch` “Cliente ativo” logo abaixo de “Notas” (campo de estado,
  antes da localização), ligado por omissão em modo criação.
- Detalhe: chip “Inativo” ao lado do nome, apenas quando inativo.
- Lista: chip/etiqueta “Inativo” e conteúdo atenuado no cartão; o contrato
  continua por baixo do nome. Sem toggle na lista (decisão de âmbito).

### Compatibilidade de payload

`ativo: bool = True` em `ClienteIn`/`ClienteOut` e `Documento`-style default nos
DTOs, para que um cliente antigo a falar com um servidor novo (ou vice-versa) não
desative ninguém por omissão.

## Risks / Trade-offs

- **Cliente antigo a fazer pull perde o estado** (ignora `ativo` e volta a enviar
  `ativo=true`) → documentar que a app deve ser atualizada depois do servidor; o
  LWW resolve com last-write-wins, mas uma edição antiga pode reativar o cliente.
- **Histórico com duas linhas na mesma edição** (campos + estado) → usar a mesma
  entrada de auditoria e juntar `ativo` aos `fields`, não criar entrada extra.
- **Migração falhar a meio** → `ALTER TABLE ... ADD COLUMN` é atómico e idempotente;
  a verificação prévia evita o erro de coluna existente.
- **Linha da lista mais alta** com contrato + chip → manter o chip na mesma linha
  do nome, com `Row` e sem alterar a altura do cartão.

## Migration Plan

1. Servidor primeiro (tolera payloads sem `ativo`): migração + contrato.
2. App depois: migração 9→10 e UI.
3. Rollback: a coluna tem default; um servidor antigo ignora o campo e a app
   mantém o estado local. Não há remoção de colunas nem alteração de dados.

## Open Questions

Nenhuma.
