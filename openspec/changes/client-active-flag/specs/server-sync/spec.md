# Spec Delta

## MODIFIED Requirements

### Requirement: Push changes

The server SHALL accept a push of client and photo changes from a client and persist them. Pushing a client SHALL also push its photos together with the client. A push SHALL be processed record by record: an invalid record SHALL NOT abort the batch, and the response SHALL carry a per-record result indicating whether each record was accepted, found duplicate, found stale, or rejected as invalid. Each pushed client SHALL carry its active/inactive status, defaulting to active when the payload omits it.

#### Scenario: Client pushes new records
- **WHEN** a client pushes a batch of new or updated client records with their photos
- **THEN** each record and photo SHALL be persisted

#### Scenario: Push is idempotent per record
- **WHEN** the same client record is pushed more than once
- **THEN** it SHALL be stored once, not duplicated

#### Scenario: An invalid record does not abort the batch
- **WHEN** a client pushes a batch in which one record is invalid (e.g. a blank name)
- **THEN** the server SHALL process the remaining records normally
- **AND** SHALL return a per-record result marking the invalid record as rejected

#### Scenario: Status is persisted with the record
- **WHEN** a client pushes a client record marked inactive
- **THEN** the server SHALL store it as inactive
- **AND** a payload without a status SHALL be stored as active

### Requirement: Pull changes

The server SHALL allow a client to pull all stored client records with their photos, including each client's active/inactive status.

#### Scenario: Client pulls all data
- **WHEN** a client requests a pull
- **THEN** the server SHALL return all stored clients with their photos

#### Scenario: Pull returns the client status
- **WHEN** a client pulls records and a stored client is inactive
- **THEN** the pulled record SHALL indicate that the client is inactive

### Requirement: Record identity

Each client record SHALL carry a stable client identifier and a modification timestamp used for conflict resolution. Photos SHALL belong to exactly one client, and a photo identifier owned by one client SHALL NOT be silently reassigned to another client. Coordinates SHALL be optional. The active/inactive status SHALL be optional in the payload and default to active.

#### Scenario: Record has identity and timestamp
- **WHEN** a client record is received
- **THEN** it SHALL have an identifier, a modification timestamp, name, and optional contract and notes
- **AND** its photos SHALL be associated only with that client

#### Scenario: Coordinates are optional
- **WHEN** a client record is received without latitude or longitude
- **THEN** the record SHALL be accepted and stored with no coordinates

#### Scenario: Pull returns null coordinates
- **WHEN** a client pulls records and a stored client has no coordinates
- **THEN** the pulled record SHALL carry null latitude and longitude

#### Scenario: Photo id owned by another client is rejected
- **WHEN** a push assigns a photo whose id is already stored and belongs to a different client
- **THEN** the server SHALL reject the record rather than reassign the photo to the new client
- **AND** the existing photo SHALL remain attached to its original client

## ADDED Requirements

### Requirement: Client status changes appear in the edit history

The server SHALL record a status change in the client's edit history when a push changes a client between active and inactive, identifying the change as an activation or a deactivation.

#### Scenario: Deactivation appears in the history
- **WHEN** a push updates a client from active to inactive
- **THEN** the stored history entry SHALL identify the status change as a deactivation

#### Scenario: Activation appears in the history
- **WHEN** a push updates a client from inactive to active
- **THEN** the stored history entry SHALL identify the status change as an activation
