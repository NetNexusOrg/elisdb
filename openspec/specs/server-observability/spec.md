## Purpose

Gives the operator visibility into server operation for a self-hosted deployment: startup, unexpected errors, and authentication rejections are recorded via structured logging, without ever recording secrets.

## Requirements
### Requirement: Operational logging

The server SHALL configure structured logging at startup (level configurable via environment) and SHALL emit log records for at least: server startup, unexpected server errors (HTTP 500 / unhandled exceptions), and authentication rejections (HTTP 401), so an operator can diagnose sync failures.

#### Scenario: Startup is logged
- **WHEN** the server process starts
- **THEN** a startup log record SHALL be emitted

#### Scenario: Server errors are logged
- **WHEN** a request causes an unhandled server error
- **THEN** the error SHALL be logged (without exposing request secrets or photo bodies)

#### Scenario: Auth rejections are logged
- **WHEN** a request is rejected for invalid/missing signature
- **THEN** the rejection SHALL be logged at an appropriate level (without exposing the signing key or photo bodies)

### Requirement: No secrets in logs

The server SHALL NOT write private seeds, signing keys, or photo binary content to its logs.

#### Scenario: Secrets are not logged
- **WHEN** the server logs a request or error
- **THEN** the log SHALL NOT include private seeds, private keys, or photo bytes
