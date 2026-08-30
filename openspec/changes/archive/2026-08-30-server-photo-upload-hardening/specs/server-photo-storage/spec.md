## MODIFIED Requirements

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
