## Context

A página de detalhe (`ClientDetailScreen.kt`) mostra o mapa do cliente num `Box` de 200dp com `OsmMap(interactive = false, draggable = false)`. A app já tem um padrão de ecrã full-screen dedicado: `PhotoViewer` (NavKey com argumentos + `entry<PhotoViewer>` em `Navigation.kt` + ecrã próprio em `ui/detalhe/`). O componente `OsmMap` já suporta pan/zoom via `interactive = true` e marcador fixo via `draggable = false`.

O objetivo é reutilizar o padrão existente (o menos código novo possível), sem tocar no servidor nem em dependências.

## Goals / Non-Goals

**Goals:**
- Tocar no mapa da detail abre um ecrã full-screen com o mapa interativo.
- Viewer full-screen: pan/zoom livre, marcador fixo na localização do cliente, botão voltar.
- Reutilizar `OsmMap`, `PhotoViewer` como modelo de ecrã e o `NavKey`/`entry` como modelo de navegação.

**Non-Goals:**
- Não alterar o picker de localização do form (`MapPicker`) nem o comportamento do form.
- Não adicionar interação de marcador no viewer (o marcador é de visualização apenas).
- Sem alterações ao servidor, à sincronização ou a dependências.

## Decisions

**D1: Novo ecrã dedicado (rota) em vez de Dialog**
O `MapPicker` usa `Dialog`, mas é um picker modal com confirmar/cancelar. O viewer é de visualização (sem confirmação) e deve ocupar o ecrã todo como o `PhotoViewer`. Seguir o padrão `PhotoViewer`: nova `NavKey MapFullscreen(clienteId)`, novo `entry<MapFullscreen>` em `Navigation.kt`, ecrã em `ui/detalhe/MapFullscreenScreen.kt` (ou `ui/map/`).
- *Alternativas consideradas*: Dialog como o `MapPicker` — rejeitado porque o viewer não pede confirmação e o padrão da app para visualização em ecrã cheio é uma rota (`PhotoViewer`).

**D2: O mapa da detail continua não-interativo; a área clicável é um overlay**
O `OsmMap` com `interactive = false` usa `NonInteractiveMapView` que consome todos os toques. Para o mapa responder ao toque sem reativar pan/zoom, envolver o `Box` num `Card(onClick = ...)` (ou `Modifier.clickable`). O toque abre a rota `MapFullscreen`; o mapa em si permanece estático no ecrã de detalhe.
- *Alternativa*: passar `interactive = true` na detail — rejeitado, entra em conflito com a cena "Detail map is not directly interactive".

**D3: Viewer full-screen usa `OsmMap(interactive = true, draggable = false)`**
O componente já tem exatamente este modo: pan/zoom ativos, marcador fixo. O `PhotoViewerScreen` serve de modelo de layout (fundo, botão voltar sobreposto).

**D4: Dados por NavKey, sem ViewModel novo**
Como no `PhotoViewer` (sem ViewModel — o `PhotoViewerViewModel` só guarda índice), o viewer precisa apenas do `clienteId` e das coordenadas. As coordenadas podem ser obtidas do repositório (`getCliente`) ou, mais simples, carregadas no `ClientDetailContent` e passadas pela NavKey. Decisão de implementação: `MapFullscreen(clienteId)` e o ecrã lê o cliente do repositório — mantém a navegação serializável (necessário para o navigation3) sem depender de estado partilhado.
- *Alternativa*: passar lat/long na NavKey — rejeitado porque as coordenadas podem estar desatualizadas entre o toque e a renderização e o navigation3 exige `@Serializable` simples.

## Risks / Trade-offs

- [Card clicável no `Box` do mapa pode capturar toques destinados ao mapa] → O mapa é não-interativo na detail; o `Card(onClick)` é o único alvo de toque, comportamento previsível.
- [Criar `MapView` extra (o viewer reutiliza o `rememberMapViewWithLifecycle`) implica descarga/recarga de tiles ao navegar] → Aceitável: mesmo comportamento do `PhotoViewer`/`MapPicker`; o osmdroid gere a cache de tiles.
- [Nova rota aumenta a pilha de navegação] → Padrão já existente (`PhotoViewer`); o back stack é gerido pelo navigation3.

## Migration Plan

- Não aplicável (mudança apenas na UI da app, sem dados nem servidor). Rollback: reverter as alterações de UI.

## Open Questions

- Nenhuma que altere specs, abordagem ou tarefas.