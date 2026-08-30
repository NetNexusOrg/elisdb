## Purpose

Coordinates offline-first syncing between the client and the server: queues local changes, transmits pending records when online, pulls the authoritative server state, and applies clears — all without the app deciding conflicts, which the server owns.

## ADDED Requirements

### Requirement: Sending local changes

When connectivity is available, the system SHALL push every pending (created or edited) client with its photos to the server, and only clear the pending marker after the server acknowledges.

#### Scenario: Pending records are pushed
- **WHEN** the app has pending client records and connectivity is available
- **THEN** those records SHALL be sent to the server
- **AND** each SHALL remain pending until the server acknowledges its individual status

#### Scenario: Push failure leaves data intact
- **WHEN** a push cannot complete (no connectivity or transient error)
- **THEN** the pending records SHALL be kept
- **AND** the app SHALL retry later without data loss

### Requirement: Sending deletions

The system SHALL send pending deletions to the server and, once the server confirms removal, permanently remove the local record.

#### Scenario: Delete transmitted and finalized
- **WHEN** a record marked pending-deletion is sent to the server and the server confirms removal
- **THEN** the local record SHALL be permanently deleted
- **AND** its pending-deletion marker SHALL not be exposed afterwards

### Requirement: Pulling the authoritative state

The system SHALL pull the full server dataset and apply it as the authoritative local state.

#### Scenario: Pulled state accepted
- **WHEN** a pull returns the server dataset
- **THEN** records with no local pending edits SHALL be overwritten with the server values

### Requirement: Sync status exposure

The system SHALL expose a synchronization status (e.g. idle, in-progress, error) and support triggering a manual sync.

#### Scenario: Sync status is observable and manual sync is supported
- **WHEN** a sync runs, fails, or completes
- **THEN** an observable sync status SHALL be updated accordingly
- **AND** the user SHALL be able to trigger a manual sync