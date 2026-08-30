## MODIFIED Requirements

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
- **AND** the settings screen SHALL show that the associated user could not be determined, distinct from "no user associated"

#### Scenario: Identity is re-resolved after a successful sync
- **WHEN** a successful sync completes and the associated user had previously not been resolved
- **THEN** the app SHALL retry the identity lookup so the user name can appear without reopening the settings screen
