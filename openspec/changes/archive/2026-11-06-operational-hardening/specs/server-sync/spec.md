# Spec Delta

## ADDED Requirements

### Requirement: Pull pages use bounded database work

A pull page SHALL be read with a bounded number of database connections and queries, independent of the number of records in the page. The server SHALL NOT open a separate connection per returned record, and photo metadata for a page SHALL be loaded in a bounded batch.

#### Scenario: Large page does not open one connection per record
- **WHEN** a client pulls a page near the maximum size
- **THEN** the server SHALL use a bounded number of connections
- **AND** the number of connections SHALL NOT grow linearly with the record count

#### Scenario: Photos for a page are loaded in batch
- **WHEN** a pull page contains clients with photos
- **THEN** the server SHALL load the photo metadata without a per-record query
- **AND** SHALL return the same data as before
