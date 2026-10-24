## MODIFIED Requirements

### Requirement: Client edit history recorded on push

The server SHALL record an audit entry each time a client change is accepted, capturing the actor's user name (from the authenticated key's allowlist comment), the actor's key id, the timestamp, whether the change created or updated the client, and what changed: the fields whose values differ (previous and new value) and the photos added, removed or reordered. The actor SHALL be read from the per-request `request.state` set by authentication. An accepted push that changes neither a field nor a photo SHALL NOT create an entry.

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

#### Scenario: Edit records the changed fields
- **WHEN** a push changes one or more of a client's fields
- **THEN** the audit entry SHALL list each changed field with its previous value and its new value

#### Scenario: Photo changes are recorded
- **WHEN** a push adds, removes or reorders a client's photos
- **THEN** the audit entry SHALL record the added photo ids, the removed photo ids and whether the remaining photos were reordered

#### Scenario: Unchanged push records no entry
- **WHEN** a push is accepted but changes neither a field nor a photo
- **THEN** the server SHALL NOT create an audit entry for it

#### Scenario: Create records its photos
- **WHEN** a client is created with photos
- **THEN** the create audit entry SHALL record the added photo ids
- **AND** SHALL NOT list field values as changes

### Requirement: Per-client edit history endpoint

The server SHALL expose an authenticated endpoint returning the audit history of a client, ordered from oldest to newest, so the app can show who created and edited the client, when, and what changed. Each entry SHALL carry the actor user name when present, the key id, the timestamp, the create/edit marker, and the recorded change details, or a null details value for entries recorded before details existed.

#### Scenario: History returned for a client
- **WHEN** the app requests the edit history of a stored client
- **THEN** the server SHALL return the client's audit entries (actor user name when present, key id, timestamp, create/edit marker), ordered by timestamp

#### Scenario: Unknown client returns empty history
- **WHEN** the app requests the edit history of a client that does not exist
- **THEN** the server SHALL return an empty history (or a not-found response), not an error with partial data

#### Scenario: History carries the recorded changes
- **WHEN** a client's history contains an entry recorded with change details
- **THEN** the response SHALL include those details (changed fields and photo changes) for that entry
- **AND** entries recorded before details existed SHALL return a null details value
