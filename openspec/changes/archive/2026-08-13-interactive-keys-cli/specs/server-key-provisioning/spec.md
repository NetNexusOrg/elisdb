## MODIFIED Requirements

### Requirement: Public key registration

The system SHALL expose a server-managed flow that ISSUES an app's ed25519 keypair: it generates the keypair, records the public key in the server allowlist together with a mandatory comment identifying the user, and emits a configuration file carrying the private seed and the server URL for the operator to import into the app. Issuance SHALL be the only way to add a public key to the allowlist: the CLI SHALL NOT offer a separate registration command for externally created keys. The issuance flow SHALL be available interactively: running the CLI without arguments SHALL prompt for the required data sequentially, and the URL shall default to the `ELISDB_SERVER_URL` environment variable when the operator does not supply one.

#### Scenario: Issue a key for a user
- **WHEN** an operator issues a key for a named user providing the server URL
- **THEN** an ed25519 keypair SHALL be generated
- **AND** the public key SHALL be added to the allowlist
- **AND** the stored key SHALL carry a comment with the user's name
- **AND** a configuration file SHALL be produced containing the server URL and the private seed

#### Scenario: Interactive issuance prompts in order
- **WHEN** an operator runs the CLI without arguments and chooses to create an authentication
- **THEN** the CLI SHALL prompt for the server URL, then the user name, in that order
- **AND** once all data is provided it SHALL produce the same result as the argument-based issuance

#### Scenario: Interactive issuance defaults the server URL
- **WHEN** an operator runs the CLI interactively without a server URL and `ELISDB_SERVER_URL` is set
- **THEN** the CLI SHALL use the environment variable as the default server URL

#### Scenario: Issued key authenticates requests
- **WHEN** a request is signed with the private seed of an issued key
- **THEN** the request SHALL be authenticated against the allowlisted public key

#### Scenario: Issuance without a user name is refused
- **WHEN** an operator attempts to issue a key without providing a user name
- **THEN** the issuance SHALL be refused
- **AND** no key SHALL be added to the allowlist

#### Scenario: Issuance without a server URL is refused
- **WHEN** an operator attempts to issue a key without providing a server URL
- **THEN** the issuance SHALL be refused
- **AND** no configuration file SHALL be produced
- **AND** no key SHALL be added to the allowlist

#### Scenario: Externally created keys are not registrable via the CLI
- **WHEN** an operator tries to register a public key that was not produced by the server-issue flow
- **THEN** the CLI SHALL NOT offer a command to register it
- **AND** the externally created key SHALL NOT be added to the allowlist by the CLI