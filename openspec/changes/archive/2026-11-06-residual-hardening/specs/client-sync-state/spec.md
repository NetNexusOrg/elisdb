# Spec Delta

## ADDED Requirements

### Requirement: A delete-pending record cannot become a pending edit

A client marked for deletion SHALL NOT be silently converted into a pending edit. A save or update targeting a record whose deletion marker is set SHALL be rejected with a visible error, and the record SHALL retain its deletion marker. No stored record SHALL carry both a pending edit and a pending deletion.

#### Scenario: Update of a delete-pending record is rejected
- **WHEN** a save or repository update targets a client that is marked for deletion
- **THEN** the operation SHALL fail with a visible message
- **AND** the client SHALL remain marked for deletion
- **AND** no pending edit SHALL be recorded for it

#### Scenario: Dual pending state is impossible
- **WHEN** any client record is persisted
- **THEN** it SHALL NOT have both its pending-edit flag and its pending-deletion flag set
- **AND** a write that would produce both flags SHALL be rejected

#### Scenario: Deletion can still complete
- **WHEN** a delete-pending record is retried by the deletion push
- **THEN** the deletion SHALL proceed normally
- **AND** the record SHALL be removed after the server acknowledges it
