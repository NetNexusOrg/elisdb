## Context

O visualizador de fotos (`PhotoViewerScreen.kt`) mostra uma `AsyncImage` (Coil) com `ContentScale.Fit` dentro de um `Box` a preto, com setas de navegação e contador geridos pelo `PhotoViewerViewModel` (índice `previous`/`next`). Não existem gestos hoje: a navegação é só por toque nas setas e não há zoom.

O projeto usa Compose (BOM 2026.03.01) com Material3 e Coil 3 para carregamento de imagens. Não há dependência de terceiros para gestos/zoom; as APIs standard do Compose (`detectTransformGestures`, `transformable`, `HorizontalPager`) cobrem o necessário.

## Goals / Non-Goals

**Goals:**
- Swipe horizontal para trocar de foto, mantendo as setas e o contador como alternativa.
- Zoom por pinça na foto, com reset do zoom ao mudar de foto.
- Manter o comportamento atual quando há uma só foto (sem swipe nem zoom que perturbem).

**Non-Goals:**
- Não adicionar double-tap para zoom, rotação de imagem, ou zoom que ultrapasse limites extremos.
- Não alterar a galeria do ecrã de detalhe nem o carregamento das fotos (Coil/AsyncImage).
- Não tocar no `PhotoViewerViewModel` para além do que já faz (índice já é suficiente).

## Decisions

**1. Swipe via `HorizontalPager` em vez de gestos manuais de arrasto.**
A opção mais simples seria `Modifier.pointerInput { detectHorizontalDragGestures(...) }` e chamar `viewModel.next()/previous()`. No entanto, `HorizontalPager` (Compose Foundation) dá transição animada entre páginas, posicionamento natural e já resolve a coerção de limites. Alternativa considerada: `detectDragGestures` manual — rejeitada por exigir mais estado e não dar animação.

**2. Zoom via `Modifier.transformable` + `rememberTransformableState`.**
Cada página do pager usa um `Modifier.transformable` com estado de zoom/offset locais (`remember { mutableStateOf(1f) }` e deslocamento). A pinça usa `detectTransformGestures` (escala) para atualizar o fator de zoom, limitado entre 1x e um máximo (ex. 4x). Alternativa considerada: biblioteca de zoom (ex. Zoomable) — rejeitada para não introduzir dependência.

**3. Zoom reset por página.**
O estado de zoom fica associado a cada página (via `key(foto.id)` e estado `remember`), por isso ao mudar de foto o novo composable começa em 1x — cumpre o cenário "Zoom resets when changing photo" sem lógica extra no ViewModel.

**4. Interação pager vs. zoom.**
O zoom por pinça é capturado dentro da página (gesto multi-toque), e o pager só reage a arrasto horizontal de um toque. Com `transformable` a coexistir com o `HorizontalPager`, o gesto de pinça é consumido pelo transform e o swipe de um dedo pelo pager. Se houver conflito, o zoom é aplicado ao contentor da imagem e o pager continua a receber o arrasto horizontal.

## Risks / Trade-offs

- [Conflito de gestos entre swipe do pager e zoom por pinça] → Usar `transformable` no contentor da imagem (pinça é multi-toque, pager reage a um toque); validar manualmente no emulador que arrastar com um dedo troca de foto e a pinça faz zoom sem disparar swipe.
- [Zoom numa imagem com `ContentScale.Fit` pode sair dos limites visuais] → Aplicar transformação de escala/offset à `AsyncImage` e limitar o offset para não perder a imagem de vista; manter `Fit` para que a foto comece enquadrada.
- [Desempenho ao carregar várias páginas full-res] → `HorizontalPager` com `beyondViewportPageCount` pequeno (default) e Coil a descarregar lazy; as fotos já são WebP comprimidas.
- [Acessibilidade: swipe pode ser difícil] → As setas e o contador permanecem como alternativa acessível.

## Migration Plan

Sem migração de dados nem de estado persistente. A mudança é só na UI do visualizador. Rollback trivial: reverter `PhotoViewerScreen.kt` para a versão com setas.

## Open Questions

Nenhuma.
