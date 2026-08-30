## MODIFIED Requirements

### Requirement: Client record schema

The system SHALL store client records containing a stable identifier, a mandatory name, an optional contract reference, latitude and longitude coordinates, optional free-text notes, a creation timestamp, and a modification timestamp, where the identifier and timestamps are compatible with the server so records can be synchronized by identifier and last-write-wins.

#### Scenario: Record carries modification timestamp
- **WHEN** a client record is updated
- **THEN** the record's modification timestamp SHALL be refreshed
- **AND** SHALL be used as the basis for server conflict resolution

#### Scenario: Identifier is shared with the server
- **WHEN** a client is synchronized with the server
- **THEN** it SHALL be identified by the same stable identifier on the device and on the server

### Requirement: Update client

The system SHALL allow editing a client's name, contract reference, coordinates, notes and photos, preserving the client's identifier and refreshing its modification timestamp.

#### Scenario: Editing updates the modification timestamp
- **WHEN** the user saves changes to an existing client
- **THEN** the updated values SHALL be persisted
- **AND** the client's identifier SHALL remain unchanged
- **AND** the modification timestamp SHALL be refreshed