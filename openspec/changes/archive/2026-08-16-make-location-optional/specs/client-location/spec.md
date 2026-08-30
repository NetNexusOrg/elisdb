## MODIFIED Requirements

### Requirement: Display a location on a map

The system SHALL display a map with a marker at a given latitude/longitude without requiring a Google Maps API key, only when a location exists.

#### Scenario: View a saved location
- **WHEN** the user opens a client detail screen with coordinates
- **THEN** a map SHALL be shown centered on the client's coordinates
- **AND** a marker SHALL be displayed at the client's coordinates

#### Scenario: Map renders map tiles
- **WHEN** a screen displays a map and the device has network access
- **THEN** the map SHALL load and show map tiles (visible map content, not a blank view)

#### Scenario: Detail map is not interactive
- **WHEN** the user views the map on a client detail screen
- **THEN** the map SHALL be non-interactive
- **AND** tapping the map SHALL NOT open any picker

#### Scenario: No location means no map
- **WHEN** a client has no coordinates
- **THEN** no map SHALL be displayed for that client

### Requirement: Navigate to a client location externally

The system SHALL launch the device's external navigation app(s) to route to a client's coordinates using an intent that the system can resolve, only when the client has coordinates.

#### Scenario: Launch external navigation
- **WHEN** the user taps "Navigate here" on a client detail screen with coordinates
- **THEN** the system SHALL open a navigation-capable app on the device targeting the client's coordinates

#### Scenario: No navigation app available
- **WHEN** the user taps "Navigate here" and no navigation app can handle the request
- **THEN** the system SHALL inform the user that no navigation app is available
- **AND** the app SHALL NOT crash

#### Scenario: No navigate action without coordinates
- **WHEN** a client has no coordinates
- **THEN** the "Navigate here" action SHALL NOT be offered