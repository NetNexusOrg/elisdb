## MODIFIED Requirements

### Requirement: Client detail screen

The system SHALL show a client's name, phone, contract reference, notes, a map with the client's location, a photo gallery, and actions to navigate to the location, edit the client, and delete the client. The edit and delete actions SHALL be offered through an overflow menu in the top bar, and the top bar SHALL NOT repeat the client's name.

#### Scenario: Show full client information
- **WHEN** the user opens a client detail screen
- **THEN** the name, phone, notes, map, and photo gallery SHALL be displayed

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
- **WHEN** the user taps "navigate" on the detail screen
- **THEN** the system SHALL launch an external navigation app to the client's location

#### Scenario: Edit from the detail screen
- **WHEN** the user opens the overflow menu on the detail screen and taps edit
- **THEN** the system SHALL open the create/edit form in edit mode for that client

#### Scenario: Delete from the detail screen
- **WHEN** the user opens the overflow menu on the detail screen and taps delete, and confirms
- **THEN** the client SHALL be deleted
- **AND** the user SHALL return to the client list
