## 1. Navegação

- [x] 1.1 Adicionar `@Serializable data class MapFullscreen(val clienteId: String) : NavKey` em `NavigationKeys.kt`
- [x] 1.2 Adicionar `entry<MapFullscreen>` em `Navigation.kt` ligando ao ecrã full-screen com `onBack = { backStack.removeLastOrNull() }`

## 2. Ecrã full-screen

- [x] 2.1 Criar `MapFullscreenScreen` (em `ui/detalhe/` ou `ui/map/`) que lê o cliente do repositório pelo id e mostra `OsmMap(interactive = true, draggable = false)` centrado nas coordenadas do cliente
- [x] 2.2 Incluir botão voltar sobreposto (modelo `PhotoViewerScreen`) e estado de carregamento/sem localização

## 3. Página de detalhe

- [x] 3.1 Envolver o `Box` do mapa em `Card(onClick = ...)` em `ClientDetailScreen` para abrir a rota `MapFullscreen(clienteId)` ao tocar, mantendo o `OsmMap` não-interativo
- [x] 3.2 Ligar o `onMapClick` ao callback `onOpenFullscreenMap` passado ao `ClientDetailContent` desde o `ClientDetailScreen`

## 4. Verificação

- [x] 4.1 Compilar com `./gradlew :app:assembleDebug` e correr os testes unitários existentes
- [x] 4.2 Testar no emulador: tocar no mapa da detail abre o viewer full-screen; pan/zoom funcionam; o marcador mantém-se fixo; voltar regressa à detail