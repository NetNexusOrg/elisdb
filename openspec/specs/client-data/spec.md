## Purpose

Stores client records and their associated photos locally on the device so the app can list, create, edit and delete clients persistently across app restarts.

## Requirements
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

### Requirement: Photo records linked to a client

The system SHALL store photo records linked to exactly one client, where each photo record holds a reference to the photo file location in app-private storage. Deleting a client SHALL delete its linked photo records and photo files.

#### Scenario: Photos belong to one client
- **WHEN** a photo is added
- **THEN** it SHALL be associated with exactly one client

#### Scenario: Deleting a client removes its photos
- **WHEN** a client is deleted
- **THEN** all of its photo records SHALL be deleted
- **AND** the underlying photo files SHALL be removed from storage

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

### Requirement: Read clients

The system SHALL allow listing all stored clients and reading a single client by its identifier, including its photos.

#### Scenario: List all clients
- **WHEN** the client list screen is shown
- **THEN** it SHALL display all stored clients

#### Scenario: Read a client with its photos
- **WHEN** a client detail screen is opened
- **THEN** it SHALL display the client's data and its photos

### Requirement: Update client

The system SHALL allow editing a client's name, contract reference, coordinates, notes and photos, preserving the client's identifier and refreshing its modification timestamp.

#### Scenario: Edit client fields
- **WHEN** the user saves changes to an existing client
- **THEN** the updated values SHALL be persisted
- **AND** the client's identifier SHALL remain unchanged

#### Scenario: Editing updates the modification timestamp
- **WHEN** the user saves changes to an existing client
- **THEN** the updated values SHALL be persisted
- **AND** the client's identifier SHALL remain unchanged
- **AND** the modification timestamp SHALL be refreshed

### Requirement: Delete client

The system SHALL allow deleting a client.

#### Scenario: Delete an existing client
- **WHEN** the user deletes a client from the detail screen
- **THEN** the client SHALL be removed from storage
- **AND** the user SHALL be returned to the client list

### Requirement: Persistence across restarts

The system SHALL persist clients and photos locally so they survive app process death and device reboots.

#### Scenario: Data survives app restart
- **WHEN** the app is restarted after clients and photos were saved
- **THEN** all saved clients and photos SHALL still be present

### Requirement: Client name stored in uppercase

The system SHALL store the client's name in uppercase letters regardless of the casing the user enters, so the same client always has a consistent casing in storage.

#### Scenario: Name stored uppercased on create
- **WHEN** a new client is created with a name containing lowercase or mixed-case letters
- **THEN** the stored name SHALL be entirely uppercase

#### Scenario: Name uppercased on edit
- **WHEN** an existing client's name is edited to contain lowercase or mixed-case letters
- **THEN** the updated stored name SHALL be entirely uppercase

#### Scenario: Stored name uppercased on sync
- **WHEN** a client name is received from the server during a sync
- **THEN** the locally stored name SHALL be entirely uppercase
