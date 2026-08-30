## Purpose

Gives the user a settings screen to view and edit the server connection (base URL and ed25519 signing key) at runtime and apply it immediately, so a device can point at a different server without a rebuild.

## ADDED Requirements

### Requirement: Server connection settings screen

The system SHALL provide a settings screen where the server base URL and the ed25519 signing key seed can be viewed and edited, prefilled with the currently effective values.

#### Scenario: View current server configuration
- **WHEN** the user opens the settings screen
- **THEN** the screen SHALL show the current server base URL and signing key seed

#### Scenario: Edit and save server configuration
- **WHEN** the user changes the server base URL or signing key seed and saves
- **THEN** the new values SHALL be persisted for future app runs
- **AND** the screen SHALL confirm the values were saved

#### Scenario: Empty configuration is accepted
- **WHEN** the user saves a blank server base URL or signing key seed
- **THEN** the values SHALL be persisted as blank
- **AND** syncs SHALL be refused locally until the values are set, without crashing

### Requirement: Apply configuration to the sync client

The system SHALL apply a saved server URL and signing key to the API client used by sync, and SHALL trigger a synchronization against the newly configured server immediately after saving.

#### Scenario: Save applies and triggers sync
- **WHEN** the user saves new server configuration
- **THEN** subsequent syncs SHALL use the saved base URL and signing key
- **AND** a synchronization SHALL attempt against the new configuration

#### Scenario: Config change with unreachable server
- **WHEN** the user saves a server configuration that cannot be reached
- **THEN** the save SHALL still take effect (acceptedated values)
- **AND** sync SHALL fail with a network error rather than a configuration error