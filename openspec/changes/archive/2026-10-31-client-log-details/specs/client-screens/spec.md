## MODIFIED Requirements

### Requirement: Log action on the client detail screen

The system SHALL offer a "Log" action on the client detail screen that shows who created the client and who edited it, when, and what changed: the fields whose values differ (previous and new value) and the photos added, removed or reordered. Entry timestamps SHALL be presented with a 24-hour clock regardless of the device locale. The view SHALL distinguish a fetch failure from genuinely empty history, SHALL note when a client has local pending changes whose history will only appear after synchronization, and SHALL still present entries recorded without change details.

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

#### Scenario: History fetch failure is reported
- **WHEN** the history fetch fails (network, authentication, or server error)
- **THEN** the log view SHALL show that the history could not be loaded, distinct from the "no history" state

#### Scenario: Pending client notes history after sync
- **WHEN** a client has local pending changes (created or edited but not yet acknowledged by the server) and the log has no server entries
- **THEN** the log view SHALL indicate that the history will appear after synchronization, rather than only saying no history is available

#### Scenario: Entry lists the changed fields
- **WHEN** an entry carries change details with changed fields
- **THEN** the log view SHALL show each changed field's name, its previous value and its new value
- **AND** SHALL still show the actor and the timestamp of the entry

#### Scenario: Entry summarizes photo changes
- **WHEN** an entry carries photo changes
- **THEN** the log view SHALL state how many photos were added and removed and whether the photos were reordered
- **AND** SHALL show a thumbnail for each added photo that still exists on the device

#### Scenario: Entry without details keeps the actor-only view
- **WHEN** an entry was recorded without change details
- **THEN** the log view SHALL present it with the actor and the timestamp, as before
- **AND** SHALL NOT show an empty or broken change section
