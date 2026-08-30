## MODIFIED Requirements

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

## ADDED Requirements

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