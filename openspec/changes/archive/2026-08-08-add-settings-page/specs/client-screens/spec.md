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

#### Scenario: Show current sync status
- **WHEN** the client list screen is shown and a sync is in progress
- **THEN** the screen SHALL indicate that a sync is in progress
- **AND** when a sync has just failed, the screen SHALL indicate a sync error
- **AND** when a sync has just succeeded, the screen SHALL indicate the last successful sync

#### Scenario: No sync activity shown when idle
- **WHEN** the client list screen is shown and no sync has run or been triggered
- **THEN** the screen SHALL NOT show a "not synchronised" message for a missing server configuration

### Requirement: Manual sync trigger

The system SHALL provide an entry point in the client list screen to trigger a synchronisation and SHALL disable repeated triggering while a sync is in progress.

#### Scenario: Trigger a manual sync
- **WHEN** a sync is not in progress and the user taps the sync action
- **THEN** the system SHALL start a synchronisation
- **AND** the screen SHALL move to a "sync in progress" state until it completes

#### Scenario: Repeated trigger is ignored while syncing
- **WHEN** the user taps the sync action while a sync is already in progress
- **THEN** the system SHALL NOT start a second synchronisation

#### Scenario: Empty server configuration is not shown as a sync error
- **WHEN** the user triggers a sync and no server / signing key is configured
- **THEN** the screen SHALL NOT show the missing server/key message as a sync error on the client list screen
- **AND** the user SHALL be directed to the settings screen to configure the connection