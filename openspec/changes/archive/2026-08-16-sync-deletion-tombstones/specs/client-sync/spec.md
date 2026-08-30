## ADDED Requirements

### Requirement: Delete competes in last-write-wins

The app SHALL send the deleting device's timestamp with a delete, and SHALL reconcile a `stale` response caused by a tombstone (a newer deletion) the same way it reconciles a superseded edit.

#### Scenario: Delete sends the device timestamp
- **WHEN** the app deletes a client
- **THEN** the delete request SHALL carry the device's current timestamp

#### Scenario: Tombstoned push is reconciled as stale
- **WHEN** the server responds `stale` to a pushed record (including because a tombstone exists)
- **THEN** the app SHALL clear the local pending marker
- **AND** report the record's name in the sync outcome

### Requirement: Pull applies tombstones locally

The app SHALL drop local records whose identifier appears in the pull response's tombstones, unless the record has pending local edits.

#### Scenario: Tombstoned local record removed
- **WHEN** a pull returns a tombstone for a locally stored record with no pending edits
- **THEN** the app SHALL delete the local record and its photos

#### Scenario: Tombstoned record with pending edits kept
- **WHEN** a pull returns a tombstone for a locally stored record that has pending edits
- **THEN** the app SHALL keep the record locally pending for the next push
