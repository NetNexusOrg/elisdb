## ADDED Requirements

### Requirement: Sync is manual only

The system SHALL NOT synchronize automatically on a schedule; data transfers (push/pull) SHALL happen only when the user triggers a sync manually.

#### Scenario: No automatic sync runs
- **WHEN** the app is running and time passes without the user triggering a sync
- **THEN** the app SHALL NOT transfer data to or from the server automatically

#### Scenario: Manual trigger transfers
- **WHEN** the user triggers a sync manually
- **THEN** the app SHALL perform the full push/pull synchronisation

#### Scenario: Opening settings does not transfer
- **WHEN** the user opens the settings screen
- **THEN** the app SHALL check pending uploads and downloads without transferring client/photo data
