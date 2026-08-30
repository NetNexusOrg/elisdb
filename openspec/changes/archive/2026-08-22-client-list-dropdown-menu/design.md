## Context

`ClientListScreen` tem um `TopAppBar` com título "Clientes" e dois `IconButton` em `actions` (Settings e Add). O objetivo é agrupá-los num menu suspenso único, tal como o ecrã de detalhe já faz com o overflow menu.

## Goals / Non-Goals

**Goals:**
- Um único ícone de overflow (três pontos) na barra superior, com um `DropdownMenu` com "Criar novo cliente" e "Definições".
- Manter o mesmo comportamento de navegação (`onAddClick`/`onSettingsClick`) e a ausência de FAB.

**Non-Goals:**
- Não alterar a lista, a pesquisa nem o estado de sincronização.

## Decisions

- **Usar `IconButton` + `DropdownMenu`/`DropdownMenuItem` do Material3** (mesmo padrão do `ClientDetailScreen`), com estado local `menuOpen` em `remember`. Alternativa considerada: `ExposedDropdownMenu` — rejeitada por ser para campos de texto, não para menus de topo.
- **Ícones/texto dos itens.** "Criar novo cliente" com `Icons.Default.Add` e "Definições" com `Icons.Default.Settings` (mantém os ícones existentes como leading icons, opcional).
- **`contentDescription` do botão de overflow** "Mais opções" (consistente com o ecrã de detalhe), para ser testável e acessível.

## Risks / Trade-offs

- [Ações menos descobertas (um toque extra para criar/settings)] → Aceite por decisão de produto; o menu de overflow é o padrão de outros ecrãs da app.
- [Testes instrumentados existentes que esperam os ícones diretos] → Ajustar para abrir o menu antes de tocar na ação.

## Migration Plan

Sem migração. Rollback trivial.

## Open Questions

Nenhuma.
