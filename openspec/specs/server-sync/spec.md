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

The server SHALL accept a push of client and photo changes from a client and persist them, including the client's optional tag. Pushing a client SHALL also push its photos together with the client. A push SHALL be processed record by record: an invalid record SHALL NOT abort the batch, and the response SHALL carry a per-record result indicating whether each record was accepted, found duplicate, found stale, or rejected as invalid. A record whose photos are referenced but missing from storage SHALL still be accepted, and the result SHALL name those photos so the client sends them again.

#### Scenario: Client pushes new records
- **WHEN** a client pushes a batch of new or updated client records with their photos
- **THEN** each record and photo SHALL be persisted
- **AND** a record carrying a tag SHALL store that tag

#### Scenario: Push is idempotent per record
- **WHEN** the same client record is pushed more than once
- **THEN** it SHALL be stored once, not duplicated

#### Scenario: An invalid record does not abort the batch
- **WHEN** a client pushes a batch in which one record is invalid (e.g. a blank name)
- **THEN** the server SHALL process the remaining records normally
- **AND** SHALL return a per-record result marking the invalid record as rejected

#### Scenario: Missing photos are reported back
- **WHEN** a pushed record references a photo whose file is not stored
- **THEN** the record SHALL be accepted
- **AND** the result SHALL list that photo as missing so the client uploads it again

#### Scenario: Pushed record without a tag clears the stored tag
- **WHEN** a client pushes an update for a stored client and omits the tag
- **THEN** the stored client SHALL be persisted without a tag

### Requirement: Pull changes

The server SHALL allow a client to pull all stored client records with their photos, including each client's active/inactive status and optional tag.

#### Scenario: Client pulls all data
- **WHEN** a client requests a pull
- **THEN** the server SHALL return all stored clients with their photos

#### Scenario: Pull returns the client status
- **WHEN** a client pulls records and a stored client is inactive
- **THEN** the pulled record SHALL indicate that the client is inactive

#### Scenario: Pull returns the client tag
- **WHEN** a client pulls records and a stored client has a tag
- **THEN** the pulled record SHALL include that tag
- **AND** a stored client without a tag SHALL be returned without one

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

The server SHALL resolve edit conflicts between records referencing the same stored client by last-write-wins, where the record with the most recent modification timestamp wins and overwrites the stored value. Only the timestamps sent by the clients SHALL decide a conflict: the delivery timestamp is the server's own cursor for incremental pulls and SHALL NOT make an otherwise newer edit lose.

#### Scenario: Newer update wins
- **WHEN** a client record is updated whose modification timestamp is newer than the stored record's
- **THEN** the server SHALL store the newer values

#### Scenario: Stale update loses
- **WHEN** a client record is pushed whose modification timestamp is older than the stored record's
- **THEN** the server SHALL keep the newer stored values
- **AND** the stale payload SHALL NOT overwrite the stored record

#### Scenario: Delivery timestamp does not decide conflicts
- **WHEN** a push carries a modification timestamp newer than the stored record's but older than that record's delivery timestamp
- **THEN** the push SHALL still win and replace the stored record

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

Each client record SHALL carry a stable client identifier and a modification timestamp used for conflict resolution. Photos SHALL belong to exactly one client, and a photo identifier owned by one client SHALL NOT be silently reassigned to another client. Coordinates SHALL be optional. The active/inactive status SHALL be optional in the payload and default to active.

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

The server SHALL accept optional `since` (return only records delivered after it) and optional `limit` (page size) query parameters on the pull endpoint, and SHALL keep the default (no parameters) response a flat list for backward compatibility. Pagination SHALL use a cursor composed of the last record's delivery timestamp and its `id` so that records sharing the same delivery timestamp are never skipped. A `limit` value SHALL be at least 1 and SHALL be capped at a server maximum; a `since_id` SHALL only be honored together with a `since` value. Every returned record and tombstone SHALL carry its delivery timestamp, which the client uses as its watermark.

#### Scenario: Pull filtered by since
- **WHEN** a client pulls with a `since` timestamp
- **THEN** the server SHALL return only records whose delivery timestamp is strictly newer than `since`

#### Scenario: Paginated pull with cursor
- **WHEN** a client pulls with a `limit`
- **THEN** the server SHALL return at most `limit` records plus a cursor composed of the last record's delivery timestamp and `id` when a further page exists
- **AND** the cursor SHALL be None when the page is the last

#### Scenario: Records with equal updated_at are not skipped
- **WHEN** a client pages through records and two or more records share the same delivery timestamp
- **THEN** a subsequent page SHALL include every remaining record with that timestamp, ordered by `id`
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

#### Scenario: Records carry their delivery timestamp
- **WHEN** a client receives a pulled record or tombstone
- **THEN** it SHALL carry the delivery timestamp the server assigned to it

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

The delete endpoint SHALL accept the deleting device's timestamp so the deletion competes in last-write-wins with concurrent edits, and SHALL record a separate delivery timestamp, assigned by the server, that is strictly greater than every timestamp already delivered so the tombstone reaches clients whose watermark is ahead of the deletion.

#### Scenario: Delete with device timestamp
- **WHEN** a client is deleted with a device-supplied timestamp
- **THEN** the server SHALL store the tombstone with that timestamp
- **AND** remove the client and its photos

#### Scenario: Delete without a timestamp uses server time
- **WHEN** a client is deleted without a device timestamp
- **THEN** the server SHALL store the tombstone with its own current time

#### Scenario: Delete is delivered even with the device clock behind
- **WHEN** a client deletes a record with a timestamp older than what other clients have already pulled
- **THEN** the tombstone's delivery timestamp SHALL still be newer than their watermark
- **AND** the deletion SHALL reach them on their next delta pull

### Requirement: Client edit history recorded on push

The server SHALL record an audit entry each time a client change is accepted, capturing the actor's user name (from the authenticated key's allowlist comment), the actor's key id, the timestamp, whether the change created or updated the client, and what changed: the fields whose values differ (previous and new value) and the photos added, removed or reordered. The actor SHALL be read from the per-request `request.state` set by authentication. An accepted push that changes neither a field nor a photo SHALL NOT create an entry.

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

#### Scenario: Edit records the changed fields
- **WHEN** a push changes one or more of a client's fields
- **THEN** the audit entry SHALL list each changed field with its previous value and its new value

#### Scenario: Photo changes are recorded
- **WHEN** a push adds, removes or reorders a client's photos
- **THEN** the audit entry SHALL record the added photo ids, the removed photo ids and whether the remaining photos were reordered

#### Scenario: Unchanged push records no entry
- **WHEN** a push is accepted but changes neither a field nor a photo
- **THEN** the server SHALL NOT create an audit entry for it

#### Scenario: Create records its photos
- **WHEN** a client is created with photos
- **THEN** the create audit entry SHALL record the added photo ids
- **AND** SHALL NOT list field values as changes

### Requirement: Per-client edit history endpoint

The server SHALL expose an authenticated endpoint returning the audit history of a client, ordered from oldest to newest, so the app can show who created and edited the client, when, and what changed. Each entry SHALL carry the actor user name when present, the key id, the timestamp, the action marker (`create`, `edit`, or `delete`), and the recorded change details, or a null details value for entries recorded before details existed. The endpoint SHALL accept an optional `limit` (1 to 200) and an optional `after_id` cursor. When a limit is supplied, the response SHALL return at most that many entries whose id is greater than `after_id`, plus a `next_after_id` value when more entries exist. Without pagination parameters, the endpoint SHALL return the full history for backward compatibility.

#### Scenario: History returned for a client
- **WHEN** the app requests the edit history of a stored client
- **THEN** the server SHALL return the client's audit entries (actor user name when present, key id, timestamp, create/edit/delete marker), ordered by timestamp

#### Scenario: Unknown client returns empty history
- **WHEN** the app requests the edit history of a client that does not exist
- **THEN** the server SHALL return an empty history (or a not-found response), not an error with partial data

#### Scenario: History carries the recorded changes
- **WHEN** a client's history contains an entry recorded with change details
- **THEN** the response SHALL include those details (changed fields and photo changes) for that entry
- **AND** entries recorded before details existed SHALL return a null details value

#### Scenario: Paginated history returns one page
- **WHEN** the app requests a client's history with a `limit`
- **THEN** the server SHALL return at most `limit` entries in ascending id order
- **AND** SHALL include `next_after_id` when further entries exist

#### Scenario: Next page continues after the cursor
- **WHEN** the app requests the next page using `next_after_id`
- **THEN** the server SHALL return only entries with an id greater than that cursor
- **AND** SHALL NOT repeat entries from the previous page

#### Scenario: Last history page has no cursor
- **WHEN** no entries remain after the requested page
- **THEN** the response SHALL carry no `next_after_id`
- **AND** the app SHALL treat the history as fully loaded

#### Scenario: Invalid history limit is rejected
- **WHEN** the app requests a history limit below 1 or above the maximum
- **THEN** the server SHALL reject the request with a client-error response

#### Scenario: History without pagination stays complete
- **WHEN** the app requests a client's history without `limit` or `after_id`
- **THEN** the server SHALL return the full history as before

### Requirement: Server reports its latest data timestamp

The server SHALL expose its latest data timestamp (the maximum delivery timestamp over stored clients and tombstones) so a client can detect work to receive without performing a full pull.

#### Scenario: Latest timestamp endpoint returns the newest update
- **WHEN** a client requests the server's latest data timestamp
- **THEN** the server SHALL return the maximum delivery timestamp among all stored clients and all tombstones
- **AND** the value SHALL be on the same epoch scale as client timestamps

#### Scenario: Latest timestamp is comparable to the pull watermark
- **WHEN** a client compares the returned latest timestamp with its own pull watermark
- **THEN** a latest timestamp strictly greater than the watermark SHALL indicate that there is server data the client has not yet pulled

#### Scenario: Latest timestamp reflects deletions too
- **WHEN** the newest change is a deletion
- **THEN** the reported timestamp SHALL be that delete's delivery timestamp

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

### Requirement: Field length limits

The server SHALL reject client records whose text fields exceed a maximum length, so a single record cannot bloat the database or the pull responses with unbounded text. The fields `nome`, `contrato`, `notas` and `tag` SHALL each have a maximum length.

#### Scenario: Oversized text field is rejected
- **WHEN** a client pushes a record whose `nome`, `contrato`, `notas` or `tag` exceeds the maximum length
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

### Requirement: Server reports its stored client count

The server's latest-data response SHALL include the number of client records it currently stores, so a client can show how many clients exist on the server without pulling the dataset. The count SHALL include only live clients: records deleted (represented by tombstones) SHALL NOT be counted.

#### Scenario: Latest response includes the client count
- **WHEN** a client requests the server's latest data timestamp
- **THEN** the response SHALL include the number of client records currently stored
- **AND** the count SHALL be returned alongside the latest timestamp in the same response

#### Scenario: Count reflects pushes
- **WHEN** clients are pushed to the server
- **THEN** the reported client count SHALL include the newly stored records

#### Scenario: Deleted clients are not counted
- **WHEN** a client is deleted from the server
- **THEN** the reported client count SHALL decrease
- **AND** the tombstone left by the deletion SHALL NOT be counted as a client

### Requirement: Client status changes appear in the edit history

The server SHALL record a status change in the client's edit history when a push changes a client between active and inactive, identifying the change as an activation or a deactivation.

#### Scenario: Deactivation appears in the history
- **WHEN** a push updates a client from active to inactive
- **THEN** the stored history entry SHALL identify the status change as a deactivation

#### Scenario: Activation appears in the history
- **WHEN** a push updates a client from inactive to active
- **THEN** the stored history entry SHALL identify the status change as an activation

### Requirement: API version in server status

The server SHALL expose an API version, independent from its release version, in
both the open health response and the authenticated latest-data response. The
API version SHALL increase whenever a contract change matters for older clients.
The field SHALL be additive so older clients keep working.

#### Scenario: Health reports the API version
- **WHEN** a client requests `/health`
- **THEN** the response SHALL include `api_version` with the current API version

#### Scenario: Latest data reports the API version
- **WHEN** an authenticated client requests `/api/latest`
- **THEN** the response SHALL include `api_version` with the current API version
- **AND** SHALL keep `latest_updated_at` and `client_count`

#### Scenario: Older clients are unaffected
- **WHEN** a client that does not know `api_version` reads either response
- **THEN** it SHALL continue to work because the field is additive

### Requirement: Push batch size is bounded

The server SHALL enforce a configured maximum number of client records per push and SHALL reject an oversized batch before persisting any of its records, returning a client-error response that states the limit. A batch within the limit SHALL be processed with the existing per-record semantics.

#### Scenario: Oversized batch is rejected atomically
- **WHEN** a client pushes more records than the configured maximum
- **THEN** the server SHALL reject the whole request with a client-error response
- **AND** SHALL NOT persist any record from that batch
- **AND** the response SHALL identify the maximum accepted batch size

#### Scenario: Batch within the limit is accepted
- **WHEN** a client pushes a batch at or below the configured maximum
- **THEN** the server SHALL process it record by record as before
- **AND** SHALL return a per-record status for every record

#### Scenario: Limit is configurable
- **WHEN** an operator configures the push batch limit
- **THEN** the server SHALL use that value for subsequent pushes
- **AND** the default SHALL remain bounded

#### Scenario: Limit is discoverable by clients
- **WHEN** a client requests the authenticated latest-data response
- **THEN** the response SHALL include the effective maximum push batch size
- **AND** the value SHALL match the limit enforced by the push endpoint

### Requirement: Deletion respects last-write-wins

A delete SHALL compete in last-write-wins with edits. The server SHALL reject a deletion whose `deleted_at` is not newer than the stored record's `updated_at`, returning a stale response, and SHALL NOT remove the record or create a tombstone in that case.

#### Scenario: Older delete loses to a newer edit
- **WHEN** a client deletes a record with a `deleted_at` older than or equal to the stored `updated_at`
- **THEN** the server SHALL reject the deletion as stale
- **AND** the record SHALL remain stored
- **AND** no tombstone SHALL be created

#### Scenario: Newer delete removes the record
- **WHEN** a client deletes a record with a `deleted_at` newer than the stored `updated_at`
- **THEN** the server SHALL delete the record and its photos
- **AND** SHALL create a tombstone as before

### Requirement: Deletion timestamps are plausible

The server SHALL reject deletion timestamps that are not plausible Unix epoch seconds, including negative values and values beyond the existing future-skew allowance, instead of accepting them into the tombstone.

#### Scenario: Negative deletion timestamp is rejected
- **WHEN** a delete supplies a negative `deleted_at`
- **THEN** the server SHALL reject the request with a client-error response
- **AND** SHALL NOT create a tombstone

#### Scenario: Far-future deletion timestamp is rejected
- **WHEN** a delete supplies a `deleted_at` beyond the allowed future skew
- **THEN** the server SHALL reject the request as before
- **AND** SHALL NOT delete the record

### Requirement: Pull pages use bounded database work

A pull page SHALL be read with a bounded number of database connections and queries, independent of the number of records in the page. The server SHALL NOT open a separate connection per returned record, and photo metadata for a page SHALL be loaded in a bounded batch.

#### Scenario: Large page does not open one connection per record
- **WHEN** a client pulls a page near the maximum size
- **THEN** the server SHALL use a bounded number of connections
- **AND** the number of connections SHALL NOT grow linearly with the record count

#### Scenario: Photos for a page are loaded in batch
- **WHEN** a pull page contains clients with photos
- **THEN** the server SHALL load the photo metadata without a per-record query
- **AND** SHALL return the same data as before

### Requirement: Deletions are recorded in the client history

The server SHALL record an audit entry when a client deletion is accepted, capturing the actor's user name (from the authenticated key's allowlist comment), the actor's key id, the timestamp, the action `delete`, and the deleted client's identifying details. A deletion rejected as stale or invalid SHALL NOT create an entry.

#### Scenario: Delete records the actor
- **WHEN** a client is deleted by an authenticated key whose allowlist entry has a user name
- **THEN** the server SHALL store a delete audit entry with the actor's user name, the key id, and the deletion timestamp

#### Scenario: Unnamed key records no user name
- **WHEN** a client is deleted by a key with no user name
- **THEN** the delete audit entry SHALL record the key id and timestamp
- **AND** SHALL carry no user name

#### Scenario: Delete appears in the history
- **WHEN** the app requests the history of a client that was deleted
- **THEN** the response SHALL include the delete entry with its action marker and details

#### Scenario: Rejected delete is not audited
- **WHEN** a deletion is rejected as stale or invalid
- **THEN** the server SHALL NOT record a delete audit entry

### Requirement: Push requests are idempotent by key

The server SHALL accept an optional idempotency key on push. When a request carries a key already used with the same payload, the server SHALL return the stored prior response without re-persisting records, photos, or audit entries. When the same key is reused with a different payload, the server SHALL reject the request with a conflict response. Requests without a key SHALL keep the existing per-record idempotency behavior.

#### Scenario: Same key and payload return the prior result
- **WHEN** a client retries a push with the same idempotency key and the same payload
- **THEN** the server SHALL return the result stored for the first request
- **AND** SHALL NOT persist the records or audit entries again
- **AND** SHALL NOT require photos already accepted to be uploaded again

#### Scenario: Same key with a different payload is rejected
- **WHEN** a client reuses an idempotency key with a different payload
- **THEN** the server SHALL reject the request with a conflict response
- **AND** SHALL NOT apply the new payload

#### Scenario: Requests without a key still work
- **WHEN** a client pushes without an idempotency key
- **THEN** the server SHALL process the records normally
- **AND** SHALL keep the existing per-record duplicate detection

#### Scenario: Retry does not duplicate audit entries
- **WHEN** a push is retried with the same key and payload
- **THEN** no duplicate create or edit audit entry SHALL be recorded
