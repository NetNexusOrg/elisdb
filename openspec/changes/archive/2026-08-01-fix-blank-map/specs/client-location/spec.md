## MODIFIED Requirements

### Requirement: Display a location on a map

The system SHALL display a map with a marker at a given latitude/longitude without requiring a Google Maps API key.

#### Scenario: View a saved location
- **WHEN** the user opens a client detail screen with coordinates
- **THEN** a map SHALL be shown centered on the client's coordinates
- **AND** a marker SHALL be displayed at the client's coordinates

#### Scenario: Map renders map tiles
- **WHEN** a screen displays a map and the device has network access
- **THEN** the map SHALL load and show map tiles (visible map content, not a blank view)
