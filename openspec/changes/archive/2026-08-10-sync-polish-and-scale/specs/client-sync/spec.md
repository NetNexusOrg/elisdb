## Purpose

Preserves the real creation date of a client record across pull, so `dataCriacao` no longer degrades to the last `updated_at`. The app stores a server-issued creation timestamp and, when the server does not provide one, keeps the local creation date instead of overwriting it with the modification timestamp.

## MODIFIED Requirements

### Requirement: Pull the authoritative dataset

The system SHALL fetch all server client and photo records via the pull endpoint, so the caller can reconcile local data with the server, and SHALL apply the returned state as authoritative for records with no pending local edits.

#### Scenario: Pulled creation date preserved
- **WHEN** a pulled server record includes a creation timestamp
- **THEN** the local creation timestamp SHALL be set from the server value, keeping the real creation date even after later edits

#### Scenario: Pull without a creation timestamp keeps local date
- **WHEN** a pulled server record does not include a creation timestamp
- **THEN** the local creation date SHALL be preserved when the record already existed locally, rather than being replaced by the modification timestamp
- **AND** a freshly fetched record without a creation timestamp SHALL fall back to its modification timestamp