## MODIFIED Requirements

### Requirement: Fullscreen photo viewer

The system SHALL display a selected photo fullscreen. When the client has more than one photo, the user SHALL be able to switch to the previous or next photo by swiping horizontally. The user SHALL be able to zoom the displayed photo in or out with a pinch gesture, and SHALL be able to pan the photo by dragging it when it is zoomed in.

#### Scenario: View a photo fullscreen
- **WHEN** the user taps a photo thumbnail in a gallery
- **THEN** the photo SHALL be displayed fullscreen

#### Scenario: Close the fullscreen viewer
- **WHEN** the user closes the fullscreen photo viewer
- **THEN** the system SHALL return to the previous screen

#### Scenario: Swipe to the next photo
- **WHEN** the client has more than one photo and the user swipes left on the fullscreen photo viewer
- **THEN** the next photo SHALL be displayed
- **AND** when the current photo is the last one, swiping left SHALL keep the last photo displayed

#### Scenario: Swipe to the previous photo
- **WHEN** the client has more than one photo and the user swipes right on the fullscreen photo viewer
- **THEN** the previous photo SHALL be displayed
- **AND** when the current photo is the first one, swiping right SHALL keep the first photo displayed

#### Scenario: Pinch to zoom in
- **WHEN** the user performs a pinch-out gesture on the displayed photo
- **THEN** the photo SHALL zoom in

#### Scenario: Pinch to zoom out
- **WHEN** the user performs a pinch-in gesture on a zoomed photo
- **THEN** the photo SHALL zoom out

#### Scenario: Pan a zoomed photo
- **WHEN** the photo is zoomed in and the user drags on it
- **THEN** the photo SHALL pan to follow the drag
- **AND** the photo SHALL NOT move beyond its edges

#### Scenario: Zoom resets when changing photo
- **WHEN** the user zooms a photo and then switches to another photo
- **THEN** the new photo SHALL be displayed without the previous zoom level applied
