## ADDED Requirements

### Requirement: Client name stored in uppercase on the server

The server SHALL store the client's name in uppercase letters when a client is pushed, regardless of the casing sent by the client, so names stay consistent across synchronized devices.

#### Scenario: Pushed name is uppercased
- **WHEN** a client is pushed whose name contains lowercase or mixed-case letters
- **THEN** the server SHALL store the name in uppercase letters

#### Scenario: Pull returns the uppercased name
- **WHEN** a client pulls records whose names were pushed in lowercase or mixed case
- **THEN** the names SHALL be returned in uppercase letters