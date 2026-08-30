## MODIFIED Requirements

### Requirement: Client edit history recorded on push

The server SHALL record an audit entry each time a client is persisted, capturing the actor's user name (from the authenticated key's allowlist comment), the actor's key id, the timestamp, and whether the persistence created or updated the client. The actor (key id and user name) SHALL be read from the per-request `request.state` set by authentication, so concurrent requests attribute the audit entry to the correct device and never to another request's identity.

#### Scenario: Create records the actor
- **WHEN** a client is created via a push from an authenticated key whose allowlist entry has a user name
- **THEN** the server SHALL store a create audit entry for that client with the actor's user name, the key id, and the creation timestamp

#### Scenario: Update records the actor
- **WHEN** an existing client is updated via a push
- **THEN** the server SHALL store an edit audit entry for that client with the actor's user name, the key id, and the update timestamp

#### Scenario: Unnamed key records no user name
- **WHEN** a client is persisted by a key whose allowlist entry has no user name
- **THEN** the audit entry SHALL record the key id and timestamp, and SHALL carry no user name

#### Scenario: Audit actor is correct under concurrent requests
- **WHEN** two clients are pushed concurrently by two different authenticated keys
- **THEN** each audit entry SHALL record the key id and user name of the request that performed that push
- **AND** no audit entry SHALL attribute a push to the other request's key
