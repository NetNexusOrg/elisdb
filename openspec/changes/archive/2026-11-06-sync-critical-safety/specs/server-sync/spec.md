# Spec Delta

## ADDED Requirements

### Requirement: Push batch size is bounded

The server SHALL enforce a configured maximum number of client records per push and SHALL reject an oversized batch before persisting any of its records, returning a client-error response that states the limit. A batch within the limit SHALL be processed with the existing per-record semantics.

#### Scenario: Oversized batch is rejected atomically
- **WHEN** a client pushes more records than the configured maximum
- **THEN** the server SHALL reject the whole request with a client-error response
- **AND** SHALL NOT persist any record from that batch
- **AND** the response SHALL identify the maximum accepted batch size

#### Scenario: Batch within the limit is accepted
- **WHEN** a client pushes a batch at or below the configured maximum
- **THEN** the server SHALL process it record by record as before
- **AND** SHALL return a per-record status for every record

#### Scenario: Limit is configurable
- **WHEN** an operator configures the push batch limit
- **THEN** the server SHALL use that value for subsequent pushes
- **AND** the default SHALL remain bounded

#### Scenario: Limit is discoverable by clients
- **WHEN** a client requests the authenticated latest-data response
- **THEN** the response SHALL include the effective maximum push batch size
- **AND** the value SHALL match the limit enforced by the push endpoint
