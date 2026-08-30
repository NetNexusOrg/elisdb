## MODIFIED Requirements

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