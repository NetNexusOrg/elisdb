## MODIFIED Requirements

### Requirement: Log action on the client detail screen

The system SHALL offer a "Log" action on the client detail screen that shows who created the client and who edited it, with the timestamps of each change. Entry timestamps SHALL be presented with a 24-hour clock regardless of the device locale.

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

#### Scenario: Entry timestamps use a 24-hour clock
- **WHEN** the log view shows an entry timestamp on a device whose locale would use a 12-hour clock
- **THEN** the time SHALL be presented in 24-hour format (no AM/PM marker)
