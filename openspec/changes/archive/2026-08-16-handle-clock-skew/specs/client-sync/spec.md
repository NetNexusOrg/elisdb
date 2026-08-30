## ADDED Requirements

### Requirement: Clock offset measured from the server

The system SHALL measure the difference between the device clock and the server clock using the server-reported time, persist it, and use it to normalize timestamps so records compare fairly in last-write-wins regardless of device clock drift.

#### Scenario: Offset measured and applied on a successful sync
- **WHEN** a sync succeeds and the server reported its current time
- **THEN** the device↔server clock offset SHALL be computed and persisted
- **AND** subsequent `updated_at` stamps SHALL use the offset-normalized time

#### Scenario: Offset not available on first sync
- **WHEN** no clock offset has been persisted yet (first ever sync)
- **THEN** the sync SHALL still proceed using the device clock as-is

#### Scenario: Stale offset is refreshed
- **WHEN** a sync runs and the server reports its current time
- **THEN** the persisted clock offset SHALL be refreshed to the newly measured value

### Requirement: Stale push surfaced to the user

The system SHALL NOT silently discard a local edit rejected as `stale` by the server; the sync outcome SHALL report which client edits were superseded by a newer server version.

#### Scenario: Stale edit reported
- **WHEN** a pushed record is acknowledged as `stale` (a newer server version exists)
- **THEN** the sync outcome SHALL include the name of the affected client
- **AND** the user SHALL be able to see that the local edit was superseded
