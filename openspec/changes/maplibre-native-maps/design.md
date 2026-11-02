# Design

## Context

Ver `proposal.md` — Why, para a motivação. O estado atual relevante para o
desenho é:

- `OsmMap.kt` e `OsmMapMarkers.kt` usam osmdroid 6.1.20 com tiles raster
  `MAPNIK`, `AndroidView`, anotações `Marker` e uma subclasse `LabeledMarker`
  que desenha a etiqueta do cliente à mão.
- `OsmMap` expõe `GeoPoint`, modo interativo/não interativo, pin arrastável,
  toque no mapa e localização atual. `OsmMapMarkers` expõe `MapPin`, seleção de
  pin, etiquetas sempre visíveis, estilos ativo/inativo, localização atual e
  `fitPins`.
- Os chamadores são `MapPicker`, `ClientMapScreen`, `MapFullscreenScreen`,
  `ClientDetailScreen` e `ClientFormScreen`. O detalhe e a pré-visualização do
  formulário usam mapas não interativos; o mapa de clientes, o visualizador
  completo e o seletor usam mapas interativos.
- `OsmMapScrollTest` é o teste de regressão que garante que um mapa não
  interativo não engole o scroll vertical do Compose. `MapPickerFormTest`,
  `ClientMapScreenTest` e `ClientDetailScreenTest` também instanciam mapas.
- O comportamento funcional está fixado em `openspec/specs/client-map/spec.md` e
  `openspec/specs/client-location/spec.md`; a migração não os altera.
- Restrições: Android View API dentro de Compose, ciclo de vida da Activity,
  minSdk 24, build release com R8, sem chave de API, documentação em português.

MapLibre Native 13.7.1 é a linha estável escolhida. O artefacto
`org.maplibre.gl:android-sdk-opengl` traz o backend OpenGL ES, as bibliotecas
nativas por ABI e regras ProGuard de consumo. `MapView` estende `FrameLayout`,
tem o seu próprio ciclo de vida e exige `MapLibre.getInstance(context)` antes de
ser criado; caso contrário lança `MapLibreConfigurationException`. O estilo e as
fontes são carregados de forma assíncrona, pelo que todas as mutações de
`Style` têm de esperar pelo callback de carregamento.

## Goals / Non-Goals

**Goals:**

- Paridade funcional com o mapa atual: pins com nome, estados ativo/inativo,
  localização atual, seleção, `fitPins`, pin arrastável, toque para colocar pin
  e mapa não interativo que não intercepta gestos.
- Manter a arquitetura: composables finos, `AndroidView`, ciclo de vida ligado
  ao `LifecycleOwner`, sem introduzir ViewModel ou camada de estado de mapa.
- Trocar a dependência e a implementação sem alterar o comportamento descrito
  nas specs `client-map` e `client-location`.
- Garantir que o build minificado continua a renderizar o mapa.
- Deixar um único ponto de configuração do estilo, sem chave de API.

**Non-Goals:**

- Mapas offline por região, download de tiles ou gestão de cache pelo
  utilizador.
- Alterar a UX, os filtros, a seleção de clientes ou os requisitos das specs.
- Suportar vários fornecedores de tiles em runtime ou uma abstração genérica de
  mapas.
- Migrar para o wrapper `maplibre-compose` (beta) ou para o backend Vulkan.
- Alterações ao servidor, base de dados, sync ou modelo de dados.
- Clustering, rotas, navegação ou novos tipos de anotação.

## Decisions

### 1. MapLibre Native Android View API com backend OpenGL

Usar `org.maplibre.gl:android-sdk-opengl:13.7.1` e manter o padrão
`AndroidView` + `MapView`, em vez de adotar `maplibre-compose`.

- **Porquê:** o wrapper atual já é uma View, o ciclo de vida e o modo não
  interativo estão resolvidos nessa camada, e a API View é a via estável e
  documentada. O backend OpenGL funciona em todos os dispositivos suportados e
  evita a matriz de risco do Vulkan.
- **Alternativas rejeitadas:** `android-sdk` (Vulkan por omissão) exclui
  dispositivos sem Vulkan 1.0; `android-sdk-vulkan-opengl` duplica backends e
  aumenta o APK (~10 MB), ficando como otimização futura; `maplibre-compose`
  0.20.0 é beta e exige artefactos de runtime adicionais.

### 2. Inicialização única na Application

Chamar `MapLibre.getInstance(this)` no `ElisdbApp.onCreate`, antes de qualquer
`MapView`; remover `Configuration` e `osmdroidUserAgent()`.

- **Porquê:** a inicialização é global e obrigatória; a Application é o ponto
  garantido antes de qualquer composable. O User-Agent por omissão do MapLibre
  já inclui o package e a versão da app, suficiente para o uso online do
  OpenFreeMap. Não se personaliza OkHttp nem se adiciona interceptor de UA.
- **Alternativa rejeitada:** inicializar dentro de cada composable criaria
  corridas e chamadas duplicadas; um UA personalizado só é necessário se se
  voltar a usar tiles raster do OSM ou downloads offline.

### 3. Estilo OpenFreeMap Liberty como predefinição

Definir uma constante única, por exemplo
`internal const val MAP_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"`,
e usá-la em `ElisMap` e `ElisMapMarkers`.

- **Porquê:** é vetorial, gratuito, sem chave nem registo, com atribuição
  automática e cobertura adequada. O estilo Liberty usa o sprite
  `https://tiles.openfreemap.org/sprites/ofm_f384/ofm`, glifos em
  `https://tiles.openfreemap.org/fonts/{fontstack}/{range}.pbf`, fonte
  `Noto Sans Bold` e o source vetorial `https://tiles.openfreemap.org/planet`
  (maxzoom 14, sobre-ampliado até 16).
- **Troca de fornecedor:** a constante é o único ponto a alterar para um estilo
  raster OSM local (`sources.osm` com
  `https://tile.openstreetmap.org/{z}/{x}/{y}.png`) ou para um estilo alojado
  pelo projeto. Não há fallback automático entre fornecedores, para evitar
  comportamento imprevisível e violações de política.
- **Falha de estilo:** mostrar “Não foi possível carregar o mapa.” com botão de
  nova tentativa; nunca deixar o spinner indefinidamente.

### 4. Tipos públicos e nomes

Substituir `GeoPoint` por `org.maplibre.android.geometry.LatLng` diretamente e
renomear os composables para `ElisMap` e `ElisMapMarkers`; `MapPin.position`
passa a `LatLng`; `DEFAULT_CENTER` passa a `LatLng(38.7223, -9.1393)`.

- **Porquê:** é a migração mínima, sem uma camada de adaptação que acrescente
  conversões em todos os chamadores. O único código afetado é interno à app.
- **Alternativa rejeitada:** um tipo próprio `MapPoint` isolava o SDK, mas
  obrigava a conversões e a manter dois modelos durante toda a migração.

### 5. Pins e etiquetas com GeoJSON + SymbolLayer

Representar pins, localização atual e pin do seletor com `GeoJsonSource` +
`SymbolLayer`, não com as anotações `Marker` do SDK.

- **Porquê:** as anotações não têm etiqueta sempre visível nem arrastar; o
  GeoJSON permite ícone, nome, estado e seleção no mesmo feature, com
  propriedades lidas por expressões.
- **IDs:** sources/layers `client-pins`/`client-pins-layer`,
  `picked-location`/`picked-location-layer`,
  `user-location`/`user-location-layer`. Imagens registadas no estilo:
  `pin-active`, `pin-inactive`, `user-location`.
- **Propriedades:** `id`, `name`, `icon`. A camada usa
  `iconImage(Expression.get("icon"))`, `textField(Expression.get("name"))`,
  `textFont(arrayOf("Noto Sans Bold"))`, âncora do ícone `BOTTOM`, texto acima
  do ícone com `textOffset` negativo, halo branco para legibilidade e
  `iconAllowOverlap`/`textAllowOverlap` para manter todos os nomes visíveis.
- **Seleção:** `queryRenderedFeatures` numa pequena `RectF` à volta do toque,
  filtrando por `client-pins-layer`, e leitura da propriedade `id`. Isto
  substitui `setOnMarkerClickListener` e mantém o comportamento de tocar no pin.
- **Localização atual:** um feature separado, com ícone circular
  `user-location`, sem etiqueta e sem interação.
- **Alternativa rejeitada:** anotações `Marker` mais um overlay de texto
  desenhado à mão repetiria o `LabeledMarker` atual e voltaria a ter problemas
  de posicionamento em zoom/rotação.

### 6. Máquina de estados de carregamento e mutações adiadas

Cada mapa tem um estado interno `Loading`, `Ready` ou `Failed`. Enquanto o
estilo não estiver carregado, as mutações (imagens, sources, layers, câmara e
features) ficam guardadas em refs e são aplicadas uma única vez no callback
`Style.OnStyleLoaded`; o estado mais recente é sempre o aplicado.

- **Porquê:** `setStyle` é assíncrono e `Style` não está disponível antes do
  callback. Sem esta barreira há `NullPointerException`/perda de pins.
- **UI:** o progresso só desaparece quando o estilo está pronto e a View tem
  dimensões; em falha aparece erro com retry. O retry volta a chamar `setStyle`
  e reaplica imagens, sources, layers e o estado atual.
- **Segurança:** as atualizações são idempotentes e podem correr antes do
  estilo; nunca removem um source/layer que ainda não existe.
- **Listeners:** usar `rememberUpdatedState` para os callbacks e remover
  listeners no `onDispose`.

### 7. Ciclo de vida completo do MapView

`rememberMapViewWithLifecycle` passa a sincronizar o `MapView` com o
`LifecycleOwner`: `onCreate(null)` uma vez, depois `onStart`/`onResume`/`onPause`
/`onStop`/`onDestroy` na ordem correta.

- Se o lifecycle já estiver em `STARTED`/`RESUMED` no momento da criação, os
  eventos correspondentes são chamados imediatamente.
- Flags `started`, `resumed` e `destroyed` evitam chamadas duplicadas e
  `onDestroy` prematuro.
- No `onDispose`: remover o observer, fazer `pause`/`stop`/`destroy` apenas se
  ainda não foram feitos.
- **Porquê:** o osmdroid só precisava de `onResume`/`onPause`/`onDetach`; o
  MapLibre tem um ciclo de vida mais completo e não o chamar corretamente
  provoca fugas de recursos nativos e crashes ao recriar a Activity.
- **Alternativa rejeitada:** usar apenas `ON_RESUME`/`ON_PAUSE`, como hoje,
  deixaria o mapa num estado inconsistente após rotação.

### 8. Modo não interativo

Criar `NonInteractiveMapView : MapView` que devolve `false` em
`dispatchTouchEvent` e `onTouchEvent`, tal como o atual
`NonInteractiveMapView`, e desativar todas as gestures via
`uiSettings.setAllGesturesEnabled(false)`. O mapa continua a renderizar e a
atualizar a câmara programaticamente.

- **Porquê:** o `MapView` marca-se como clickable e chama
  `requestDisallowInterceptTouchEvent(true)` na inicialização; sem esta
  subclasse o scroll do Compose e o clique do cartão de detalhe deixam de
  funcionar. É o comportamento coberto por `MapScrollTest`.

### 9. Pin arrastável do seletor

Implementar um `DraggableMarkerController` na app, ligado ao `MapView` por
`setOnTouchListener`, em vez de portar o `DraggableSymbolsManager` completo do
exemplo oficial.

- No `DOWN`, fazer hit-test à posição de ecrã do pin atual com tolerância de
  ~48 dp; se acertar, consumir o evento, pedir ao pai para não intercetar e
  desativar todas as gestures do mapa.
- No `MOVE`, converter `Projection.fromScreenLocation` em `LatLng`, atualizar o
  feature do source `picked-location` e chamar `onMarkerMoved`.
- No `UP`/`CANCEL`, reativar as gestures e libertar o intercept; um segundo
  dedo cancela o arrasto para permitir pinch.
- Um toque fora do pin continua a ser tratado por `addOnMapClickListener` e
  coloca o pin nesse ponto (`onMapTapped`).
- Não mover a câmara durante o arrasto.
- **Porquê:** evita a complexidade de long-press do exemplo oficial e mantém a
  interação direta atual. O controlador isola a lógica de toque testável e não
  contamina o composable.

### 10. Câmara, fit e layout

- `ElisMap`: zoom inicial 16, centrar no pin na primeira posição não nula ou em
  `DEFAULT_CENTER`; não recentrar a cada atualização nem durante o arrasto.
- `ElisMapMarkers`: com localização atual, centrar e zoom 15, marcando
  `centeredOnUser = true`; sem localização, fazer fit aos pins (um pin: zoom 16
  e centro; vários: bounds com 96 px de padding). Voltar a fazer fit quando a
  assinatura dos pins muda e não há localização atual; não recentrar o
  visualizador completo quando a localização chega depois.
- Usar `moveCamera` para fits determinísticos e `animateCamera` apenas onde já
  havia animação.
- Fits de bounds exigem dimensões não nulas: helper `applyWhenLaidOut` com
  `post`/`OnLayoutChangeListener`.
- **Porquê:** preserva exatamente a semântica atual e evita saltos de câmara
  causados por atualizações de estado do Compose.

### 11. Atribuição e logo

Manter o logo e a atribuição do MapLibre/OpenFreeMap sempre visíveis. Como os
cartões e botões inferiores tapam a zona por omissão, posicionar o logo no
canto superior direito (margem ~8 dp) e a atribuição por baixo (~48 dp), com
`UiSettings` e cor legível.

- **Porquê:** é uma obrigação legal do OpenFreeMap/OSM; desativar ou esconder
  não é aceitável. A posição no topo evita conflito com a UI existente.

### 12. Cache e offline

Manter a cache ambiente do MapLibre (ativa por omissão) e documentar que o mapa
continua a precisar de rede ou de tiles em cache; não prometer mapas offline nem
implementar downloads de região.

- **Porquê:** o osmdroid também dependia de rede/cache; introduzir offline
  regional seria uma funcionalidade nova, fora do âmbito.

### 13. Build, manifesto e R8

- Substituir a dependência e remover o bloco ProGuard do osmdroid; confiar nas
  regras de consumo do AAR do MapLibre, sem `-keep org.maplibre.**` global.
- O manifesto fundido passa a incluir `ACCESS_NETWORK_STATE` e
  `ACCESS_WIFI_STATE`; `INTERNET` e localização já existem. Não há novas
  permissões de runtime.
- Verificar `assembleDebug`, testes e lint; o release minificado é validado no
  CI ou localmente com as credenciais de assinatura.
- **Porquê:** as regras do AAR já cobrem Gson/GeoJSON e as classes nativas; um
  keep global esconderia problemas e aumentaria o APK.

## Risks / Trade-offs

| Risco | Mitigação |
| --- | --- |
| OpenFreeMap não tem SLA e pode ficar indisponível | Estado de erro com retry, constante única de estilo e documentação do ponto de troca; não há fallback automático silencioso. |
| Política de uso de tiles/atribuição | Manter atribuição e logo visíveis; não fazer downloads offline; não usar tiles do OSM como predefinição. |
| OpenGL vs. Vulkan em dispositivos novos | Escolher o artefacto OpenGL, que funciona em todos os dispositivos suportados; Vulkan/dual fica como otimização futura. |
| Aumento do APK por bibliotecas nativas | Medir o APK; aceitar o custo nesta migração; ABI splits ficam para uma alteração futura se necessário. |
| Regras R8/nativas em release | Remover só as regras do osmdroid, confiar no AAR e executar o smoke test do release minificado. |
| Regressão de toque (mapa não interativo) | Manter `NonInteractiveMapView` e o `MapScrollTest`; testar também o cartão de detalhe e a pré-visualização do formulário. |
| Crash/fuga por ciclo de vida | Máquina de estados com flags e chamadas idempotentes; testar rotação e navegação para trás. |
| Corrida entre `setStyle` e atualizações | Adiar mutações até `OnStyleLoaded`, reaplicar estado no retry e tornar updates idempotentes. |
| Arrastar o pin é complexo | Controlador dedicado, hit-test de 48 dp, cancelamento com multi-toque e testes manuais no seletor. |
| Etiquetas vetoriais sobrepostas/ilegíveis | `textAllowOverlap`, halo branco, fonte `Noto Sans Bold`, verificação em ecrã pequeno e zoom baixo. |
| Testes instrumentados sem inicialização | `MapLibre.getInstance` na `Application` usada pelos testes; se o teste usar `ComponentActivity`, garantir a Application de teste ou inicialização explícita. |
| Atribuição tapada pela UI | Logo/atribuição no topo direito com margens; verificar nos ecrãs com cartão inferior. |

## Migration Plan

1. **Preparação:** criar branch de trabalho; adicionar `maplibre` ao catálogo e a
   dependência OpenGL; inicializar na `ElisdbApp`, mantendo temporariamente o
   osmdroid para permitir rollback. Compilar.
2. **Infraestrutura de mapa:** adicionar `ElisMapSupport` (ciclo de vida,
   `NonInteractiveMapView`, estados, helpers de estilo/feature) e `ElisMap`.
   Trocar primeiro a pré-visualização do formulário, o detalhe e o seletor;
   validar scroll, clique no cartão e pin arrastável.
3. **Marcadores:** implementar `ElisMapMarkers` com GeoJSON/SymbolLayer,
   seleção, etiquetas, estados e fit; trocar `ClientMapScreen` e o visualizador
   completo.
4. **Limpeza:** remover dependência, inicialização e regras ProGuard do
   osmdroid; apagar `OsmMap.kt`/`OsmMapMarkers.kt`; renomear o teste para
   `MapScrollTest`; atualizar chamadores e documentação.
5. **Verificação:** testes unitários e instrumentados, lint, `assembleDebug` e
   smoke test manual em emulador/dispositivo (tiles, pan/zoom, pins, etiquetas,
   seleção, localização, fit, seletor, permissões, offline/cache, temas,
   rotação). Release minificado no CI ou com credenciais.
6. **Rollback:** enquanto a verificação não terminar, o osmdroid permanece no
   histórico e a migração está em commits isolados; reverter os commits da
   migração restaura a implementação anterior. Não fazer merge para `main` nem
   release sem os testes e o smoke test do release aprovados.
