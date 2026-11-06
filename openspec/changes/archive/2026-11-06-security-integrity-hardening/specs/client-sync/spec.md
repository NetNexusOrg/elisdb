# Spec Delta

## ADDED Requirements

### Requirement: Tombstones do not delete a newer local edit

When applying a tombstone, the app SHALL compare the tombstone's deletion timestamp with the local record's `updatedAt`. If the local record is newer and is not pending deletion, the app SHALL preserve it and re-push it as a re-creation instead of deleting it. A record with a pending local edit SHALL never be deleted by a tombstone.

#### Scenario: Tombstone is older than the local edit
- **WHEN** a pull returns a tombstone whose `deleted_at` is older than the local record's `updatedAt`
- **THEN** the app SHALL keep the local record and its photos
- **AND** SHALL mark it for re-push so the server can resurrect it

#### Scenario: Pending local edit survives a tombstone
- **WHEN** a tombstone names a record that has pending local changes
- **THEN** the app SHALL NOT delete the record
- **AND** SHALL keep the local change pending

#### Scenario: Tombstone newer than the local record deletes it
- **WHEN** a tombstone is newer than the local record and the record has no pending local changes
- **THEN** the app SHALL delete the local record and its photos as before
