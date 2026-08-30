## Purpose

Provides a minimal, standalone database server that persists clients and their photos in a single shared store, rejects duplicate records, and resolves edit conflicts by last-write-wins so that offline-first clients can sync their data through a simple push/pull HTTP API.

## Requirements

### Requirement: Authoritative remote store

The server SHALL act as the authoritative, persistent store for client records and their associated photos, and SHALL own all database management responsibilities including persistence, duplicate detection and conflict resolution.

#### Scenario: Server persists records
- **WHEN** a client with photos is pushed to the server
- **THEN** the server SHALL store the client and its photos durably

#### Scenario: Data survives server restart
- **WHEN** the server is restarted
- **THEN** all previously stored clients and photos SHALL still be available

### Requirement: Push changes

The server SHALL accept a push of client and photo changes from a client and persist them. Pushing a client SHALL also push its photos together with the client.

#### Scenario: Client pushes new records
- **WHEN** a client pushes a batch of new or updated client records with their photos
- **THEN** each record and photo SHALL be persisted

#### Scenario: Push is idempotent per record
- **WHEN** the same client record is pushed more than once
- **THEN** it SHALL be stored once, not duplicated

### Requirement: Pull changes

The server SHALL allow a client to pull all stored client records with their photos.

#### Scenario: Client pulls all data
- **WHEN** a client requests a pull
- **THEN** the server SHALL return all stored clients with their photos

### Requirement: Duplicate detection

The server SHALL reject records that represent the same real-world entity as an existing record to prevent duplicates. Records SHALL be compared by the client name; case and surrounding whitespace SHALL be ignored.

#### Scenario: Duplicate name rejected on create
- **WHEN** a client is pushed whose name matches an existing client's name, ignoring case and surrounding whitespace
- **THEN** the server SHALL refuse to create a duplicate
- **AND** SHALL return an error identifying the conflict

#### Scenario: Same-name record is a duplicate
- **WHEN** two client records have the same name ignoring case and surrounding whitespace
- **THEN** the server SHALL treat them as duplicates of one another

### Requirement: Conflict resolution

The server SHALL resolve edit conflicts between records referencing the same stored client by last-write-wins, where the record with the most recent modification timestamp wins and overwrites the stored value.

#### Scenario: Newer update wins
- **WHEN** a client record is updated whose modification timestamp is newer than the stored record's
- **THEN** the server SHALL store the newer values

#### Scenario: Stale update loses
- **WHEN** a client record is pushed whose modification timestamp is older than the stored record's
- **THEN** the server SHALL keep the newer stored values
- **AND** the stale payload SHALL NOT overwrite the stored record

### Requirement: Duplicate push response carries the canonical record

When a push is acknowledged as `duplicate`, the server SHALL include the canonical record (client fields and its photos) in the response so the client can reconcile without a separate round-trip, and SHALL NOT have modified the canonical record by refusing the pushed record.

#### Scenario: Duplicate response includes canonical client and photos
- **WHEN** a client pushes a record that the server acknowledges as `duplicate`
- **THEN** the response SHALL include the canonical record's client fields and its photos
- **AND** the canonical record SHALL be returned unchanged from its stored state

#### Scenario: Pushed duplicate is not persisted
- **WHEN** a client pushes a record acknowledged as `duplicate`
- **THEN** the pushed record SHALL NOT be stored under its own identifier
- **AND** the canonical record and its photos SHALL remain intact

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

### Requirement: Simple management API

The server SHALL expose a minimal management surface: list all clients and delete a client by identifier (cascading to its photos).

#### Scenario: List all clients
- **WHEN** a management request lists clients
- **THEN** all stored clients SHALL be returned

#### Scenario: Delete a client and its photos
- **WHEN** a management request deletes a client by identifier
- **THEN** the client and its photos SHALL be removed from the store

### Requirement: Creation timestamp on records

The server SHALL store a creation timestamp on each client record, set once when the record is first persisted and SHALL NOT change it on later updates. Records inserted by the migration SHALL back-fill the creation timestamp from the modification timestamp.

#### Scenario: Creation timestamp set on first push
- **WHEN** a client record is persisted for the first time
- **THEN** its creation timestamp SHALL be recorded and fixed

#### Scenario: Creation timestamp unchanged on update
- **WHEN** an existing client record is updated with newer values
- **THEN** the stored creation timestamp SHALL NOT be replaced by the new modification timestamp

#### Scenario: Pull returns the creation timestamp
- **WHEN** a client pulls records
- **THEN** each record SHALL include its persisted creation timestamp alongside its modification timestamp

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

### Requirement: Client name stored in uppercase on the server

The server SHALL store the client's name in uppercase letters when a client is pushed, regardless of the casing sent by the client, so names stay consistent across synchronized devices.

#### Scenario: Pushed name is uppercased
- **WHEN** a client is pushed whose name contains lowercase or mixed-case letters
- **THEN** the server SHALL store the name in uppercase letters

#### Scenario: Pull returns the uppercased name
- **WHEN** a client pulls records whose names were pushed in lowercase or mixed case
- **THEN** the names SHALL be returned in uppercase letters

### Requirement: Server reports current time

The server SHALL expose its current time so clients can measure their clock offset and normalize timestamps.

#### Scenario: Time endpoint returns server time
- **WHEN** a client requests the server time endpoint
- **THEN** the server SHALL return its current time as a Unix timestamp
- **AND** the endpoint SHALL be reachable without authentication, like the health endpoint

#### Scenario: Time value is usable for offset computation
- **WHEN** a client receives the server time
- **THEN** the value SHALL be on the same epoch scale as client timestamps so the offset is computable directly

### Requirement: Delete endpoint accepts a deletion timestamp

The delete endpoint SHALL accept the deleting device's timestamp so the deletion competes in last-write-wins with concurrent edits.

#### Scenario: Delete with device timestamp
- **WHEN** a client is deleted with a device-supplied timestamp
- **THEN** the server SHALL store the tombstone with that timestamp
- **AND** remove the client and its photos

#### Scenario: Delete without a timestamp uses server time
- **WHEN** a client is deleted without a device timestamp
- **THEN** the server SHALL store the tombstone with its own current time

### Requirement: Client edit history recorded on push

The server SHALL record an audit entry each time a client is persisted, capturing the actor's user name (from the authenticated key's allowlist comment), the actor's key id, the timestamp, and whether the persistence created or updated the client. The actor (key id and user name) SHALL be read from the per-request `request.state` set by authentication, so concurrent requests attribute the audit entry to the correct device and never to another request's identity.

#### Scenario: Create records the actor
- **WHEN** a client is created via a push from an authenticated key whose allowlist entry has a user name
- **THEN** the server SHALL store a create audit entry for that client with the actor's user name, the key id, and the creation timestamp

#### Scenario: Update records the actor
- **WHEN** an existing client is updated via a push
- **THEN** the server SHALL store an edit audit entry for that client with the actor's user name, the key id, and the update timestamp

#### Scenario: Unnamed key records no user name
- **WHEN** a client is persisted by a key whose allowlist entry has no user name
- **THEN** the audit entry SHALL record the key id and timestamp, and SHALL carry no user name

#### Scenario: Audit actor is correct under concurrent requests
- **WHEN** two clients are pushed concurrently by two different authenticated keys
- **THEN** each audit entry SHALL record the key id and user name of the request that performed that push
- **AND** no audit entry SHALL attribute a push to the other request's key

### Requirement: Per-client edit history endpoint

The server SHALL expose an authenticated endpoint returning the audit history of a client, ordered from oldest to newest, so the app can show who created and edited the client and when.

#### Scenario: History returned for a client
- **WHEN** the app requests the edit history of a stored client
- **THEN** the server SHALL return the client's audit entries (actor user name when present, key id, timestamp, create/edit marker), ordered by timestamp

#### Scenario: Unknown client returns empty history
- **WHEN** the app requests the edit history of a client that does not exist
- **THEN** the server SHALL return an empty history (or a not-found response), not an error with partial data

### Requirement: Server reports its latest data timestamp

The server SHALL expose its latest data timestamp (the maximum `updated_at` over stored clients and the maximum `deleted_at` over tombstones) so a client can detect work to receive without performing a full pull.

#### Scenario: Latest timestamp endpoint returns the newest update
- **WHEN** a client requests the server's latest data timestamp
- **THEN** the server SHALL return the maximum `updated_at` among all stored clients and the maximum `deleted_at` among all tombstones
- **AND** the value SHALL be on the same epoch scale as client timestamps

#### Scenario: Latest timestamp is comparable to the pull watermark
- **WHEN** a client compares the returned latest timestamp with its own pull watermark
- **THEN** a latest timestamp strictly greater than the watermark SHALL indicate that there is server data the client has not yet pulled

### Requirement: Concurrent access safety

The server SHALL configure its SQLite store for concurrent access so that multiple devices synronizing in parallel do not cause "database is locked" errors. The server SHALL enable WAL journal mode and a `busy_timeout` on its database connections, and SHALL apply the schema and any migrations exactly once at process startup rather than re-running them on every request. A connection opened per request is acceptable for the single-process server, provided these properties are already established.

#### Scenario: Concurrent pushes do not error with database locked
- **WHEN** several authenticated devices push client records at the same time
- **THEN** the server SHALL persist all of them
- **AND** SHALL NOT return a "database is locked" error to any of the requests

#### Scenario: Schema and migrations run once at startup
- **WHEN** the server process starts
- **THEN** the schema and any pending migrations SHALL be applied a single time
- **AND** subsequent per-request connections SHALL NOT re-run schema or migrations

#### Scenario: WAL mode is enabled
- **WHEN** the server opens its database
- **THEN** the connection SHALL use WAL journal mode and a non-zero busy_timeout
- **AND** concurrent readers and a writer SHALL be permitted without locking errors