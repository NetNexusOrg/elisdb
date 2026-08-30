## Purpose

Provides a minimal, standalone database server that persists clients and their photos in a single shared store, rejects duplicate records, and resolves edit conflicts by last-write-wins so that offline-first clients can sync their data through a simple push/pull HTTP API.

## ADDED Requirements

### Requirement: Authoritative remote store
The server SHALL act as the authoritative, persistent store for client records and their associated photos, and SHALL own all database management responsibilities including persistence, duplicate detection and conflict resolution.

#### Scenario: Server persists records
- **WHEN** a client with photos is pushed to the server
- **THEN** the server SHALL store the client and its photos durably

#### Scenario: Data survives server restart
- **WHEN** the server is restarted
- **THEN** all previously stored clients and photos SHALL still be available

### Requirement: Push changes
The server SHALL accept a push of client and photo changes from a client and persist them. Pushing a client SHALL also push its photos together with the client.

#### Scenario: Client pushes new records
- **WHEN** a client pushes a batch of new or updated client records with their photos
- **THEN** each record and photo SHALL be persisted

#### Scenario: Push is idempotent per record
- **WHEN** the same client record is pushed more than once
- **THEN** it SHALL be stored once, not duplicated

### Requirement: Pull changes
The server SHALL allow a client to pull all stored client records with their photos.

#### Scenario: Client pulls all data
- **WHEN** a client requests a pull
- **THEN** the server SHALL return all stored clients with their photos

### Requirement: Duplicate detection
The server SHALL reject records that represent the same real-world entity as an existing record to prevent duplicates. Records SHALL be compared by the client name; case and surrounding whitespace SHALL be ignored.

#### Scenario: Duplicate name rejected on create
- **WHEN** a client is pushed whose name matches an existing client's name, ignoring case and surrounding whitespace
- **THEN** the server SHALL refuse to create a duplicate
- **AND** SHALL return an error identifying the conflict

#### Scenario: Same-name record is a duplicate
- **WHEN** two client records have the same name ignoring case and surrounding whitespace
- **THEN** the server SHALL treat them as duplicates of one another

### Requirement: Conflict resolution
The server SHALL resolve edit conflicts between records referencing the same stored client by last-write-wins, where the record with the most recent modification timestamp wins and overwrites the stored value.

#### Scenario: Newer update wins
- **WHEN** a client record is updated whose modification timestamp is newer than the stored record's
- **THEN** the server SHALL store the newer values

#### Scenario: Stale update loses
- **WHEN** a client record is pushed whose modification timestamp is older than the stored record's
- **THEN** the server SHALL keep the newer stored values
- **AND** the stale payload SHALL NOT overwrite the stored record

### Requirement: Record identity
Each client record SHALL carry a stable client identifier and a modification timestamp used for conflict resolution. Photos SHALL belong to exactly one client.

#### Scenario: Record has identity and timestamp
- **WHEN** a client record is received
- **THEN** it SHALL have an identifier, a modification timestamp, name, coordinates, and optional contract and notes
- **AND** its photos SHALL be associated only with that client

### Requirement: Simple management API
The server SHALL expose a minimal management surface: list all clients and delete a client by identifier (cascading to its photos).

#### Scenario: List all clients
- **WHEN** a management request lists clients
- **THEN** all stored clients SHALL be returned

#### Scenario: Delete a client and its photos
- **WHEN** a management request deletes a client by identifier
- **THEN** the client and its photos SHALL be removed from the store