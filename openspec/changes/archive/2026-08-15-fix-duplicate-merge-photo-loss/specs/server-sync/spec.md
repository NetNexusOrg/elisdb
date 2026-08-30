## ADDED Requirements

### Requirement: Duplicate push response carries the canonical record

When a push is acknowledged as `duplicate`, the server SHALL include the canonical record (client fields and its photos) in the response so the client can reconcile without a separate round-trip, and SHALL NOT have modified the canonical record by refusing the pushed record.

#### Scenario: Duplicate response includes canonical client and photos
- **WHEN** a client pushes a record that the server acknowledges as `duplicate`
- **THEN** the response SHALL include the canonical record's client fields and its photos
- **AND** the canonical record SHALL be returned unchanged from its stored state

#### Scenario: Pushed duplicate is not persisted
- **WHEN** a client pushes a record acknowledged as `duplicate`
- **THEN** the pushed record SHALL NOT be stored under its own identifier
- **AND** the canonical record and its photos SHALL remain intact