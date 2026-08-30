## Purpose

Improves sync observability and diagnostics: the true client creation date is preserved across syncs, authentication rejections caused by clock skew are reported accurately, and the sync state plus a manual trigger stay visible from the Settings screen.

## Requirements

### Requirement: Creation date preserved across sync

The system SHALL preserve the original creation date of a client across pulls so the UI's "criado em" never shows the last update time.

#### Scenario: Creation date survives edits
- **WHEN** a client is created locally, synced, then edited on the server and re-pulled
- **THEN** the app's `dataCriacao` for that client SHALL remain the original creation date
- **AND** SHALL NOT be overwritten by the server's `updatedAt`

### Requirement: Clock-skew-aware auth errors

The system SHALL tell the user whether a rejected sync comes from a device clock outside the signing window or from an invalid/unregistered key.

#### Scenario: Stale clock reported as such
- **WHEN** the server rejects a request because the timestamp is outside the freshness window
- **THEN** the app SHALL surface a message that the device clock is out of sync (e.g. "relógio do dispositivo dessincronizado")
- **AND** SHALL NOT present the "chave não registada" message for that rejection

#### Scenario: Genuine key problem still reported
- **WHEN** the server rejects a request because the signature does not verify against any registered key
- **THEN** the app SHALL surface the "chave não registada" (or equivalent) message

### Requirement: Lost-conflict outcome visible

The system SHALL surface a lost local edit (a `stale` push) in the observable sync status so the user is not left unaware that their edit was superseded.

#### Scenario: Stale names surfaced in sync status
- **WHEN** a sync completes with one or more records rejected as `stale`
- **THEN** the observable sync result SHALL carry the names of those records
- **AND** the sync status SHALL present the loss to the user (e.g., via a result message), distinct from a plain success

#### Scenario: No stale records reports plain success
- **WHEN** a sync completes with no `stale` records
- **THEN** the observable sync result SHALL be a plain success with no loss warning