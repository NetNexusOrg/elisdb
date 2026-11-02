# Proposal

## Why

O mapa de clientes fica congestionado quando existem vários clientes próximos: os pins e as etiquetas sobrepõem-se e tornam a leitura difícil. Além disso, o logótipo e o controlo de atribuição nativos da MapLibre ocupam espaço no canto superior, sem uma apresentação consistente com o resto da app.

## What Changes

- Agrupar clientes próximos com clustering GeoJSON: círculo de cluster com contagem, pins individuais apenas para clientes não agrupados e expansão ao tocar no cluster.
- Reduzir a poluição visual das etiquetas: esconder etiquetas que colidem, limitar a largura e manter os pins visíveis.
- Ocultar o logótipo e o controlo de atribuição nativos da MapLibre.
- Adicionar um controlo compacto `© OpenStreetMap` que abre um diálogo com a atribuição obrigatória (OpenStreetMap, OpenFreeMap, OpenMapTiles e MapLibre Native) e ligações.
- Manter o comportamento existente de pins, localização atual, filtros e câmara.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `client-map`: passa a exigir clustering de clientes próximos e um controlo de atribuição próprio, acessível e visível.

## Impact

- `elisdb-android/app/src/main/java/com/example/elisdb/ui/map/ElisMapSupport.kt`
- `elisdb-android/app/src/main/java/com/example/elisdb/ui/map/ElisMapMarkers.kt`
- `elisdb-android/app/src/main/java/com/example/elisdb/ui/map/ElisMap.kt`
- Sem alterações ao servidor, base de dados, sincronização ou contrato da API.
- A atribuição legal continua visível e acessível.
