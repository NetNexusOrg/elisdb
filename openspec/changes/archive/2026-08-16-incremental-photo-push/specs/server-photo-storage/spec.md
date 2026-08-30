## ADDED Requirements

### Requirement: Server keeps unchanged photos on merge

The server SHALL, when saving a client whose push references photos without data, keep the existing stored files and rows for those photos instead of deleting and rewriting them.

#### Scenario: Referenced photo kept unchanged
- **WHEN** a push includes a photo reference (no data) for a photo the server already stores
- **THEN** the server SHALL keep the existing file and row for that photo
- **AND** SHALL NOT delete or rewrite it

#### Scenario: Reference with no stored copy is ignored
- **WHEN** a push references a photo by id without data and the server has no stored copy
- **THEN** the server SHALL ignore that reference (no file is created and no row is added for it)
