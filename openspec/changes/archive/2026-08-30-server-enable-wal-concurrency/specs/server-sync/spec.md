## MODIFIED Requirements

### Requirement: Concurrent access safety

The server SHALL configure its SQLite store for concurrent access so that multiple devices synronizing in parallel do not cause "database is locked" errors. The server SHALL enable WAL journal mode and a `busy_timeout` on its database connections, and SHALL apply the schema and any migrations exactly once at process startup rather than re-running them on every request. A connection opened per request is acceptable for the single-process server, provided these properties are already established.

#### Scenario: Concurrent pushes do not error with database locked
- **WHEN** several authenticated devices push client records at the same time
- **THEN** the server SHALL persist all of them
- **AND** SHALL NOT return a "database is locked" error to any of the requests

#### Scenario: Schema and migrations run once at startup
- **WHEN** the server process starts
- **THEN** the schema and any pending migrations SHALL be applied a single time
- **AND** subsequent per-request connections SHALL NOT re-run schema or migrations

#### Scenario: WAL mode is enabled
- **WHEN** the server opens its database
- **THEN** the connection SHALL use WAL journal mode and a non-zero busy_timeout
- **AND** concurrent readers and a writer SHALL be permitted without locking errors
