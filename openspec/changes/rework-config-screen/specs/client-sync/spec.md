# Spec Delta

## REMOVED Requirements

### Requirement: Dedicated synchronization screen

**Reason**: Replaced by the configuration-screen requirements. The synchronization summary, the data counts and the server configuration now live together on a single screen, so there is no dedicated synchronization screen and no separate server screen.

**Migration**: Open the configuration screen from the client list menu; it holds the sync summary, the data counts and the server configuration.

### Requirement: Check for updates action

**Reason**: The check is now performed automatically when the configuration screen opens, so a separate action adds a second way to trigger the same read-only query without adding information.

**Migration**: Open the configuration screen; it refreshes the pending indicators and the server client total on open. To transfer data, use the single manual sync control.

### Requirement: Check action stays in a fixed position

**Reason**: The check-for-updates action it referred to no longer exists.

**Migration**: None needed; the sync summary no longer contains an action, so there is no position to keep stable.

## MODIFIED Requirements

### Requirement: Server and device client totals shown

The configuration screen SHALL show the number of clients stored on the server alongside the number stored on the device. When the server total is unknown, the screen SHALL show a placeholder instead of a misleading zero. The server total SHALL be refreshed by the check that runs when the screen opens, without transferring client records.

#### Scenario: Both totals are shown
- **WHEN** the user opens the configuration screen with a configured server and a reachable connection
- **THEN** the screen SHALL show how many clients are stored on the server
- **AND** the screen SHALL show how many clients are stored on the device

#### Scenario: Unknown server total shows a placeholder
- **WHEN** the server total has not been retrieved (no configuration, no connection, or never fetched)
- **THEN** the server row SHALL show a placeholder rather than `0`

#### Scenario: Server total refreshes without transferring data
- **WHEN** the user opens the configuration screen
- **THEN** the screen SHALL refresh the server client total
- **AND** it SHALL NOT push or pull client records

#### Scenario: Server total updates after a sync
- **WHEN** a sync completes successfully
- **THEN** the screen SHALL show the server client total as of that sync

## ADDED Requirements

### Requirement: Configuration screen

The system SHALL provide a single configuration screen, reached from the client list menu, combining the sync summary, the data counts and the server configuration: the last successful sync as a caption, device and server client totals side by side, the pending work in the summary's detail line, and the server URL, masked key and associated user, with an import action while unconfigured and only removal once configured.

#### Scenario: Configuration screen is opened from the client list menu
- **WHEN** the user opens the client list menu and taps the configuration action
- **THEN** the system SHALL open the configuration screen
- **AND** the menu SHALL NOT offer separate synchronization and server entries

#### Scenario: Total client count is shown
- **WHEN** the user opens the configuration screen
- **THEN** the screen SHALL show the total number of clients stored on the device
- **AND** clients pending deletion SHALL NOT be counted
- **AND** the device total SHALL be shown alongside the server total for comparison

#### Scenario: Last successful sync time is shown
- **WHEN** the user opens the configuration screen and at least one sync has completed successfully
- **THEN** the screen SHALL show the date and time of the last successful synchronization as a caption of the sync summary
- **AND** the shown time SHALL be the one persisted from the last successful sync, surviving app restarts

#### Scenario: Never synchronised is shown
- **WHEN** the user opens the configuration screen and no sync has ever completed successfully
- **THEN** the screen SHALL indicate that no synchronization has completed yet

#### Scenario: Pending work is reported by the summary
- **WHEN** there are local records awaiting upload or server changes to receive
- **THEN** the sync summary SHALL name the pending work in its detail line, with the number of records awaiting upload
- **AND** the data card SHALL show only the device and server client totals

#### Scenario: Server configuration is shown
- **WHEN** the user opens the configuration screen and a server configuration is loaded
- **THEN** the screen SHALL show the server URL, the signing key masked, and the user associated with the key
- **AND** the key SHALL NOT be shown in full

#### Scenario: Configuration is imported on the same screen
- **WHEN** the user taps the import configuration action and picks a valid configuration file
- **THEN** the screen SHALL show the imported URL and masked key
- **AND** the system SHALL start a synchronisation

#### Scenario: Configuration can be removed
- **WHEN** the user taps the remove configuration action
- **THEN** the URL and the key SHALL be shown as not configured
- **AND** the stored configuration SHALL be cleared

#### Scenario: Import is not offered once configured
- **GIVEN** a configuration is already present
- **THEN** the server card SHALL offer the removal action and SHALL NOT offer to import another configuration

#### Scenario: Invalid configuration file is rejected
- **WHEN** the picked file is not a valid configuration
- **THEN** the screen SHALL show an error explaining the expected format
- **AND** the current configuration SHALL be kept

### Requirement: Sync summary and manual sync control

The sync summary SHALL be the screen's prominent block: a state icon, a short state title, and a detail line naming the pending work. A single full-width manual sync control SHALL be the only sync action; when no server is configured the summary SHALL state that synchronization is not available instead of offering an action.

#### Scenario: Sync status is shown
- **WHEN** the user opens the configuration screen
- **THEN** the screen SHALL show the current sync status as an icon and a short title, formatted more prominently than the surrounding detail rows
- **AND** when a sync is in progress the title SHALL indicate that a sync is in progress
- **AND** when a sync has just succeeded the title SHALL indicate that the data is synchronized
- **AND** when a sync has just failed the title SHALL indicate the failure

#### Scenario: Pending work is summarised in one line
- **WHEN** the screen shows pending uploads, pending downloads, or both
- **THEN** the sync summary SHALL show a detail line with what is pending (the number of changes to send and/or the availability of updates to receive)
- **AND** the screen SHALL NOT need to be read row by row to know whether a sync is worth running

#### Scenario: A single sync action is offered
- **WHEN** the configuration screen is shown
- **THEN** the screen SHALL offer a single, full-width manual sync control
- **AND** SHALL NOT offer a separate check-for-updates action
- **AND** the manual sync control SHALL perform the data transfer

#### Scenario: Prominent manual sync control
- **WHEN** the configuration screen is shown
- **THEN** a full-width manual sync control SHALL be available
- **AND** while a sync is in progress the control SHALL indicate that a sync is in progress and SHALL NOT start a second synchronisation

#### Scenario: No configuration shows not configured
- **WHEN** no server configuration is loaded
- **THEN** the sync summary SHALL indicate that the server is not configured, without requiring a sync attempt
- **AND** it SHALL state that synchronization is not available
- **AND** the import action SHALL be offered only in the server card, as a single entry point
- **AND** no control in the summary SHALL start a synchronisation

#### Scenario: Missing server configuration is shown
- **WHEN** a sync fails because the server is not configured
- **THEN** the sync summary SHALL indicate that the server is not configured, using a short message ("Não configurado")

#### Scenario: Nothing to sync is shown
- **WHEN** the configuration screen is opened, a server configuration is loaded, and there are no pending uploads and no pending downloads
- **THEN** the sync summary SHALL indicate that the data is synchronized

#### Scenario: Send-only is shown
- **WHEN** the configuration screen is opened and there are pending uploads but no pending downloads
- **THEN** the sync summary SHALL indicate that there are changes to send

#### Scenario: Receive-only is shown
- **WHEN** the configuration screen is opened and there are pending downloads but no pending uploads
- **THEN** the sync summary SHALL indicate that there are updates to receive

#### Scenario: Send and receive is shown
- **WHEN** the configuration screen is opened and there are both pending uploads and pending downloads
- **THEN** the sync summary SHALL indicate that there are changes to send and updates to receive

#### Scenario: Sync fault messages are actionable
- **WHEN** a sync has failed
- **THEN** the configuration screen SHALL show a message explaining the failure (missing server configuration, unregistered key, or network problem) in the same place as the sync status

#### Scenario: Manual sync from the configuration screen
- **WHEN** a sync is not in progress and the user taps the full-width manual sync control
- **THEN** the system SHALL start a synchronisation
- **AND** the screen SHALL move to a "sync in progress" state until it completes

#### Scenario: Repeated trigger is ignored while syncing
- **WHEN** the user taps the full-width manual sync control while a sync is already in progress
- **THEN** the system SHALL NOT start a second synchronisation

### Requirement: Pending state refreshed on open

When the configuration screen opens, the system SHALL refresh the pending-upload and pending-download indicators and the server client total by querying the server without transferring data.

#### Scenario: Pending state is checked when the screen opens
- **WHEN** the user opens the configuration screen
- **THEN** the system SHALL query the server for the pending indicators and the server client total
- **AND** the system SHALL NOT transfer client records while doing so
