## MODIFIED Requirements

### Requirement: Client record schema

The system SHALL store client records containing a stable identifier, a mandatory name, an optional contract reference, an optional set of tags stored in a single text field separated by `;`, optional latitude and longitude coordinates, optional free-text notes, an active/inactive status, a creation timestamp, and a modification timestamp, where the identifier and timestamps are compatible with the server so records can be synchronized by identifier and last-write-wins. Tags SHALL be stored trimmed, uppercased, without duplicates and sorted alphabetically.

#### Scenario: A client record has all required fields
- **WHEN** a client is created
- **THEN** the record MUST contain a name and a creation timestamp
- **AND** the contract reference, tags, coordinates, and notes MAY be empty

#### Scenario: Client with multiple tags
- **WHEN** a client is stored with the tags `GOLD`, `PREMIUM` and `ROTA 21`
- **THEN** the record SHALL keep all three tags in the tag field
- **AND** the tags SHALL be stored uppercased, without duplicates and alphabetically ordered

#### Scenario: Existing single tag remains a one-tag set
- **WHEN** an existing client already has a single tag
- **THEN** it SHALL remain readable as a set containing that tag
- **AND** no data migration SHALL be required

#### Scenario: Status defaults to active
- **WHEN** a client is created without an explicit status
- **THEN** the record SHALL be stored as active

#### Scenario: Existing clients survive the status migration
- **WHEN** the local database is migrated to include the status
- **THEN** every previously stored client SHALL remain active
- **AND** no client data SHALL be lost

#### Scenario: Existing clients survive the tag migration
- **WHEN** the local database is migrated to include the tag
- **THEN** every previously stored client SHALL keep its data
- **AND** its tag SHALL be empty

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

The system SHALL allow creating a client with a name, contract reference, a set of tags, coordinates, notes, photos and an active/inactive status.

#### Scenario: Create client with all fields
- **WHEN** the user saves a new client with a name, contract reference, tags, coordinates, notes and photos
- **THEN** the client SHALL be persisted
- **AND** the photos SHALL be linked to the created client

#### Scenario: Create client with multiple tags
- **WHEN** the user saves a new client with more than one tag
- **THEN** all the tags SHALL be persisted
- **AND** they SHALL be normalized and ordered alphabetically

#### Scenario: Create client without name is prevented
- **WHEN** the user attempts to save a client without a name
- **THEN** the save SHALL be rejected
- **AND** the user SHALL be informed that the name is required

#### Scenario: Create client without coordinates
- **WHEN** the user saves a new client with a name but without coordinates
- **THEN** the client SHALL be persisted without coordinates

#### Scenario: Create an inactive client
- **WHEN** the user saves a new client with the active switch turned off
- **THEN** the client SHALL be persisted as inactive

#### Scenario: Create a client without a tag
- **WHEN** the user saves a new client leaving the tags empty
- **THEN** the client SHALL be persisted with no tags

### Requirement: Update client

The system SHALL allow editing a client's name, contract reference, set of tags, coordinates, notes, photos and active/inactive status, preserving the client's identifier and refreshing its modification timestamp.

#### Scenario: Edit client fields
- **WHEN** the user saves changes to an existing client
- **THEN** the updated values SHALL be persisted
- **AND** the client's identifier SHALL remain unchanged

#### Scenario: Editing updates the modification timestamp
- **WHEN** the user saves changes to an existing client
- **THEN** the updated values SHALL be persisted
- **AND** the client's identifier SHALL remain unchanged
- **AND** the modification timestamp SHALL be refreshed

#### Scenario: Edit the set of tags
- **WHEN** the user adds or removes a tag and saves
- **THEN** the stored set of tags SHALL reflect the change
- **AND** the tags SHALL remain normalized and ordered alphabetically

#### Scenario: Deactivate a client
- **WHEN** the user turns the active switch off and saves an existing client
- **THEN** the client SHALL be stored as inactive
- **AND** the client's photos and history SHALL be preserved

#### Scenario: Clear a client's tag
- **WHEN** the user removes every tag of an existing client and saves
- **THEN** the client SHALL be stored without tags
- **AND** the client's other fields SHALL be preserved
