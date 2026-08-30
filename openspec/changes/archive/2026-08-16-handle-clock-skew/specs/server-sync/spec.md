## ADDED Requirements

### Requirement: Server reports current time

The server SHALL expose its current time so clients can measure their clock offset and normalize timestamps.

#### Scenario: Time endpoint returns server time
- **WHEN** a client requests the server time endpoint
- **THEN** the server SHALL return its current time as a Unix timestamp
- **AND** the endpoint SHALL be reachable without authentication, like the health endpoint

#### Scenario: Time value is usable for offset computation
- **WHEN** a client receives the server time
- **THEN** the value SHALL be on the same epoch scale as client timestamps so the offset is computable directly
