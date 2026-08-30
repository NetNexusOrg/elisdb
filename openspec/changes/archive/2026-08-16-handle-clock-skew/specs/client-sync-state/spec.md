## ADDED Requirements

### Requirement: Persisted clock offset

The system SHALL persist the device↔server clock offset so it survives app restarts and is available for stamping records created offline.

#### Scenario: Offset survives restart
- **WHEN** a clock offset has been measured and persisted
- **THEN** it SHALL still be available after the app restarts

#### Scenario: No offset before first measurement
- **WHEN** no offset has been measured yet
- **THEN** the store SHALL report an absent offset, and the app SHALL fall back to the device clock
