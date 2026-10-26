## MODIFIED Requirements

### Requirement: Push records to the server

The system SHALL send local client and photo records to the server via the push endpoint, returning a per-record status, and SHALL represent unsynced records as base64 photos. Each record SHALL remain pending until the server acknowledges its state, and the acknowledgement SHALL NOT write record content: a record edited while the request was in flight SHALL stay pending with the user's edit intact. Photos the server reports as missing SHALL be uploaded again instead of being remembered as delivered.

#### Scenario: Push returns per-record status
- **WHEN** the app sends a batch of client and photo records to the server
- **THEN** the server response SHALL be interpreted as a per-record status per each pushed client
- **AND** the statuses SHALL be (accepted/duplicate/stale) and returned onward to the caller

#### Scenario: Sending local changes clears pending after ack
- **WHEN** the app has pending (created or edited) client records with their photos and connectivity is available
- **THEN** those records SHALL be sent to the server
- **AND** each SHALL remain pending until the server acknowledges its individual status

#### Scenario: Push failure leaves data intact
- **WHEN** a push cannot complete (no connectivity or transient error)
- **THEN** the pending records SHALL be kept
- **AND** the app SHALL retry later without data loss

#### Scenario: Edit during the push is not overwritten
- **WHEN** the user edits a record while its push is in flight
- **THEN** the edit SHALL be kept
- **AND** the record SHALL remain pending

#### Scenario: Stale ack does not discard the local edit
- **WHEN** the server reports a pushed record as stale and the record was not edited during the push
- **THEN** the app SHALL clear its pending flag
- **AND** the server's version SHALL be applied by the following pull

#### Scenario: Missing photos are pushed again
- **WHEN** the push result names photos the server does not have
- **THEN** the app SHALL NOT record them as delivered
- **AND** SHALL upload them on the next push

### Requirement: Pull applies tombstones locally

The app SHALL drop local records whose identifier appears in the pull response's tombstones, unless the record has pending local edits. Tombstones are delivered by delivery timestamp, so a deletion made by a device with a slow clock still reaches devices whose watermark is ahead of the deletion timestamp.

#### Scenario: Tombstoned local record removed
- **WHEN** a pull returns a tombstone for a locally stored record with no pending edits
- **THEN** the app SHALL delete the local record and its photos

#### Scenario: Tombstoned record with pending edits kept
- **WHEN** a pull returns a tombstone for a locally stored record that has pending edits
- **THEN** the app SHALL keep the record locally pending for the next push

#### Scenario: Deletion from a slow device still arrives
- **WHEN** a record is deleted with a timestamp older than the local watermark
- **THEN** the next pull SHALL still return its tombstone

### Requirement: Pull exhausts pagination

The app SHALL follow the server's pagination cursor returned by the pull endpoint: after each page it SHALL inspect `next_since` (and `next_since_id` when present) and, when a cursor is returned, SHALL request the next page, accumulating records across all pages, before reconciling local data. The app SHALL NOT delete or drop local client records based on a single, partial page of a pull. A local-record sweep (removing clients absent from the server) SHALL run only after the full set of pages has been received, or never for an incremental (delta) pull. The watermark SHALL NOT advance past a record the app deliberately skipped because it had pending edits.

#### Scenario: Multi-page pull is fully consumed
- **WHEN** a pull returns a page together with a `next_since` cursor
- **THEN** the app SHALL request subsequent pages using the cursor
- **AND** SHALL accumulate every returned record before reconciling

#### Scenario: Local records are not dropped on a partial page
- **WHEN** a (full) pull returns only a subset of server records in its first page
- **THEN** the app SHALL NOT delete local records merely because they are absent from that page
- **AND** the app SHALL continue paging until the response carries no `next_since` before any removal sweep

#### Scenario: Watermark advances from the last page
- **WHEN** a paginated pull completes
- **THEN** the app SHALL set its pull watermark from the cursor of the final page

#### Scenario: Skipped record holds the watermark back
- **WHEN** a page contains a record skipped because it has pending edits
- **THEN** the watermark SHALL stay below that record's delivery timestamp
- **AND** the next pull SHALL request it again
