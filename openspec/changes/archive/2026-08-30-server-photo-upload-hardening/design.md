## Context

`put_photo` (`app/main.py:130-134`) faz `await request.body()` sem teto; qualquer upload esgota RAM. O endpoint aceita qualquer `PUT` autenticado e escreve o ficheiro antes de um push o referenciar, ficando órfão se o push falhar/nunca vier. Não há GC de ficheiros sem linha `fotos` (parte desta proposta e de `server-orphan-photo-cleanup`).

## Goals / Non-Goals

**Goals:**
- Limitar tamanho do upload de foto (413 se exceder).
- GC de ficheiros PUT nunca referenciados.

**Non-Goals:**
- Não mudar o formato do endpoint (PUT /api/photos/{id}).
- Não implementar chunked upload complexo; um teto de content-length basta.

## Decisions

**D1: Teto de tamanho**
Ler `Content-Length`; se ausente ou acima de `MAX_PHOTO_BYTES` (configurável, ex.: 20 MB), devolver 413 imediatamente sem ler o body. Opcionalmente usar `request.stream()` com contador para não confiar só no header.

**D2: GC partilhado**
Reutilizar `gc_orphan_photos()` de `server-orphan-photo-cleanup` para remover ficheiros PUT nunca referenciados. Garantir que o GC não apaga ficheiros de um push em curso (correr em altura segura).

## Risks / Trade-offs
- [Teto muito baixo quebra fotos grandes] → 20 MB é razoável para fotos de telemóvel; configurable.
- [GC apaga ficheiro de push em vias de referenciar] → corre no startup/GC; o push cria a linha `fotos` antes ou logo a seguir ao PUT, pelo que o GC de startup é seguro.

## Migration Plan
- Sem migração de dados.

## Open Questions
- Nenhuma que altere specs, abordagem ou tarefas.
