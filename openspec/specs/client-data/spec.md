## Purpose

Stores client records and their associated photos locally on the device so the app can list, create, edit and delete clients persistently across app restarts.

## Requirements

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

### Requirement: Read clients

The system SHALL allow listing all stored clients and reading a single client by its identifier, including its photos.

#### Scenario: List all clients
- **WHEN** the client list screen is shown
- **THEN** it SHALL display all stored clients

#### Scenario: Read a client with its photos
- **WHEN** a client detail screen is opened
- **THEN** it SHALL display the client's data and its photos

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

### Requirement: Schema migrations run on every supported Android version

Every database migration SHALL be expressible in the oldest SQLite version shipped on a supported Android release, so an app update never fails to open its database on an older device. Changing a table's columns SHALL rebuild the table (create, copy, drop, rename) instead of using statements that only newer SQLite versions understand.

#### Scenario: Update on an older device opens the database
- **WHEN** the app is updated on a device below Android 14 and the stored schema needs the phone column removed
- **THEN** the migration SHALL complete
- **AND** the client records and photos SHALL be preserved

#### Scenario: Migration keeps the current schema
- **WHEN** the migration finishes
- **THEN** the resulting table SHALL match the schema declared by the current database version
