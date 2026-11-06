# Spec Delta

## Purpose

Defines a consistent, verifiable backup and restore procedure for the server's SQLite database and photo files so an operator can recover from data loss, a failed upgrade, or a lost volume.

## ADDED Requirements

### Requirement: Backup is transactionally consistent

The server SHALL provide a backup procedure that produces a transactionally consistent copy of the SQLite database and a copy of every photo referenced by the database, without requiring the server to stop accepting requests. The database copy SHALL be created with SQLite's online backup mechanism or an equivalent consistent snapshot, not by copying a live database file while WAL writes are in progress.

#### Scenario: Backup while writes are running
- **WHEN** a backup runs while clients are pushing or pulling
- **THEN** the database copy SHALL represent a single consistent committed state
- **AND** the backup SHALL NOT contain a torn WAL or partial transaction

#### Scenario: Every referenced photo is copied
- **WHEN** a backup captures a client with photos
- **THEN** the backup SHALL include a file for every photo row referenced by that database snapshot
- **AND** SHALL fail if a referenced file cannot be copied

#### Scenario: Orphan files are not required
- **WHEN** the photos directory contains a file with no matching photo row
- **THEN** the backup MAY omit it
- **AND** the backup SHALL still be considered complete

### Requirement: Backup is verifiable

A backup SHALL include a manifest with the database checksum, the checksum and relative path of every photo file, the schema version, and a creation timestamp. Verification SHALL fail if a manifest entry is missing, a checksum differs, or the database fails an integrity check.

#### Scenario: Manifest covers the backup
- **WHEN** a backup completes
- **THEN** its manifest SHALL list the database and every included photo with a checksum

#### Scenario: Corrupt file is detected
- **WHEN** a file's checksum differs from the manifest during verification
- **THEN** verification SHALL fail and name the affected file

#### Scenario: Database integrity is checked
- **WHEN** a backup is verified
- **THEN** the database SHALL pass SQLite's integrity check before the backup is accepted

### Requirement: Restore is verified and atomic

Restore SHALL verify the backup manifest and database integrity before replacing live data, restore the database and photos together, and refuse an incomplete or corrupt backup without modifying the current data. A failed restore SHALL leave the previous database and photo set usable.

#### Scenario: Corrupt backup is refused
- **WHEN** an operator restores a backup whose checksum or integrity check fails
- **THEN** the restore SHALL abort before changing live data
- **AND** the current data SHALL remain intact

#### Scenario: Missing photo is refused
- **WHEN** a backup manifest references a photo that is absent from the backup
- **THEN** the restore SHALL refuse the backup
- **AND** SHALL NOT start a server with an incomplete photo set

#### Scenario: Successful restore
- **WHEN** a valid backup is restored
- **THEN** the database and photos SHALL be replaced together
- **AND** the server SHALL start and serve the restored records

#### Scenario: Restore rolls back on failure
- **WHEN** replacing the live database or photos fails midway
- **THEN** the restore SHALL leave the previous database and photos usable
- **AND** SHALL report the failure

### Requirement: Backup and restore are documented and exercised

The repository SHALL document the backup and restore commands, their required environment, and how to verify a backup. An automated check SHALL exercise a backup, verification, and restore round trip against a disposable data directory.

#### Scenario: Documented commands run as written
- **WHEN** an operator follows the documented backup and restore commands
- **THEN** the commands SHALL produce a verified backup and restore it without undocumented manual steps

#### Scenario: Automated round trip
- **WHEN** the backup round-trip check runs in CI or the test suite
- **THEN** it SHALL create a backup, verify it, restore it into a clean directory, and confirm the restored records and photo files match the source

#### Scenario: Backup does not expose secrets in output
- **WHEN** the backup or restore command logs progress
- **THEN** it SHALL NOT print private seeds, signing keys, or photo bytes
