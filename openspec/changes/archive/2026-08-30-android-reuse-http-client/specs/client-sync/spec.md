## MODIFIED Requirements

### Requirement: HTTP client lifecycle

The app SHALL reuse a single long-lived HTTP client (e.g. one Ktor `HttpClient`) for all requests to the server across the whole process lifetime, and SHALL NOT construct a new HTTP client for each sync, history fetch, or identity lookup. The shared client SHALL be created once (for example in the app container) and closed only when the process terminates.

#### Scenario: Repeated syncs reuse one client
- **WHEN** the app performs several syncs over its lifetime
- **THEN** all of them SHALL use the same underlying HTTP client instance
- **AND** no new client SHALL be constructed per sync

#### Scenario: History and identity fetches share the client
- **WHEN** the app fetches the latest updated-at, a key's identity, or a client's edit history
- **THEN** those requests SHALL use the same shared HTTP client as the sync path

#### Scenario: No client leak across syncs
- **WHEN** many syncs run back to back
- **THEN** the app SHALL NOT accumulate unclosed HTTP clients or connection pools
