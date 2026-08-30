## ADDED Requirements

### Requirement: Delete endpoint accepts a deletion timestamp

The delete endpoint SHALL accept the deleting device's timestamp so the deletion competes in last-write-wins with concurrent edits.

#### Scenario: Delete with device timestamp
- **WHEN** a client is deleted with a device-supplied timestamp
- **THEN** the server SHALL store the tombstone with that timestamp
- **AND** remove the client and its photos

#### Scenario: Delete without a timestamp uses server time
- **WHEN** a client is deleted without a device timestamp
- **THEN** the server SHALL store the tombstone with its own current time
