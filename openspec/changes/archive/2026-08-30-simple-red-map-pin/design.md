## Context

O `Marker` da osmdroid em `OsmMap.kt` é criado sem `icon`, usando o ícone por defeito da biblioteca. O mesmo `OsmMap` é partilhado pelo detalhe, viewer fullscreen, picker e preview do formulário, pelo que basta um ponto de alteração. Ver proposal.md - Why.

## Goals / Non-Goals

**Goals:**
- Pino vermelho simples e comum em todos os mapas.

**Non-Goals:**
- Não alterar anchor, arrasto, fixação, zoom nem qualquer comportamento do marcador.

## Decisions

- **Vector drawable em vez de PNG.** Um `ic_map_pin.xml` (vector) é independente de densidade e mantém-se nítido a qualquer zoom. Usa a forma clássica de pino (círculo com bico na base) com preenchimento vermelho e um círculo interior branco. Alternativa considerada: PNG de um pin — rejeitada por exigir vários tamanhos por densidade.

- **Definir `marker.icon` a partir do drawable.** Em `OsmMap.kt`, carregar `context.getDrawable(R.drawable.ic_map_pin)` e atribuir a `marker.icon`. O anchor existente (`ANCHOR_CENTER`/`ANCHOR_BOTTOM`) mantém-se, colocando a ponta do pino sobre as coordenadas. O vector deve declarar uma `width`/`height` razoável (ex. 32dp) para o marcador não aparecer nem demasiado pequeno nem demasiado grande.

## Risks / Trade-offs

- [Ícone mal dimensionado no mapa] → Definir dimensões intrínsecas do vector (~32dp); validar no emulador.
- [Regressão no comportamento do marcador] → Apenas `marker.icon` é alterado; arrasto/fixação/centro não são tocados.

## Migration Plan

Sem migração. Rollback trivial (remover a atribuição de `icon`).
