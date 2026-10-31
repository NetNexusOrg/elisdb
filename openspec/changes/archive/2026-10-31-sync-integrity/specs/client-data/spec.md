## ADDED Requirements

### Requirement: Schema migrations run on every supported Android version

Every database migration SHALL be expressible in the oldest SQLite version shipped on a supported Android release, so an app update never fails to open its database on an older device. Changing a table's columns SHALL rebuild the table (create, copy, drop, rename) instead of using statements that only newer SQLite versions understand.

#### Scenario: Update on an older device opens the database
- **WHEN** the app is updated on a device below Android 14 and the stored schema needs the phone column removed
- **THEN** the migration SHALL complete
- **AND** the client records and photos SHALL be preserved

#### Scenario: Migration keeps the current schema
- **WHEN** the migration finishes
- **THEN** the resulting table SHALL match the schema declared by the current database version
