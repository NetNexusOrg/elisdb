# Spec Delta

## ADDED Requirements

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
