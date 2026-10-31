## Purpose

Lets the user import a server-issued ed25519 authentication key into the app settings so the device can sign requests as the server-issued identity, completing server-side controlled onboarding.

## Requirements
### Requirement: Import a server configuration file

The system SHALL provide an action in the app settings to import a server-issued configuration file containing the server URL, the ed25519 authentication key and, when present, the user name the server registered for that key, and SHALL store all fields so the device is fully configured in one step. The `server_url` SHALL be validated on import: a URL with the `http` or `https` scheme SHALL be accepted for any host; any other scheme SHALL be rejected at import time with a clear message.

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
- **AND** the stored user name SHALL be replaced by the one in the file, or cleared when the file carries none
- **AND** subsequent syncs SHALL use the new configuration

### Requirement: Signing key is never embedded in the build

The system SHALL NOT ship or use a real signing seed baked into the APK. The signing key used to authenticate requests SHALL come from a per-device import of a server-issued configuration (see "Import a server configuration file"). Any build-time seed present in `BuildConfig` SHALL be ignored for authentication and SHALL NOT be used to sign requests.

#### Scenario: Imported key is the source of signing
- **WHEN** a user imports a valid configuration file
- **THEN** the app SHALL sign requests with the imported seed
- **AND** SHALL NOT fall back to any build-time seed

#### Scenario: Build-time seed is ignored
- **WHEN** the build contains a `BuildConfig` signing seed value
- **THEN** the app SHALL NOT use it to authenticate against the server
- **AND** SHALL require an imported key before syncing

#### Scenario: No embedded key authenticates
- **WHEN** no key has been imported and only a build-time seed exists
- **THEN** the app SHALL report a configuration fault and SHALL NOT sign requests with the embedded seed

### Requirement: Imported key shows its associated user

The system SHALL display the user name associated with an imported signing key. The name carried by the configuration file SHALL be authoritative and SHALL be shown without contacting the server; when the file carries no name, the app SHALL fall back to asking the configured server. A missing name SHALL NOT be fabricated.

#### Scenario: Associated user is shown after import
- **WHEN** the user imports a key and the configured server reports a user name for it
- **THEN** the settings screen SHALL show that user name next to the imported key

#### Scenario: Imported name is shown without a server lookup
- **WHEN** the user imports a configuration file that carries a `user_name`
- **THEN** the settings screen SHALL show that name as the associated user
- **AND** SHALL NOT need a server request to display it

#### Scenario: Server lookup when the file has no name
- **WHEN** the imported configuration file carries no `user_name` and the configured server reports a user name for the key
- **THEN** the settings screen SHALL show the name reported by the server

#### Scenario: No associated user is shown as absent
- **WHEN** neither the configuration file nor the configured server provides a user name for the imported key
- **THEN** the settings screen SHALL indicate that no user is associated with the key
- **AND** SHALL NOT fabricate a name

#### Scenario: Identity lookup is non-blocking
- **WHEN** the server cannot be reached while looking up the key identity (network or server error)
- **THEN** the imported key SHALL remain usable for sync
- **AND** the missing identity SHALL NOT surface as a configuration error
- **AND** the settings screen SHALL show that the associated user could not be determined, distinct from "no user associated"

#### Scenario: Identity is re-resolved after a successful sync
- **WHEN** a successful sync completes and the associated user had previously not been resolved
- **THEN** the app SHALL retry the identity lookup so the user name can appear without reopening the settings screen
