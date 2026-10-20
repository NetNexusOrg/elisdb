## ADDED Requirements

### Requirement: Server and device client totals shown

The synchronization screen SHALL show the number of clients stored on the server alongside the number stored on the device. When the server total is unknown, the screen SHALL show a placeholder instead of a misleading zero. The server total SHALL be refreshed by the status check that refreshes the pending-downloads indicator, without transferring client records.

#### Scenario: Both totals are shown
- **WHEN** the user opens the synchronization screen with a configured server and a reachable connection
- **THEN** the screen SHALL show how many clients are stored on the server
- **AND** the screen SHALL show how many clients are stored on the device

#### Scenario: Unknown server total shows a placeholder
- **WHEN** the server total has not been retrieved (no configuration, no connection, or never fetched)
- **THEN** the server row SHALL show a placeholder rather than `0`

#### Scenario: Server total refreshes without transferring data
- **WHEN** the user opens the screen or taps the check-for-updates action
- **THEN** the screen SHALL refresh the server client total
- **AND** it SHALL NOT push or pull client records

#### Scenario: Server total updates after a sync
- **WHEN** a sync completes successfully
- **THEN** the screen SHALL show the server client total as of that sync
