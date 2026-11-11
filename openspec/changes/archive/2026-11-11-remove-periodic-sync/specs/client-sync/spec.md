# Spec Delta — client-sync

## REMOVED Requirements

### Requirement: Sync happens on launch, save and manual trigger

**Reason**: the requirement allowed an opt-in 15-minute periodic background
schedule, which is being removed. Transfers now happen only on launch, after a
save or delete, and on the manual trigger.

**Migration**: no user action. A previously enabled `elisdb-periodic-sync` job is
cancelled automatically on the next app start. The transient-failure retry
remains covered by `client-resilience`.

## ADDED Requirements

### Requirement: Sync happens on launch, save, delete and manual trigger

The system SHALL synchronize on app start, when the user saves a client, when the
user deletes a client, and when the user triggers a sync manually. The app SHALL
NOT run a periodic background synchronisation.

#### Scenario: No automatic sync runs

- **WHEN** the app stays open or in the background without a launch, a save, a
  delete, or a manual trigger
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
- **AND** the app SHALL return to the previous screen without waiting for the
  synchronisation to finish

#### Scenario: Deleting a client triggers a sync

- **WHEN** the user confirms the deletion of a client
- **THEN** the app SHALL mark it for deletion and start a synchronisation in the
  background
- **AND** SHALL NOT require a manual sync to send the deletion

#### Scenario: Saving while a sync is running joins it

- **WHEN** the user saves a client while a synchronisation is already in progress
- **THEN** the app SHALL NOT start a second concurrent synchronisation
- **AND** the pending changes SHALL be sent by the run already in progress or by
  the next sync

#### Scenario: Opening settings does not transfer

- **WHEN** the user opens the settings screen
- **THEN** the app SHALL check pending uploads and downloads without transferring
  client/photo data
