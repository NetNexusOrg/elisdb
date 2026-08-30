## MODIFIED Requirements

### Requirement: Sync status and manual trigger on the settings screen

The system SHALL show the current sync status on the settings screen, including an icon and a message for each state, and SHALL provide a manual sync trigger there.

#### Scenario: Sync status is shown in settings
- **WHEN** the user opens the settings screen
- **THEN** the screen SHALL show the current sync status
- **AND** when a sync is in progress the screen SHALL indicate that a sync is in progress
- **AND** when a sync has just succeeded the screen SHALL indicate the last successful sync
- **AND** when a sync has just failed the screen SHALL indicate a sync error

#### Scenario: Sync fault messages are actionable
- **WHEN** a sync has failed
- **THEN** the settings screen SHALL show a message explaining the failure (missing server configuration, unregistered key, or network problem) in the same place as the sync status

#### Scenario: Manual sync from settings
- **WHEN** a sync is not in progress and the user taps the sync action on the settings screen
- **THEN** the system SHALL start a synchronisation
- **AND** the settings screen SHALL move to a "sync in progress" state until it completes

#### Scenario: Repeated trigger is ignored while syncing
- **WHEN** the user taps the sync action while a sync is already in progress
- **THEN** the system SHALL NOT start a second synchronisation

## ADDED Requirements

### Requirement: Sync action stays in a fixed position

The system SHALL keep the sync action (button/icon) in the settings sync status row at the same position regardless of the current sync state, so the user can always find and tap it without it moving.

#### Scenario: Sync action position is stable across sync states
- **WHEN** the settings screen shows the sync status row in any state (idle, in progress, succeeded, failed)
- **THEN** the sync action SHALL occupy the same position in the row in every state
- **AND** the status message and icon SHALL NOT displace the sync action

#### Scenario: Sync action position is stable when a message is present
- **WHEN** the sync status row shows a status message or icon alongside the sync action
- **THEN** the sync action SHALL remain in its fixed position
- **AND** the message SHALL be laid out so it does not shift the sync action

#### Scenario: In-progress indicator stays in place
- **WHEN** a sync is in progress and the sync action shows an in-progress indicator
- **THEN** the in-progress indicator SHALL appear in the same position as the sync action