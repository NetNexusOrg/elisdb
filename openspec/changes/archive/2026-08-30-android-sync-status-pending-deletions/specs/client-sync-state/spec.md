## MODIFIED Requirements

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
