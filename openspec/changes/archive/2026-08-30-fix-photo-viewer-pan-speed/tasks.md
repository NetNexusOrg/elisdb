## 1. Corrigir o pan 1:1

- [x] 1.1 No `ZoomablePhoto` (em `PhotoViewerScreen.kt`), no ramo de 1 dedo com `scale > 1f`, somar `calculatePan() * scale.floatValue` ao offset em vez de `calculatePan()`
- [x] 1.2 No ramo de pinça (`pressedCount > 1`), somar `calculatePan() * newScale` ao offset (antes de `clampOffset`)
- [x] 1.3 Confirmar que `clampOffset` permanece inalterado e que o offset passa a estar em píxeis de ecrã (limite `size * (scale - 1) / 2` coerente)

## 2. Verificação

- [x] 2.1 Reforçar o teste instrumentado em `ClientDetailScreenTest` para verificar que o arrasto com zoom desloca a imagem na mesma proporção do dedo (não apenas que não troca de foto), mantendo `photoViewer_arrastoComZoomFazPanNaoSwipe` e `photoViewer_swipeEsquerdaMostraFotoSeguinte`
- [x] 2.2 Correr `./gradlew assembleDebug lintDebug testDebugUnitTest` e confirmar build verde
- [x] 2.3 Smoke-test manual no emulador: com zoom, o arrasto acompanha o dedo 1:1; a 1x o swipe continua a trocar de foto; zoom/pan repõem ao mudar de foto
