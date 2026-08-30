## Purpose

Ensures the app performs at most one synchronization at a time, regardless of trigger source, and that both manual and automatic syncs update the single observable sync state.

## ADDED Requirements

### Requirement: One in-flight sync at a time

The system SHALL serialize sync executions so that no two syncs run concurrently, across both manual triggers and periodic background workers.

#### Scenario: Manual sync during periodic sync
- **WHEN** a manual sync is triggered while a periodic sync is already in flight
- **THEN** SHALL NOT start a second concurrent full sync
- **AND** the manual trigger SHALL either join the in-flight run or be queued until it finishes

#### Scenario: Duplicate triggers do not duplicate work
- **WHEN** two sync requests arrive within the same period
- **THEN** the data transfer SHALL be performed at most once
- **AND** no concurrent pushes or pulls SHALL be observed

### Requirement: Automatic sync status is observable

The system SHALL publish the outcome of every sync — including those started by the periodic worker — through the same observable sync status used for manual syncs.

#### Scenario: Periodic sync failure is surfaced
- **WHEN** a periodic sync fails (network, auth, or config)
- **THEN** the observable sync status SHALL reflect the error with the appropriate fault type
- **AND** the UI SHALL display the same message as a failed manual sync

#### Scenario: Periodic sync success updates status
- **WHEN** a periodic sync completes successfully
- **THEN** the observable sync status SHALL transition to success