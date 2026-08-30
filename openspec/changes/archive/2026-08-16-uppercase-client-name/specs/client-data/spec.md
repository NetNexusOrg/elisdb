## ADDED Requirements

### Requirement: Client name stored in uppercase

The system SHALL store the client's name in uppercase letters regardless of the casing the user enters, so the same client always has a consistent casing in storage.

#### Scenario: Name stored uppercased on create
- **WHEN** a new client is created with a name containing lowercase or mixed-case letters
- **THEN** the stored name SHALL be entirely uppercase

#### Scenario: Name uppercased on edit
- **WHEN** an existing client's name is edited to contain lowercase or mixed-case letters
- **THEN** the updated stored name SHALL be entirely uppercase

#### Scenario: Stored name uppercased on sync
- **WHEN** a client name is received from the server during a sync
- **THEN** the locally stored name SHALL be entirely uppercase