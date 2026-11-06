# Spec Delta

## MODIFIED Requirements

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

## ADDED Requirements

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
