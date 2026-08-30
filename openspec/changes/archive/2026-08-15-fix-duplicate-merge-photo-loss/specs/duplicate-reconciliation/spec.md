## MODIFIED Requirements

### Requirement: Merge preserves local data

The system SHALL merge the user's locally entered client fields (e.g. contract, notes, coordinates) and photos from the duplicate local record onto the retained canonical record when a duplicate push is acknowledged, preserving the canonical record's existing photos as well so no photo is lost.

#### Scenario: Fields merged onto existing record
- **WHEN** a local record acknowledged as `duplicate` has locally entered fields that differ from the server's existing record
- **THEN** the local fields SHALL be applied to the canonical record
- **AND** the canonical record SHALL be flagged as pending and re-pushed so the server stores the merged values

#### Scenario: Photos transferred
- **WHEN** a local record acknowledged as `duplicate` has photos
- **THEN** those photos SHALL be associated with the canonical record
- **AND** SHALL be pushed to the server with the canonical record

## ADDED Requirements

### Requirement: Duplicate merge preserves canonical photos

The system SHALL preserve the canonical record's photos when merging a duplicate: the retained record SHALL carry the union of the canonical record's photos and the duplicate local record's photos, and the re-push SHALL transmit that complete set so the server does not drop any photo.

#### Scenario: Canonical photos merged with local photos
- **WHEN** a local record with photos is acknowledged as `duplicate` and the canonical record has photos of its own
- **THEN** the retained record SHALL include the canonical record's photos alongside the local record's photos
- **AND** SHALL be re-pushed with the complete union of both

#### Scenario: Canonical photos available when not stored locally
- **WHEN** a record is acknowledged as `duplicate` and the canonical record does not exist locally
- **THEN** the canonical record's photos SHALL be obtainable from the duplicate acknowledgement and materialized before the merge

#### Scenario: Merge never amputates photos
- **WHEN** a duplicate merge completes and its merged record is re-pushed
- **THEN** no photo that existed on the canonical record or on the local record SHALL be removed from the server as a consequence of the merge