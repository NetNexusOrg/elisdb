## Why

Depois de aplicar zoom, arrastar com um dedo desloca a foto muito lentamente — não acompanha o dedo (1:1). A causa é que o gesto de arrasto de um dedo é disputado entre o handler de pan e o scroll horizontal do `HorizontalPager`: o pager também arranca sobre o mesmo `down` não consumido e, enquanto está ampliado, parte do deslocamento do dedo é gasto a deslocar a página em vez da imagem, pelo que a foto se move menos do que o dedo.

## What Changes

- Desativar o scroll do pager enquanto a foto está ampliada (`HorizontalPager(userScrollEnabled = scale <= 1f)`), para que o arrasto de um dedo vá na totalidade para o pan.
- Elevar o estado de zoom/pan (`scale`/`offset`) para o nível do pager e repô-lo ao mudar de foto, para que o pager conheça o zoom atual e o pan seja aplicado sem competir com o scroll.
- Manter o pan 1:1 usando o delta direto do pointer.

## Capabilities

### New Capabilities

- Nenhuma.

### Modified Capabilities

- Nenhuma. A especificação `client-screens` já exige que "the photo SHALL pan to follow the drag"; a mudança só faz o pan acompanhar mesmo o dedo. Declarada com `skip_specs: true`.

## Impact

- `app/src/main/java/com/example/elisdb/ui/detalhe/PhotoViewerScreen.kt`: elevar `scale`/`offset` de `ZoomablePhoto` para `PhotoPager`, repor ao mudar de página, e passar `userScrollEnabled = scale <= 1f` ao `HorizontalPager`.
- Testes: ajustar o teste instrumentado `photoViewer_arrastoComZoomFazPanNaoSwipe` se necessário; sem novas dependências.
