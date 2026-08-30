## Purpose

Tracks which client and photo records have unsynced local changes so an offline-first sync engine can transmit exactly what changed and resolve deletions against the server.

## ADDED Requirements

### Requirement: Pending change markers

The system SHALL mark a client record as pending when it is created or edited locally, and clear the pending marker once the change is acknowledged by the server.

#### Scenario: New client is pending
- **WHEN** a client is created on the device
- **THEN** the client SHALL be marked as pending
- **AND** remain pending until the server acknowledges the record

#### Scenario: Edited client is pending
- **WHEN** an existing client's fields are edited on the device
- **THEN** the client SHALL be marked as pending
- **AND** its previous pending status SHALL be preserved if it was already pending

### Requirement: Data pending deletion

Deleting a client SHALL not remove the record immediately; instead it SHALL be marked for deletion and hidden from normal lists, remaining deletable until the server confirms removal.

#### Scenario: Offline deletion is deferred
- **WHEN** the user deletes a client while offline
- **THEN** the client record SHALL be marked for deletion
- **AND** SHALL no longer appear in the normal client list
- **AND** SHALL remain until the server confirms the deletion

#### Scenario: Pending deletion acknowledged
- **WHEN** the server confirms a previously-deleted record was removed
- **THEN** the local record SHALL be permanently removed

### Requirement: Server synchronization status

The system SHALL expose a status reflecting whether an offline change is waiting to be sent to the server.

#### Scenario: Sync status is observable
- **WHEN** the app has pending or deleted-pending records
- **THEN** a sync status SHALL be exposed indicating pending uploads
- **AND** the status SHALL clear once all pending records are acknowledged