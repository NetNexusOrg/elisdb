## Context

`ZoomablePhoto` mantém `scale`/`offset`/`containerSize` num `remember` local e usa um `pointerInput` próprio que, com `scale > 1f`, consome o arrasto de um dedo para fazer pan (`offset += calculatePan()`). O `HorizontalPager` está num `pointerInput` ancestral e também arranca o seu drag sobre o mesmo `down` não consumido (`awaitFirstDown(requireUnconsumed = false)` no filho). Enquanto ampliado, o deslocamento do dedo é repartido entre o pan (filho) e o scroll da página (pager), o que torna o pan mais lento do que o dedo.

## Goals / Non-Goals

**Goals:**
- Pan 1:1: a foto acompanha exatamente o dedo quando ampliada.
- Swipe a 1x continua a trocar de foto; zoom/pan repõem ao mudar de foto.

**Non-Goals:**
- Não adicionar focal-point zoom (o zoom continua centrado) nem rotação.
- Não alterar a galeria, o ViewModel nem o carregamento de fotos.

## Decisions

- **Desativar o scroll do pager quando ampliado.** `HorizontalPager(userScrollEnabled = !isZoomed)` elimina a competição: com zoom, o pager não tenta deslocar a página, logo todo o arrasto vai para o pan.
- **Elevar apenas um booleano `isZoomed` para `PhotoPager`.** O `ZoomablePhoto` mantém o seu `scale`/`offset`/`containerSize` locais e reporta `onZoomChanged(scale > 1f)`; o pager usa isso para `userScrollEnabled` e repõe `isZoomed = false` no `LaunchedEffect(pagerState.currentPage)`. Evita reestruturar o estado por página.
- **Manter o pan por delta direto.** `calculatePan()` (delta do centroid) já é 1:1 para um dedo; mantém-se, agora sem o pager a roubar deslocamento.

## Risks / Trade-offs

- [Mudança de foto com zoom ativo] → Com `userScrollEnabled = false` quando `scale > 1f`, o utilizador tem de voltar a 1x antes de deslizar; comportamento intencional e coerente com "zoom repõe ao mudar de foto".
- [Estado partilhado entre páginas] → Só a página visível é composta (`beyondViewportPageCount = 0`), e o reset no `LaunchedEffect(currentPage)` garante que a página nova começa a 1x/zero.
- [Regressão do swipe] → Validar que a 1x o swipe continua a trocar de foto (teste instrumentado existente).

## Migration Plan

Sem migração. Rollback trivial.

## Open Questions

Nenhuma.
