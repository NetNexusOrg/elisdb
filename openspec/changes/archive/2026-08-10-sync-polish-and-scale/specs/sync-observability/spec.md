## Purpose

Improves sync observability and diagnostics: the true client creation date is preserved across syncs, authentication rejections caused by clock skew are reported accurately, and the sync state plus a manual trigger are visible from the main client list screen.

## ADDED Requirements

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

### Requirement: Sync observable from the client list

The system SHALL expose the sync status and a manual sync trigger from the main client list screen.

#### Scenario: Status visible on main screen
- **WHEN** the user views the client list
- **THEN** the current sync state SHALL be displayed on that screen
- **AND** a manual sync action SHALL be available there
- **AND** any sync error SHALL be communicated on that screen with an actionable message