## Purpose

Extends the sync-status capability to require observability of the sync status and a triggerable manual sync from within the running application's UI, so the sync engine's state is reflected to the user.

## MODIFIED Requirements

### Requirement: Sync status exposure

The following scenarios are added to the existing sync status requirement. The sync engine SHALL report results, including distinct configuration faults, to the UI.

#### Scenario: Manually triggered sync is reported
- **WHEN** the user triggers a manual sync from the UI
- **THEN** the sync engine SHALL run a full push/pull synchronisation
- **AND** the observable sync status SHALL transition to in-progress and then to success or error with a result

#### Scenario: Sync result distinguishes configuration faults
- **WHEN** a sync fails because no server base URL is configured or no signing key is available
- **THEN** the sync result SHALL indicate the configuration fault distinctly from a transient network error, so the UI can show an actionable message
- **AND** the observable sync status SHALL be reported as an error