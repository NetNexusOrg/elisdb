## ADDED Requirements

### Requirement: Deletions propagate on delta pulls

The incremental pull SHALL carry tombstones so a deleted record is removed on other devices even when the pull is incremental (not a full snapshot).

#### Scenario: Tombstone returned on incremental pull
- **WHEN** the app pulls incrementally (with a `since` watermark)
- **THEN** the response SHALL include tombstones created since that watermark
- **AND** the app SHALL remove locally stored records named by those tombstones (when not pending)
