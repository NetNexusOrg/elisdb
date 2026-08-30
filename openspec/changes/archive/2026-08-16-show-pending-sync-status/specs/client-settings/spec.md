## ADDED Requirements

### Requirement: Pending sync state shown in settings

The system SHALL show pending synchronization states in the settings sync status row — "synchronization pending" when there are local changes not yet acknowledged by the server, and "updates to receive" when the server holds data newer than this device's last pull — instead of showing a stale success or error from an earlier sync.

#### Scenario: Pending uploads state shown
- **WHEN** the user opens the settings screen and the app has pending or deleted-pending records
- **THEN** the sync status row SHALL indicate that a synchronization is pending
- **AND** the row SHALL NOT show a success/error from a previous sync as the current state

#### Scenario: Pending downloads state shown
- **WHEN** the user opens the settings screen and the server holds data newer than the device's last pull
- **THEN** the sync status row SHALL indicate that there are updates to receive
- **AND** the row SHALL NOT show a success/error from a previous sync as the current state

#### Scenario: Pending state clears after a successful sync
- **WHEN** a sync completes and all pending records are acknowledged and all newer server data is retrieved
- **THEN** the sync status row SHALL show the acknowledged outcome (success) instead of the pending states

#### Scenario: Pending state survives navigation
- **WHEN** the user leaves and reopens the settings screen while local changes remain pending or server updates remain unretrieved
- **THEN** the sync status row SHALL still indicate the pending synchronization state

