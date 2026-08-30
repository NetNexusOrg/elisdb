## MODIFIED Requirements

### Requirement: Record identity

Each client record SHALL carry a stable client identifier and a modification timestamp used for conflict resolution. Photos SHALL belong to exactly one client, and a photo identifier owned by one client SHALL NOT be silently reassigned to another client. Coordinates SHALL be optional.

#### Scenario: Record has identity and timestamp
- **WHEN** a client record is received
- **THEN** it SHALL have an identifier, a modification timestamp, name, and optional contract and notes
- **AND** its photos SHALL be associated only with that client

#### Scenario: Coordinates are optional
- **WHEN** a client record is received without latitude or longitude
- **THEN** the record SHALL be accepted and stored with no coordinates

#### Scenario: Pull returns null coordinates
- **WHEN** a client pulls records and a stored client has no coordinates
- **THEN** the pulled record SHALL carry null latitude and longitude

#### Scenario: Photo id owned by another client is rejected
- **WHEN** a push assigns a photo whose id is already stored and belongs to a different client
- **THEN** the server SHALL reject the record rather than reassign the photo to the new client
- **AND** the existing photo SHALL remain attached to its original client

### Requirement: Incremental pull

The server SHALL accept optional `since` (return only records whose `updated_at` is newer) and optional `limit` (page size) query parameters on the pull endpoint, and SHALL keep the default (no parameters) response a flat list for backward compatibility. Pagination SHALL use a cursor composed of the last record's `updated_at` and its `id` so that records sharing the same `updated_at` are never skipped.

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