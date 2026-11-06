# Spec Delta

## ADDED Requirements

### Requirement: Transient failures retry with backoff

The app SHALL retry transient network and server failures with exponential backoff and jitter, capped at a configured maximum. Authentication, configuration and validation failures SHALL NOT be retried automatically. A manual sync SHALL reset the backoff and attempt immediately.

#### Scenario: Transient failure is retried later
- **WHEN** a sync fails with a transient network or server error
- **THEN** the app SHALL schedule a retry with exponential backoff and jitter
- **AND** SHALL surface the last error while waiting

#### Scenario: Auth or configuration failure is not retried
- **WHEN** a sync fails because of an invalid key or invalid configuration
- **THEN** the app SHALL NOT retry automatically
- **AND** SHALL show an actionable configuration error

#### Scenario: Manual sync overrides the wait
- **WHEN** the user triggers a manual sync while a backoff is pending
- **THEN** the app SHALL attempt immediately
- **AND** SHALL reset the backoff on success
