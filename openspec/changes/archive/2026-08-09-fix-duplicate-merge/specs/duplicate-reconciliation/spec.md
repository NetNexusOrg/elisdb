## Purpose

Defines how the app responds when the server acknowledges a pushed client with a `duplicate` status: it must adopt the server's existing record while preserving the user's locally entered data, never silently deleting the local record.

## ADDED Requirements

### Requirement: Duplicate push acknowledges the existing record

The system SHALL handle a `duplicate` push acknowledgement by reconciling the local record against the server's `existing_id` returned in the acknowledgement, instead of treating it as a plain acknowledgement.

#### Scenario: Duplicate acknowledged on push
- **WHEN** a pushed client record receives a `duplicate` status
- **THEN** the system SHALL use the `existing_id` from the acknowledgement as the canonical record
- **AND** SHALL reconcile the local record's data onto that canonical record

#### Scenario: Duplicate never silently deleted
- **WHEN** a record was acknowledged as `duplicate`
- **THEN** the subsequent `pull` SHALL NOT permanently delete the local record
- **AND** the user's locally entered fields and photos SHALL remain available on the retained record

### Requirement: Merge preserves local data

The system SHALL merge the user's locally entered client fields (e.g. contract, notes, coordinates) and photos from the duplicate local record onto the retained canonical record when a duplicate push is acknowledged.

#### Scenario: Fields merged onto existing record
- **WHEN** a local record acknowledged as `duplicate` has locally entered fields that differ from the server's existing record
- **THEN** the local fields SHALL be applied to the canonical record
- **AND** the canonical record SHALL be flagged as pending and re-pushed so the server stores the merged values

#### Scenario: Photos transferred
- **WHEN** a local record acknowledged as `duplicate` has photos
- **THEN** those photos SHALL be associated with the canonical record
- **AND** SHALL be pushed to the server with the canonical record

### Requirement: Duplicate merge is user-visible

The system SHALL surface an observable and user-facing indication when a locally created record was merged into an existing record as a duplicate.

#### Scenario: Merge completion is reported
- **WHEN** a duplicate merge completes during a sync
- **THEN** the sync outcome SHALL carry a duplicate-merge result
- **AND** the UI SHALL display a message naming the merged record so the user understands what happened