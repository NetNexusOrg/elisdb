## Purpose

Gives the user a server screen to view the server connection (base URL, masked ed25519 signing key and associated user name) configured by importing the server-issued configuration file, in read-only form, so a device is configured in one step without manual entry.

## Requirements

### Requirement: Server connection settings screen

The system SHALL provide a server screen that shows the server connection (base URL, masked ed25519 signing key, and associated user name) in read-only, non-editable form, configured by importing the server-issued configuration file, and where the entire configuration can be removed in one action. The connection details SHALL be presented with a consistent visual hierarchy: labels and values in clearly distinct, uniform font sizes, and related fields grouped together so the screen is easy to scan.

#### Scenario: View current server configuration
- **WHEN** the user opens the server screen after importing a configuration file
- **THEN** the screen SHALL show the server base URL, the signing key masked so the full secret is not visible, and the associated user name when known
- **AND** none of these values SHALL be editable

#### Scenario: Configuration is not manually editable
- **WHEN** the user opens the server screen
- **THEN** there SHALL be no field to edit the server base URL and no save action for the connection configuration

#### Scenario: View configuration when no key is set
- **WHEN** the user opens the server screen and no signing key is stored
- **THEN** the screen SHALL indicate that no key is configured

#### Scenario: Associated user is always shown when a key is set
- **WHEN** the user opens the server screen and a signing key is stored
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
- **WHEN** the user views the server connection configuration
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

### Requirement: Import configuration from a file

The system SHALL offer an action to import the server configuration file. The server screen SHALL NOT show an explanatory paragraph about how the file is issued.

#### Scenario: Import action is available
- **WHEN** the user opens the server screen
- **THEN** an action to import the server configuration file SHALL be available

#### Scenario: No import instructions paragraph
- **WHEN** the server screen shows the import action
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

### Requirement: Outdated server warning

The configuration screen SHALL warn when the server reports an API version older
than the minimum the app relies on, or when it reports no API version at all.
The warning SHALL explain that some changes may not be stored and SHALL suggest
updating the server.

#### Scenario: Outdated server is flagged
- **WHEN** the configuration screen loads server information whose `api_version`
  is below the app minimum
- **THEN** the synchronization card SHALL show the outdated-server warning

#### Scenario: Server without an API version is flagged
- **WHEN** the server information has no `api_version`
- **THEN** the synchronization card SHALL show the outdated-server warning

#### Scenario: Current server shows no warning
- **WHEN** the server reports an API version greater than or equal to the app minimum
- **THEN** the warning SHALL NOT be shown
