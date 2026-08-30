## 1. Pan com zoom

- [x] 1.1 Em `ZoomablePhoto`, adicionar estado `offset` (e o tamanho do container) e aplicar `translationX`/`translationY` no `graphicsLayer` juntamente com a escala
- [x] 1.2 Reescrever o `pointerInput`: 2+ dedos fazem zoom+pan (consome), 1 dedo com `scale > 1f` faz pan (consome), 1 dedo com `scale == 1f` não consome (deixa o pager fazer swipe)
- [x] 1.3 Limitar o offset com `coerceIn` baseado no tamanho do container e na escala, para a foto não sair dos limites
- [x] 1.4 Repor `offset` a zero quando a escala regressa a 1f (e garantir reset ao mudar de foto via `key(foto.id)`)

## 2. Verificação

- [x] 2.1 Correr `./gradlew assembleDebug lintDebug testDebugUnitTest` e confirmar build verde
- [x] 2.2 Adicionar/ajustar teste instrumentado em `ClientDetailScreenTest` cobrindo pan com zoom (pinça + arrasto) sem mudar de foto
- [x] 2.3 Smoke-test manual no emulador: pinça faz zoom, arrasto com zoom desloca a foto, a 1x o swipe continua a trocar de foto, e zoom/pan repõem ao mudar de foto
