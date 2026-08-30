## Purpose

Tracks which client and photo records have unsynced local changes so an offline-first sync engine can transmit exactly what changed and resolve deletions against the server.

## Requirements

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

The system SHALL expose a status reflecting whether there is synchronization work outstanding in either direction — local changes waiting to be sent to the server, or server data newer than the device's last pull waiting to be received — and SHALL surface that status to the UI so the user can see that a synchronization is pending rather than a stale result from an earlier attempt. Outstanding work SHALL include both pending edits/creates AND pending deletions.

#### Scenario: Sync status is observable
- **WHEN** the app has pending or deleted-pending records
- **THEN** a sync status SHALL be exposed indicating pending uploads
- **AND** the status SHALL clear once all pending records are acknowledged

#### Scenario: Pending uploads shown before the next sync
- **WHEN** the app has pending or deleted-pending records and no sync has completed since they were created
- **THEN** the observable sync status SHALL indicate that a synchronization is pending
- **AND** SHALL NOT report the outcome of a previous sync (e.g. a stale success) as the current state

#### Scenario: Pending deletions count as pending uploads
- **WHEN** the app has a record marked for deletion that has not yet been acknowledged by the server
- **THEN** the observable sync status SHALL indicate pending uploads (a synchronization is required)
- **AND** SHALL NOT report the state as fully synced

#### Scenario: Pending downloads shown before the next sync
- **WHEN** the server holds data newer than the device's last pull watermark
- **THEN** the observable sync status SHALL indicate that there is work to receive
- **AND** SHALL NOT report the outcome of a previous sync (e.g. a stale success) as the current state

#### Scenario: Pending downloads clear after a pull
- **WHEN** a pull retrieves all server data newer than the device's watermark
- **THEN** the pending-downloads signal SHALL clear

### Requirement: Persisted clock offset

The system SHALL persist the device↔server clock offset so it survives app restarts and is available for stamping records created offline.

#### Scenario: Offset survives restart
- **WHEN** a clock offset has been measured and persisted
- **THEN** it SHALL still be available after the app restarts

#### Scenario: No offset before first measurement
- **WHEN** no offset has been measured yet
- **THEN** the store SHALL report an absent offset, and the app SHALL fall back to the device clock