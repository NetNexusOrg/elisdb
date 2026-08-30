## 1. Pan 1:1 sem competir com o pager

- [x] 1.1 Elevar um estado booleano `isZoomed` para `PhotoPager`, atualizado por `ZoomablePhoto` via callback `onZoomChanged(scale > 1f)`
- [x] 1.2 Repor `isZoomed = false` no `LaunchedEffect(pagerState.currentPage)`
- [x] 1.3 Definir `HorizontalPager(userScrollEnabled = !isZoomed)` para o pager não roubar o arrasto quando ampliado
- [x] 1.4 Confirmar que o pan usa o delta direto do pointer (`calculatePan`), agora sem deslocamento do pager

## 2. Verificação

- [x] 2.1 Correr `./gradlew assembleDebug lintDebug testDebugUnitTest` e confirmar build verde
- [x] 2.2 Confirmar testes instrumentados `photoViewer_arrastoComZoomFazPanNaoSwipe` e `photoViewer_swipeEsquerdaMostraFotoSeguinte`
- [x] 2.3 Smoke-test manual no emulador: com zoom, o arrasto acompanha o dedo 1:1; a 1x o swipe troca de foto; zoom/pan repõem ao mudar de foto
