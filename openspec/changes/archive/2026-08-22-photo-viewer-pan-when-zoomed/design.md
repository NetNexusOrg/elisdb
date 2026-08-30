## Context

`ZoomablePhoto` (em `PhotoViewerScreen.kt`) aplica zoom por pinça num `graphicsLayer` (escala centrada), mas só reage a 2+ dedos: um dedo é sempre deixado ao `HorizontalPager` para o swipe. Por isso, com zoom aplicado, não há como deslocar a imagem.

## Goals / Non-Goals

**Goals:**
- Com `scale > 1f`, arrastar com um dedo faz pan da imagem; com `scale == 1f`, arrastar continua a trocar de foto.
- Limitar o pan aos limites da imagem (não perder a foto de vista) e repor zoom+pan ao mudar de foto.

**Non-Goals:**
- Não adicionar double-tap nem rotação.
- Não alterar a galeria, o ViewModel nem o carregamento de imagens.

## Decisions

- **Adicionar estado `offset` e reescrever o `pointerInput` para distinguir os gestos.**
  No `awaitEachGesture`:
  - 2+ dedos → pinça: aplicar `calculateZoom()` à escala e `calculatePan()` ao offset, consumindo o evento.
  - 1 dedo e `scale > 1f` → pan: somar `calculatePan()` (ou o deslocamento do pointer) ao offset, consumindo o evento para o pager não disparar swipe.
  - 1 dedo e `scale == 1f` → não consumir, deixando o pager fazer swipe.
  Alternativa considerada: `Modifier.transformable`/`detectTransformGestures` — rejeitada porque consome o arrasto de um dedo mesmo a 1x, bloqueando o swipe do pager.

- **Limitar o offset ao excesso da escala.** Com `ContentScale.Fit` e escala centrada, o offset máximo em cada eixo é `(size * (scale - 1)) / 2`; o pan deve ser coberto por `coerceIn` sobre esses limites, usando o tamanho do container capturado via `onSizeChanged` (ou um `BoxWithConstraints`/`onGloballyPositioned`).

- **Reset ao voltar a 1x e ao mudar de foto.** Quando a escala regressa a 1f, `offset` volta a `Offset.Zero`; como o estado vive em `remember` por página (`key(foto.id)`), trocar de foto repõe zoom e pan.

- **Pager desligado enquanto está ampliado.** Quando `scale > 1f`, o pan é consumido no filho, por isso o pager não recebe o arrasto; não é necessário `userScrollEnabled` dinâmico.

## Risks / Trade-offs

- [Conflito de gestos pager vs pan] → Consumir o evento de 1 dedo apenas quando `scale > 1f`; validar manualmente no emulador que a 1x o swipe muda de foto e ampliado o arrasto faz pan.
- [Pan sair dos limites] → `coerceIn` do offset com base no tamanho do container e na escala; se o container ainda não tiver tamanho, tratar 0 como sem pan.
- [Desempenho ao recalcular limites a cada frame] → guardar o tamanho do container em estado local, atualizado em `onSizeChanged`, e usar apenas nos limites do `coerceIn`.

## Migration Plan

Sem migração. Mudança local ao `ZoomablePhoto`. Rollback trivial.

## Open Questions

Nenhuma.
