## Purpose

Gives the user a settings screen to view the server connection (base URL, masked ed25519 signing key and associated user name) configured by importing the server-issued configuration file, in read-only form, so a device is configured in one step without manual entry.

## Requirements
### Requirement: Server connection settings screen

The system SHALL provide a settings screen that shows the server connection (base URL, masked ed25519 signing key, and associated user name) in read-only, non-editable form, configured by importing the server-issued configuration file, and where the entire configuration can be removed in one action. The connection details SHALL be presented with a consistent visual hierarchy: labels and values in clearly distinct, uniform font sizes, and related fields grouped together so the screen is easy to scan.

#### Scenario: View current server configuration
- **WHEN** the user opens the settings screen after importing a configuration file
- **THEN** the screen SHALL show the server base URL, the signing key masked so the full secret is not visible, and the associated user name when known
- **AND** none of these values SHALL be editable

#### Scenario: Configuration is not manually editable
- **WHEN** the user opens the settings screen
- **THEN** there SHALL be no field to edit the server base URL and no save action for the connection configuration

#### Scenario: View configuration when no key is set
- **WHEN** the user opens the settings screen and no signing key is stored
- **THEN** the screen SHALL indicate that no key is configured

#### Scenario: Associated user is always shown when a key is set
- **WHEN** the user opens the settings screen and a signing key is stored
- **THEN** the screen SHALL always show the "associated user" information, even when the identity lookup has not completed yet
- **AND** while the identity lookup is in progress the screen SHALL indicate that the user is being resolved
- **AND** when the lookup succeeds the screen SHALL show the reported user name

#### Scenario: No associated user is shown as absent
- **WHEN** the configured server reports no user name for the stored key
- **THEN** the screen SHALL indicate that no user is associated with the key
- **AND** SHALL NOT fabricate a name

#### Scenario: Unresolvable identity is shown as unavailable, not absent
- **WHEN** a signing key is stored but the identity lookup cannot reach the server or fails (network or server error)
- **THEN** the screen SHALL show that the associated user could not be determined
- **AND** SHALL NOT fabricate a name
- **AND** SHALL NOT claim no user is associated with the key

#### Scenario: Fields use a consistent type hierarchy
- **WHEN** the user views the server connection settings
- **THEN** every field label SHALL use the same label style and every field value SHALL use the same value style
- **AND** the value of each field SHALL be visually distinct from its label by font size
- **AND** related fields SHALL be grouped together, visually separated from other groups

#### Scenario: Remove the entire server configuration
- **WHEN** the user taps the remove-configuration action for a stored server configuration
- **THEN** the stored base URL and signing key SHALL be removed together
- **AND** the screen SHALL indicate that no server is configured and no key is configured
- **AND** the associated user SHALL no longer be shown

### Requirement: Apply configuration to the sync client

The system SHALL apply the server URL and signing key configured by importing the configuration file to the API client used by sync, and SHALL trigger a synchronization against the configured server immediately after importing.

#### Scenario: Import applies and triggers sync
- **WHEN** the user imports a configuration file
- **THEN** subsequent syncs SHALL use the imported base URL and signing key
- **AND** a synchronization SHALL attempt against the configured server

#### Scenario: Import shows no success confirmation
- **WHEN** the user imports a configuration file successfully
- **THEN** the screen SHALL NOT show a "configuration imported and saved" confirmation message

#### Scenario: Config change with unreachable server
- **WHEN** the user imports a server configuration that cannot be reached
- **THEN** the imported values SHALL still take effect
- **AND** sync SHALL fail with a network error rather than a configuration error

### Requirement: Sync status and manual trigger on the settings screen

The system SHALL show the current sync status on the settings screen, including an icon and a message for each state, and SHALL provide a manual sync trigger there. When the Settings screen is opened, the system SHALL check for pending uploads (local changes) and pending downloads (server data newer than the pull watermark) and present the result as a "synchronization available" message. The sync status row SHALL always show a message.

#### Scenario: Sync status is shown in settings
- **WHEN** the user opens the settings screen
- **THEN** the screen SHALL show the current sync status
- **AND** when a sync is in progress the screen SHALL indicate that a sync is in progress
- **AND** when a sync has just succeeded the screen SHALL indicate the last successful sync
- **AND** when a sync has just failed the screen SHALL indicate a sync error

#### Scenario: No configuration shows not configured
- **WHEN** no server configuration is loaded
- **THEN** the sync status row SHALL indicate that the server is not configured, without requiring a sync attempt

#### Scenario: Missing server configuration is shown
- **WHEN** a sync fails because the server is not configured
- **THEN** the sync status row SHALL indicate that the server is not configured, using a short message ("Não configurado")

#### Scenario: Nothing to sync is shown
- **WHEN** the settings screen is opened, a server configuration is loaded, and there are no pending uploads and no pending downloads
- **THEN** the sync status row SHALL indicate that the data is synchronized

#### Scenario: Send-only is shown
- **WHEN** the settings screen is opened and there are pending uploads but no pending downloads
- **THEN** the sync status row SHALL indicate that a synchronization is available to send

#### Scenario: Receive-only is shown
- **WHEN** the settings screen is opened and there are pending downloads but no pending uploads
- **THEN** the sync status row SHALL indicate that a synchronization is available to receive

#### Scenario: Send and receive is shown
- **WHEN** the settings screen is opened and there are both pending uploads and pending downloads
- **THEN** the sync status row SHALL indicate that a synchronization is available to send and receive

#### Scenario: Sync fault messages are actionable
- **WHEN** a sync has failed
- **THEN** the settings screen SHALL show a message explaining the failure (missing server configuration, unregistered key, or network problem) in the same place as the sync status

#### Scenario: Manual sync from settings
- **WHEN** a sync is not in progress and the user taps the sync action on the settings screen
- **THEN** the system SHALL start a synchronisation
- **AND** the settings screen SHALL move to a "sync in progress" state until it completes

#### Scenario: Repeated trigger is ignored while syncing
- **WHEN** the user taps the sync action while a sync is already in progress
- **THEN** the system SHALL NOT start a second synchronisation

### Requirement: Sync action stays in a fixed position

The system SHALL keep the sync action (button/icon) in the settings sync status row at the same position regardless of the current sync state, so the user can always find and tap it without it moving.

#### Scenario: Sync action position is stable across sync states
- **WHEN** the settings screen shows the sync status row in any state (idle, in progress, succeeded, failed)
- **THEN** the sync action SHALL occupy the same position in the row in every state
- **AND** the status message and icon SHALL NOT displace the sync action

#### Scenario: Sync action position is stable when a message is present
- **WHEN** the sync status row shows a status message or icon alongside the sync action
- **THEN** the sync action SHALL remain in its fixed position
- **AND** the message SHALL be laid out so it does not shift the sync action

#### Scenario: In-progress indicator stays in place
- **WHEN** a sync is in progress and the sync action shows an in-progress indicator
- **THEN** the in-progress indicator SHALL appear in the same position as the sync action

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


### Requirement: Import configuration from a file

The system SHALL offer an action to import the server configuration file. The Settings screen SHALL NOT show an explanatory paragraph about how the file is issued.

#### Scenario: Import action is available
- **WHEN** the user opens the settings screen
- **THEN** an action to import the server configuration file SHALL be available

#### Scenario: No import instructions paragraph
- **WHEN** the settings screen shows the import action
- **THEN** the screen SHALL NOT show a paragraph explaining how the configuration file is emitted

### Requirement: Remove server configuration

The system SHALL remove the stored server configuration in one action. Removing the configuration SHALL NOT show a confirmation message.

#### Scenario: Remove configuration without confirmation
- **WHEN** the user removes the stored server configuration
- **THEN** the stored base URL and signing key SHALL be removed
- **AND** the screen SHALL NOT show a "configuration removed" confirmation message

### Requirement: Signing key encrypted at rest

The system SHALL store the server signing key (and the server base URL) encrypted at rest, with the encryption key held in the platform keystore, so the secret is not readable from plain shared preferences.

#### Scenario: Key is not stored in plain text
- **WHEN** the app persists the server connection
- **THEN** the signing key SHALL be stored encrypted (not readable as plain text in the preferences file)

#### Scenario: Existing configuration keeps working after upgrade
- **WHEN** the app is upgraded from a version that stored the key in plain text
- **THEN** the previously stored values SHALL still be available (migrated), and the plain-text copy SHALL be removed
