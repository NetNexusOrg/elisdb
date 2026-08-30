## MODIFIED Requirements

### Requirement: Content hash decides push payload

The system SHALL use the photo's content-derived identity (byte hash) to decide whether a photo must be re-uploaded, comparing the local content hash against the hash of the last-pushed version.

#### Scenario: Same hash means reference-only
- **WHEN** a photo's current content hash equals the last-pushed hash
- **THEN** the photo SHALL be sent as a reference without content
- **AND** the server SHALL recognize it by id and keep its file

#### Scenario: Different hash means full upload
- **WHEN** a photo's current content hash differs from the last-pushed hash
- **THEN** the photo SHALL be uploaded to the photo endpoint with its binary content