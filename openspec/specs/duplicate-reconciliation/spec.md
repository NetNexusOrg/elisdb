## Purpose

Defines how the app responds when the server acknowledges a pushed client with a `duplicate` status: it must adopt the server's existing record while preserving the user's locally entered data, never silently deleting the local record.

## Requirements
### Requirement: Duplicate push acknowledges the existing record

The system SHALL handle a `duplicate` push acknowledgement by reconciling the local record against the server's `existing_id` returned in the acknowledgement, instead of treating it as a plain acknowledgement.

#### Scenario: Duplicate acknowledged on push
- **WHEN** a pushed client record receives a `duplicate` status
- **THEN** the system SHALL use the `existing_id` from the acknowledgement as the canonical record
- **AND** SHALL reconcile the local record's data onto that canonical record

#### Scenario: Duplicate never silently deleted
- **WHEN** a record was acknowledged as `duplicate`
- **THEN** the subsequent `pull` SHALL NOT permanently delete the local record
- **AND** the user's locally entered fields and photos SHALL remain available on the retained record

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

### Requirement: Duplicate merge is user-visible

The system SHALL surface an observable and user-facing indication when a locally created record was merged into an existing record as a duplicate.

#### Scenario: Merge completion is reported
- **WHEN** a duplicate merge completes during a sync
- **THEN** the sync outcome SHALL carry a duplicate-merge result
- **AND** the UI SHALL display a message naming the merged record so the user understands what happened

### Requirement: Superseded photo files deleted only after commit

When a duplicate merge supersedes a local record's photos, the merge SHALL collect the paths of photos that are no longer needed and SHALL remove those files from disk only AFTER the enclosing persistence transaction has committed successfully, so a later failure in the same sync cannot leave `Foto` rows pointing at deleted files.

#### Scenario: Merge collects paths but defers deletion
- **WHEN** a duplicate merge determines that some local photo files are superseded
- **THEN** the merge SHALL record those paths and SHALL NOT delete the files while the persistence transaction is still open

#### Scenario: Deletion happens after commit
- **WHEN** the push/edit transaction commits successfully
- **THEN** the superseded photo files SHALL be removed from disk

#### Scenario: Failed transaction leaves files intact
- **WHEN** the persistence transaction rolls back (e.g. a later record in the same batch fails)
- **THEN** the superseded photo files SHALL remain on disk
- **AND** no `Foto` row SHALL reference a missing file as a result of the rollback