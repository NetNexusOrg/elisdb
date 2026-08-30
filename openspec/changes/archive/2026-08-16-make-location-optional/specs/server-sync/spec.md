## MODIFIED Requirements

### Requirement: Record identity

Each client record SHALL carry a stable client identifier and a modification timestamp used for conflict resolution. Photos SHALL belong to exactly one client. Coordinates SHALL be optional.

#### Scenario: Record has identity and timestamp
- **WHEN** a client record is received
- **THEN** it SHALL have an identifier, a modification timestamp, name, and optional contract and notes
- **AND** its photos SHALL be associated only with that client

#### Scenario: Coordinates are optional
- **WHEN** a client record is received without latitude or longitude
- **THEN** the record SHALL be accepted and stored with no coordinates

#### Scenario: Pull returns null coordinates
- **WHEN** a client pulls records and a stored client has no coordinates
- **THEN** the pulled record SHALL carry null latitude and longitude