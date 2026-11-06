# Spec Delta

## ADDED Requirements

### Requirement: Push is chunked and bounded

The app SHALL send pending client records in bounded batches that never exceed the server's configured maximum, apply the per-record results of each batch before starting the next, and keep records pending until their own result is acknowledged. A batch failure SHALL NOT prevent later batches or unrelated records from being attempted, and the app SHALL validate the per-client photo limit before uploading so an oversized client produces an actionable error instead of blocking every sync.

#### Scenario: More pending records than one batch
- **WHEN** the app has more pending records than the server's maximum batch size
- **THEN** the app SHALL split them into multiple push requests, each within the limit
- **AND** every record SHALL be acknowledged across the batches
- **AND** the sync SHALL NOT fail solely because the total number of pending records exceeds one batch

#### Scenario: One batch fails
- **WHEN** one push batch fails with a transient or invalid-record response
- **THEN** the app SHALL keep that batch's records pending for a later retry
- **AND** SHALL still attempt the remaining batches
- **AND** SHALL report the failure without discarding acknowledged records

#### Scenario: Client learns the server batch limit
- **WHEN** the server advertises its maximum push batch size
- **THEN** the app SHALL use a batch size that does not exceed it
- **AND** when the server does not advertise a limit, the app SHALL fall back to a conservative size below the server default

#### Scenario: Client exceeds the photo limit
- **WHEN** a client to push has more photos than the server accepts per client
- **THEN** the app SHALL reject the push for that client with an actionable message before uploading its photos
- **AND** SHALL NOT leave the whole sync stuck on a repeated server error

#### Scenario: Photos are not orphaned by a failed push
- **WHEN** a push batch is not accepted after its photos were uploaded
- **THEN** the app SHALL retry or clean up those uploads so they do not remain orphaned indefinitely

### Requirement: Pull reconciliation requires a completed pull

The app SHALL treat a pull as complete only when it has followed the pagination cursor to a response with no next cursor. A pull aborted by a repeated cursor, a partial page, or a network failure SHALL NOT be used to delete or drop local records that are absent from the received pages.

#### Scenario: Cursor does not advance
- **WHEN** the server returns a pagination cursor that does not advance
- **THEN** the app SHALL stop the pull and report an error
- **AND** SHALL NOT delete local records based on that partial result

#### Scenario: Complete pull permits reconciliation
- **WHEN** the pull reaches the final page with no next cursor
- **THEN** the app SHALL be allowed to reconcile local records against the complete server set
- **AND** records with pending local changes SHALL still be preserved
