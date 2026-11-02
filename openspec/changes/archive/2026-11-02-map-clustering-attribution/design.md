# Design

## Contexto

O mapa de clientes usa MapLibre Native com uma `GeoJsonSource` e uma `SymbolLayer` para os pins. Com muitos clientes próximos, os pins e as etiquetas sobrepõem-se. O logótipo e o controlo de atribuição nativos da MapLibre também ocupam espaço fixo no canto superior.

## Objetivos

- Reduzir a sobreposição de pins e etiquetas em zonas com muitos clientes.
- Manter a seleção de clientes e a localização atual.
- Ocultar o logótipo e o controlo nativo de atribuição sem remover a atribuição legal.
- Manter o comportamento de câmara existente.

## Não-objetivos

- Alterar o modelo de dados, sincronização ou servidor.
- Implementar mapas offline completos.
- Introduzir um novo fornecedor de tiles ou uma chave de API.

## Decisões

### Clustering na GeoJSON source

A fonte `client-pins` usa `GeoJsonOptions` com `cluster=true`, raio de 50 px e zoom máximo 16. O clustering é calculado pelo MapLibre, sem lógica de agrupamento na app.

### Camadas de cluster

São adicionadas, por baixo da camada de pins:

- `client-clusters`: `CircleLayer` azul com contorno branco, filtrada por `point_count`.
- `client-cluster-count`: `SymbolLayer` com o número de clientes.
- `client-pins-layer`: pins individuais, filtrada por `has("id")`, pelo que só recebe features não agrupadas.

### Etiquetas

A camada de pins usa `textOptional=true`, `textAllowOverlap=false`, `textIgnorePlacement=false` e `textMaxWidth=8`. Os pins continuam com sobreposição permitida; apenas as etiquetas são sujeitas a colisão.

### Expansão do cluster

Ao tocar num cluster, a app consulta a feature renderizada, obtém o `cluster_id` e usa `getClusterExpansionZoom` para aproximar. Se o valor não estiver disponível, aproxima pelo menos um nível acima do zoom atual.

### Atribuição

`configureUi` desativa `setLogoEnabled` e `setAttributionEnabled`. É adicionado um controlo compacto `© OpenStreetMap` no canto superior, que abre um `AlertDialog` com os créditos e ligações. A atribuição permanece visível em todos os estados do mapa.

## Riscos e compromissos

- Clientes com coordenadas exatamente iguais continuam a sobrepor-se depois da expansão; a contagem do cluster indica que existem vários. Uma evolução futura pode adicionar uma lista de clientes do cluster.
- O zoom máximo do clustering (16) está alinhado com o zoom inicial do mapa (15); se o zoom inicial mudar, o valor deve ser revisto.
- As etiquetas podem ser omitidas em zooms baixos para evitar colisões; o pin continua visível.
- O controlo de atribuição próprio tem de ser mantido se os fornecedores de tiles mudarem.

## Plano de migração

Sem migração de dados. A alteração é confinada às camadas Compose/MapLibre e pode ser revertida removendo o clustering e restaurando o controlo nativo de atribuição.
