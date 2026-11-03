# Tasks

## 1. Pins adaptativos

- [x] 1.1 Remover clustering do source/layers e criar layers de pins e bolas, verificando que o projeto compila.
- [x] 1.2 Implementar escala por densidade e troca pin→bola, verificando com testes unitários de `pinScale` e `computePinDisplays`.
- [x] 1.3 Manter o cliente selecionado com pin completo e etiqueta, verificando visualmente que o pin cresce ao selecionar uma bola.
- [x] 1.4 Separar coordenadas exatamente iguais em leque a partir do zoom 14, verificando com teste unitário e no emulador.
- [x] 1.5 Atualizar o hit-test para pins e bolas e recalcular no `onCameraIdle`, verificando que tocar numa bola abre o cliente correto.

## 2. Verificação e spec

- [x] 2.1 Correr `:app:testDebugUnitTest`, `:app:lintDebug` e `:app:assembleDebug`, confirmando sucesso.
- [x] 2.2 Correr `:app:connectedDebugAndroidTest`, confirmando 138 testes sem falhas.
- [x] 2.3 Verificar no emulador com host GPU pins completos, pins reduzidos/bolas, seleção e etiquetas.
- [x] 2.4 Atualizar a spec `client-map` e validar a change com `openspec validate map-pin-density`.

## Workflow follow-up

- Arquivar a change após revisão e confirmar que a spec principal ficou sincronizada.
