## Purpose

Adds a creation timestamp to each stored client record so the real creation date survives edits, is returned to clients on pull, and is never overwritten by last-write-wins updates. Also makes the pull endpoint incremental and backward-compatible.

## ADDED Requirements

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

The server SHALL accept optional `since` (return only records whose `updated_at` is newer) and optional `limit` (page size) query parameters on the pull endpoint, and SHALL keep the default (no parameters) response a flat list for backward compatibility.

#### Scenario: Pull filtered by since
- **WHEN** a client pulls with a `since` timestamp
- **THEN** the server SHALL return only records whose `updated_at` is strictly newer than `since`

#### Scenario: Paginated pull with cursor
- **WHEN** a client pulls with a `limit`
- **THEN** the server SHALL return at most `limit` records plus a `next_since` cursor when a further page exists
- **AND** `next_since` SHALL be None when the page is the last

#### Scenario: Unrestricted pull stays a flat list
- **WHEN** a client pulls without a `limit`
- **THEN** the server SHALL return the historical flat list of records (ordered by name), so existing clients keep working unchanged