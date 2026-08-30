## Purpose

Stores client records and their associated photos locally on the device so the app can list, create, edit and delete clients persistently across app restarts.

## ADDED Requirements

### Requirement: Client record schema

The system SHALL store client records containing an identifier, a mandatory name, an optional phone number, latitude and longitude coordinates, optional free-text notes, and the creation timestamp.

#### Scenario: A client record has all required fields
- **WHEN** a client is created
- **THEN** the record MUST contain a name, latitude, longitude and creation timestamp
- **AND** the phone number and notes MAY be empty

#### Scenario: Unique client identifiers
- **WHEN** multiple clients exist
- **THEN** each client SHALL have a unique identifier assigned automatically at creation

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

The system SHALL allow creating a client with a name, phone number, coordinates, notes and photos.

#### Scenario: Create client with all fields
- **WHEN** the user saves a new client with a name, phone, coordinates, notes and photos
- **THEN** the client SHALL be persisted
- **AND** the photos SHALL be linked to the created client

#### Scenario: Create client without name is prevented
- **WHEN** the user attempts to save a client without a name
- **THEN** the save SHALL be rejected
- **AND** the user SHALL be informed that the name is required

### Requirement: Read clients

The system SHALL allow listing all stored clients and reading a single client by its identifier, including its photos.

#### Scenario: List all clients
- **WHEN** the client list screen is shown
- **THEN** it SHALL display all stored clients

#### Scenario: Read a client with its photos
- **WHEN** a client detail screen is opened
- **THEN** it SHALL display the client's data and its photos

### Requirement: Update client

The system SHALL allow editing a client's name, phone number, coordinates, notes and photos, preserving the client's identifier.

#### Scenario: Edit client fields
- **WHEN** the user saves changes to an existing client
- **THEN** the updated values SHALL be persisted
- **AND** the client's identifier SHALL remain unchanged

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
