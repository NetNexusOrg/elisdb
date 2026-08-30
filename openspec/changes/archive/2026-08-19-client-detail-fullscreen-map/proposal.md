## Why

Na página de detalhe do cliente o mapa ocupa apenas 200dp e não é interativo. Quando o utilizador precisa de ver a localização com contexto à volta (ruas, zonas vizinhas) ou simplesmente olhar melhor, não tem forma de o fazer sem sair da app para o navegador. Tocar no mapa deve abrir um ecrã de mapa em full screen para inspeção.

## What Changes

- A página de detalhe passa a permitir tocar no mapa para abrir um ecrã full-screen dedicado.
- Novo ecrã de visualização de mapa em full screen: interativo (pan/zoom), marcador não-dragável, botão voltar.
- A cena existente "Detail map is not interactive" é alterada: o mapa de detalhe deixa de ser puramente estático — tocar nele abre o viewer full-screen (nunca um picker).
- Navegação: nova rota `MapFullscreen` seguindo o padrão existente (`PhotoViewer`).

## Capabilities

### New Capabilities
- Nenhuma. A visualização full-screen é comportamento da capability de localização existente.

### Modified Capabilities
- `client-location`: a requirement "Display a location on a map" muda — o mapa de detalhe passa a abrir o viewer full-screen ao tocar; e é adicionada uma requirement nova "View a client location in fullscreen" descrevendo o comportamento do ecrã full-screen.

## Impact

- `NavigationKeys.kt`: nova `NavKey` (`MapFullscreen(clienteId)`).
- `Navigation.kt`: novo `entry<MapFullscreen>`.
- `ClientDetailScreen.kt`: mapa da detail fica clicável (o `OsmMap` em si continua não-interativo no ecrã de detalhe).
- Novo ficheiro de ecrã full-screen (padrão do `PhotoViewerScreen`), reutilizando `OsmMap`.
- Sem impacto no servidor, sem novas dependências.