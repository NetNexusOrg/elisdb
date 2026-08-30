## Purpose

Stores server-side photo binary content as files on a managed filesystem, referenced from the database by path, so the SQLite database stays free of large BLOBs while photos remain durable and cascade-deleted with their client.

## Requirements
### Requirement: Photo files stored on disk

The system SHALL persist photo binary content as files in a server-managed directory and SHALL store in the database a stable reference (path) to each photo file instead of the binary bytes.

#### Scenario: Upload writes photo to disk as a file
- **WHEN** a client uploads a photo's binary content to the photo endpoint
- **THEN** the photo's binary content SHALL be written to a file in the managed directory
- **AND** the database SHALL store a reference to that file rather than the photo bytes

#### Scenario: Pull returns photo metadata with a content hash
- **WHEN** a stored client's photos are pulled
- **THEN** each photo SHALL be returned as metadata (id, mime, order) with a content hash
- **AND** the pull SHALL NOT inline the photo's binary bytes

### Requirement: Photo content served and stored as binary files

The system SHALL expose authenticated endpoints to download a photo's raw bytes by identifier and to upload a photo's raw bytes by identifier, with the media type recorded for that photo.

#### Scenario: Photo downloaded by identifier
- **WHEN** an authenticated client requests a stored photo by its identifier
- **THEN** the server SHALL return the photo's raw bytes with the stored media type

#### Scenario: Photo uploaded by identifier
- **WHEN** an authenticated client uploads a photo's raw bytes by its identifier
- **THEN** the server SHALL store the bytes in the managed directory under that identifier

#### Scenario: Unknown photo identifier returns not found
- **WHEN** an authenticated client requests a photo identifier that does not exist
- **THEN** the server SHALL return a not-found response

### Requirement: Photo storage durability

Photo files SHALL persist across server restarts alongside the database.

#### Scenario: Photo files survive server restart
- **WHEN** the server is restarted after photos were saved
- **THEN** the photo files SHALL still be present and readable

### Requirement: Cascading photo deletion

Deleting a client SHALL remove its stored photos' files from disk, matching the database-level deletion of the photo records. Removing an individual photo from a client (via an edit/push that no longer includes that photo) SHALL also remove that photo's file from disk, not only its database row.

#### Scenario: Deleting a client removes its photo files
- **WHEN** a client is deleted from the server
- **THEN** the client's photo files SHALL be removed from the managed directory

#### Scenario: Removing a photo from a client removes its file
- **WHEN** a client is edited/pushed and a previously stored photo is no longer included
- **THEN** the server SHALL delete that photo's database row AND its file from disk

### Requirement: Photo storage configuration

The server SHALL allow the photo storage directory to be configured, defaulting to a subdirectory of the database location.

#### Scenario: Default photo directory
- **WHEN** no photo directory is explicitly configured
- **THEN** photos SHALL be stored relative to the database location

#### Scenario: Configured photo directory
- **WHEN** a photo directory is explicitly configured
- **THEN** photos SHALL be stored under that configured path

### Requirement: Server keeps unchanged photos on merge

The server SHALL, when saving a client whose push references photos without data, keep the existing stored files and rows for those photos instead of deleting and rewriting them.

#### Scenario: Referenced photo kept unchanged
- **WHEN** a push includes a photo reference (no data) for a photo the server already stores
- **THEN** the server SHALL keep the existing file and row for that photo
- **AND** SHALL NOT delete or rewrite it

#### Scenario: Reference with no stored copy is ignored
- **WHEN** a push references a photo by id without data and the server has no stored copy
- **THEN** the server SHALL ignore that reference (no file is created and no row is added for it)

### Requirement: Photo paths confined to the photos directory

The system SHALL ensure that every photo file path is resolved inside the configured photos directory, regardless of the photo identifier supplied by a client. Photo identifiers that would escape the directory (containing path separators or traversal sequences) SHALL be rejected.

#### Scenario: Traversal photo id is rejected
- **WHEN** a push includes a photo whose id contains a path separator or a traversal sequence (e.g. `../`)
- **THEN** the server SHALL reject that photo (the record is not persisted with a file outside the photos directory)

#### Scenario: Normal photo id writes inside the directory
- **WHEN** a push includes a photo with a plain identifier (no separators)
- **THEN** the server SHALL store the photo file inside the configured photos directory as before

### Requirement: Orphaned photo files are removed

The server SHALL NOT leave photo files on disk that have no corresponding `fotos` row. When a photo row is deleted (cascade or edit), its file SHALL be removed; and the server SHALL provide a cleanup pass that deletes any file in the photos directory lacking a matching `fotos` row, so storage does not grow from orphaned uploads.

#### Scenario: Edited-out photo file is deleted
- **WHEN** a push removes a photo from a client
- **THEN** both the row and the file SHALL be gone

#### Scenario: Orphan files are garbage collected
- **WHEN** the cleanup pass runs
- **THEN** every file in the photos directory without a matching `fotos` row SHALL be removed
- **AND** files that still have a row SHALL be kept

### Requirement: Bounded photo upload

The server SHALL reject a photo upload whose size exceeds a configured maximum, returning a client-error response (e.g. 413) instead of buffering the entire body in memory, so a single device or a replayed request cannot exhaust server memory.

#### Scenario: Oversized upload is rejected
- **WHEN** an authenticated client uploads a photo larger than the configured maximum
- **THEN** the server SHALL reject the upload with a client-error response
- **AND** SHALL NOT load the full body into memory

#### Scenario: Within-limit upload is accepted
- **WHEN** an authenticated client uploads a photo within the configured maximum
- **THEN** the server SHALL store the bytes as normal

### Requirement: Unreferenced photo files are garbage collected

The server SHALL NOT retain photo files written by an authenticated `PUT /api/photos/{id}` that are never referenced by any client push. A cleanup pass SHALL remove files in the photos directory that have no corresponding `fotos` row, so unlinked uploads from rejected or abandoned pushes do not accumulate on disk.

#### Scenario: Abandoned PUT file is cleaned
- **WHEN** a photo file is written via PUT but no subsequent push references its id
- **THEN** the cleanup pass SHALL remove the unreferenced file

#### Scenario: Referenced photo file is kept
- **WHEN** a photo file is written via PUT and a push later references its id
- **THEN** the file SHALL be retained as a normal stored photo