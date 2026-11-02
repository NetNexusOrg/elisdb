# client-map Specification

## Purpose
Map of clients with valid coordinates, including filters, selection, pin labels,
and the current-location marker.

## Requirements

### Requirement: Client map overview

A app SHALL provide a map screen that shows clients with valid coordinates,
independently of the search or filter applied to the client list. By default it
SHALL show only active clients, and a menu SHALL allow showing all clients or
only inactive clients. The app SHALL request location permission when needed,
center the map on the current location and show a marker there.

#### Scenario: Open the map from the client list
- **WHEN** the user taps the map action in the client list top bar
- **THEN** the app SHALL open the client map screen
- **AND** the screen SHALL show only active clients with valid coordinates by
  default

#### Scenario: Center on the current location
- **WHEN** location permission is granted and a current location is available
- **THEN** the map SHALL open centered on that location with a location marker
- **AND** it SHALL keep the client pins loaded without fitting them to the view

#### Scenario: Location unavailable
- **WHEN** location permission is denied or no current location is available
- **THEN** the map SHALL keep working and fit the visible client pins

#### Scenario: Detail fullscreen map
- **WHEN** the user opens the fullscreen map from a client detail
- **THEN** the map SHALL center on that client's coordinates
- **AND** it SHALL show the current location marker when permission and location
  are available
- **AND** it SHALL keep the client marker visible

#### Scenario: Change the map filter
- **WHEN** the user opens the map menu and selects all clients, only active
  clients or only inactive clients
- **THEN** the map SHALL show the clients matching that filter
- **AND** the client list filter and search SHALL remain unchanged

#### Scenario: Client without coordinates
- **WHEN** a client has no latitude/longitude, or the values are outside the
  valid ranges
- **THEN** the client SHALL NOT have a pin

#### Scenario: Distinguish inactive clients
- **WHEN** a client is inactive
- **THEN** its pin SHALL use the inactive style

#### Scenario: Pin label
- **WHEN** a client has a pin on the map
- **THEN** the pin SHALL show the client's name above it

#### Scenario: Select a client pin
- **WHEN** the user taps a client pin
- **THEN** the app SHALL show a card with the client's name, contract and state
- **AND** the card SHALL offer an action to open the client detail
- **AND** tapping the map or closing the card SHALL clear the selection

#### Scenario: Empty map
- **WHEN** there are no clients with valid coordinates for the selected filter
  and no current location is available
- **THEN** the app SHALL show an explanatory empty state instead of the map

#### Scenario: Filter without results
- **WHEN** the selected filter matches no client with valid coordinates
- **THEN** the app SHALL keep the map visible when a current location is
  available
- **AND** it SHALL explain that no client matches the filter

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
