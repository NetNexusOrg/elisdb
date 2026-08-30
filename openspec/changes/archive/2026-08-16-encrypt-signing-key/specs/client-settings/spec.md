## ADDED Requirements

### Requirement: Signing key encrypted at rest

The system SHALL store the server signing key (and the server base URL) encrypted at rest, with the encryption key held in the platform keystore, so the secret is not readable from plain shared preferences.

#### Scenario: Key is not stored in plain text
- **WHEN** the app persists the server connection
- **THEN** the signing key SHALL be stored encrypted (not readable as plain text in the preferences file)

#### Scenario: Existing configuration keeps working after upgrade
- **WHEN** the app is upgraded from a version that stored the key in plain text
- **THEN** the previously stored values SHALL still be available (migrated), and the plain-text copy SHALL be removed
