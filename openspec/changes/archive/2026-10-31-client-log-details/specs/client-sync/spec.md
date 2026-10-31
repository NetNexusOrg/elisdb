## MODIFIED Requirements

### Requirement: Edit history fetched from the server

The system SHALL fetch a client's edit history from the server, including what changed in each entry, so the app can display who created and edited the client, when, and which fields or photos were affected.

#### Scenario: History loaded for the detail screen
- **WHEN** the user opens the client detail and requests the log
- **THEN** the app SHALL fetch the client's edit history from the server
- **AND** SHALL present the create entry and the edit entries to the user

#### Scenario: History fetch failure is non-blocking
- **WHEN** the server cannot be reached or the history fetch fails
- **THEN** the app SHALL show the detail screen without the log data
- **AND** SHALL inform the user that the history could not be loaded

#### Scenario: History carries change details
- **WHEN** the server returns history entries recorded with change details
- **THEN** the app SHALL keep those details available to the log view
- **AND** SHALL still handle entries returned without details
