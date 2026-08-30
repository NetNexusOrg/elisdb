# photo-content-identity Specification

## Purpose

Defines how locally stored photos are compared against server data so unchanged photos are never rewritten, and how photo files are committed so a failed sync never leaves rows pointing at missing files.

## Requirements

### Requirement: Unchanged photos are not rewritten

The system SHALL, when applying a pulled server record, compare photos by content and skip both the disk write and DB replacement for any photo whose content is unchanged.

#### Scenario: Unchanged photo is preserved
- **WHEN** a pull returns a photo with the same content as the existing local photo
- **THEN** the app SHALL keep the existing local photo file and DB row
- **AND** SHALL NOT re-encode, re-write, or delete it

#### Scenario: Changed photo is materialized
- **WHEN** a pull returns a photo whose content differs from the local one (or which is newly added)
- **THEN** the app SHALL replace the local photo file with the new content
- **AND** SHALL update the DB row to reference the new file

### Requirement: Photo files commit outside the DB transaction

The system SHALL sequence photo writes so that a file is fully written before its DB row is committed, and old files are only deleted after the new row is committed, preventing rows from referencing missing files if the pull fails midway.

#### Scenario: Failed pull keeps last good photo
- **WHEN** a pull fails while materializing photos (decode or disk write error)
- **THEN** all previously existing photo files and rows SHALL remain intact
- **AND** the sync SHALL be reported as a failure to be retried later
- **AND** retrying SHALL be idempotent (same result)

#### Scenario: New photo file written before DB row
- **WHEN** a new photo is materialized
- **THEN** the photo file SHALL be written to disk before its DB row is inserted
- **AND** if the write fails, no DB row SHALL be added for that photo

### Requirement: Content-based identity for photos

The system SHALL establish a stable content-derived identity (e.g. hash of the photo bytes) used to determine that two photos are the same.

#### Scenario: Same photo recognized by content
- **WHEN** two photo payloads have identical byte hashes
- **THEN** the system SHALL treat them as the same photo regardless of file names or row ids

### Requirement: Content hash decides push payload

The system SHALL use the photo's content-derived identity (byte hash) to decide whether a photo must be re-uploaded, comparing the local content hash against the hash of the last-pushed version.

#### Scenario: Same hash means reference-only
- **WHEN** a photo's current content hash equals the last-pushed hash
- **THEN** the photo SHALL be sent as a reference without data
- **AND** the server SHALL recognize it by id and keep its file

#### Scenario: Different hash means full upload
- **WHEN** a photo's current content hash differs from the last-pushed hash
- **THEN** the photo SHALL be uploaded to the photo endpoint with its binary content