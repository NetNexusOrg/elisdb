## Context

A `SyncStatusBar` é partilhada entre as Definições (`SettingsScreen.kt`) e a lista
de clientes (`ClientListScreen.kt`). A mensagem "Servidor não configurado. Abre as
Definições para configurar." é produzida por `syncMessage()` para
`SyncStatus.ERROR` + `SyncFault.CONFIG`. Motivação em `proposal.md` — Why.

## Goals / Non-Goals

**Goals:**
- A lista de clientes não apresenta o aviso de configuração em falta.
- Estados reais de sincronização e a ação manual continuam na lista.
- As Definições mantêm o aviso completo.

**Non-Goals:**
- Não alterar a lógica de sincronização nem a classificação de `SyncFault`.
- Não remover a `SyncStatusBar` da lista por completo.

## Decisions

**D1 — Supressão opt-in na `SyncStatusBar`, não na ViewModel.**
Adicionar `suppressConfigMessage: Boolean = false` a `SyncStatusBar`. Quando
`true` e a mensagem a renderizar for a de configuração em falta (`CONFIG`), a barra
não mostra texto (só a ação de sincronizar). `SettingsScreen` usa o valor por
omissão (`false`) e mantém o comportamento atual.
Alternativas consideradas:
- Filtrar `syncFault` para `null` no `ClientListViewModel` — rejeitada: corrompe a
  semântica do estado de falha e afetaria o consumo futuro.
- Remover a `SyncStatusBar` da lista — rejeitada: perde o estado real e a ação
  manual, contrariando `sync-observability`.

**D2 — Ocultar apenas a linha de texto; manter o botão de sincronizar.**
Nenhum estado é alterado; apenas a apresentação do `CONFIG` deixa de ocupar espaço
na lista. Estados `IDLE`/`SUCCESS`/outros `ERROR` continuam idênticos.

## Risks / Trade-offs

- [Ocultar o aviso torna menos óbvio que falta configurar o servidor] → Mitigação:
  o aviso permanece nas Definições (ecrã destinado a configuração); a ação de
  sincronizar continua na lista e revela o estado por defeito.
- [Diferença subtil de comportamento entre Definições e lista] → Garantida por
  teste do caso `CONFIG` com e sem a flag.