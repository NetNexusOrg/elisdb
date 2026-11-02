# Proposal

## Why

O mapa da app usa osmdroid, uma biblioteca raster antiga que depende de tiles
OpenStreetMap e de código de desenho próprio para os pins e etiquetas. A
migração para MapLibre Native dá renderização vetorial por GPU, estilos
padronizados, melhor desempenho e uma base mantida ativamente, sem alterar a
experiência de mapa já definida nas specs.

## What Changes

- Substitui a dependência `org.osmdroid:osmdroid-android` por
  `org.maplibre.gl:android-sdk-opengl`, fixada numa versão estável.
- Inicializa o MapLibre uma única vez no arranque da aplicação.
- Reescreve `OsmMap` e `OsmMapMarkers` sobre a API Android View do MapLibre,
  mantendo o wrapper `AndroidView`, o ciclo de vida e o modo não interativo.
- Renomeia os composables para `ElisMap` e `ElisMapMarkers` e substitui
  `GeoPoint` por `LatLng` nos chamadores.
- Desenha pins, etiquetas e localização atual com `GeoJsonSource` +
  `SymbolLayer`, preservando cores ativo/inativo, nome por cima e seleção.
- Implementa o pin arrastável do seletor com a abordagem oficial de símbolos
  arrastáveis, mantendo também o toque no mapa para colocar o pin.
- Usa o estilo vetorial público OpenFreeMap Liberty por predefinição, sem chave
  de API, com um único ponto de configuração para trocar para um estilo raster
  OSM ou alojado pelo projeto.
- Mantém a atribuição do MapLibre/OpenFreeMap e verifica o comportamento em
  builds minificados.
- Remove as regras ProGuard do osmdroid e ajusta dependências, manifesto
  fundido, testes e documentação.
- **BREAKING**: o tipo público dos composables de mapa passa de
  `org.osmdroid.util.GeoPoint` para `org.maplibre.android.geometry.LatLng`.
  Apenas código interno da app é afetado.

## Capabilities

### New Capabilities

Nenhuma. A migração não introduz comportamento funcional novo; as capacidades
`client-map` e `client-location` mantêm os requisitos atuais.

### Modified Capabilities

- `app-build`: o requisito de build minificado passa a exigir renderização de
  mapa com MapLibre Native em vez de osmdroid.

## Impact

- `elisdb-android/gradle/libs.versions.toml`, `elisdb-android/app/build.gradle.kts`
- `elisdb-android/app/proguard-rules.pro`, `AndroidManifest.xml`, `ElisdbApp.kt`
- `ui/map/OsmMap.kt`, `ui/map/OsmMapMarkers.kt`, `ui/map/MapPicker.kt`
- `ui/cliente/ClientMapScreen.kt`, `ui/detalhe/MapFullscreenScreen.kt`,
  `ui/detalhe/ClientDetailScreen.kt`, `ui/form/ClientFormScreen.kt`
- Testes instrumentados de mapa e documentação da app
- Tamanho do APK e bibliotecas nativas (`arm64-v8a`, `armeabi-v7a`, `x86`,
  `x86_64`), permissões fundidas `ACCESS_NETWORK_STATE`/`ACCESS_WIFI_STATE`
- Sem alterações ao servidor, à base de dados ou ao protocolo de sync.
