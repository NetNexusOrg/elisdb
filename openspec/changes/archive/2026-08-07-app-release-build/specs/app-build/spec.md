## Purpose

Defines the release build requirements for the Android app: reproducible, signed, minified artifacts that embed the correct configuration, so a shipped release installs and runs identically to the configured environment.

## ADDED Requirements

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

The system SHALL allow the server base URL and signing key to be configured at build time, and SHALL embed the configured values in the release artifact.

#### Scenario: Release embeds configured server settings
- **WHEN** a release is built with a configured server base URL and signing key
- **THEN** the artifact SHALL communicate with that server URL and sign requests with that key