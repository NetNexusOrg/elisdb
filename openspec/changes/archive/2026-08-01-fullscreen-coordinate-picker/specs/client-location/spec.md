## MODIFIED Requirements

### Requirement: Display a location on a map

The system SHALL display a map with a marker at a given latitude/longitude without requiring a Google Maps API key.

#### Scenario: View a saved location
- **WHEN** the user opens a client detail screen with coordinates
- **THEN** a map SHALL be shown centered on the client's coordinates
- **AND** a marker SHALL be displayed at the client's coordinates

#### Scenario: Detail map is not interactive
- **WHEN** the user views the map on a client detail screen
- **THEN** the map SHALL be non-interactive
- **AND** tapping the map SHALL NOT open any picker

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
