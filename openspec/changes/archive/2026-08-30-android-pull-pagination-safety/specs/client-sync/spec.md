## MODIFIED Requirements

### Requirement: Pull exhausts pagination

The app SHALL follow the server's pagination cursor returned by the pull endpoint: after each page it SHALL inspect `next_since` (and `next_since_id` when present) and, when a cursor is returned, SHALL request the next page, accumulating records across all pages, before reconciling local data. The app SHALL NOT delete or drop local client records based on a single, partial page of a pull. A local-record sweep (removing clients absent from the server) SHALL run only after the full set of pages has been received, or never for an incremental (delta) pull.

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
