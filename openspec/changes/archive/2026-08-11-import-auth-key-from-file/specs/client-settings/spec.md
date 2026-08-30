## MODIFIED Requirements

### Requirement: Server connection settings screen

The system SHALL provide a settings screen where the server base URL can be edited and the ed25519 signing key seed is shown in masked, non-editable form, prefilled with the currently effective values, and where the key can be imported from a file or cleared.

#### Scenario: View current server configuration
- **WHEN** the user opens the settings screen
- **THEN** the screen SHALL show the current server base URL and an indicator that a signing key is set
- **AND** the key SHALL be presented masked so the full secret is not visible

#### Scenario: View configuration when no key is set
- **WHEN** the user opens the settings screen and no signing key is stored
- **THEN** the screen SHALL indicate that no key is configured

#### Scenario: Edit and save server configuration
- **WHEN** the user changes the server base URL and saves
- **THEN** the new value SHALL be persisted for future app runs
- **AND** the screen SHALL confirm the value was saved

#### Scenario: Empty configuration is accepted
- **WHEN** the user clears the signing key or saves a blank server base URL
- **THEN** the values SHALL be persisted as blank
- **AND** syncs SHALL be refused locally until the values are set, without crashing

#### Scenario: Clear a stored signing key
- **WHEN** the user taps the clear action for a stored signing key
- **THEN** the stored key SHALL be removed
- **AND** the screen SHALL indicate that no key is configured