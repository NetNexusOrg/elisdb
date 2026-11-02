## ADDED Requirements

### Requirement: Client map overview

A app SHALL provide a map screen that shows all clients with valid coordinates,
independently of the search or filter applied to the client list.

#### Scenario: Open the map from the client list
- **WHEN** the user taps the map action in the client list top bar
- **THEN** the app SHALL open the client map screen
- **AND** the screen SHALL show every client with valid coordinates

#### Scenario: Client without coordinates
- **WHEN** a client has no latitude/longitude, or the values are outside the
  valid ranges
- **THEN** the client SHALL NOT have a pin

#### Scenario: Distinguish inactive clients
- **WHEN** a client is inactive
- **THEN** its pin SHALL use the inactive style

#### Scenario: Select a client pin
- **WHEN** the user taps a client pin
- **THEN** the app SHALL show a card with the client's name, contract and state
- **AND** the card SHALL offer an action to open the client detail
- **AND** tapping the map or closing the card SHALL clear the selection

#### Scenario: Empty map
- **WHEN** there are no clients with valid coordinates
- **THEN** the app SHALL show an explanatory empty state instead of the map
