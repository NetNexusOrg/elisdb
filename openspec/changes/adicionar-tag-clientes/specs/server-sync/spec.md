## MODIFIED Requirements

### Requirement: Push changes

The server SHALL accept a push of client and photo changes from a client and persist them, including the client's optional tag. Pushing a client SHALL also push its photos together with the client. A push SHALL be processed record by record: an invalid record SHALL NOT abort the batch, and the response SHALL carry a per-record result indicating whether each record was accepted, found duplicate, found stale, or rejected as invalid. A record whose photos are referenced but missing from storage SHALL still be accepted, and the result SHALL name those photos so the client sends them again.

#### Scenario: Client pushes new records
- **WHEN** a client pushes a batch of new or updated client records with their photos
- **THEN** each record and photo SHALL be persisted
- **AND** a record carrying a tag SHALL store that tag

#### Scenario: Push is idempotent per record
- **WHEN** the same client record is pushed more than once
- **THEN** it SHALL be stored once, not duplicated

#### Scenario: An invalid record does not abort the batch
- **WHEN** a client pushes a batch in which one record is invalid (e.g. a blank name)
- **THEN** the server SHALL process the remaining records normally
- **AND** SHALL return a per-record result marking the invalid record as rejected

#### Scenario: Missing photos are reported back
- **WHEN** a pushed record references a photo whose file is not stored
- **THEN** the record SHALL be accepted
- **AND** the result SHALL list that photo as missing so the client uploads it again

#### Scenario: Pushed record without a tag clears the stored tag
- **WHEN** a client pushes an update for a stored client and omits the tag
- **THEN** the stored client SHALL be persisted without a tag

### Requirement: Pull changes

The server SHALL allow a client to pull all stored client records with their photos, including each client's active/inactive status and optional tag.

#### Scenario: Client pulls all data
- **WHEN** a client requests a pull
- **THEN** the server SHALL return all stored clients with their photos

#### Scenario: Pull returns the client status
- **WHEN** a client pulls records and a stored client is inactive
- **THEN** the pulled record SHALL indicate that the client is inactive

#### Scenario: Pull returns the client tag
- **WHEN** a client pulls records and a stored client has a tag
- **THEN** the pulled record SHALL include that tag
- **AND** a stored client without a tag SHALL be returned without one

### Requirement: Field length limits

The server SHALL reject client records whose text fields exceed a maximum length, so a single record cannot bloat the database or the pull responses with unbounded text. The fields `nome`, `contrato`, `notas` and `tag` SHALL each have a maximum length.

#### Scenario: Oversized text field is rejected
- **WHEN** a client pushes a record whose `nome`, `contrato`, `notas` or `tag` exceeds the maximum length
- **THEN** the server SHALL reject that record rather than storing the oversized text

#### Scenario: Within-limit fields are accepted
- **WHEN** a client pushes a record whose text fields are within the maximum lengths
- **THEN** the server SHALL store the record as normal
