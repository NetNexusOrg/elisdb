## MODIFIED Requirements

### Requirement: Cascading photo deletion

Deleting a client SHALL remove its stored photos' files from disk, matching the database-level deletion of the photo records. Removing an individual photo from a client (via an edit/push that no longer includes that photo) SHALL also remove that photo's file from disk, not only its database row.

#### Scenario: Deleting a client removes its photo files
- **WHEN** a client is deleted from the server
- **THEN** the client's photo files SHALL be removed from the managed directory

#### Scenario: Removing a photo from a client removes its file
- **WHEN** a client is edited/pushed and a previously stored photo is no longer included
- **THEN** the server SHALL delete that photo's database row AND its file from disk

### Requirement: Orphaned photo files are removed

The server SHALL NOT leave photo files on disk that have no corresponding `fotos` row. When a photo row is deleted (cascade or edit), its file SHALL be removed; and the server SHALL provide a cleanup pass that deletes any file in the photos directory lacking a matching `fotos` row, so storage does not grow from orphaned uploads.

#### Scenario: Edited-out photo file is deleted
- **WHEN** a push removes a photo from a client
- **THEN** both the row and the file SHALL be gone

#### Scenario: Orphan files are garbage collected
- **WHEN** the cleanup pass runs
- **THEN** every file in the photos directory without a matching `fotos` row SHALL be removed
- **AND** files that still have a row SHALL be kept
