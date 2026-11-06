# Spec Delta

## MODIFIED Requirements

### Requirement: Signing key encrypted at rest

The system SHALL store the server signing key (and the server base URL) encrypted at rest, with the encryption key held in the platform keystore, so the secret is not readable from plain shared preferences. If the platform keystore or encrypted store is unavailable, the app SHALL fail the configuration operation with a visible, actionable error and SHALL NOT write the signing key to plain-text storage. Existing plain-text values SHALL be migrated only after the encrypted copy is written and verified; until that succeeds, the previous configuration SHALL remain readable so the app can start.

#### Scenario: Key is not stored in plain text
- **WHEN** the app persists the server connection
- **THEN** the signing key SHALL be stored encrypted (not readable as plain text in the preferences file)

#### Scenario: Existing configuration keeps working after upgrade
- **WHEN** the app is upgraded from a version that stored the key in plain text
- **THEN** the previously stored values SHALL still be available (migrated), and the plain-text copy SHALL be removed

#### Scenario: Keystore unavailable fails closed
- **WHEN** the platform keystore or encrypted preferences cannot be created or opened
- **THEN** the app SHALL report that secure storage is unavailable
- **AND** SHALL NOT save the signing key in plain-text preferences
- **AND** SHALL NOT silently degrade to an unencrypted store

#### Scenario: Migration failure preserves the previous configuration
- **WHEN** writing or verifying the encrypted copy fails during migration
- **THEN** the previous configuration SHALL remain readable
- **AND** the app SHALL still start
- **AND** the plain-text copy SHALL NOT be removed before the encrypted copy is verified
