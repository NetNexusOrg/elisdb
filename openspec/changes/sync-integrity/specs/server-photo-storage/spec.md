## ADDED Requirements

### Requirement: Schema migrations preserve photo rows

A schema migration SHALL NOT delete or orphan existing `fotos` rows. Rebuilding a table that other tables reference by foreign key SHALL disable foreign key enforcement outside any transaction, verify that enforcement is really off before rebuilding, and SHALL NOT complete if the number of photo rows changed.

#### Scenario: Upgrade from a schema without optional coordinates keeps photos
- **WHEN** a database whose `clientes` table still requires coordinates is opened
- **THEN** the table SHALL be rebuilt
- **AND** every client and every photo row SHALL still exist afterwards

#### Scenario: A rebuild that would lose photos aborts
- **WHEN** a rebuild would remove photo rows
- **THEN** the migration SHALL fail instead of committing the loss

## MODIFIED Requirements

### Requirement: Orphaned photo files are removed

The server SHALL NOT leave photo files on disk that have no corresponding `fotos` row. When a photo row is deleted (cascade or edit), its file SHALL be removed; and the server SHALL provide a cleanup pass that deletes any file in the photos directory lacking a matching `fotos` row, so storage does not grow from orphaned uploads.

The cleanup pass SHALL compare stored paths with absolute paths, so a relative photos directory (development, or a mount configured without a leading slash) never makes every stored photo look orphaned. Entries in the photos directory that are not regular files SHALL be left alone instead of aborting the pass.

#### Scenario: Edited-out photo file is deleted
- **WHEN** a push removes a photo from a client
- **THEN** the photo row and its file SHALL be removed

#### Scenario: Orphan files are garbage collected
- **WHEN** the cleanup pass runs and a file has no matching row
- **THEN** that file SHALL be deleted
- **AND** files with a matching row SHALL be kept

#### Scenario: Relative photos directory does not trigger deletion
- **WHEN** the photos directory is configured as a relative path
- **THEN** the cleanup pass SHALL still recognise the stored photos
- **AND** SHALL NOT delete them

#### Scenario: Non-file entries are ignored
- **WHEN** the photos directory contains a subdirectory
- **THEN** the cleanup pass SHALL skip it without failing

### Requirement: Unreferenced photo files are garbage collected

The server SHALL NOT retain photo files written by an authenticated `PUT /api/photos/{id}` that are never referenced by any client push. A cleanup pass SHALL remove files in the photos directory that have no corresponding `fotos` row, so unlinked uploads from rejected or abandoned pushes do not accumulate on disk.

#### Scenario: Abandoned PUT file is cleaned
- **WHEN** a photo was uploaded but no push ever referenced it
- **THEN** the cleanup pass SHALL delete it

#### Scenario: Referenced photo file is kept
- **WHEN** a photo file is referenced by a client record
- **THEN** the cleanup pass SHALL keep it
