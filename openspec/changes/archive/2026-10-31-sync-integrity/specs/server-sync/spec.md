## MODIFIED Requirements

### Requirement: Push changes

The server SHALL accept a push of client and photo changes from a client and persist them. Pushing a client SHALL also push its photos together with the client. A push SHALL be processed record by record: an invalid record SHALL NOT abort the batch, and the response SHALL carry a per-record result indicating whether each record was accepted, found duplicate, found stale, or rejected as invalid. A record whose photos are referenced but missing from storage SHALL still be accepted, and the result SHALL name those photos so the client sends them again.

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

#### Scenario: Missing photos are reported back
- **WHEN** a pushed record references a photo whose file is not stored
- **THEN** the record SHALL be accepted
- **AND** the result SHALL list that photo as missing so the client uploads it again

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
