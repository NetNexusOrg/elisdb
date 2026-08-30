## MODIFIED Requirements

### Requirement: Sync status and manual trigger on the settings screen

The system SHALL show the current sync status on the settings screen, including an icon and a message for each state, and SHALL provide a manual sync trigger there. The sync status row SHALL always show a message — including an informative idle state when there is no activity and no pending changes — so the synchronization state is never shown as an empty row.

#### Scenario: Sync status is shown in settings
- **WHEN** the user opens the settings screen
- **THEN** the screen SHALL show the current sync status
- **AND** when a sync is in progress the screen SHALL indicate that a sync is in progress
- **AND** when a sync has just succeeded the screen SHALL indicate the last successful sync
- **AND** when a sync has just failed the screen SHALL indicate a sync error

#### Scenario: Idle state shows an informative message
- **WHEN** the settings screen is shown and there is no sync activity and no pending changes
- **THEN** the sync status row SHALL show an informative idle message (e.g. "Sem sincronizações pendentes") alongside the sync action, instead of an empty row

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
