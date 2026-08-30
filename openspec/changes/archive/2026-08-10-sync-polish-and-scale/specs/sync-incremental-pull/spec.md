## Purpose

Reduces the bandwidth and processing cost of a server pull so the app only downloads records that changed since the last sync instead of the whole catalog.

## ADDED Requirements

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

The app SHALL persist the largest server `updated_at` seen on pulls and SHALL send it back as `since` on subsequent pulls, so a later pull is incremental.

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

### Requirement: Photo skip by content identity (bandwidth)

In a delta pull, photos whose content is already present locally SHALL be represented without retransmitting identical base64 bytes when the changed record's photo set matches the local one.

#### Scenario: Unchanged photos not retransmitted
- **WHEN** a delta pull includes a record whose photos are unchanged from what the app already has
- **THEN** the transfer SHALL omit the identical photo base64 payloads for those photos

> **Note:** the photo-skip requirement depends on the unchanged-photo compare from
> `sync-incremental-photos` (R3). Until it lands, the app keeps `incrementalPullEnabled =
> false` (the R7 gate) and always performs full pulls, so delta behavior is dormant.