# Spec Delta

## MODIFIED Requirements

### Requirement: Pull returns changes since a point in time

The system SHALL support a pull that returns only client and photo records delivered after a caller-supplied timestamp (`since`), instead of always returning the full dataset. Records are compared by the delivery timestamp the server assigned when it accepted them, not by the editing device's `updated_at`, so a record from a device with a slow clock is still delivered to a client whose watermark is ahead of it.

#### Scenario: Client pulls only changed records
- **WHEN** the app requests a pull with a `since` value
- **THEN** the server SHALL return only records whose delivery timestamp is newer than that value (and their photos)
- **AND** the app SHALL apply them as authoritative for records with no pending local edits

#### Scenario: Full snapshot when no since provided
- **WHEN** the app requests a pull without a `since` value
- **THEN** the server SHALL return the full dataset as today
- **AND** the app SHALL preserve local non-pending records that are absent from the server response
- **AND** SHALL NOT delete them automatically

#### Scenario: Default pull response remains a flat list
- **WHEN** the app pulls without requesting pagination
- **THEN** the server SHALL return a flat list of records exactly as before, so older app versions keep parsing the response

#### Scenario: Paginated pull returns a page and a cursor
- **WHEN** the app requests a pull with a `limit`
- **THEN** the server SHALL return at most `limit` records together with a `next_since` cursor when a further page exists
- **AND** `next_since` SHALL be None when the returned page was the last

#### Scenario: Record from a slow device is not skipped
- **WHEN** a record is accepted from a device whose clock is behind the caller's watermark
- **THEN** the next pull with that watermark SHALL still return it

## ADDED Requirements

### Requirement: Destructive reconciliation requires explicit consent

A full pull SHALL be non-destructive by default: local records absent from the server SHALL be retained and identified as local-only. The app SHALL only replace local data with the server dataset after an explicit user confirmation, and SHALL require the server instance identity to match the identity stored for the current configuration. Before replacing data, the app SHALL create a recoverable local snapshot.

#### Scenario: New or empty server does not wipe local data
- **WHEN** the app pulls from a server that returns no record for a local non-pending client
- **THEN** the local client and its photos SHALL be preserved
- **AND** the app SHALL identify it as local-only instead of deleting it

#### Scenario: Server identity changes
- **WHEN** the configured server reports an instance identity different from the stored one
- **THEN** the app SHALL require explicit confirmation before reconciling data
- **AND** SHALL NOT silently delete local records

#### Scenario: User confirms destructive replacement
- **WHEN** the user explicitly confirms replacing local data with the server dataset
- **THEN** the app SHALL create a recoverable local snapshot before deleting anything
- **AND** SHALL only then remove local records absent from a complete server pull

#### Scenario: Pull is incomplete
- **WHEN** pagination stops before a final page without a next cursor (for example a repeated cursor)
- **THEN** the app SHALL NOT perform destructive reconciliation
