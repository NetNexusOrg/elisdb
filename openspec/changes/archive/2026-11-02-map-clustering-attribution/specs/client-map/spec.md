# Spec Delta

## ADDED Requirements

### Requirement: Agrupar clientes próximos

O mapa SHALL agrupar visualmente clientes cujos pins colidem no ecrã, apresentando um único cluster com a contagem de clientes agrupados.

#### Scenario: Clientes próximos formam um cluster

- **WHEN** dois ou mais clientes com coordenadas válidas estão suficientemente próximos no zoom atual
- **THEN** o mapa SHALL apresentar um cluster com o número de clientes agrupados em vez de pins individuais sobrepostos

#### Scenario: Cluster é expandido

- **WHEN** o utilizador toca num cluster
- **THEN** o mapa SHALL aproximar a câmara para a zona do cluster e SHALL mostrar os pins individuais quando o nível de zoom permitir

#### Scenario: Clientes distantes mantêm pins individuais

- **WHEN** um cliente não colide com outro pin no zoom atual
- **THEN** o mapa SHALL continuar a mostrar o pin individual desse cliente

### Requirement: Etiquetas de clientes sem sobreposição

O mapa SHALL evitar etiquetas de cliente sobrepostas, mantendo os pins visíveis mesmo quando a etiqueta é omitida.

#### Scenario: Etiquetas colidem

- **WHEN** duas etiquetas de cliente ocupam a mesma área no ecrã
- **THEN** o mapa SHALL omitir ou reposicionar a etiqueta secundária sem esconder o respetivo pin

### Requirement: Atribuição do mapa acessível

O mapa SHALL ocultar o logótipo e o controlo de atribuição nativos da MapLibre e SHALL apresentar um controlo compacto de atribuição que dá acesso aos créditos obrigatórios.

#### Scenario: Atribuição visível e acessível

- **WHEN** o mapa está visível
- **THEN** o utilizador SHALL conseguir abrir a atribuição com OpenStreetMap, OpenFreeMap, OpenMapTiles e MapLibre Native, incluindo ligações para as respetivas páginas

#### Scenario: Interface do mapa limpa

- **WHEN** o mapa é apresentado
- **THEN** o logótipo e o ícone de informação nativos da MapLibre SHALL estar ocultos
- **AND** o controlo compacto de atribuição SHALL permanecer visível
