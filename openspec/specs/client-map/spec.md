# client-map Specification

## Purpose
Map of clients with valid coordinates, including filters, selection, pin labels,
and the current-location marker.

## Requirements

### Requirement: Client map overview

A app SHALL provide a map screen that shows clients with valid coordinates,
independently of the search or filter applied to the client list. By default it
SHALL show only active clients, and a menu SHALL allow showing all clients, only
inactive clients, or only clients that include a chosen tag. The app SHALL
request location permission when needed, center the map on the current location
and show a marker there.

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

#### Scenario: Filter the map by tag
- **WHEN** the user opens the map menu, expands Tags and selects one of the
  available tags
- **THEN** the map SHALL show only clients whose set of tags includes that tag
- **AND** the tag options SHALL come from the stored clients, in uppercase and
  sorted
- **AND** the client list filter and search SHALL remain unchanged

#### Scenario: Clear the map tag filter
- **WHEN** the user chooses "Todas as tags" in the map menu
- **THEN** the map SHALL show the clients matching the status filter, regardless
  of their tags

#### Scenario: Client without coordinates
- **WHEN** a client has no latitude/longitude, or the values are outside the
  valid ranges
- **THEN** the client SHALL NOT have a pin

#### Scenario: Distinguish inactive clients
- **WHEN** a client is inactive
- **THEN** its pin SHALL use the inactive style

#### Scenario: Pin label
- **WHEN** a client's pin is full size or the client is selected
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

### Requirement: Adaptar pins à densidade

O mapa SHALL manter cada cliente na sua coordenada real e SHALL reduzir o tamanho dos pins quando estes colidem no ecrã, em vez de os mover para um cluster.

#### Scenario: Pins afastados mantêm tamanho completo

- **WHEN** os pins de dois ou mais clientes não colidem no zoom atual
- **THEN** cada cliente SHALL continuar representado pelo pin completo na sua coordenada
- **AND** o pin SHALL mostrar a etiqueta quando houver espaço

#### Scenario: Pins próximos encolhem

- **WHEN** dois ou mais pins colidem no ecrã
- **THEN** os pins SHALL encolher progressivamente
- **AND** abaixo do tamanho mínimo SHALL passar a uma bola pequena
- **AND** o cliente SHALL continuar na sua coordenada real e continuar selecionável

#### Scenario: Cliente selecionado

- **WHEN** o utilizador seleciona um cliente
- **THEN** o pin desse cliente SHALL voltar ao tamanho completo
- **AND** SHALL mostrar a etiqueta com o nome
- **AND** SHALL ficar visualmente acima dos restantes

#### Scenario: Atualização com o mapa

- **WHEN** o utilizador faz pan ou zoom no mapa
- **THEN** o tamanho dos pins SHALL ser recalculado após a câmara estabilizar

#### Scenario: Coordenadas exatamente iguais

- **WHEN** dois ou mais clientes têm coordenadas exatamente iguais e o zoom é 14 ou superior
- **THEN** os respetivos pins SHALL ser separados em leque no ecrã
- **AND** cada cliente SHALL continuar selecionável

### Requirement: Etiquetas de clientes sem sobreposição

O mapa SHALL evitar etiquetas de cliente sobrepostas, mantendo os pins visíveis mesmo quando a etiqueta é omitida.

#### Scenario: Etiquetas colidem

- **WHEN** duas etiquetas de cliente ocupam a mesma área no ecrã
- **THEN** o mapa SHALL omitir ou reposicionar a etiqueta secundária sem esconder o respetivo pin

### Requirement: Etiquetas de clientes adaptativas

O mapa SHALL mostrar etiquetas apenas quando o pin tiver tamanho suficiente ou o cliente estiver selecionado, mantendo o pin visível quando a etiqueta é omitida.

#### Scenario: Etiqueta omitida por falta de espaço

- **WHEN** um pin é demasiado pequeno ou a sua etiqueta colide com outra
- **THEN** o mapa SHALL omitir a etiqueta
- **AND** SHALL manter o pin ou bola visível e selecionável

### Requirement: Remember the client map filter

The system SHALL remember the last selected client map filter across app restarts. When no saved preference exists, the default SHALL be to show only active clients. The client list filter and search SHALL remain independent from the map filter.

#### Scenario: Restore the saved map filter
- **WHEN** the client map opens and a saved map filter exists
- **THEN** the map SHALL show only clients matching that filter
- **AND** the filter control SHALL indicate the active filter

#### Scenario: Persist a map filter change
- **WHEN** the user selects a map filter
- **THEN** the selection SHALL be saved for the next app launch
- **AND** the client list filter and search SHALL remain unchanged

#### Scenario: Default to active clients without a saved filter
- **WHEN** the client map opens and no saved map filter exists
- **THEN** the map SHALL show only active clients
