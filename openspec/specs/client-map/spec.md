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
