## Why

No visualizador de fotos, a troca de foto já é feita por swipe horizontal; os botões de seta `<` e `>` ficam redundantes, ocupam espaço sobre a imagem e poluem a interface.

## What Changes

- Remover os botões de seta anterior/seguinte (`<` e `>`) do visualizador de fotos, mantendo o swipe entre fotos e o contador "n / total".
- Remover o código que ficou sem uso (parâmetros `onPrevious`/`onNext` do `PhotoPager` e os métodos `previous()`/`next()` do `PhotoViewerViewModel`), bem como o teste unitário que os cobria.

## Capabilities

### New Capabilities

- Nenhuma.

### Modified Capabilities

- Nenhuma. Mudança de UI interna (a especificação `client-screens` não menciona os botões de seta). Declarada com `skip_specs: true`.

## Impact

- `app/src/main/java/com/example/elisdb/ui/detalhe/PhotoViewerScreen.kt`: remover os dois `IconButton` de seta e os parâmetros/argumentos `onPrevious`/`onNext`.
- `app/src/main/java/com/example/elisdb/ui/detalhe/PhotoViewerViewModel.kt`: remover `previous()` e `next()` (só eram usados pelas setas).
- `app/src/test/java/com/example/elisdb/ui/detalhe/PhotoViewerViewModelTest.kt`: remover/ajustar o teste de navegação anterior/seguinte.
