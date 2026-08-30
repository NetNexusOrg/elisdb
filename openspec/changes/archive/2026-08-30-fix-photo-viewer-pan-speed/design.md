## Context

`ZoomablePhoto` aplica zoom/pan num `graphicsLayer` (`scaleX/scaleY`, `translationX/translationY`) e captura os gestos num `pointerInput` colocado **depois** (por dentro) do `graphicsLayer`. Nessa ordem, o `pointerInput` recebe as coordenadas no espaço do conteúdo já escalado: um movimento de `D` píxeis no ecrã corresponde a `D / scale` píxeis no espaço do conteúdo.

`calculatePan()` devolve, portanto, o delta do dedo **no espaço escalado** (`pan = D / scale`). O código atual soma esse valor diretamente a `translationX/Y`, que o `graphicsLayer` aplica em píxeis de ecrã (após a escala). Resultado: a imagem desloca-se `D / scale` no ecrã para um dedo que se moveu `D` — a `1/scale`× da velocidade do dedo, pior quanto maior o zoom.

O offset (pan) e o `clampOffset` já estavam desalinhados por esta mesma razão: `clampOffset` calcula `max = size * (scale - 1) / 2`, que é o limite **em píxeis de ecrã**, enquanto o offset acumulava píxeis do espaço do conteúdo.

## Goals / Non-Goals

**Goals:**
- Pan 1:1: com zoom, a imagem acompanha exatamente o dedo (arrasto de um dedo e pan durante a pinça).
- Manter o offset em píxeis de ecrã, tornando `clampOffset` coerente com o offset.

**Non-Goals:**
- Não adicionar focal-point zoom (o zoom continua centrado) nem rotação.
- Não alterar o `HorizontalPager`, o `PhotoPager`, o ViewModel nem o carregamento de fotos.

## Decisions

- **Converter o pan para píxeis de ecrã multiplicando pela escala.** Nos dois ramos do gesto, usar `panChange * scale` em vez de `panChange`:
  - 1 dedo com `scale > 1f`: `offset += calculatePan() * scale.floatValue`.
  - Pinça (`pressedCount > 1`): `offset += calculatePan() * newScale` (a escala já atualizada pelo `calculateZoom()` nesse evento).
  Isto converte o delta do espaço do conteúdo para o espaço do ecrã, onde a `translationX/Y` é aplicada, resultando em 1:1.

  Alternativas consideradas:
  - **Trocar a ordem de `graphicsLayer`/`pointerInput`** (pôr o `pointerInput` por fora para ler píxeis de ecrã diretamente). Rejeitada: reordenar os modificadores da `AsyncImage` muda a área de hit-test e o `onSizeChanged`, com mais risco de regressão do que uma multiplicação local.
  - **Passar para `detectTransformGestures`**. Não resolve: devolve o mesmo `pan` no espaço do conteúdo; a multiplicação pela escala continuaria a ser necessária.

- **Manter `clampOffset` inalterado.** Com o offset a passar a estar em píxeis de ecrã, `max = size * (scale - 1) / 2` fica correto por construção (o conteúdo ampliado é `size * scale`, centrado, logo o deslocamento máximo é `(size*scale - size)/2`).

## Risks / Trade-offs

- [Regressão do swipe a 1x] → O ramo de 1 dedo só corre quando `scale > 1f`; a 1x nada muda. Validar com o teste instrumentado existente `photoViewer_swipeEsquerdaMostraFotoSeguinte`.
- [Pan demasiado rápido noutros dispositivos] → A multiplicação é exata (não há fator arbitrário); o fator é a própria escala. Smoke-test manual no emulador confirma 1:1.
- [Limites do pan] → `clampOffset` continua a impor `coerceIn`; com o offset em píxeis de ecrã o limite passa a estar correto.

## Migration Plan

Sem migração. Alteração localizada a uma função; rollback trivial (reverter a multiplicação).

## Open Questions

Nenhuma.
