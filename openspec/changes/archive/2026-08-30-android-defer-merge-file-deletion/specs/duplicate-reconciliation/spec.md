## MODIFIED Requirements

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
