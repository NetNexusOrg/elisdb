# Spec Delta

## MODIFIED Requirements

### Requirement: Sync is manual only

The system SHALL NOT synchronize automatically on a schedule; data transfers (push/pull) SHALL happen only in response to a user action, either an explicit manual sync or saving a client.

#### Scenario: No automatic sync runs
- **WHEN** the app is running and time passes without the user triggering a sync
- **THEN** the app SHALL NOT transfer data to or from the server automatically

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
