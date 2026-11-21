# Spec Delta

## ADDED Requirements

### Requirement: Require a complete user configuration before operating

The app SHALL NOT show the client list, the map, the main menu or any other
operational screen unless the stored configuration contains a server URL, a
signing key and the user name. While any of the three is missing, the app SHALL
show a setup screen that explains what is missing and offers **Abrir
configuração**; the configuration screen SHALL remain reachable.

#### Scenario: Complete configuration opens the app

- **WHEN** the app opens with a server URL, a signing key and a user name stored
- **THEN** the app SHALL show the normal start screen
- **AND** it SHALL NOT show the setup screen

#### Scenario: Missing configuration blocks the app

- **WHEN** the app opens without a server URL, a signing key or a user name
- **THEN** the app SHALL show the setup screen instead of the client list
- **AND** the setup screen SHALL explain that a credential file must be imported
- **AND** **Abrir configuração** SHALL open the configuration screen

#### Scenario: Setup distinguishes a missing user name

- **WHEN** a server URL and a signing key are stored but the user name is blank
- **THEN** the setup screen SHALL state that the user name is missing
- **AND** it SHALL ask for the credential file to be imported again

#### Scenario: Importing a complete credential unlocks the app

- **WHEN** the user imports a credential file with server URL, key and user name from the setup screen
- **THEN** the configuration screen SHALL stay open until the user leaves it
- **AND** leaving it SHALL show the normal start screen

#### Scenario: Removing the credential blocks the app again

- **WHEN** the user removes the credential from the configuration screen
- **THEN** the app SHALL return to the setup screen
- **AND** the client list SHALL NOT be reachable

### Requirement: Recover the user name of legacy credentials

When a server URL and signing key are stored without a user name, the app SHALL
ask the server which user owns the key and persist that name. When the lookup
cannot complete (offline, server error or no associated user), the app SHALL keep
the setup screen asking for the credential file.

#### Scenario: Server resolves the missing user name

- **WHEN** the app starts with a URL and key but no user name and the server resolves the key owner
- **THEN** the app SHALL persist the resolved user name
- **AND** the normal start screen SHALL be shown

#### Scenario: Name cannot be recovered

- **WHEN** the app starts with a URL and key but no user name and the lookup fails or returns no user
- **THEN** the setup screen SHALL ask for the credential file
- **AND** the client list SHALL NOT be shown
