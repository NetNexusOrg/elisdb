## Purpose

Lets the user import a server-issued ed25519 authentication key into the app settings so the device can sign requests as the server-issued identity, completing server-side controlled onboarding.

## ADDED Requirements

### Requirement: Import a server-issued key

The system SHALL provide an action in the app settings to import a server-issued ed25519 authentication key, and SHALL store the imported seed so sync signs with it.

#### Scenario: Import an issued key
- **WHEN** the user imports a server-issued private seed (base64)
- **THEN** the seed SHALL be stored as the app signing key
- **AND** subsequent syncs SHALL sign requests with it

#### Scenario: Invalid seed is rejected
- **WHEN** the user imports a value that is not a valid 32-byte base64 ed25519 seed
- **THEN** the import SHALL be refused and the user SHALL be told the value is invalid
- **AND** the previously stored key SHALL be left unchanged

#### Scenario: Import completes onboarding
- **WHEN** the user imports a valid key with the server URL set
- **THEN** the settings SHALL allow saving and a sync SHALL be triggerable against the configured server