## MODIFIED Requirements

### Requirement: Create/edit client form

The system SHALL provide a form to create a new client or edit an existing one, with fields for name (required), contract (optional), location selection (optional), notes, and a photo management section, plus a save action. A form opened in create mode SHALL start with all fields empty and no location selected. A form opened in edit mode SHALL load the data of the selected client only, regardless of which forms were opened previously.

#### Scenario: Required name validation
- **WHEN** the user tries to save with an empty name
- **THEN** the save SHALL be prevented and the name field SHALL be flagged as required

#### Scenario: Save a new client
- **WHEN** the user fills valid data and taps save in create mode
- **THEN** a new client SHALL be created
- **AND** the user SHALL return to the client list

#### Scenario: Save a new client without a location
- **WHEN** the user fills a name but no location and taps save in create mode
- **THEN** a new client SHALL be created without coordinates
- **AND** the user SHALL return to the client list

#### Scenario: Save edits to a client
- **WHEN** the user edits a client and taps save in edit mode
- **THEN** the client SHALL be updated
- **AND** the user SHALL return to the client detail screen

#### Scenario: Edit removes a client's location
- **WHEN** the user edits a client and clears its location before saving
- **THEN** the client SHALL be updated without coordinates
- **AND** the user SHALL return to the client detail screen

#### Scenario: Create form starts empty after another form was opened
- **WHEN** the user opens a create/edit form, leaves it, and opens the create form again
- **THEN** every field SHALL be empty
- **AND** no location SHALL be selected

#### Scenario: Edit form loads only the selected client
- **WHEN** the user opens the edit form for a client after having previously opened a form for a different client
- **THEN** the form SHALL show the name, contract, notes and location of the selected client only

### Requirement: Client detail screen

The system SHALL show a client's name, contract reference, notes, a map with the client's location when one exists, a photo gallery, and actions to navigate to the location, edit the client, and delete the client. When the client has no location, the map and the navigate action SHALL NOT be shown. The edit and delete actions SHALL be offered through an overflow menu in the top bar, and the top bar SHALL NOT repeat the client's name.

#### Scenario: Show full client information
- **WHEN** the user opens a client detail screen
- **THEN** the name, notes, map, and photo gallery SHALL be displayed

#### Scenario: Client without a location shows no map
- **WHEN** the user opens a client detail screen for a client without coordinates
- **THEN** the map SHALL NOT be displayed
- **AND** the navigate action SHALL NOT be shown

#### Scenario: Top bar does not repeat the client name
- **WHEN** the user opens a client detail screen
- **THEN** the top bar SHALL NOT display the client's name

#### Scenario: Contract shown above the name
- **WHEN** a client with a contract reference is viewed
- **THEN** the contract reference SHALL be displayed above the client's name

#### Scenario: No contract on the client
- **WHEN** a client without a contract reference is viewed
- **THEN** no contract SHALL be displayed above the name

#### Scenario: Navigate to the client location
- **WHEN** the user taps "navigate" on the detail screen of a client with coordinates
- **THEN** the system SHALL launch an external navigation app to the client's location

#### Scenario: Edit from the detail screen
- **WHEN** the user opens the overflow menu on the detail screen and taps edit
- **THEN** the system SHALL open the create/edit form in edit mode for that client

#### Scenario: Delete from the detail screen
- **WHEN** the user opens the overflow menu on the detail screen and taps delete, and confirms
- **THEN** the client SHALL be deleted
- **AND** the user SHALL return to the client list