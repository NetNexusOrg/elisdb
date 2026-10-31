## ADDED Requirements

### Requirement: Local revision guards the acknowledgement

Every local change to a client SHALL increment a local revision, and the push acknowledgement SHALL clear the pending flag only when the stored revision still matches the one that was sent.

#### Scenario: Edit during the push is preserved
- **WHEN** a client is edited while its push is in flight
- **THEN** the acknowledgement SHALL NOT clear the pending flag
- **AND** the newer edit SHALL be pushed on the next synchronization

#### Scenario: Unchanged record is acknowledged
- **WHEN** the server accepts the record and no local change happened meanwhile
- **THEN** the pending flag SHALL be cleared

#### Scenario: Deletion during the push is preserved
- **WHEN** a client is deleted locally while its push is in flight
- **THEN** the acknowledgement SHALL NOT clear the deletion

### Requirement: A stuck record does not stall synchronization

A client record left pending for longer than the configured hold period SHALL stop holding the pull watermark back, so the rest of the dataset keeps synchronizing.

#### Scenario: Recent pending record holds the watermark
- **WHEN** a record was skipped during the pull and is pending for a recent edit
- **THEN** the watermark SHALL stay below that record's delivery timestamp

#### Scenario: Old pending record stops holding the watermark
- **WHEN** a skipped record has been pending for longer than the hold period
- **THEN** the watermark SHALL advance past it
- **AND** the record SHALL remain pending for a future push

### Requirement: Saving a deleted client reports failure

Saving a client that no longer exists locally SHALL fail with a visible message instead of reporting success.

#### Scenario: Client removed by a pull
- **WHEN** the user saves a record that was removed by a synchronization
- **THEN** the screen SHALL report that the client no longer exists
- **AND** SHALL NOT report the save as successful

### Requirement: Changing the server resets synchronization state

Importing a different server configuration SHALL clear the pull watermark, the photo push hashes and the clock offset, while keeping the local records.

#### Scenario: Configuration change clears the cursor
- **WHEN** a new configuration is imported
- **THEN** the watermark, photo hashes and clock offset SHALL be cleared
- **AND** the next pull SHALL be a full pull

#### Scenario: Local records survive the change
- **WHEN** the configuration is replaced
- **THEN** the local clients SHALL be kept and reconciled by the next pull

### Requirement: Edits use the server clock

Timestamps written by local edits SHALL include the offset measured against the server, like the timestamps of the sync requests.

#### Scenario: Device clock behind the server
- **WHEN** the device clock is behind and the offset is known
- **THEN** a saved client SHALL carry the corrected timestamp

### Requirement: Encrypted key storage migration is lossless

Migrating the signing key from plain-text preferences SHALL write and verify the encrypted copy before removing the plain-text copy, and a failure SHALL NOT prevent the app from starting.

#### Scenario: Encryption unavailable
- **WHEN** the encrypted store cannot be created
- **THEN** the plain-text configuration SHALL be kept
- **AND** the app SHALL still start

#### Scenario: Encryption succeeds
- **WHEN** the encrypted copy is written
- **THEN** the plain-text values SHALL be removed

### Requirement: Long transfers are not cut off by the short timeout

Requests that move large payloads SHALL use a timeout suited to their size instead of the short timeout used for metadata calls.

#### Scenario: Downloading a release
- **WHEN** the package download takes longer than the metadata timeout
- **THEN** the download SHALL continue until it completes or fails on its own timeout
