## Context

`SyncManager` (`SyncManager.kt:69-74`) observa `dao.observeAll()` para o sinal de uploads pendentes; `ClienteDao.observeAll()` (`ClienteDao.kt:15-16`) filtra `deletePending = 0`. Logo eliminações pendentes não contam para o indicador, que mostra "sincronizado" com deletes por enviar — contradiz `client-sync-state` "Server synchronization status".

## Goals / Non-Goals

**Goals:**
- Incluir eliminações pendentes no sinal de uploads pendentes.
- Indicador reflete o estado real.

**Non-Goals:**
- Não alterar o modelo de dados (já existe `deletePending`).
- Não mudar o comportamento de sync em si.

## Decisions

**D1: Observar eliminações pendentes**
`SyncManager` passa a combinar `observeAll()` (criações/edições) com `getPendingDeletions()` (ou `getAll()`) para calcular `hasPendingUploads`. O `StateFlow` reflete `pendingEdits || pendingDeletions`.
- *Alternativa*: mudar `observeAll()` para não filtrar `deletePending` — rejeitada, `observeAll()` é usado para listas de UI que devem esconder apagados.

**D2: Reatividade**
Manter a combinação num `combine` de Flows para o estado atualizar em tempo real.

## Risks / Trade-offs
- [Indicador fica "pendente" mais vezes] → é o comportamento correto; o utilizador vê que há um delete por enviar.

## Migration Plan
- Sem migração.

## Open Questions
- Nenhuma que altere specs, abordagem ou tarefas.
