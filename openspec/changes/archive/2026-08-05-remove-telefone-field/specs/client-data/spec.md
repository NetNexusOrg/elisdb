## MODIFIED Requirements

### Requirement: Client record schema

The system SHALL store client records containing an identifier, a mandatory name, an optional contract reference, latitude and longitude coordinates, optional free-text notes, and the creation timestamp.

#### Scenario: A client record has all required fields
- **WHEN** a client is created
- **THEN** the record MUST contain a name, latitude, longitude and creation timestamp
- **AND** the contract reference and notes MAY be empty

#### Scenario: Unique client identifiers
- **WHEN** multiple clients exist
- **THEN** each client SHALL have a unique identifier assigned automatically at creation

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

### Requirement: Update client

The system SHALL allow editing a client's name, contract reference, coordinates, notes and photos, preserving the client's identifier.

#### Scenario: Edit client fields
- **WHEN** the user saves changes to an existing client
- **THEN** the updated values SHALL be persisted
- **AND** the client's identifier SHALL remain unchanged