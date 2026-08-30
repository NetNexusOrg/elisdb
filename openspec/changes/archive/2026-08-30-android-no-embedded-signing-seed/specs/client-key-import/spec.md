## MODIFIED Requirements

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
