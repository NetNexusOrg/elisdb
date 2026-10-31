## MODIFIED Requirements

### Requirement: Clock offset measured from the server

The system SHALL measure the difference between the device clock and the server clock, persist it and use it to normalize timestamps so records compare fairly in last-write-wins. The offset SHALL be refreshed from the `Date` header of every server response and at app start, and a signed request rejected as stale SHALL be retried once with a corrected timestamp. A skew of 120 seconds or more SHALL be shown to the user with a correction action.

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

#### Scenario: Offset refreshed from every response
- **WHEN** any server response carries a `Date` header
- **THEN** the persisted offset SHALL be updated from it without an extra request

#### Scenario: Offset measured at app start
- **WHEN** the app starts with a server configuration available
- **THEN** the offset SHALL be measured before the first sync of that session

#### Scenario: Stale timestamp retried once
- **WHEN** the server rejects a signed request because the timestamp is stale
- **THEN** the app SHALL correct the offset from the server time
- **AND** repeat the request once with a freshly signed timestamp
- **AND** only report a clock fault if the retry is also rejected

#### Scenario: Invalid key is not retried
- **WHEN** the server rejects a signed request because the key or signature is invalid
- **THEN** the app SHALL report the auth failure without retrying

#### Scenario: Large skew surfaced with a correction action
- **WHEN** the measured device↔server skew is 120 seconds or more
- **THEN** the configuration screen SHALL show how far off the clock is and in which direction
- **AND** offer an action that re-measures the offset immediately
- **AND** the skew warning SHALL disappear once the skew is below the threshold
