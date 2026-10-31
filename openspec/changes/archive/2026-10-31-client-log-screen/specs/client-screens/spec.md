## MODIFIED Requirements

### Requirement: Log action on the client detail screen

The system SHALL provide a dedicated page for a client's log, opened from the client detail screen, showing who created the client and who edited it with the timestamp of each change. Entries SHALL be grouped by day and timestamps SHALL use a 24-hour clock regardless of the device locale. The page SHALL distinguish a fetch failure from genuinely empty history, offer a retry action after a failure, and note when local pending changes will only appear in the history after synchronization.

#### Scenario: Log action opens the history view
- **WHEN** the user taps "Log" on a client detail screen
- **THEN** the app SHALL open a full-screen log page with a way back to the client
- **AND** the page SHALL show the client's edit history: the create entry and the edit entries
- **AND** each entry SHALL show the actor (user name when known) and the timestamp

#### Scenario: Log is reachable from the detail screen
- **WHEN** the client detail screen is shown
- **THEN** a "Log" action SHALL be available to the user

#### Scenario: Entries are grouped by day
- **WHEN** the log page shows entries from more than one day
- **THEN** entries SHALL be grouped under a date heading for each day, in chronological order

#### Scenario: Empty history is presented clearly
- **WHEN** a client has no recorded history (e.g. migrated data)
- **THEN** the log page SHALL indicate that no history is available

#### Scenario: Entry timestamps use a 24-hour clock
- **WHEN** the log page shows an entry timestamp on a device whose locale would use a 12-hour clock
- **THEN** the time SHALL be presented in 24-hour format (no AM/PM marker)

#### Scenario: History fetch failure is reported
- **WHEN** the history fetch fails (network, authentication, or server error)
- **THEN** the log page SHALL show that the history could not be loaded, distinct from the "no history" state
- **AND** offer a retry action that re-runs the request

#### Scenario: Pending client notes history after sync
- **WHEN** a client has local pending changes (created or edited but not yet acknowledged by the server) and the log has no server entries
- **THEN** the log page SHALL indicate that the history will appear after synchronization, rather than only saying no history is available
