# Spec Delta

## ADDED Requirements

### Requirement: Photo push streams one photo at a time

The app SHALL read, hash, and upload photos one at a time during a push and SHALL NOT hold all photo bytes of a client in memory simultaneously. A missing photo SHALL be reported and skipped without preventing other photos or unrelated clients from being processed.

#### Scenario: Large client is uploaded incrementally
- **WHEN** a client has many photos to push
- **THEN** the app SHALL process one photo at a time
- **AND** SHALL release that photo's bytes before reading the next one

#### Scenario: Missing photo does not abort the client
- **WHEN** one of a client's photo files is missing during the push
- **THEN** the app SHALL report that photo as missing
- **AND** SHALL still process the client's remaining photos and records

#### Scenario: Upload order is deterministic
- **WHEN** a client's photos are pushed
- **THEN** the app SHALL upload them in a stable order derived from the photo order and identifier
- **AND** a retry of the same push SHALL use the same order

### Requirement: Push retries are idempotent

The app SHALL attach a stable idempotency key to each push batch and SHALL reuse that key when retrying the same batch after a timeout or transient failure. A new key SHALL be generated only when the batch content changes (for example after a new local edit or a different set of records), so the server can return the prior result without duplicating writes or requiring photos to be uploaded again.

#### Scenario: Timeout retry reuses the key
- **WHEN** a push request times out or fails transiently and the app retries the same batch
- **THEN** the retry SHALL carry the same idempotency key
- **AND** the server SHALL be able to return the original per-record result

#### Scenario: Changed batch uses a new key
- **WHEN** a record in the batch is edited or the batch membership changes after a failed attempt
- **THEN** the next push SHALL use a new idempotency key
- **AND** SHALL NOT reuse the result of the previous batch

#### Scenario: Successful retry does not duplicate work
- **WHEN** a push succeeds but its response is lost and the app retries with the same key
- **THEN** the server SHALL NOT persist the records twice or create duplicate audit entries
- **AND** the app SHALL treat the returned result as the acknowledgement
