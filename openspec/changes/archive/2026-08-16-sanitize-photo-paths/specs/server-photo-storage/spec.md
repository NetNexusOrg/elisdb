## ADDED Requirements

### Requirement: Photo paths confined to the photos directory

The system SHALL ensure that every photo file path is resolved inside the configured photos directory, regardless of the photo identifier supplied by a client. Photo identifiers that would escape the directory (containing path separators or traversal sequences) SHALL be rejected.

#### Scenario: Traversal photo id is rejected
- **WHEN** a push includes a photo whose id contains a path separator or a traversal sequence (e.g. `../`)
- **THEN** the server SHALL reject that photo (the record is not persisted with a file outside the photos directory)

#### Scenario: Normal photo id writes inside the directory
- **WHEN** a push includes a photo with a plain identifier (no separators)
- **THEN** the server SHALL store the photo file inside the configured photos directory as before
