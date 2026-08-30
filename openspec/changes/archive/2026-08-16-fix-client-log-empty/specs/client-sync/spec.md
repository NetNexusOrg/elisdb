## ADDED Requirements

### Requirement: History fetch errors propagate

The system SHALL NOT convert a history fetch failure into an empty result; a failed fetch SHALL be reported so the UI can show an error rather than a misleading "no history".

#### Scenario: Fetch failure surfaces as an error
- **WHEN** the app requests a client's edit history and the request fails
- **THEN** the app SHALL surface the failure (not an empty list)
- **AND** the UI SHALL present it as a load error
