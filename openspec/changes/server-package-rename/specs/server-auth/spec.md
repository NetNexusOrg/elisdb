# server-auth — delta

## MODIFIED Requirements

### Requirement: Management of trusted keys

The server SHALL add trusted public keys exclusively through the server-issue flow (`python -m elisdb.keys`), which SHALL be available both with arguments (`issue <user-name> --server-url <url>`) and interactively, and registers each issued public key with the user name as comment. The server SHALL also provision trusted keys automatically at container startup for each user listed in the `ELISDB_USERS` environment variable, using the same server-issue flow. The CLI SHALL expose interactive commands to LIST and REMOVE trusted keys in addition to issuing them, picked from a numbered menu when the CLI runs without arguments. The allowlist SHALL remain observable so operators can confirm configuration, and an empty/first-run allowlist SHALL be distinguishable from a listing that contains keys.

#### Scenario: Issue adds a trusted key

- **WHEN** an operator runs the server-issue command or the interactive flow with a user name and a server URL
- **THEN** the issued public key SHALL be stored in the allowlist with the user name as comment
- **AND** the key SHALL be usable for authentication

#### Scenario: Startup provisioning adds a trusted key

- **WHEN** the container starts with `ELISDB_USERS` set and a listed user has no trusted key yet
- **THEN** a public key SHALL be issued for that user through the server-issue flow
- **AND** the public key SHALL be stored in the allowlist with the user name as comment
- **AND** the key SHALL be usable for authentication

#### Scenario: Interactive menu exposes issue, list and remove

- **WHEN** an operator runs the CLI without arguments
- **THEN** the CLI SHALL present a numbered menu with options to create (issue), list and remove trusted keys, and to exit
- **AND** the operator SHALL select an option by typing its number

#### Scenario: Key listing shows user comments

- **WHEN** an operator lists the trusted keys
- **THEN** each listing entry SHALL include the key's user-name comment alongside its id, public key, and creation time

#### Scenario: Remove a key by id with confirmation

- **WHEN** an operator removes a trusted key by its id through the interactive flow
- **THEN** the CLI SHALL ask for confirmation before deleting
- **AND** after confirmation the key SHALL be removed from the allowlist
- **AND** requests signed with that key SHALL no longer be accepted

#### Scenario: Allowlist stays observable when empty

- **WHEN** an operator checks the trusted keys and none are registered
- **THEN** the CLI SHALL distinguish an empty allowlist from a listing that contains keys
