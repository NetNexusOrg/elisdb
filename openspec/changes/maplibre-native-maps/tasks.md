# Tasks

## 1. Dependência e inicialização

- [x] 1.1 Adicionar `maplibre = "13.7.1"` e o alias
  `maplibre-android = { module = "org.maplibre.gl:android-sdk-opengl" }` ao
  `gradle/libs.versions.toml`, trocar a dependência de mapas em
  `app/build.gradle.kts` e confirmar que
  `cd elisdb-android && ./gradlew :app:assembleDebug` resolve o artefacto e
  compila.
- [x] 1.2 Chamar `MapLibre.getInstance(this)` no `ElisdbApp.onCreate`, mantendo
  temporariamente a inicialização do osmdroid, e confirmar que a app arranca
  sem `MapLibreConfigurationException` e que o mapa antigo continua a abrir.
- [x] 1.3 Registar no `design.md`/`tasks.md` a versão e o backend escolhidos e
  verificar que a dependência resolve apenas
  `org.maplibre.gl:android-sdk-opengl:13.7.1` (sem o artefacto Vulkan implícito)
  com `./gradlew :app:dependencies --configuration debugRuntimeClasspath`.

## 2. Infraestrutura partilhada e `ElisMap`

- [x] 2.1 Criar `ui/map/ElisMapSupport.kt` com a constante
  `MAP_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"`, o estado
  `Loading/Ready/Failed`, o helper de mutações adiadas até `OnStyleLoaded`, o
  helper `applyWhenLaidOut` e a `NonInteractiveMapView`; verificar com testes
  unitários dos helpers puros e com `:app:assembleDebug`.
- [x] 2.2 Implementar `ui/map/ElisMap.kt` com a mesma API de `OsmMap`, mas com
  `LatLng`, câmara inicial zoom 16, pin e localização atual via
  `GeoJsonSource`/`SymbolLayer`, toque no mapa, estado de erro com retry e
  atribuição/logo visíveis; verificar manualmente num emulador que os tiles
  carregam, que o pin aparece e que o toque devolve coordenadas.
- [x] 2.3 Implementar o ciclo de vida completo do `MapView`
  (`onCreate/onStart/onResume/onPause/onStop/onDestroy`) com sincronização ao
  estado atual do `LifecycleOwner` e proteção contra chamadas duplicadas;
  verificar com rotação de ecrã e navegação para trás que não há crash nem
  ecrã preto.
- [x] 2.4 Renomear `OsmMapScrollTest` para `MapScrollTest`, atualizar para
  `LatLng`/`ElisMap` e confirmar que
  `./gradlew :app:connectedDebugAndroidTest` executa
  `mapaNaoInterativoNaoEngoleScrollVertical` com sucesso.
- [x] 2.5 Trocar a pré-visualização do formulário (`ClientFormScreen`) para
  `ElisMap(interactive = false)` e confirmar que `MapPickerFormTest` continua a
  abrir o seletor ao tocar em `map_preview`.
- [x] 2.6 Trocar o cartão do detalhe (`ClientDetailScreen`) e o visualizador
  completo (`MapFullscreenScreen`) para `ElisMap`;
  confirmar que o cartão não interceta o scroll, que o toque abre o
  visualizador e que `ClientDetailScreenTest` passa.

## 3. Marcadores e mapa de clientes

- [x] 3.1 Implementar `ui/map/ElisMapMarkers.kt` com `MapPin.position: LatLng`,
  source/layer `client-pins`/`client-pins-layer`, imagens
  `pin-active`/`pin-inactive`/`user-location`, etiquetas com nome, seleção por
  `queryRenderedFeatures` e `fitPins` com padding de 96 px; adicionar testes
  unitários para os construtores de features/assinatura de pins e verificar que
  passam com `:app:testDebugUnitTest`.
- [x] 3.2 Trocar `ClientMapScreen` para `ElisMapMarkers` e `LatLng`;
  confirmar com `ClientMapScreenTest` e manualmente que os pins, os nomes, os
  estados ativo/inativo, a seleção e o cartão inferior funcionam.
- [x] 3.3 Ajustar `MapFullscreenScreen` para receber `LatLng` e localização
  atual, e confirmar manualmente que o mapa centra no cliente, mostra o pin e a
  localização atual sem recentrar quando a localização chega depois.
- [ ] 3.4 Verificar o comportamento de `fitPins` com zero, um e vários pins,
  com e sem localização atual, e confirmar que o filtro All/Active/Inactive
  atualiza apenas os pins e não a câmara.

## 4. Seletor com pin arrastável

- [x] 4.1 Implementar `DraggableMarkerController` com hit-test de ~48 dp,
  atualização do feature `picked-location` durante o movimento, cancelamento
  com multi-toque e reposição das gestures no fim; adicionar testes unitários
  para o hit-test e confirmar que passam.
- [x] 4.2 Trocar `MapPicker` para `ElisMap`, ligar o controlador e o toque no
  mapa para colocar o pin; confirmar manualmente que arrastar atualiza as
  coordenadas, que tocar no mapa move o pin e que confirmar/cancelar mantêm o
  comportamento atual.
- [x] 4.3 Confirmar que `MapPickerFormTest` continua a passar, que o pin
  inicial aparece quando já existem coordenadas e que o pin fica em
  `DEFAULT_CENTER` quando não existem.

## 5. Limpeza do osmdroid e documentação

- [x] 5.1 Remover a dependência e o alias do osmdroid,
  `Configuration.getInstance().userAgentValue`, `osmdroidUserAgent()` e o bloco
  ProGuard do osmdroid; apagar `OsmMap.kt` e `OsmMapMarkers.kt`; confirmar com
  `rg -n "osmdroid|OsmMap|GeoPoint" elisdb-android` que não restam referências
  de código e que `:app:assembleDebug` compila.
- [x] 5.2 Atualizar `elisdb-android/README.md`, `elisdb-android/docs/uso.md` e
  `CHANGELOG.md` para descrever MapLibre Native/OpenFreeMap no lugar do
  osmdroid/OSM raster; confirmar que os comandos e caminhos documentados
  continuam válidos.
- [x] 5.3 Verificar o manifesto fundido e confirmar que
  `ACCESS_NETWORK_STATE`/`ACCESS_WIFI_STATE` foram adicionados, que
  `INTERNET`/localização já existiam e que não há novas permissões de runtime.
- [ ] 5.4 Verificar que não ficaram regras ProGuard globais de MapLibre e que
  as regras de consumo do AAR são suficientes para o build minificado.

## 6. Integração, release e validação final

- [x] 6.1 Executar `./gradlew :app:testDebugUnitTest :app:lintDebug
  :app:assembleDebug` e confirmar que todos os testes, o lint e o build passam.
- [x] 6.2 Executar os testes instrumentados de mapa, formulário e detalhe
  (`MapScrollTest`, `MapPickerFormTest`, `ClientMapScreenTest`,
  `ClientDetailScreenTest`) num emulador com rede e confirmar que passam.
- [ ] 6.3 Construir o release minificado no CI ou localmente com as variáveis
  `KEYSTORE_*` e fazer o smoke test: sync, mapa, fotos e persistência continuam
  funcionais; confirmar que o mapa renderiza após R8.
- [ ] 6.4 Fazer a verificação manual final: tiles, pan/zoom, pin selecionado,
  etiquetas, ativo/inativo, localização atual, fit, seletor (toque e arrasto),
  permissão concedida/negada, cache/offline, tema claro/escuro, rotação e
  atribuição/logo visíveis.
- [x] 6.5 Revisar `git diff` e o tamanho do APK, confirmar que não há secrets nem
  ficheiros gerados e registar o resultado antes de pedir confirmação para
  commit.

## Workflow follow-up

- Sincronizar a spec `app-build` e arquivar a change após a implementação e a
  verificação do release.
- Confirmar o resultado arquivado com `openspec validate --archived` ou o
  comando equivalente do projeto.
