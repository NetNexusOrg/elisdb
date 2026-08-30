## Why

No menu suspenso da lista de clientes, o texto "Criar novo cliente" é demasiado longo. Simplificar para "Novo" torna o menu mais limpo e consistente.

## What Changes

- Texto do primeiro item do menu na lista de clientes: "Criar novo cliente" → "Novo".

## Capabilities

Nenhuma alteração de comportamento ao nível de specs — mudança cosmética apenas.

## Impact

- `ClientListScreen.kt`: alterar o texto do `DropdownMenuItem` de "Criar novo cliente" para "Novo".
- Sem alterações a specs nem a testes.
