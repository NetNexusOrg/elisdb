## Why

Após o zoom por pinça, o utilizador não consegue deslocar a foto para ver outras zonas: o gesto de arrasto de um dedo está sempre reservado ao swipe entre fotos, mesmo com a imagem ampliada. Isso torna o zoom pouco útil para inspecionar detalhes.

## What Changes

- No visualizador de fotos, quando a foto está com zoom aplicado, arrastar com um dedo passa a **deslocar (pan)** a imagem em vez de mudar de foto.
- O pan é limitado aos limites da imagem para que a foto não saia totalmente do ecrã.
- Com zoom a 1x, arrastar continua a trocar de foto (swipe), mantendo o comportamento atual.
- O zoom e o pan são repostos ao mudar de foto.

## Capabilities

### New Capabilities

- Nenhuma.

### Modified Capabilities

- `client-screens`: o requisito "Fullscreen photo viewer" passa a incluir o pan da foto quando está com zoom.

## Impact

- `app/src/main/java/com/example/elisdb/ui/detalhe/PhotoViewerScreen.kt`: adicionar estado de offset (pan) e tratar o arrasto de um dedo quando `scale > 1f`, consumindo o gesto nesse caso e deixando o swipe para o pager quando `scale == 1f`.
- Sem novas dependências (gestos via APIs standard do Compose).
- Testes: adicionar/ajustar teste instrumentado em `ClientDetailScreenTest` para o pan com zoom.
