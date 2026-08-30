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

## REMOVED Requirements

### Requirement: Manual sync trigger

**Reason**: The manual sync trigger moved to the settings screen alongside the sync status it controls.

**Migration**: Trigger sync from the settings screen; the settings screen is reached via the settings action on the client list.