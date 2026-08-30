## 1. Swipe entre fotos

- [x] 1.1 Substituir a `AsyncImage` única de `PhotoViewerScreen` por um `HorizontalPager` paginado sobre `state.fotos`, mantendo `state.indice` como página inicial e o fundo preto
- [x] 1.2 Sincronizar o pager com `state.indice`: ao deslizar, atualizar o índice do ViewModel; manter `previous`/`next` coerentes com o pager
- [x] 1.3 Manter as setas laterais e o contador "n / total" funcionais, agora apontando para a página do pager

## 2. Zoom por pinça

- [x] 2.1 Adicionar zoom por pinça em cada página via gesto de pinça (`pointerInput` com `calculateZoom`), aplicando escala à imagem
- [x] 2.2 Limitar o zoom a um intervalo razoável (1x a 4x) e garantir que a foto em `ContentScale.Fit` começa enquadrada
- [x] 2.3 Garantir reset do zoom ao mudar de foto (estado de zoom por página, `key(foto.id)`)

## 3. Caso de uma só foto / vazio

- [x] 3.1 Confirmar que com uma única foto não há swipe nem contador e que o zoom continua a funcionar
- [x] 3.2 Manter o estado "Sem fotos para mostrar." e o indicador de carregamento inalterados

## 4. Verificação

- [x] 4.1 Correr `./gradlew assembleDebug lintDebug` e confirmar build verde
- [x] 4.2 Correr `./gradlew testDebugUnitTest` e confirmar que os testes unitários passam
- [x] 4.3 Adicionar/ajustar teste instrumentado em `ClientDetailScreenTest` cobrindo swipe entre fotos e abertura do viewer
- [x] 4.4 Smoke-test manual no emulador: swipe esquerda/direita troca de foto, pinça faz zoom in/out, zoom repõe ao mudar de foto e setas/contador continuam a funcionar
