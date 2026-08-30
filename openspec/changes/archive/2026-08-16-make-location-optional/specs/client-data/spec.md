## MODIFIED Requirements

### Requirement: Client record schema

The system SHALL store client records containing a stable identifier, a mandatory name, an optional contract reference, optional latitude and longitude coordinates, optional free-text notes, a creation timestamp, and a modification timestamp, where the identifier and timestamps are compatible with the server so records can be synchronized by identifier and last-write-wins.

#### Scenario: A client record has all required fields
- **WHEN** a client is created
- **THEN** the record MUST contain a name and a creation timestamp
- **AND** the contract reference, coordinates, and notes MAY be empty

#### Scenario: Unique client identifiers
- **WHEN** multiple clients exist
- **THEN** each client SHALL have a unique identifier assigned automatically at creation

#### Scenario: Record carries modification timestamp
- **WHEN** a client record is updated
- **THEN** the record's modification timestamp SHALL be refreshed
- **AND** SHALL be used as the basis for server conflict resolution

#### Scenario: Identifier is shared with the server
- **WHEN** a client is synchronized with the server
- **THEN** it SHALL be identified by the same stable identifier on the device and on the server

#### Scenario: Client without coordinates is stored
- **WHEN** a client is created without latitude or longitude
- **THEN** the record SHALL be stored with no coordinates

### Requirement: Create client

The system SHALL allow creating a client with a name, contract reference, coordinates, notes and photos.

#### Scenario: Create client with all fields
- **WHEN** the user saves a new client with a name, contract reference, coordinates, notes and photos
- **THEN** the client SHALL be persisted
- **AND** the photos SHALL be linked to the created client

#### Scenario: Create client without name is prevented
- **WHEN** the user attempts to save a client without a name
- **THEN** the save SHALL be rejected
- **AND** the user SHALL be informed that the name is required

#### Scenario: Create client without coordinates
- **WHEN** the user saves a new client with a name but without coordinates
- **THEN** the client SHALL be persisted without coordinates