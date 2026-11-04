## MODIFIED Requirements

### Requirement: Client list screen

The system SHALL show a list of all clients, each row displaying the client's name, the contract reference when present, the tag when present, and a thumbnail: the client's first photo when present, otherwise a generic profile image. Inactive clients SHALL be labelled. The system SHALL allow filtering the list by a search term and by status. The screen SHALL have a top bar with a title and a menu holding the create-client, synchronization, server and about actions.

#### Scenario: List shows clients with thumbnails
- **WHEN** the client list screen is shown and clients exist
- **THEN** each row SHALL display the client's name
- **AND** rows for clients with photos SHALL display the first photo as a thumbnail

#### Scenario: Contract reference under the name
- **WHEN** a listed client has a contract reference
- **THEN** the row SHALL display the contract reference below the client's name
- **AND** rows for clients without a contract SHALL NOT leave an empty line

#### Scenario: Tag shown on the contract line, aligned right
- **WHEN** a listed client has a tag
- **THEN** the row SHALL display the tag on the same line as the contract reference and the inactive label, aligned to the right side of the card
- **AND** rows for clients without a tag SHALL NOT leave an empty placeholder

#### Scenario: Generic image for clients without a photo
- **WHEN** the client list screen is shown and a client has no photo
- **THEN** the row SHALL display a generic profile image in place of the photo thumbnail

#### Scenario: Inactive clients are identifiable in the list
- **WHEN** the client list screen shows a client whose status is inactive
- **THEN** the row SHALL carry a visible "inactive" label
- **AND** the row SHALL be visually dimmed compared with active clients

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

#### Scenario: Filter the list by status
- **WHEN** the user opens the status filter from the search field and chooses active or inactive
- **THEN** the list SHALL show only clients with that status
- **AND** the filter control SHALL indicate that a filter is applied
- **AND** the default selection SHALL show every client regardless of status

#### Scenario: Search matches no client
- **WHEN** the search term does not match any client
- **THEN** the system SHALL indicate that no clients were found

#### Scenario: Clear the search
- **WHEN** the user clears the search field
- **THEN** the full list of clients SHALL be displayed again

#### Scenario: Top bar hosts the screen actions
- **WHEN** the client list screen is shown
- **THEN** the screen SHALL show a top bar with a title for the screen
- **AND** the top bar SHALL host a menu that contains the add-a-new-client action
- **AND** the top bar SHALL host a menu that contains the action to open the synchronization screen
- **AND** the top bar SHALL host a menu that contains the action to open the server screen
- **AND** the top bar SHALL host a menu that contains the action to open the about screen
- **AND** the create-client action SHALL NOT be shown as a floating action button

#### Scenario: Show current sync status
- **WHEN** the client list screen is shown and a sync is in progress
- **THEN** the screen SHALL indicate that a sync is in progress
- **AND** when a sync has just failed, the screen SHALL indicate a sync error
- **AND** when a sync has just succeeded, the screen SHALL indicate the last successful sync

#### Scenario: No sync activity shown when idle
- **WHEN** the client list screen is shown and no sync has run or been triggered
- **THEN** the screen SHALL NOT show a "not synchronised" message for a missing server configuration

### Requirement: Create/edit client form

The system SHALL provide a form to create a new client or edit an existing one, with fields for name (required), contract (optional), tag (optional), location selection (optional), notes, an active switch, and a photo management section, plus a save action. A form opened in create mode SHALL start with all fields empty, no location selected, and the active switch on. A form opened in edit mode SHALL load the data of the selected client only, regardless of which forms were opened previously.

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

#### Scenario: Save a client with a tag
- **WHEN** the user fills a name and a tag and taps save in create mode
- **THEN** the new client SHALL be stored with that tag
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
- **THEN** every field SHALL be empty, including the tag
- **AND** no location SHALL be selected
- **AND** the active switch SHALL be on

#### Scenario: Edit form loads only the selected client
- **WHEN** the user opens the edit form for a client after having previously opened a form for a different client
- **THEN** the form SHALL show the name, contract, tag, notes, location and status of the selected client only

#### Scenario: Toggle the client status
- **WHEN** the user turns the active switch off and saves
- **THEN** the client SHALL be stored as inactive
- **AND** when the switch is on the client SHALL be stored as active

### Requirement: Client detail screen

The system SHALL show a client's name, tag, notes, a map with the client's location when one exists, a photo gallery, an "inactive" label when the client is inactive, and actions to navigate to the location, edit the client, and delete the client. When the client has a contract reference, the top bar SHALL display it next to the back button; the body SHALL NOT show a contract line above the name. The body sections SHALL be presented in the following order: name, tag (when present), map (when the location exists), notes, photo gallery. When the client has no location, the map and the navigate action SHALL NOT be shown. The edit and delete actions SHALL be offered through an overflow menu in the top bar, and the top bar SHALL NOT repeat the client's name.

#### Scenario: Show full client information
- **WHEN** the user opens a client detail screen
- **THEN** the name, notes, map, and photo gallery SHALL be displayed

#### Scenario: Tag shown below the name
- **WHEN** the user opens the detail screen of a client with a tag
- **THEN** the tag SHALL be displayed as a label below the client's name

#### Scenario: No tag on the client
- **WHEN** the user opens the detail screen of a client without a tag
- **THEN** no tag label SHALL be displayed

#### Scenario: Client without a location shows no map
- **WHEN** the user opens a client detail screen for a client without coordinates
- **THEN** the map SHALL NOT be displayed
- **AND** the navigate action SHALL NOT be shown

#### Scenario: Top bar does not repeat the client name
- **WHEN** the user opens a client detail screen
- **THEN** the top bar SHALL NOT display the client's name

#### Scenario: Contract shown in the top bar
- **WHEN** a client with a contract reference is viewed
- **THEN** the contract reference SHALL be displayed in the top bar next to the back button
- **AND** no contract SHALL be displayed above the client's name in the body

#### Scenario: No contract on the client
- **WHEN** a client without a contract reference is viewed
- **THEN** the top bar SHALL NOT display a contract reference

#### Scenario: Inactive client is labelled
- **WHEN** the user opens the detail screen of an inactive client
- **THEN** the screen SHALL show an "inactive" label next to the client's name

#### Scenario: Sections are presented in order
- **WHEN** a client detail screen is shown for a client with coordinates and a tag
- **THEN** the body sections SHALL appear in this order: name, tag, map, notes, photo gallery

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

## ADDED Requirements

### Requirement: Client tag presented in uppercase

The system SHALL present the client tag in uppercase letters in the client list, the detail screen, the create/edit form and the edit history, regardless of the casing stored or typed.

#### Scenario: Tag uppercased while typing
- **WHEN** the user types a tag in the create/edit form using lowercase or mixed-case letters
- **THEN** the tag field SHALL display the entered text in uppercase letters as it is typed

#### Scenario: Tag shown uppercased in the list
- **WHEN** the client list screen is shown and a client has a tag
- **THEN** the tag SHALL be displayed in uppercase letters

#### Scenario: Tag shown uppercased in the detail screen
- **WHEN** a client detail screen is shown and the client has a tag
- **THEN** the tag SHALL be displayed in uppercase letters

#### Scenario: Historical tag values shown uppercased in the log
- **WHEN** the edit history shows a changed tag value
- **THEN** the previous and new tag values SHALL be displayed in uppercase letters
