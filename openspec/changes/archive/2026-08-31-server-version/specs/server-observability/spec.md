## MODIFIED Requirements

### Requirement: Operational logging

The server SHALL configure structured logging at startup (level configurable via environment) and SHALL emit log records for at least: server startup (including version), unexpected server errors (HTTP 500 / unhandled exceptions), and authentication rejections (HTTP 401), so an operator can diagnose sync failures.

#### Scenario: Startup is logged
- **WHEN** the server process starts
- **THEN** a startup log record SHALL be emitted including the server version

#### Scenario: Server errors are logged
- **WHEN** a request causes an unhandled server error
- **THEN** the error SHALL be logged (without exposing request secrets or photo bodies)

#### Scenario: Auth rejections are logged
- **WHEN** a request is rejected for invalid/missing signature
- **THEN** the rejection SHALL be logged at an appropriate level (without exposing the signing key or photo bodies)