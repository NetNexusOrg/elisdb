# Spec Delta

## ADDED Requirements

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
