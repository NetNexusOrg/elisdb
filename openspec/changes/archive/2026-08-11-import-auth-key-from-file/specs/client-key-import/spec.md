## MODIFIED Requirements

### Requirement: Import a server-issued key

The system SHALL provide an action in the app settings to import a server-issued ed25519 authentication key from a file, and SHALL store the imported seed so sync signs with it.

#### Scenario: Import an issued key from a file
- **WHEN** the user selects a file containing a server-issued private seed
- **THEN** the seed SHALL be stored as the app signing key
- **AND** subsequent syncs SHALL sign requests with it

#### Scenario: Import tolerates the issue command output
- **WHEN** the selected file contains the full output of `python -m app.keys issue <nome>` rather than only the bare seed
- **THEN** the private seed SHALL be extracted from the output and stored
- **AND** the extra text in the file SHALL be ignored

#### Scenario: Invalid file content is rejected
- **WHEN** the selected file does not contain a valid 32-byte base64 ed25519 seed
- **THEN** the import SHALL be refused and the user SHALL be told the value is invalid
- **AND** the previously stored key SHALL be left unchanged

#### Scenario: Import completes onboarding
- **WHEN** the user imports a valid key with the server URL set
- **THEN** the settings SHALL allow saving and a sync SHALL be triggerable against the configured server

#### Scenario: Import updates an existing key
- **WHEN** a key is already stored and the user imports a new valid key
- **THEN** the new seed SHALL replace the stored signing key
- **AND** subsequent syncs SHALL sign with the new key