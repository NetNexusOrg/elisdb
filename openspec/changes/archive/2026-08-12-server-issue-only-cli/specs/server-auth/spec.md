## MODIFIED Requirements

### Requirement: Management of trusted keys

The server SHALL add trusted public keys exclusively through the server-issue command (`python -m app.keys issue <user-name> --server-url <url>`), which registers each issued public key with the user name as comment. The CLI SHALL NOT provide commands to generate, add, remove, or list trusted keys individually. The allowlist SHALL remain observable so operators can confirm configuration, and an empty/first-run allowlist SHALL be distinguishable from a listing that contains keys.

#### Scenario: Issue adds a trusted key
- **WHEN** an operator runs the server-issue command with a user name and a server URL
- **THEN** the issued public key SHALL be stored in the allowlist with the user name as comment
- **AND** the key SHALL be usable for authentication

#### Scenario: No separate key commands are exposed by the CLI
- **WHEN** an operator runs `generate`, `add`, `remove`, or `list` as the `app.keys` command
- **THEN** the CLI SHALL refuse them
- **AND** the allowlist SHALL NOT be modified

#### Scenario: Allowlist stays observable
- **WHEN** an operator checks the trusted keys
- **THEN** the allowlist SHALL be observable
- **AND** an empty allowlist SHALL be distinguishable from one that contains keys