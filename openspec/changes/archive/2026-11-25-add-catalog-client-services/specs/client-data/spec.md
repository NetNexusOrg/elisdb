# Spec Delta

## ADDED Requirements

### Requirement: Catalog and client-service records persisted locally

The system SHALL persist catalog items and client service associations in the local database. Catalog items SHALL carry their category, active state, modification timestamp and synchronization state. Client service associations SHALL reference a client and a catalog item and SHALL carry a quantity only for device categories. Deleting a client SHALL delete its associations.

#### Scenario: Catalog items survive restart

- **WHEN** the app is restarted after catalog items were stored
- **THEN** every catalog item SHALL still be present
- **AND** its category, active state and pending state SHALL be preserved

#### Scenario: Associations survive restart

- **WHEN** the app is restarted after client associations were stored
- **THEN** every association SHALL still be present with its quantity

#### Scenario: Deleting a client removes its associations

- **WHEN** a client is deleted
- **THEN** all of its service associations SHALL be deleted
- **AND** the catalog items SHALL remain

#### Scenario: Association before item is known

- **WHEN** an association is stored for an item that has not yet been pulled
- **THEN** the association SHALL be stored without requiring the catalog item to exist locally

#### Scenario: Existing data survives the migration

- **WHEN** an existing local database is migrated to include catalog and association storage
- **THEN** every existing client and photo SHALL be preserved
- **AND** the resulting schema SHALL match the current database version

#### Scenario: Destructive reset clears the feature data

- **WHEN** the user performs a destructive local reset
- **THEN** catalog items and service associations SHALL be cleared with the clients
- **AND** a subsequent full pull SHALL restore the server's catalog and associations
