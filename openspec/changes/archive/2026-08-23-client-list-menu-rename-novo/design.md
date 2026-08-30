## Context

O menu de overflow na lista de clientes tem dois itens: "Criar novo cliente" e "Definições". O primeiro é demasiado longo.

## Goals / Non-Goals

**Goals:**
- Simplificar o texto para "Novo".

**Non-Goals:**
- Não alterar o comportamento, specs nem testes.

## Decisions

- **Apenas alterar a string** no `DropdownMenuItem`. Sem novos imports, sem alterações de UI.

## Risks / Trade-offs

Nenhum significativo.

## Open Questions

Nenhuma.
