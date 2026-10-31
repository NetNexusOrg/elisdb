## Purpose

Adds server-side deletion markers (tombstones) so a delete participates in last-write-wins like any edit: a concurrent edit from another device cannot silently resurrect a deleted client, and deletions propagate to other devices on incremental pulls.

## Requirements
### Requirement: Server keeps deletion tombstones

The server SHALL record a tombstone (identifier, deletion timestamp and delivery timestamp) when a client is deleted, instead of forgetting the identifier, so later pushes can be judged against the deletion and other devices can receive the deletion on an incremental pull.

The deletion timestamp SHALL be the one supplied by the deleting device (or the server's time when absent) and SHALL remain the value used to decide whether a later push is a stale edit or a deliberate re-creation. The delivery timestamp SHALL be assigned by the server, strictly greater than every delivery timestamp already issued, under the same write lock used by pushes.

#### Scenario: Delete writes a tombstone
- **WHEN** a client is deleted
- **THEN** the server SHALL store a tombstone with the client's identifier, a deletion timestamp and a delivery timestamp
- **AND** the client's record and photos SHALL be removed as before

#### Scenario: Delete carries the device timestamp
- **WHEN** a client is deleted
- **THEN** the deletion timestamp SHALL be the deleting device's supplied timestamp when present, otherwise the server's current time

#### Scenario: Delivery timestamp outranks the device clock
- **WHEN** a client is deleted with a deletion timestamp older than the newest data already delivered
- **THEN** the tombstone's delivery timestamp SHALL still be newer than that data
- **AND** other devices SHALL receive the deletion on their next incremental pull

### Requirement: Push honours tombstones

The server SHALL reject a push for a tombstoned identifier as `stale` unless the pushed record is newer than the tombstone, in which case the record SHALL be resurrected.

#### Scenario: Concurrent edit after delete is rejected
- **WHEN** a record is pushed whose identifier has a tombstone and whose `updated_at` is not newer than the tombstone's `deleted_at`
- **THEN** the server SHALL respond with `stale`
- **AND** the tombstone SHALL remain intact

#### Scenario: Deliberate re-creation resurrects the client
- **WHEN** a record is pushed whose identifier has a tombstone and whose `updated_at` is newer than the tombstone's `deleted_at`
- **THEN** the server SHALL remove the tombstone and persist the record as a new one
- **AND** the response SHALL be `accepted`

### Requirement: Pull propagates tombstones

The pull endpoint SHALL return the tombstones so other devices can drop deleted records locally, including on incremental pulls, and SHALL select them by the server-assigned delivery timestamp rather than by the device's deletion timestamp.

#### Scenario: Pull returns tombstones
- **WHEN** a client pulls records (full or incremental)
- **THEN** the response SHALL include the tombstoned identifiers with their deletion and delivery timestamps
- **AND** the response SHALL still include the records and photos as before

#### Scenario: Tombstone is not skipped by a newer watermark
- **WHEN** a device pulls incrementally with a watermark newer than the deletion timestamp but older than the tombstone's delivery timestamp
- **THEN** the tombstone SHALL be included in the response
