# server-backup Specification

## Purpose
Defines a consistent, verifiable backup and restore procedure for the server's SQLite database and photo files so an operator can recover from data loss, a failed upgrade, or a lost volume.

## Requirements

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

### Requirement: Backups can run automatically on a schedule

The server SHALL create a backup on a configurable interval and SHALL keep the
feature disabled when `ELISDB_BACKUP_INTERVAL_SECONDS` is `0`. Automatic
backups SHALL be written as `auto-YYYYmmdd-HHMMSS` directories inside
`ELISDB_BACKUP_DIR` (default `<data dir>/backups`).

#### Scenario: Interval elapses

- **WHEN** the configured interval has passed since the newest automatic backup
- **THEN** the server creates a new backup in the background without blocking requests
- **AND** the server log records the backup path and photo count

#### Scenario: Automatic backups are disabled

- **WHEN** `ELISDB_BACKUP_INTERVAL_SECONDS` is `0`
- **THEN** no automatic backup is ever created
- **AND** manual and pre-deploy backups continue to work unchanged

#### Scenario: Startup does not lose a day of backups

- **WHEN** the server starts and the newest automatic backup is older than the interval
- **THEN** a backup is created shortly after startup
- **AND** the HTTP server answers requests while the backup runs

### Requirement: Only verified backups count

A scheduled backup SHALL be verified with the existing manifest and checksums
before it is treated as successful. A failed verification SHALL be logged and
SHALL NOT delete the directory, so an operator can inspect it.

#### Scenario: Backup verifies

- **WHEN** a scheduled backup finishes and its manifest and photo checksums match
- **THEN** the run is logged as successful and counts towards retention

#### Scenario: Backup fails verification

- **WHEN** the manifest is missing, incomplete or a checksum differs
- **THEN** the failure is logged with the backup path
- **AND** the directory is kept for inspection and excluded from retention

### Requirement: Retention removes only automatic backups

Retention SHALL delete only directories whose name starts with `auto-` and
SHALL keep the newest `ELISDB_BACKUP_RETENTION` of them. Manual, pre-deploy and
pre-migration backups SHALL never be removed by the server.

#### Scenario: Retention keeps the configured number

- **WHEN** more automatic backups exist than `ELISDB_BACKUP_RETENTION`
- **THEN** the oldest automatic backups are deleted
- **AND** the newest ones are kept

#### Scenario: Manual backups survive retention

- **WHEN** retention runs and the backup directory contains `pre-deploy-*` or
  other non-`auto-*` directories
- **THEN** those directories are left untouched

### Requirement: Scheduled backups respect free disk space

Before copying, the server SHALL estimate the backup size and SHALL skip the run
when free space is below twice that estimate plus a safety margin.

#### Scenario: Not enough free space

- **WHEN** the backup directory has less free space than twice the estimated size plus the margin
- **THEN** the run is skipped with a warning in the log
- **AND** no partial backup directory is created
