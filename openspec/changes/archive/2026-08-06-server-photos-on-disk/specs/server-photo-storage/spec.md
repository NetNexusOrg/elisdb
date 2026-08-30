## Purpose

Stores server-side photo binary content as files on a managed filesystem, referenced from the database by path, so the SQLite database stays free of large BLOBs while photos remain durable and cascade-deleted with their client.

## ADDED Requirements

### Requirement: Photo files stored on disk

The system SHALL persist photo binary content as files in a server-managed directory and SHALL store in the database a stable reference (path) to each photo file instead of the binary bytes.

#### Scenario: Push persists photo to disk as a file
- **WHEN** a client with one or more photos is pushed to the server
- **THEN** each photo's binary content SHALL be written to a file in the managed directory
- **AND** the database SHALL store a reference to that file rather than the photo bytes

#### Scenario: Photo file returned as base64
- **WHEN** a stored client's photos are pulled
- **THEN** each photo SHALL be returned as its binary content in base64 from the referenced file

### Requirement: Photo storage durability

Photo files SHALL persist across server restarts alongside the database.

#### Scenario: Photo files survive server restart
- **WHEN** the server is restarted after photos were saved
- **THEN** the photo files SHALL still be present and readable

### Requirement: Cascading photo deletion

Deleting a client SHALL remove its stored photos' files from disk, matching the database-level deletion of the photo records.

#### Scenario: Deleting a client removes its photo files
- **WHEN** a client is deleted from the server
- **THEN** the client's photo files SHALL be removed from the managed directory

### Requirement: Photo storage configuration

The server SHALL allow the photo storage directory to be configured, defaulting to a subdirectory of the database location.

#### Scenario: Default photo directory
- **WHEN** no photo directory is explicitly configured
- **THEN** photos SHALL be stored relative to the database location

#### Scenario: Configured photo directory
- **WHEN** a photo directory is explicitly configured
- **THEN** photos SHALL be stored under that configured path