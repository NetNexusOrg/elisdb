## ADDED Requirements

### Requirement: Log action on the client detail screen

The system SHALL offer a "Log" action on the client detail screen that shows who created the client and who edited it, with the timestamps of each change.

#### Scenario: Log action opens the history view
- **WHEN** the user taps "Log" on a client detail screen
- **THEN** the app SHALL show the client's edit history: the create entry and the edit entries
- **AND** each entry SHALL show the actor (user name when known) and the timestamp

#### Scenario: Log is reachable from the detail screen
- **WHEN** the client detail screen is shown
- **THEN** a "Log" action SHALL be available to the user

#### Scenario: Empty history is presented clearly
- **WHEN** a client has no recorded history (e.g. migrated data)
- **THEN** the log view SHALL indicate that no history is available
