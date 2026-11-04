## MODIFIED Requirements

### Requirement: Push records to the server

The system SHALL send local client and photo records to the server via the push endpoint, including the client's optional tag, returning a per-record status, and SHALL represent unsynced records as base64 photos. Each record SHALL remain pending until the server acknowledges its state, and the acknowledgement SHALL NOT write record content: a record edited while the request was in flight SHALL stay pending with the user's edit intact. Photos the server reports as missing SHALL be uploaded again instead of being remembered as delivered.

#### Scenario: Push returns per-record status
- **WHEN** the app sends a batch of client and photo records to the server
- **THEN** the server response SHALL be interpreted as a per-record status per each pushed client
- **AND** the statuses SHALL be (accepted/duplicate/stale) and returned onward to the caller

#### Scenario: Sending local changes clears pending after ack
- **WHEN** the app has pending (created or edited) client records with their photos and connectivity is available
- **THEN** those records SHALL be sent to the server
- **AND** each SHALL remain pending until the server acknowledges its individual status

#### Scenario: Push failure leaves data intact
- **WHEN** a push cannot complete (no connectivity or transient error)
- **THEN** the pending records SHALL be kept
- **AND** the app SHALL retry later without data loss

#### Scenario: Edit during the push is not overwritten
- **WHEN** the user edits a record while its push is in flight
- **THEN** the edit SHALL be kept
- **AND** the record SHALL remain pending

#### Scenario: Stale ack does not discard the local edit
- **WHEN** the server reports a pushed record as stale and the record was not edited during the push
- **THEN** the app SHALL clear its pending flag
- **AND** the server's version SHALL be applied by the following pull

#### Scenario: Missing photos are pushed again
- **WHEN** the push result names photos the server does not have
- **THEN** the app SHALL NOT record them as delivered
- **AND** SHALL upload them on the next push

#### Scenario: Tag is included in the pushed record
- **WHEN** the app pushes a client that has a tag
- **THEN** the pushed record SHALL carry that tag
- **AND** a client without a tag SHALL be pushed without one

### Requirement: Pull the authoritative dataset

The system SHALL fetch all server client and photo records via the pull endpoint, including each client's optional tag, so the caller can reconcile local data with the server, and SHALL apply the returned state as authoritative for records with no pending local edits.

#### Scenario: Pull returns all records with photos
- **WHEN** the app requests a pull
- **THEN** all server-stored clients with their photos SHALL be returned
- **AND** photos SHALL be decoded from base64 to app-private photo files
- **AND** photos whose content is unchanged SHALL be kept as-is without being re-materialized

#### Scenario: Pulled state accepted
- **WHEN** a pull returns the server dataset
- **THEN** records with no local pending edits SHALL be overwritten with the server values

#### Scenario: Pulled tag is applied locally
- **WHEN** a pulled server record carries a tag
- **THEN** the locally stored client SHALL keep that tag
- **AND** a pulled record without a tag SHALL store no tag locally

#### Scenario: Pulled creation date preserved
- **WHEN** a pulled server record includes a creation timestamp
- **THEN** the local creation timestamp SHALL be set from the server value, keeping the real creation date even after later edits

#### Scenario: Pull without a creation timestamp keeps local date
- **WHEN** a pulled server record does not include a creation timestamp
- **THEN** the local creation date SHALL be preserved when the record already existed locally, rather than being replaced by the modification timestamp
- **AND** a freshly fetched record without a creation timestamp SHALL fall back to its modification timestamp
