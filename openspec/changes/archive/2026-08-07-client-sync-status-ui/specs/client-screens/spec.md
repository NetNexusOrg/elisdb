## Purpose

Adds sync status feedback and a manual sync trigger to the client list screen, so the user can observe whether their data is in sync and start a synchronisation manually.

## MODIFIED Requirements

### Requirement: Client list screen

The following scenarios are added to the existing client list requirement. The screen SHALL reflect sync state and expose a manual sync trigger.

#### Scenario: Show current sync status
- **WHEN** the client list screen is shown and a sync is in progress
- **THEN** the screen SHALL indicate that a sync is in progress
- **AND** when a sync has just failed, the screen SHALL indicate a sync error
- **AND** when a sync has just succeeded, the screen SHALL indicate the last successful sync

#### Scenario: No sync activity shown when idle
- **WHEN** the client list screen is shown and no sync has run or been triggered
- **THEN** the screen SHALL indicate that the data has not been synchronised, without showing an error

### Requirement: Manual sync trigger

The system SHALL provide an entry point in the client list screen to trigger a synchronisation and SHALL disable repeated triggering while a sync is in progress.

#### Scenario: Trigger a manual sync
- **WHEN** a sync is not in progress and the user taps the sync action
- **THEN** the system SHALL start a synchronisation
- **AND** the screen SHALL move to a "sync in progress" state until it completes

#### Scenario: Repeated trigger is ignored while syncing
- **WHEN** the user taps the sync action while a sync is already in progress
- **THEN** the system SHALL NOT start a second synchronisation

#### Scenario: Empty server configuration feedback
- **WHEN** the user triggers a sync and no server / signing key is configured
- **THEN** the screen SHALL show an actionable message explaining that the server or key is missing, distinct from a network sync error