# Spec Delta

## MODIFIED Requirements

### Requirement: Sync happens on launch, save and manual trigger

The system SHALL synchronize on app start, when the user saves a client, when the user deletes a client, and when the user triggers a sync manually. The app MAY also synchronize on a periodic background schedule when that schedule is enabled; the periodic schedule SHALL be opt-in, SHALL use the single-flight behavior, and SHALL use exponential backoff after transient failures.

#### Scenario: No automatic sync runs
- **WHEN** the app stays open or in the background without a launch, a save, a delete, a manual trigger, or an enabled periodic schedule
- **THEN** the app SHALL NOT transfer data to or from the server

#### Scenario: Initial sync on app start
- **WHEN** the app process starts and a server configuration exists
- **THEN** the app SHALL start a synchronisation in the background
- **AND** the app SHALL be usable without waiting for it to finish

#### Scenario: Start without a server configuration
- **WHEN** the app starts and no signing key is configured
- **THEN** the app SHALL NOT attempt a synchronisation
- **AND** no synchronisation error SHALL be shown

#### Scenario: Manual trigger transfers
- **WHEN** the user triggers a sync manually
- **THEN** the app SHALL perform the full push/pull synchronisation

#### Scenario: Saving a client triggers a sync
- **WHEN** the user saves a new or edited client and the save succeeds
- **THEN** the app SHALL start a synchronisation in the background
- **AND** the app SHALL return to the previous screen without waiting for the synchronisation to finish

#### Scenario: Deleting a client triggers a sync
- **WHEN** the user confirms the deletion of a client
- **THEN** the app SHALL mark it for deletion and start a synchronisation in the background
- **AND** SHALL NOT require a manual sync to send the deletion

#### Scenario: Saving while a sync is running joins it
- **WHEN** the user saves a client while a synchronisation is already in progress
- **THEN** the app SHALL NOT start a second concurrent synchronisation
- **AND** the pending changes SHALL be sent by the run already in progress or by the next sync

#### Scenario: Opening settings does not transfer
- **WHEN** the user opens the settings screen
- **THEN** the app SHALL check pending uploads and downloads without transferring client/photo data

#### Scenario: Enabled periodic sync runs in the background
- **WHEN** the periodic schedule is enabled and its interval elapses
- **THEN** the app SHALL run a single synchronisation through the same single-flight path
- **AND** SHALL publish the same observable status as a manual sync

## ADDED Requirements

### Requirement: Shared HTTP client is thread-safe

The app SHALL create, reuse, and replace the shared HTTP client safely under concurrent callers. A configuration change SHALL NOT close a client that is still in use, and concurrent requests SHALL NOT create duplicate clients or leak connection pools.

#### Scenario: Concurrent callers share one client
- **WHEN** sync, identity and history requests start concurrently
- **THEN** they SHALL observe the same client instance for the current configuration
- **AND** no additional client SHALL be constructed

#### Scenario: Configuration change does not close an in-use client
- **WHEN** the configuration changes while a request is using the current client
- **THEN** the app SHALL let that request finish or fail cleanly before closing the client
- **AND** SHALL NOT hand a closed client to a new request
