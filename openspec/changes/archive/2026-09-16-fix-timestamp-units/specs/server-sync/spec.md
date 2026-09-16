## ADDED Requirements

### Requirement: Data timestamps are epoch seconds

The server SHALL persist and exchange all data timestamps — a record's modification timestamp (`updated_at`), its creation timestamp (`created_at`), a deletion's timestamp (`deleted_at`), and the audit history timestamp (`ts`) — as Unix epoch seconds, on the same scale as the request-signing timestamp and the `/api/time` endpoint, so last-write-wins, incremental pulls and the latest-timestamp endpoint compare values of one unit.

#### Scenario: updated_at is stored in seconds
- **WHEN** a client pushes a record whose modification timestamp is epoch seconds
- **THEN** the server SHALL store that `updated_at` value unchanged
- **AND** SHALL use it for last-write-wins against other records on the same scale

#### Scenario: deleted_at and updated_at compare on one scale
- **WHEN** a record is deleted and a concurrent edit for the same id is pushed
- **THEN** the tombstone's `deleted_at` and the record's `updated_at` SHALL be comparable on the same epoch-seconds scale to resolve last-write-wins

#### Scenario: Latest timestamp is in seconds
- **WHEN** a client requests the server's latest data timestamp
- **THEN** the returned maximum over `updated_at` and `deleted_at` SHALL be epoch seconds, comparable to the client's pull watermark

#### Scenario: Audit history timestamp is in seconds
- **WHEN** a client requests a record's edit history
- **THEN** each entry's `ts` SHALL be epoch seconds, so the client renders it as a wall-clock time on the same scale as its other timestamps

### Requirement: Legacy millisecond timestamps are migrated

The server SHALL convert any legacy timestamps stored in milliseconds to seconds on startup, so databases created before the seconds normalization keep working correctly. The conversion SHALL be idempotent and SHALL NOT re-convert values already stored in seconds.

#### Scenario: Millisecond values are converted once
- **WHEN** the server starts against a database whose `updated_at`/`created_at`/audit `ts` are stored in milliseconds
- **THEN** those values SHALL be divided to seconds
- **AND** a subsequent start SHALL NOT convert them again

#### Scenario: Second values are left unchanged
- **WHEN** the server starts against a database whose timestamps are already in seconds
- **THEN** those values SHALL remain unchanged
