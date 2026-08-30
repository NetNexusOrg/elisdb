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

## ADDED Requirements

### Requirement: Imported key shows its associated user

The system SHALL display the user name associated with an imported signing key, as reported by the configured server, so the user can confirm which identity the app is using.

#### Scenario: Associated user is shown after import
- **WHEN** the user imports a key and the configured server reports a user name for it
- **THEN** the settings screen SHALL show that user name next to the imported key

#### Scenario: No associated user is shown as absent
- **WHEN** the configured server reports no user name for the imported key
- **THEN** the settings screen SHALL indicate that no user is associated with the key
- **AND** SHALL NOT fabricate a name

#### Scenario: Identity lookup is non-blocking
- **WHEN** the server cannot be reached while looking up the key identity (network or server error)
- **THEN** the imported key SHALL remain usable for sync
- **AND** the missing identity SHALL NOT surface as a configuration error