## ADDED Requirements

### Requirement: Server reports its latest data timestamp

The server SHALL expose its latest data timestamp (the maximum `updated_at` over stored clients and the maximum `deleted_at` over tombstones) so a client can detect work to receive without performing a full pull.

#### Scenario: Latest timestamp endpoint returns the newest update
- **WHEN** a client requests the server's latest data timestamp
- **THEN** the server SHALL return the maximum `updated_at` among all stored clients and the maximum `deleted_at` among all tombstones
- **AND** the value SHALL be on the same epoch scale as client timestamps

#### Scenario: Latest timestamp is comparable to the pull watermark
- **WHEN** a client compares the returned latest timestamp with its own pull watermark
- **THEN** a latest timestamp strictly greater than the watermark SHALL indicate that there is server data the client has not yet pulled
