# Spec Delta

## ADDED Requirements

### Requirement: Deletion respects last-write-wins

A delete SHALL compete in last-write-wins with edits. The server SHALL reject a deletion whose `deleted_at` is not newer than the stored record's `updated_at`, returning a stale response, and SHALL NOT remove the record or create a tombstone in that case.

#### Scenario: Older delete loses to a newer edit
- **WHEN** a client deletes a record with a `deleted_at` older than or equal to the stored `updated_at`
- **THEN** the server SHALL reject the deletion as stale
- **AND** the record SHALL remain stored
- **AND** no tombstone SHALL be created

#### Scenario: Newer delete removes the record
- **WHEN** a client deletes a record with a `deleted_at` newer than the stored `updated_at`
- **THEN** the server SHALL delete the record and its photos
- **AND** SHALL create a tombstone as before

### Requirement: Deletion timestamps are plausible

The server SHALL reject deletion timestamps that are not plausible Unix epoch seconds, including negative values and values beyond the existing future-skew allowance, instead of accepting them into the tombstone.

#### Scenario: Negative deletion timestamp is rejected
- **WHEN** a delete supplies a negative `deleted_at`
- **THEN** the server SHALL reject the request with a client-error response
- **AND** SHALL NOT create a tombstone

#### Scenario: Far-future deletion timestamp is rejected
- **WHEN** a delete supplies a `deleted_at` beyond the allowed future skew
- **THEN** the server SHALL reject the request as before
- **AND** SHALL NOT delete the record
