## Why

Depois de aplicar zoom por pinça, arrastar a foto com um dedo desloca-a mais devagar do que o dedo (a imagem não acompanha o dedo 1:1). A causa não é o pager (já corrigido em `fix-photo-viewer-pan-tracking`), mas o cálculo do pan: o delta do dedo é lido num espaço de coordenadas já escalado pelo zoom, pelo que a imagem se move `1/scale`× a velocidade do dedo.

## What Changes

- No visualizador de fotos, o pan de uma foto ampliada passa a acompanhar exatamente o dedo (1:1), multiplicando o delta do gesto pela escala atual antes de o somar ao offset.
- A correção aplica-se tanto ao arrasto de um dedo com zoom como ao pan durante a pinça.
- O limite do pan (`clampOffset`) continua a impedir que a foto saia das bordas, agora com o offset expresso em píxeis de ecrã de forma consistente.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

Nenhuma. A especificação `client-screens` já exige que "the photo SHALL pan to follow the drag"; a mudança só torna esse comportamento efetivo. Declarada com `skip_specs: true`.

## Impact

- `app/src/main/java/com/example/elisdb/ui/detalhe/PhotoViewerScreen.kt`: no `ZoomablePhoto`, multiplicar o `calculatePan()` pela escala (`scale.floatValue`) em ambos os ramos do gesto antes de atualizar o offset.
- Testes: reforçar/adicionar teste instrumentado em `ClientDetailScreenTest` que verifica que o arrasto com zoom desloca a imagem na mesma proporção do dedo (não apenas que não troca de foto).
- Sem novas dependências.
