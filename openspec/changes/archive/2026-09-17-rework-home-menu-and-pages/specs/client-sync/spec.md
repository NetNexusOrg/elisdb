## ADDED Requirements

### Requirement: Dedicated synchronization screen

The system SHALL provide a synchronization screen, separate from the server configuration screen, reached from the client list menu. The screen SHALL show the current sync status (icon and message for each state) and SHALL provide two distinct actions: a "check for updates" action in the sync status row, which queries the server for updates without transferring data, and a prominent full-width manual sync control, which performs the actual data transfer. The screen SHALL show the time of the last successful synchronization, or indicate that no synchronization has completed yet. The screen SHALL show the total number of clients stored on the device (excluding clients pending deletion) and explicit rows for pending uploads (with a count) and pending downloads. When the synchronization screen is opened, the system SHALL check for pending uploads (local changes) and pending downloads (server data newer than the pull watermark) and present the result as a "synchronization available" message. The sync status row SHALL always show a message.

#### Scenario: Synchronization screen is opened from the client list menu
- **WHEN** the user opens the client list menu and taps the synchronization action
- **THEN** the system SHALL open the synchronization screen
- **AND** the server configuration screen SHALL NOT be shown

#### Scenario: Two distinct actions are offered
- **WHEN** the synchronization screen is shown
- **THEN** the screen SHALL offer a check-for-updates action and a full-width manual sync control
- **AND** the check-for-updates action SHALL only query the server and update the displayed indicators, without transferring client data
- **AND** the full-width manual sync control SHALL perform the data transfer

#### Scenario: Sync status is shown
- **WHEN** the user opens the synchronization screen
- **THEN** the screen SHALL show the current sync status
- **AND** when a sync is in progress the screen SHALL indicate that a sync is in progress
- **AND** when a sync has just succeeded the screen SHALL indicate the last successful sync
- **AND** when a sync has just failed the screen SHALL indicate a sync error

#### Scenario: Total client count is shown
- **WHEN** the user opens the synchronization screen
- **THEN** the screen SHALL show the total number of clients stored on the device
- **AND** clients pending deletion SHALL NOT be counted

#### Scenario: Last successful sync time is shown
- **WHEN** the user opens the synchronization screen and at least one sync has completed successfully
- **THEN** the screen SHALL show the date and time of the last successful synchronization
- **AND** the shown time SHALL be the one persisted from the last successful sync, surviving app restarts

#### Scenario: Never synchronised is shown
- **WHEN** the user opens the synchronization screen and no sync has ever completed successfully
- **THEN** the screen SHALL indicate that no synchronization has completed yet

#### Scenario: Pending uploads row shows a count
- **WHEN** the user opens the synchronization screen
- **THEN** the screen SHALL show a row with the number of local records still awaiting upload (including records pending deletion)

#### Scenario: Pending downloads row shows availability
- **WHEN** the user opens the synchronization screen
- **THEN** the screen SHALL show a row indicating whether there is server data still to be received

#### Scenario: Prominent manual sync control
- **WHEN** the synchronization screen is shown
- **THEN** a full-width manual sync control SHALL be available
- **AND** while a sync is in progress the control SHALL indicate that a sync is in progress and SHALL NOT start a second synchronisation

#### Scenario: No configuration shows not configured
- **WHEN** no server configuration is loaded
- **THEN** the sync status row SHALL indicate that the server is not configured, without requiring a sync attempt

#### Scenario: Missing server configuration is shown
- **WHEN** a sync fails because the server is not configured
- **THEN** the sync status row SHALL indicate that the server is not configured, using a short message ("Não configurado")

#### Scenario: Nothing to sync is shown
- **WHEN** the synchronization screen is opened, a server configuration is loaded, and there are no pending uploads and no pending downloads
- **THEN** the sync status row SHALL indicate that the data is synchronized

#### Scenario: Send-only is shown
- **WHEN** the synchronization screen is opened and there are pending uploads but no pending downloads
- **THEN** the sync status row SHALL indicate that a synchronization is available to send

#### Scenario: Receive-only is shown
- **WHEN** the synchronization screen is opened and there are pending downloads but no pending uploads
- **THEN** the sync status row SHALL indicate that a synchronization is available to receive

#### Scenario: Send and receive is shown
- **WHEN** the synchronization screen is opened and there are both pending uploads and pending downloads
- **THEN** the sync status row SHALL indicate that a synchronization is available to send and receive

#### Scenario: Sync fault messages are actionable
- **WHEN** a sync has failed
- **THEN** the synchronization screen SHALL show a message explaining the failure (missing server configuration, unregistered key, or network problem) in the same place as the sync status

#### Scenario: Manual sync from the synchronization screen
- **WHEN** a sync is not in progress and the user taps the full-width manual sync control
- **THEN** the system SHALL start a synchronisation
- **AND** the screen SHALL move to a "sync in progress" state until it completes

#### Scenario: Repeated trigger is ignored while syncing
- **WHEN** the user taps the full-width manual sync control while a sync is already in progress
- **THEN** the system SHALL NOT start a second synchronisation

### Requirement: Check for updates action

The system SHALL provide a check-for-updates action in the synchronization screen's sync status row. The action SHALL query the server for newer data and refresh the displayed pending indicators and status message, and SHALL NOT transfer client data (no push, no pull of records). The action SHALL show an in-progress indicator while checking and SHALL NOT allow a second check to start while one is in progress. The action SHALL be disabled when no server is configured.

#### Scenario: Check queries without transferring data
- **WHEN** the user taps the check-for-updates action
- **THEN** the system SHALL query the server for updates and refresh the pending indicators
- **AND** the system SHALL NOT push or pull client records

#### Scenario: Check shows progress
- **WHEN** a check is in progress
- **THEN** the check action SHALL show an in-progress indicator and SHALL be disabled

#### Scenario: Repeated check is ignored while checking
- **WHEN** the user taps the check action while a check is already in progress
- **THEN** the system SHALL NOT start a second check

#### Scenario: Check is disabled without configuration
- **WHEN** no server is configured
- **THEN** the check action SHALL be disabled

### Requirement: Check action stays in a fixed position

The system SHALL keep the check-for-updates action (icon) in the synchronization screen's sync status row at the same position regardless of the current sync state or check state, so the user can always find and tap it without it moving.

#### Scenario: Check action position is stable across states
- **WHEN** the synchronization screen shows the sync status row in any state (idle, in progress, succeeded, failed)
- **THEN** the check action SHALL occupy the same position in the row in every state
- **AND** the status message and icon SHALL NOT displace the check action

#### Scenario: Check action position is stable when a message is present
- **WHEN** the sync status row shows a status message or icon alongside the check action
- **THEN** the check action SHALL remain in its fixed position
- **AND** the message SHALL be laid out so it does not shift the check action

#### Scenario: Check in-progress indicator stays in place
- **WHEN** a check is in progress and the check action shows an in-progress indicator
- **THEN** the in-progress indicator SHALL appear in the same position as the check action

### Requirement: Pending sync state shown on the synchronization screen

The system SHALL show pending synchronization states in the synchronization screen's sync status row — "synchronization pending" when there are local changes not yet acknowledged by the server, and "updates to receive" when the server holds data newer than this device's last pull — instead of showing a stale success or error from an earlier sync.

#### Scenario: Pending uploads state shown
- **WHEN** the user opens the synchronization screen and the app has pending or deleted-pending records
- **THEN** the sync status row SHALL indicate that a synchronization is pending
- **AND** the row SHALL NOT show a success/error from a previous sync as the current state

#### Scenario: Pending downloads state shown
- **WHEN** the user opens the synchronization screen and the server holds data newer than the device's last pull
- **THEN** the sync status row SHALL indicate that there are updates to receive
- **AND** the row SHALL NOT show a success/error from a previous sync as the current state

#### Scenario: Pending state clears after a successful sync
- **WHEN** a sync completes and all pending records are acknowledged and all newer server data is retrieved
- **THEN** the sync status row SHALL show the acknowledged outcome (success) instead of the pending states

#### Scenario: Pending state survives navigation
- **WHEN** the user leaves and reopens the synchronization screen while local changes remain pending or server updates remain unretrieved
- **THEN** the sync status row SHALL still indicate the pending synchronization state
