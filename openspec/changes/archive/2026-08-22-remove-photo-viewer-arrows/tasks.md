## 1. Remover as setas

- [x] 1.1 Em `PhotoViewerScreen.kt`, remover os dois `IconButton` de seta (`ChevronLeft`/`ChevronRight`) do `PhotoPager`, mantendo o contador "n / total"
- [x] 1.2 Remover os parâmetros `onPrevious`/`onNext` de `PhotoPager` e os argumentos na chamada em `PhotoViewerScreen`
- [x] 1.3 Em `PhotoViewerViewModel.kt`, remover `previous()` e `next()` (ficam sem uso; `setIndice` permanece)
- [x] 1.4 Ajustar `PhotoViewerViewModelTest`: remover/adaptar o teste `navegaAnteriorESeguinte`

## 2. Verificação

- [x] 2.1 Correr `./gradlew assembleDebug lintDebug testDebugUnitTest` e confirmar build verde
- [x] 2.2 Correr `./gradlew connectedDebugAndroidTest` e confirmar que os testes do viewer passam
