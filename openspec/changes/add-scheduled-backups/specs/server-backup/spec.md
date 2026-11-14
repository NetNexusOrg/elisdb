# server-backup — Delta

## ADDED Requirements

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
