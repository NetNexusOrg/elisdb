## ADDED Requirements

### Requirement: Server reports its stored client count

The server's latest-data response SHALL include the number of client records it currently stores, so a client can show how many clients exist on the server without pulling the dataset. The count SHALL include only live clients: records deleted (represented by tombstones) SHALL NOT be counted.

#### Scenario: Latest response includes the client count
- **WHEN** a client requests the server's latest data timestamp
- **THEN** the response SHALL include the number of client records currently stored
- **AND** the count SHALL be returned alongside the latest timestamp in the same response

#### Scenario: Count reflects pushes
- **WHEN** clients are pushed to the server
- **THEN** the reported client count SHALL include the newly stored records

#### Scenario: Deleted clients are not counted
- **WHEN** a client is deleted from the server
- **THEN** the reported client count SHALL decrease
- **AND** the tombstone left by the deletion SHALL NOT be counted as a client
