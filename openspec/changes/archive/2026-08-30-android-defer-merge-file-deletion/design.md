## Context

`pushEdits` (`SyncCoordinator.kt:115`) envolve o loop numa transação; `mergeDuplicate` (`SyncCoordinator.kt:208`) elimina ficheiros de foto depois de retornar, ainda com a transação externa aberta. Falha posterior no loop faz rollback das linhas mas os ficheiros já foram apagados → `Foto` a apontar a ficheiro em falta.

## Goals / Non-Goals

**Goals:**
- Adiar a eliminação de ficheiros de foto do merge para após o commit do push.
- Nunca deixar `Foto` a apontar a ficheiro inexistente por rollback.

**Non-Goals:**
- Não mudar a lógica de adoção do `existing_id`.
- Não alterar o protocolo de merge.

## Decisions

**D1: Recolher caminhos e apagar após commit**
`mergeDuplicate` passa a devolver (ou acumular numa lista partilhada do `pushEdits`) os caminhos a apagar, sem os remover. Após a transação do `pushEdits` fazer commit com sucesso, o `SyncCoordinator` remove esses ficheiros.
- *Alternativa*: remover dentro da transação e aceitar o risco — rejeitada, quebra a invariant DB/disco.

**D2: Tratar exceções de ficheiro**
O `os.remove` pós-commit tolera ficheiro já inexistente (ignorer `FileNotFoundError`).

## Risks / Trade-offs
- [Ficheiro órfão se o processo morrer entre commit e delete] → raro; um GC futuro (ver `photo-hygiene`) recupera; aceitável vs. deixar linha a apontar a nada.

## Migration Plan
- Sem migração.

## Open Questions
- Nenhuma que altere specs, abordagem ou tarefas.
