## Purpose

Provides map-based display and selection of client locations and lets the user launch an external navigation app to route to a client.

## ADDED Requirements

### Requirement: Display a location on a map

The system SHALL display a map with a marker at a given latitude/longitude without requiring a Google Maps API key.

#### Scenario: View a saved location
- **WHEN** the user opens a client detail screen with coordinates
- **THEN** a map SHALL be shown centered on the client's coordinates
- **AND** a marker SHALL be displayed at the client's coordinates

### Requirement: Select a location with a draggable marker

The system SHALL allow the user to set a client's location by dragging a marker on the map, updating the selected coordinates as the marker moves.

#### Scenario: Drag the marker
- **WHEN** the user drags the marker to a new position
- **THEN** the selected coordinates SHALL update to the marker's position
- **AND** the saved coordinates SHALL be the marker's final position when the client is saved

### Requirement: Use current device location

The system SHALL offer a "use current location" action that positions the map and marker at the device's current location when the user grants location permission.

#### Scenario: Current location is available and permitted
- **WHEN** the user taps "use current location" and the device location is available and permission is granted
- **THEN** the map SHALL center on the device's current location
- **AND** the marker SHALL move to the device's current location

#### Scenario: Location permission denied
- **WHEN** the user taps "use current location" and location permission is denied
- **THEN** the system SHALL prompt for the location permission
- **AND** if denied, the selected location SHALL remain unchanged

### Requirement: Navigate to a client location externally

The system SHALL launch the device's external navigation app(s) to route to a client's coordinates using an intent that the system can resolve.

#### Scenario: Launch external navigation
- **WHEN** the user taps "Navigate here" on a client detail screen
- **THEN** the system SHALL open a navigation-capable app on the device targeting the client's coordinates

#### Scenario: No navigation app available
- **WHEN** the user taps "Navigate here" and no navigation app can handle the request
- **THEN** the system SHALL inform the user that no navigation app is available
- **AND** the app SHALL NOT crash
