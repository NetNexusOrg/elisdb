## MODIFIED Requirements

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
