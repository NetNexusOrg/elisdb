## ADDED Requirements

### Requirement: Lost-conflict outcome visible

The system SHALL surface a lost local edit (a `stale` push) in the observable sync status so the user is not left unaware that their edit was superseded.

#### Scenario: Stale names surfaced in sync status
- **WHEN** a sync completes with one or more records rejected as `stale`
- **THEN** the observable sync result SHALL carry the names of those records
- **AND** the sync status SHALL present the loss to the user (e.g., via a result message), distinct from a plain success

#### Scenario: No stale records reports plain success
- **WHEN** a sync completes with no `stale` records
- **THEN** the observable sync result SHALL be a plain success with no loss warning
