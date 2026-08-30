## MODIFIED Requirements

### Requirement: Sync status and manual trigger on the settings screen

The system SHALL show the current sync status on the settings screen, including an icon and a message for each state, and SHALL provide a manual sync trigger there. When the Settings screen is opened, the system SHALL check for pending uploads (local changes) and pending downloads (server data newer than the pull watermark) and present the result as a "synchronization available" message. The sync status row SHALL always show a message.

#### Scenario: Sync status is shown in settings
- **WHEN** the user opens the settings screen
- **THEN** the screen SHALL show the current sync status
- **AND** when a sync is in progress the screen SHALL indicate that a sync is in progress
- **AND** when a sync has just succeeded the screen SHALL indicate the last successful sync
- **AND** when a sync has just failed the screen SHALL indicate a sync error

#### Scenario: Nothing to sync is shown
- **WHEN** the settings screen is opened and there are no pending uploads and no pending downloads
- **THEN** the sync status row SHALL indicate that there are no pending synchronizations

#### Scenario: Send-only is shown
- **WHEN** the settings screen is opened and there are pending uploads but no pending downloads
- **THEN** the sync status row SHALL indicate that a synchronization is available to send

#### Scenario: Receive-only is shown
- **WHEN** the settings screen is opened and there are pending downloads but no pending uploads
- **THEN** the sync status row SHALL indicate that a synchronization is available to receive

#### Scenario: Send and receive is shown
- **WHEN** the settings screen is opened and there are both pending uploads and pending downloads
- **THEN** the sync status row SHALL indicate that a synchronization is available to send and receive

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
