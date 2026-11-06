# Spec Delta

## MODIFIED Requirements

### Requirement: Import a server configuration file

The system SHALL provide an action in the app settings to import a server-issued configuration file containing the server URL, the ed25519 authentication key and, when present, the user name the server registered for that key, and SHALL store all fields so the device is fully configured in one step. The `server_url` SHALL be validated on import against the app's server URL policy before any value is stored: HTTPS SHALL be accepted for any host, HTTP SHALL be accepted only for localhost, loopback, and private/link-local/CGNAT addresses, and any other scheme or insecure host SHALL be rejected at import time with a clear message.

#### Scenario: Import a configuration file configures URL and key
- **WHEN** the user selects a configuration file containing a `server_url` and a valid `private_seed`
- **THEN** the server URL SHALL be stored as the configured base URL
- **AND** the seed SHALL be stored as the app signing key
- **AND** subsequent syncs SHALL sign requests with the stored key against the stored URL

#### Scenario: User name in the file is stored with the configuration
- **WHEN** the selected configuration file carries a `user_name`
- **THEN** the name SHALL be stored alongside the URL and the key
- **AND** the settings screen SHALL show it as the associated user

#### Scenario: Missing user name is not an error
- **WHEN** the selected configuration file has no `user_name`
- **THEN** the import SHALL proceed with the URL and the key
- **AND** the associated user SHALL be obtained from the server instead

#### Scenario: Invalid file content is rejected
- **WHEN** the selected file is not a valid configuration file (missing or malformed fields, or an invalid 32-byte base64 ed25519 seed)
- **THEN** the import SHALL be refused and the user SHALL be told the file is invalid
- **AND** the previously stored URL, key and user name SHALL be left unchanged

#### Scenario: http URL for a domain or IP is accepted
- **WHEN** the configuration file contains a `server_url` with the `http` scheme for localhost or a private/link-local/CGNAT address
- **THEN** the import SHALL proceed and store the URL

#### Scenario: public http URL is rejected
- **WHEN** the configuration file contains a `server_url` with the `http` scheme for a public host
- **THEN** the import SHALL be refused with a clear message before any sync attempt
- **AND** the previously stored configuration SHALL remain unchanged

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
- **AND** the stored user name SHALL be replaced by the one in the file, or cleared when the file carries none
- **AND** subsequent syncs SHALL use the new configuration

## ADDED Requirements

### Requirement: Invalid import does not reset sync state

An import rejected by URL or seed validation SHALL leave the stored configuration and all sync state untouched: no URL, key or user name change, no watermark clear, no photo-hash clear, and no sync attempt.

#### Scenario: Rejected import leaves the device usable
- **WHEN** the user selects a configuration file that fails URL or seed validation
- **THEN** the existing server configuration SHALL remain active
- **AND** the sync watermark and pending state SHALL NOT be cleared
- **AND** no sync SHALL be triggered
