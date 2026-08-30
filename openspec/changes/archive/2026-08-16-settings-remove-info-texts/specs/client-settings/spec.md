## MODIFIED Requirements

### Requirement: Apply configuration to the sync client

The system SHALL apply the server URL and signing key configured by importing the configuration file to the API client used by sync, and SHALL trigger a synchronization against the configured server immediately after importing. Importing SHALL NOT show a success confirmation message.

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
