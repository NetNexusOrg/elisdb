## Purpose

Provides map-based display and selection of client locations and lets the user launch an external navigation app to route to a client.

## Requirements
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

### Requirement: Select a location with a draggable marker

The system SHALL let the user set a client's location by opening a fullscreen interactive map from the create/edit form's map preview, moving to the desired spot, and placing the marker by dragging it or tapping the map; the chosen coordinates SHALL be returned to the form when the user confirms.

#### Scenario: Open the fullscreen picker from the form
- **WHEN** the user taps the map box on the create/edit form
- **THEN** a fullscreen interactive map SHALL open
- **AND** the map SHALL be centered on the currently selected coordinates (or the map default when none are selected)

#### Scenario: Drag the marker in the picker
- **WHEN** the user drags the marker to a new position in the fullscreen picker
- **THEN** the marker SHALL move to the new position
- **AND** the picker's selected coordinates SHALL update to the marker's position

#### Scenario: Tap the map to place the marker
- **WHEN** the user taps a spot on the fullscreen picker map
- **THEN** the marker SHALL move to that spot
- **AND** the picker's selected coordinates SHALL update to that spot

#### Scenario: Confirm the selection
- **WHEN** the user confirms the selection in the fullscreen picker
- **THEN** the picker SHALL close
- **AND** the form SHALL show the chosen coordinates on its map preview

#### Scenario: Cancel the picker
- **WHEN** the user cancels the fullscreen picker
- **THEN** the picker SHALL close
- **AND** the form's previous coordinates SHALL remain unchanged

#### Scenario: Form map preview is not directly interactive
- **WHEN** the user interacts with the map box on the create/edit form outside the fullscreen picker
- **THEN** the map box SHALL NOT be directly interactive (no marker dragging or panning)
- **AND** tapping it SHALL open the fullscreen picker

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
