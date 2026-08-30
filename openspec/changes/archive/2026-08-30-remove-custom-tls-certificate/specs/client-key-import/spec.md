## MODIFIED Requirements

### Requirement: Import a server configuration file

The system SHALL provide an action in the app settings to import a server-issued configuration file containing the server URL and the ed25519 authentication key, and SHALL store both so the device is fully configured in one step. The `server_url` SHALL be validated on import: a URL with the `http` or `https` scheme SHALL be accepted for any host (a domain or an IP address); any other scheme SHALL be rejected at import time with a clear message.

#### Scenario: Import a configuration file configures URL and key
- **WHEN** the user selects a configuration file containing a `server_url` and a valid `private_seed`
- **THEN** the server URL SHALL be stored as the configured base URL
- **AND** the seed SHALL be stored as the app signing key
- **AND** subsequent syncs SHALL sign requests with the stored key against the stored URL

#### Scenario: Invalid file content is rejected
- **WHEN** the selected file is not a valid configuration file (missing or malformed fields, or an invalid 32-byte base64 ed25519 seed)
- **THEN** the import SHALL be refused and the user SHALL be told the file is invalid
- **AND** the previously stored URL and key SHALL be left unchanged

#### Scenario: http URL for a domain or IP is accepted
- **WHEN** the configuration file contains a `server_url` with the `http` scheme for a non-local host (a domain or an IP address)
- **THEN** the import SHALL proceed and store the URL

#### Scenario: Unsupported scheme is rejected at import
- **WHEN** the configuration file contains a `server_url` with a scheme other than `http` or `https`
- **THEN** the import SHALL be refused at import time with a clear message
- **AND** the error SHALL be surfaced before any sync attempt

#### Scenario: Import completes onboarding
- **WHEN** the user imports a valid configuration file
- **THEN** the settings SHALL allow saving and a sync SHALL be triggerable against the configured server without further setup

#### Scenario: Import updates existing configuration
- **WHEN** a configuration is already stored and the user imports a new valid configuration file
- **THEN** the new URL SHALL replace the stored base URL
- **AND** the new seed SHALL replace the stored signing key
- **AND** subsequent syncs SHALL use the new configuration
