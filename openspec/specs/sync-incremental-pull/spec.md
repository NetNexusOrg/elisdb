## Purpose

Reduces the bandwidth and processing cost of a server pull so the app only downloads records that changed since the last sync instead of the whole catalog.

## Requirements

### Requirement: Pull returns changes since a point in time

The system SHALL support a pull that returns only client and photo records modified after a caller-supplied timestamp (`since`), instead of always returning the full dataset.

#### Scenario: Client pulls only changed records
- **WHEN** the app requests a pull with a `since` value
- **THEN** the server SHALL return only records whose updated_at is newer than that value (and their photos)
- **AND** the app SHALL apply them as authoritative for records with no pending local edits

#### Scenario: Full snapshot when no since provided
- **WHEN** the app requests a pull without a `since` value
- **THEN** the server SHALL return the full dataset as today
- **AND** the current behavior (full pull + local reconciliation) SHALL be preserved for first sync / compat

#### Scenario: Default pull response remains a flat list
- **WHEN** the app pulls without requesting pagination
- **THEN** the server SHALL return a flat list of records exactly as before, so older app versions keep parsing the response

#### Scenario: Paginated pull returns a page and a cursor
- **WHEN** the app requests a pull with a `limit`
- **THEN** the server SHALL return at most `limit` records together with a `next_since` cursor when a further page exists
- **AND** `next_since` SHALL be None when the returned page was the last

### Requirement: Pull watermark

The app SHALL persist the largest server `updated_at` seen on pulls and SHALL send it back as `since` on subsequent pulls, so a later pull is incremental. When the server returns a `next_since` cursor, the app SHALL continue requesting pages using that cursor until the server returns no further cursor, accumulating records across pages, and SHALL only then advance its watermark. The app SHALL NOT stop after the first page of a paginated pull.

#### Scenario: Watermark advanced after a pull
- **WHEN** a pull completes
- **THEN** the app SHALL record the maximum `updated_at` among the returned records

#### Scenario: First pull is full
- **WHEN** no watermark exists yet (first pull)
- **THEN** the app SHALL send no `since`, requesting the full dataset

#### Scenario: Delta pull skips the local-drop sweep
- **WHEN** a pull is incremental (a `since` was sent)
- **THEN** the app SHALL NOT drop local records absent from the response, because an older record merely predates `since`
- **AND** deletions SHALL continue to be handled by the delete endpoint

#### Scenario: Paginated pull is followed to completion
- **WHEN** a pull returns a `next_since` cursor
- **THEN** the app SHALL request further pages with the cursor
- **AND** SHALL advance the watermark only after the final page with no cursor

### Requirement: Photo skip by content identity (bandwidth)

In a pull, photos SHALL be represented by their content hash rather than their bytes, and the app SHALL download a photo's binary content from the dedicated photo endpoint only when it does not already hold a local copy with the same hash.

#### Scenario: Unchanged photos not retransmitted
- **WHEN** a pull returns a photo whose content hash matches a photo the app already stores locally
- **THEN** the app SHALL NOT download that photo again

#### Scenario: New or changed photos downloaded as binary
- **WHEN** a pull returns a photo whose content hash is missing locally or differs from the local copy
- **THEN** the app SHALL download that photo's binary content from the dedicated photo endpoint
- **AND** the downloaded bytes SHALL be stored locally for future pulls

### Requirement: Deletions propagate on delta pulls

The incremental pull SHALL carry tombstones so a deleted record is removed on other devices even when the pull is incremental (not a full snapshot).

#### Scenario: Tombstone returned on incremental pull
- **WHEN** the app pulls incrementally (with a `since` watermark)
- **THEN** the response SHALL include tombstones created since that watermark
- **AND** the app SHALL remove locally stored records named by those tombstones (when not pending)