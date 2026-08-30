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

#### Scenario: Detail map is not directly interactive
- **WHEN** the user views the map on a client detail screen
- **THEN** the map SHALL be non-interactive (no panning, zooming or marker dragging)
- **AND** tapping the map SHALL open the fullscreen location viewer
- **AND** tapping the map SHALL NOT open any picker

#### Scenario: No location means no map
- **WHEN** a client has no coordinates
- **THEN** no map SHALL be displayed for that client

## ADDED Requirements

### Requirement: View a client location in fullscreen

The system SHALL open an interactive fullscreen map centered on a client's saved location when the user taps the map on the client detail screen, letting the user pan and zoom freely while keeping the marker fixed at the client's coordinates.

#### Scenario: Open the fullscreen viewer from the detail map
- **WHEN** the user taps the map on a client detail screen with coordinates
- **THEN** a fullscreen map SHALL open centered on the client's coordinates
- **AND** a marker SHALL be displayed at the client's coordinates

#### Scenario: Fullscreen viewer is interactive
- **WHEN** the fullscreen map viewer is open
- **THEN** the user SHALL be able to pan and zoom the map freely
- **AND** the marker SHALL remain fixed at the client's coordinates

#### Scenario: Close the fullscreen viewer
- **WHEN** the user taps back in the fullscreen map viewer
- **THEN** the viewer SHALL close
- **AND** the detail screen SHALL be shown again unchanged