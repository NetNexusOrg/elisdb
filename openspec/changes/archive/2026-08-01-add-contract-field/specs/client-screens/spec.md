## MODIFIED Requirements

### Requirement: Create/edit client form

The system SHALL provide a form to create a new client or edit an existing one, with fields for name (required), phone (optional), contract (optional), location selection, notes, and a photo management section, plus a save action.

#### Scenario: Required name validation
- **WHEN** the user tries to save with an empty name
- **THEN** the save SHALL be prevented and the name field SHALL be flagged as required

#### Scenario: Save a new client
- **WHEN** the user fills valid data and taps save in create mode
- **THEN** a new client SHALL be created
- **AND** the user SHALL return to the client list

#### Scenario: Save edits to a client
- **WHEN** the user edits a client and taps save in edit mode
- **THEN** the client SHALL be updated
- **AND** the user SHALL return to the client detail screen

### Requirement: Client detail screen

The system SHALL show a client's name, phone, contract reference, notes, a map with the client's location, a photo gallery, and actions to navigate to the location, edit the client, and delete the client.

#### Scenario: Show full client information
- **WHEN** the user opens a client detail screen
- **THEN** the name, phone, notes, map, and photo gallery SHALL be displayed

#### Scenario: Contract shown above the name
- **WHEN** a client with a contract reference is viewed
- **THEN** the contract reference SHALL be displayed above the client's name

#### Scenario: No contract on the client
- **WHEN** a client without a contract reference is viewed
- **THEN** no contract SHALL be displayed above the name

#### Scenario: Navigate to the client location
- **WHEN** the user taps "navigate" on the detail screen
- **THEN** the system SHALL launch an external navigation app to the client's location

#### Scenario: Edit from the detail screen
- **WHEN** the user taps edit on the detail screen
- **THEN** the system SHALL open the create/edit form in edit mode for that client

#### Scenario: Delete from the detail screen
- **WHEN** the user taps delete on the detail screen and confirms
- **THEN** the client SHALL be deleted
- **AND** the user SHALL return to the client list
