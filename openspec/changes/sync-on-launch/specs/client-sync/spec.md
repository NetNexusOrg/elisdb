# Spec Delta

## RENAMED Requirements

- FROM: `### Requirement: Sync is manual only`
- TO: `### Requirement: Sync happens on launch, save and manual trigger`

## MODIFIED Requirements

### Requirement: Sync happens on launch, save and manual trigger

The system SHALL NOT synchronize on a timer: transfers SHALL happen only when the app starts, when the user saves a client, or when the user triggers a sync manually.

#### Scenario: No automatic sync runs
- **WHEN** the app stays open or in the background without a launch, a save or a manual trigger
- **THEN** the app SHALL NOT transfer data to or from the server on a timer

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

#### Scenario: Saving while a sync is running joins it
- **WHEN** the user saves a client while a synchronisation is already in progress
- **THEN** the app SHALL NOT start a second concurrent synchronisation
- **AND** the pending changes SHALL be sent by the run already in progress or by the next sync

#### Scenario: Opening settings does not transfer
- **WHEN** the user opens the settings screen
- **THEN** the app SHALL check pending uploads and downloads without transferring client/photo data
