## ADDED Requirements

### Requirement: Client edit history recorded on push

The server SHALL record an audit entry each time a client is persisted, capturing the actor's user name (from the authenticated key's allowlist comment), the actor's key id, the timestamp, and whether the persistence created or updated the client.

#### Scenario: Create records the actor
- **WHEN** a client is created via a push from an authenticated key whose allowlist entry has a user name
- **THEN** the server SHALL store a create audit entry for that client with the actor's user name, the key id, and the creation timestamp

#### Scenario: Update records the actor
- **WHEN** an existing client is updated via a push
- **THEN** the server SHALL store an edit audit entry for that client with the actor's user name, the key id, and the update timestamp

#### Scenario: Unnamed key records no user name
- **WHEN** a client is persisted by a key whose allowlist entry has no user name
- **THEN** the audit entry SHALL record the key id and timestamp, and SHALL carry no user name

### Requirement: Per-client edit history endpoint

The server SHALL expose an authenticated endpoint returning the audit history of a client, ordered from oldest to newest, so the app can show who created and edited the client and when.

#### Scenario: History returned for a client
- **WHEN** the app requests the edit history of a stored client
- **THEN** the server SHALL return the client's audit entries (actor user name when present, key id, timestamp, create/edit marker), ordered by timestamp

#### Scenario: Unknown client returns empty history
- **WHEN** the app requests the edit history of a client that does not exist
- **THEN** the server SHALL return an empty history (or a not-found response), not an error with partial data
