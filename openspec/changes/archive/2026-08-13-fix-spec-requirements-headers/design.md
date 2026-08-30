## Context

Veja `proposal.md` para a motivação. Estado verificado com `openspec validate
--all`: 8 specs passam, 13 falham com o mesmo erro de estrutura — "Spec must
have a Requirements section" e "Missing required sections". As 13 falhas
revisadas têm todas os requisitos e cenários intactos; falta apenas o cabeçalho
`## Requirements` como secção de nível 2 entre `## Purpose` e as
`### Requirement:`.

Formato correto (confirmado nas specs que passam, ex.: `server-sync`,
`sync-observability`):

```
## Purpose
...descrição...

## Requirements

### Requirement: <nome>
...descrição...

#### Scenario: <nome>
- **WHEN** ...
- **THEN** ...
```

## Goals / Non-Goals

**Goals:**
- `openspec validate --all` passa a validar as 21 specs.
- Cabeçalho `## Requirements` presente em todas as 13 specs afetadas.
- `client-sync-state` apresentada na forma de main spec (`## Requirements`), não
  de delta (`## ADDED Requirements`).

**Non-Goals:**
- Não alterar o conteúdo de nenhum requisito, cenário, descrição ou cenário.
- Não reordenar requisitos.
- Não fundir/alterar outras specs que já passam.
- Não abordar o estado da change ativa `client-list-hide-config-notice`
  (arquivo/sync é decisão separada; fica fora do âmbito).

## Decisions

**D1 — Inserir `## Requirements` imediatamente antes da primeira
`### Requirement:` (após o bloco `## Purpose` + descrição).**
É o formato mínimo que satisfaz o validador e alinha com as 8 specs que já
passam. Cada ficheiro ganha exatamente uma linha nova (`## Requirements`, com o
parágrafo em branco existente antes da primeira `### Requirement:`).
Alternativa considerada: reescrever os ficheiros por completo — rejeitada por ser
destrutiva e desnecessária (o conteúdo está correto).

**D2 — Em `client-sync-state`, o cabeçalho é `## ADDED Requirements`; usa
`sed`/edições pontuais para o trocar por `## Requirements`.** O resto do ficheiro
(marca "ADDED", cenários) mantém-se: a spec passa a ser lida como main spec.
Alternativa: regenerar do zero a partir dos deltas arquivados — rejeitada: não
há perda de conteúdo e o contexto histórico não muda.

**D3 — Verificação por `openspec validate --all` após a edição.** É o critério de
aceitação objetivo (de 8→21 aprovadas), além de uma greppe `^## Requirements`
por cada uma das 13.

## Risks / Trade-offs

- [Risco: um ficheiro ter outra coisa entre `## Purpose` e os requisitos] → já
  verificado: em todas as 13 o primeiro `### Requirement:` é o primeiro bloco
  depois do Purpose; a inserção é segura na posição determinada.
- [Risco: `client-sync-state` ter `## ADDED Requirements` + mais secções] →
  verificado: tem exatamente esse cabeçalho e requisitos; a substituição do
  cabeçalho é pontual.
- [Trade-off: mudança aparentemente mecânica] → apesar de simples, é necessária
  e transversal; o critério de verificação (`validate --all`) elimina falsos
  positivos.

## Migration Plan

Mudança em ficheiros de texto apenas. Não há dados, código nem runtime a
migrar. Rollback: reverter a inserção dos cabeçalhos (alterações mínimas e
localizadas).

## Open Questions

Nenhuma.