## Purpose

Expõe a versão do servidor ElisDB para diagnóstico, suporte e alinhamento entre cliente/servidor.

## Requirements

### Requirement: Health endpoint exposes version

The server SHALL include the version string in the `/health` endpoint response.

#### Scenario: Health returns version
- **WHEN** a client requests `GET /health`
- **THEN** the response SHALL include a `version` field with the server's SemVer version string

#### Scenario: Version is SemVer format
- **WHEN** the server returns the version in the health response
- **THEN** the version string SHALL conform to SemVer format (MAJOR.MINOR.PATCH)

### Requirement: Version is configurable at build time

The server SHALL accept a version string via Docker build arg `VERSION` and make it available at runtime.

#### Scenario: Build arg propagates version
- **WHEN** the Docker image is built with `--build-arg VERSION=1.2.3`
- **THEN** the running server SHALL report version `1.2.3` in the health endpoint

#### Scenario: Default version when not provided
- **WHEN** the Docker image is built without specifying `VERSION`
- **THEN** the server SHALL use a default version string `0.0.0-dev`

### Requirement: Version available via environment variable

The server SHALL expose the version via the `ELISDB_VERSION` environment variable.

#### Scenario: Environment variable is set
- **WHEN** the server process starts
- **THEN** the `ELISDB_VERSION` environment variable SHALL be set to the current version

### Requirement: Server exposes a stable instance identity

The server SHALL expose a stable instance identifier in its open health response and in its authenticated latest-data response. The identifier SHALL remain the same across server restarts for the same database, and SHALL change when the server starts with a different or recreated database, so a client can distinguish the same server from a new or restored one.

#### Scenario: Health response carries the identity
- **WHEN** a client or monitor requests the health endpoint
- **THEN** the response SHALL include the server instance identifier
- **AND** SHALL remain compatible with clients that ignore the new field

#### Scenario: Latest-data response carries the identity
- **WHEN** an authenticated client requests the latest-data endpoint
- **THEN** the response SHALL include the same instance identifier
- **AND** the client SHALL be able to compare it with the identifier stored for its configuration

#### Scenario: Identity survives a restart
- **WHEN** the server restarts with the same database
- **THEN** it SHALL report the same instance identifier

#### Scenario: Recreated database gets a new identity
- **WHEN** the server starts with a new or recreated database
- **THEN** it SHALL report a different instance identifier
