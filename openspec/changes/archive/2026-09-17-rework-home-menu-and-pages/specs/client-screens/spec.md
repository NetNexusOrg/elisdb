## MODIFIED Requirements

### Requirement: Client list screen

The system SHALL show a list of all clients, each row displaying the client's name and a thumbnail: the client's first photo when present, otherwise a generic profile image. The system SHALL allow filtering the list by a search term. The screen SHALL have a top bar that shows a title for the screen and SHALL host a menu containing the create-client action, the synchronization action, the server action, and the about action.

#### Scenario: List shows clients with thumbnails
- **WHEN** the client list screen is shown and clients exist
- **THEN** each row SHALL display the client's name
- **AND** rows for clients with photos SHALL display the first photo as a thumbnail

#### Scenario: Generic image for clients without a photo
- **WHEN** the client list screen is shown and a client has no photo
- **THEN** the row SHALL display a generic profile image in place of the photo thumbnail

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
