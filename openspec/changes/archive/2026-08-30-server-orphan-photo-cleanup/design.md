## Context

`_replace_fotos` (`app/database.py:303-321`) faz `DELETE FROM fotos WHERE cliente_id = ? AND id NOT IN (...)` para fotos removidas numa edição, mas não apaga os ficheiros em `self.fotos_dir`. Só `delete_client` (`database.py:542-544`) remove ficheiros. Assim, remover uma foto de um cliente deixa o ficheiro órfão. Não há GC de ficheiros sem linha.

## Goals / Non-Goals

**Goals:**
- Remover ficheiros de fotos apagadas em edições.
- GC de ficheiros sem linha `fotos`.

**Non-Goals:**
- Não mudar o modelo de path (`fotos_dir/<id>`).
- Não alterar a lógica de duplicados.

## Decisions

**D1: Apagar ficheiro em `_replace_fotos`**
Antes/depois do `DELETE`, obter os `caminho`s das fotos a remover (`SELECT caminho FROM fotos WHERE ...`) e `os.remove` de cada um. Fazer dentro da transação é aceitável aqui porque é a própria operação de remoção (não um merge de vários registos); o risco de rollback é baixo e o ficheiro já não é referenciado.

**D2: GC de órfãos**
Nova função `gc_orphan_photos()` que lista ficheiros em `fotos_dir` e remove os que não têm linha `fotos`. Correr no startup (após migrações) ou num endpoint de manutenção. Reutilizável por `server-photo-upload-hardening`.

## Risks / Trade-offs
- [Remover ficheiro antes do commit deixa ficheiro em falta se o DELETE falhar] → ler os caminhos e remover só após o DELETE ter sucesso na mesma transação; risco mínimo.
- [GC apaga ficheiro de foto em vias de ser referenciada] → o GC corre no startup, altura em que não há push a meio; seguro.

## Migration Plan
- Sem migração de dados. Recupera órfãos existentes num GC de startup.

## Open Questions
- Nenhuma que altere specs, abordagem ou tarefas.
