## MODIFIED Requirements

### Requirement: Incremental pull

The server SHALL accept optional `since` (return only records whose `updated_at` is newer) and optional `limit` (page size) query parameters on the pull endpoint, and SHALL keep the default (no parameters) response a flat list for backward compatibility. Pagination SHALL use a cursor composed of the last record's `updated_at` and its `id` so that records sharing the same `updated_at` are never skipped. A `limit` value SHALL be at least 1 and SHALL be capped at a server maximum; a `since_id` SHALL only be honored together with a `since` value.

#### Scenario: Pull filtered by since
- **WHEN** a client pulls with a `since` timestamp
- **THEN** the server SHALL return only records whose `updated_at` is strictly newer than `since`

#### Scenario: Paginated pull with cursor
- **WHEN** a client pulls with a `limit`
- **THEN** the server SHALL return at most `limit` records plus a cursor composed of the last record's `updated_at` and `id` when a further page exists
- **AND** the cursor SHALL be None when the page is the last

#### Scenario: Records with equal updated_at are not skipped
- **WHEN** a client pages through records and two or more records share the same `updated_at`
- **THEN** a subsequent page SHALL include every remaining record with that `updated_at`, ordered by `id`
- **AND** no record SHALL be skipped at a page boundary

#### Scenario: Unrestricted pull stays a flat list
- **WHEN** a client pulls without a `limit`
- **THEN** the server SHALL return the historical flat list of records (ordered by name), so existing clients keep working unchanged

#### Scenario: Invalid limit is rejected
- **WHEN** a client pulls with a `limit` below 1 or above the server maximum
- **THEN** the server SHALL reject the request with a client-error response instead of applying an unbounded or non-positive page size

#### Scenario: since_id without since is rejected
- **WHEN** a client pulls with a `since_id` but no `since`
- **THEN** the server SHALL reject the request with a client-error response rather than silently ignoring the `since_id`

### Requirement: Push changes

The server SHALL accept a push of client and photo changes from a client and persist them. Pushing a client SHALL also push its photos together with the client. A push SHALL be processed record by record: an invalid record SHALL NOT abort the batch, and the response SHALL carry a per-record result indicating whether each record was accepted, found duplicate, found stale, or rejected as invalid.

#### Scenario: Client pushes new records
- **WHEN** a client pushes a batch of new or updated client records with their photos
- **THEN** each record and photo SHALL be persisted

#### Scenario: Push is idempotent per record
- **WHEN** the same client record is pushed more than once
- **THEN** it SHALL be stored once, not duplicated

#### Scenario: An invalid record does not abort the batch
- **WHEN** a client pushes a batch in which one record is invalid (e.g. a blank name)
- **THEN** the server SHALL process the remaining records normally
- **AND** SHALL return a per-record result marking the invalid record as rejected

## ADDED Requirements

### Requirement: Field length limits

The server SHALL reject client records whose text fields exceed a maximum length, so a single record cannot bloat the database or the pull responses with unbounded text. The fields `nome`, `contrato` and `notas` SHALL each have a maximum length.

#### Scenario: Oversized text field is rejected
- **WHEN** a client pushes a record whose `nome`, `contrato` or `notas` exceeds the maximum length
- **THEN** the server SHALL reject that record rather than storing the oversized text

#### Scenario: Within-limit fields are accepted
- **WHEN** a client pushes a record whose text fields are within the maximum lengths
- **THEN** the server SHALL store the record as normal

### Requirement: Future timestamps are rejected

The server SHALL reject a client record whose modification timestamp (`updated_at`) or deletion timestamp (`deleted_at`) lies in the far future relative to the server clock, so a malicious or broken timestamp cannot permanently win last-write-wins and break incremental pull for other devices.

#### Scenario: Far-future updated_at is rejected
- **WHEN** a client pushes a record whose `updated_at` is ahead of the server clock beyond the allowed window
- **THEN** the server SHALL reject that record

#### Scenario: Far-future deleted_at is rejected
- **WHEN** a client deletes a record with a `deleted_at` ahead of the server clock beyond the allowed window
- **THEN** the server SHALL reject the deletion

#### Scenario: Near-future timestamp is accepted
- **WHEN** a client pushes a record whose `updated_at` is within the allowed window of the server clock
- **THEN** the server SHALL accept the record as normal
