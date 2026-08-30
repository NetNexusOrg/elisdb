## MODIFIED Requirements

### Requirement: Client list screen

The system SHALL show a list of all clients, each row displaying the client's name and, when present, a thumbnail of the client's first photo, and SHALL allow filtering the list by a search term.

#### Scenario: List shows clients with thumbnails
- **WHEN** the client list screen is shown and clients exist
- **THEN** each row SHALL display the client's name
- **AND** rows for clients with photos SHALL display the first photo as a thumbnail

#### Scenario: Open a client from the list
- **WHEN** the user taps a client row
- **THEN** the system SHALL open that client's detail screen

#### Scenario: Empty list
- **WHEN** the client list screen is shown and no clients exist
- **THEN** the system SHALL indicate that no clients are stored yet

#### Scenario: Filter the list by a search term
- **WHEN** the user types a search term in the search field
- **THEN** the list SHALL show only clients whose name, contract or notes contain the term
- **AND** the comparison SHALL ignore letter case and diacritics

#### Scenario: Search matches no client
- **WHEN** the search term does not match any client
- **THEN** the system SHALL indicate that no clients were found

#### Scenario: Clear the search
- **WHEN** the user clears the search field
- **THEN** the full list of clients SHALL be displayed again

### Requirement: Create/edit client form

The system SHALL provide a form to create a new client or edit an existing one, with fields for name (required), contract (optional), location selection, notes, and a photo management section, plus a save action. A form opened in create mode SHALL start with all fields empty and no location selected. A form opened in edit mode SHALL load the data of the selected client only, regardless of which forms were opened previously.

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

#### Scenario: Create form starts empty after another form was opened
- **WHEN** the user opens a create/edit form, leaves it, and opens the create form again
- **THEN** every field SHALL be empty
- **AND** no location SHALL be selected

#### Scenario: Edit form loads only the selected client
- **WHEN** the user opens the edit form for a client after having previously opened a form for a different client
- **THEN** the form SHALL show the name, contract, notes and location of the selected client only

### Requirement: Client detail screen

The system SHALL show a client's name, contract reference, notes, a map with the client's location, a photo gallery, and actions to navigate to the location, edit the client, and delete the client.

#### Scenario: Show full client information
- **WHEN** the user opens a client detail screen
- **THEN** the name, notes, map, and photo gallery SHALL be displayed

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