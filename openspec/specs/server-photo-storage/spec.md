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

The cleanup pass SHALL compare stored paths with absolute paths, so a relative photos directory (development, or a mount configured without a leading slash) never makes every stored photo look orphaned. Entries in the photos directory that are not regular files SHALL be left alone instead of aborting the pass.

#### Scenario: Edited-out photo file is deleted
- **WHEN** a push removes a photo from a client
- **THEN** the photo row and its file SHALL be removed

#### Scenario: Orphan files are garbage collected
- **WHEN** the cleanup pass runs and a file has no matching row
- **THEN** that file SHALL be deleted
- **AND** files with a matching row SHALL be kept

#### Scenario: Relative photos directory does not trigger deletion
- **WHEN** the photos directory is configured as a relative path
- **THEN** the cleanup pass SHALL still recognise the stored photos
- **AND** SHALL NOT delete them

#### Scenario: Non-file entries are ignored
- **WHEN** the photos directory contains a subdirectory
- **THEN** the cleanup pass SHALL skip it without failing

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
- **WHEN** a photo was uploaded but no push ever referenced it
- **THEN** the cleanup pass SHALL delete it

#### Scenario: Referenced photo file is kept
- **WHEN** a photo file is referenced by a client record
- **THEN** the cleanup pass SHALL keep it

### Requirement: Schema migrations preserve photo rows

A schema migration SHALL NOT delete or orphan existing `fotos` rows. Rebuilding a table that other tables reference by foreign key SHALL disable foreign key enforcement outside any transaction, verify that enforcement is really off before rebuilding, and SHALL NOT complete if the number of photo rows changed.

#### Scenario: Upgrade from a schema without optional coordinates keeps photos
- **WHEN** a database whose `clientes` table still requires coordinates is opened
- **THEN** the table SHALL be rebuilt
- **AND** every client and every photo row SHALL still exist afterwards

#### Scenario: A rebuild that would lose photos aborts
- **WHEN** a rebuild would remove photo rows
- **THEN** the migration SHALL fail instead of committing the loss

### Requirement: Photo upload requires a referenced photo

The server SHALL accept the binary upload of a photo identifier only when that identifier is referenced by a persisted client photo row or by an explicit upload reservation created for an accepted push. An upload for an unknown or unreferenced identifier SHALL be rejected and SHALL NOT create a file.

#### Scenario: Referenced photo is uploaded
- **WHEN** an authenticated client uploads bytes for a photo identifier that belongs to a stored client
- **THEN** the server SHALL store the bytes as before

#### Scenario: Unknown photo id is rejected
- **WHEN** an authenticated client uploads bytes for an identifier with no photo row or reservation
- **THEN** the server SHALL reject the upload with a client-error response
- **AND** SHALL NOT write a file for it

#### Scenario: Upload is tied to an accepted push
- **WHEN** a push accepts a record whose photo bytes are still missing
- **THEN** the server SHALL allow the client to upload those referenced photo identifiers
- **AND** SHALL reject identifiers not referenced by that accepted record

### Requirement: Photo storage quotas are enforced

The server SHALL enforce configurable quotas on photo storage per key and for the whole instance, counting stored bytes and rejecting an upload that would exceed a quota with a client-error response. The quota check SHALL happen before writing the file, and the server SHALL expose enough information for an operator to detect quota pressure.

#### Scenario: Per-key quota is exceeded
- **WHEN** an authenticated key uploads a photo that would exceed its configured storage quota
- **THEN** the server SHALL reject the upload without writing the file
- **AND** the response SHALL indicate that the quota was exceeded

#### Scenario: Instance quota is exceeded
- **WHEN** an upload would exceed the instance-wide storage quota
- **THEN** the server SHALL reject it without writing the file
- **AND** SHALL keep serving already stored photos

#### Scenario: Upload within quota succeeds
- **WHEN** an upload keeps both quotas within their limits
- **THEN** the server SHALL store the photo normally

### Requirement: Orphan photo cleanup runs periodically

The server SHALL run the orphan-photo cleanup on a periodic schedule independent of client pushes, in addition to the startup and post-push passes, so unreferenced uploads cannot accumulate for the lifetime of a process that receives no pushes.

#### Scenario: Cleanup runs without a push
- **WHEN** the server has been running without any push and an unreferenced photo file exists past the grace period
- **THEN** the periodic cleanup SHALL remove it

#### Scenario: Referenced files survive periodic cleanup
- **WHEN** the periodic cleanup runs
- **THEN** files referenced by photo rows SHALL be kept

### Requirement: Photo content hash is persisted and reused

When the server computes a photo's content hash for a record with no stored hash, it SHALL persist that hash in the same committed operation and reuse it on later reads. A read SHALL NOT leave an uncommitted hash update that is discarded and recomputed on every request.

#### Scenario: Missing hash is persisted
- **WHEN** a stored photo has no content hash and its file exists
- **THEN** the server SHALL compute the hash
- **AND** SHALL persist it so later reads return the stored value

#### Scenario: Later reads reuse the hash
- **WHEN** the same photo is read again after the hash was persisted
- **THEN** the server SHALL NOT recompute the file hash
- **AND** SHALL return the persisted hash
