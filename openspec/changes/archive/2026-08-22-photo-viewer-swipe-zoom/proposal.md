## Why

No fullscreen photo viewer, ao abrir uma foto, o utilizador só consegue trocar de foto tocando nas setas laterais e não pode aproximar a imagem para ver detalhes. Isso torna a consulta de fotos pouco natural em comparação com os gestos habituais dos telemóveis (swipe e pinça).

## What Changes

- O visualizador de fotos em ecrã inteiro passa a responder a um **swipe horizontal** (esquerda/direita) para mudar para a foto anterior/seguinte, além das setas já existentes.
- O visualizador passa a suportar **zoom por gesto de pinça** (zoom in/out) na foto apresentada, mantendo a possibilidade de ver a foto em modo fit.
- As setas e o contador de fotos mantêm-se, servindo de alternativa aos gestos.

## Capabilities

### New Capabilities

- Nenhuma.

### Modified Capabilities

- `client-screens`: o requisito "Fullscreen photo viewer" muda para incluir a navegação por swipe entre fotos e o zoom por gesto de pinça.

## Impact

- `app/src/main/java/com/example/elisdb/ui/detalhe/PhotoViewerScreen.kt`: adicionar gestos de swipe e pinça ao componente de imagem; ajustar a forma como a imagem é apresentada (transformações de escala/translação).
- `app/src/main/java/com/example/elisdb/ui/detalhe/PhotoViewerViewModel.kt`: manter a lógica de índice anterior/seguinte já existente; sem alterações obrigatórias.
- Testes: `PhotoViewerViewModelTest` e o instrumentado `ClientDetailScreenTest` podem precisar de novos casos.
- Sem novas dependências (gestos via APIs standard do Compose).
