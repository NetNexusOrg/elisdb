## Purpose

Defines the release build requirements for the Android app: reproducible, signed, minified artifacts that embed the correct configuration, so a shipped release installs and runs identically to the configured environment.

## Requirements
### Requirement: Reproducible release versioning

The system SHALL produce a release artifact whose version code and version name come from a single, non-hard-coded source, and SHALL expose the version name to the running app.

#### Scenario: Version derived from a single source
- **WHEN** a release is built
- **THEN** the version code and version name SHALL be read from a single configured source (gradle properties or CI)
- **AND** the version name SHALL be exposed to the running application

### Requirement: Release signing

The system SHALL require a release build to be signed, reading the keystore from a non-version-controlled source, and SHALL fail the build when signing details are missing.

#### Scenario: Release build requires signing credentials
- **WHEN** a release build runs without keystore credentials configured
- **THEN** the build SHALL fail with a clear message rather than producing an unsigned artifact

#### Scenario: Secrets are not committed
- **WHEN** the repository is inspected
- **THEN** keystore files and passwords SHALL NOT be present in version control

### Requirement: Minified release preserves behavior

The system SHALL produce a release artifact with minification enabled that still works: data sync (Ktor client, kotlinx-serialization), map rendering (osmdroid), photo handling (CameraX/Coil), and persistence (Room).

#### Scenario: Minified build preserves runtime features
- **WHEN** a release artifact with minification enabled is installed and exercised
- **THEN** sync, maps, photos and persistence SHALL function as in the debug build

### Requirement: Configurable server configuration

The system SHALL require the server base URL and signing key to be configured explicitly at build time, and SHALL NOT embed an implicit default server URL or signing key. Absent a configured value, the corresponding build config field SHALL be blank so the app is left unconfigured rather than pointed at an implied host.

#### Scenario: Release embeds explicitly configured server settings
- **WHEN** a release is built with a server base URL and signing key explicitly provided
- **THEN** the artifact SHALL communicate with that server URL and sign requests with that key

#### Scenario: No default server URL is embedded
- **WHEN** a release is built without an explicit server base URL
- **THEN** the artifact SHALL NOT contain a baked-in fallback server URL
- **AND** the server base URL build config field SHALL be blank

#### Scenario: No default signing key is embedded
- **WHEN** a release is built without an explicit signing key
- **THEN** the signing key build config field SHALL be blank
- **AND** a build that ships blank is unconfigured until a key is provided