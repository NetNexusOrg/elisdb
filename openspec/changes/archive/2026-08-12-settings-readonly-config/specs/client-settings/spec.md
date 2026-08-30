## MODIFIED Requirements

### Requirement: Server connection settings screen

The system SHALL provide a settings screen that shows the server connection (base URL, masked ed25519 signing key, and associated user name) in read-only, non-editable form, configured by importing the server-issued configuration file, and where the key can be cleared.

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

#### Scenario: No associated user is shown as absent
- **WHEN** the configured server reports no user name for the stored key
- **THEN** the screen SHALL indicate that no user is associated with the key
- **AND** SHALL NOT fabricate a name

#### Scenario: Clear a stored signing key
- **WHEN** the user taps the clear action for a stored signing key
- **THEN** the stored key SHALL be removed
- **AND** the screen SHALL indicate that no key is configured

### Requirement: Apply configuration to the sync client

The system SHALL apply the server URL and signing key configured by importing the configuration file to the API client used by sync, and SHALL trigger a synchronization against the configured server immediately after importing.

#### Scenario: Import applies and triggers sync
- **WHEN** the user imports a configuration file
- **THEN** subsequent syncs SHALL use the imported base URL and signing key
- **AND** a synchronization SHALL attempt against the configured server

#### Scenario: Config change with unreachable server
- **WHEN** the user imports a server configuration that cannot be reached
- **THEN** the imported values SHALL still take effect
- **AND** sync SHALL fail with a network error rather than a configuration error