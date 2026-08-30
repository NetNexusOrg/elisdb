## ADDED Requirements

### Requirement: Push omits unchanged photos

The system SHALL allow a push to reference unchanged photos by identifier without retransmitting their base64 content, so editing a client's other fields does not re-upload every photo.

#### Scenario: Unchanged photos sent as references only
- **WHEN** the app pushes a client whose photos are unchanged since the last successful push
- **THEN** the push payload SHALL include those photos as references (id, no data payload)
- **AND** the server SHALL keep its stored photo files for them

#### Scenario: New or changed photos carry data
- **WHEN** the app pushes a client with newly added or modified photos
- **THEN** the push payload SHALL include the base64 content for those photos
- **AND** the server SHALL store their files

#### Scenario: First push sends all photos with data
- **WHEN** the app pushes a client for the first time
- **THEN** every photo SHALL be sent with its base64 content (no server copy exists to reference)
