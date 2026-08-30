## MODIFIED Requirements

### Requirement: Public key registration

The system SHALL expose a server-managed flow that ISSUES an app's ed25519 keypair: it generates the keypair, records the public key in the server allowlist together with a mandatory comment identifying the user, and emits a configuration file carrying the private seed and the server URL for the operator to import into the app.

#### Scenario: Issue a key for a user
- **WHEN** an operator issues a key for a named user providing the server URL
- **THEN** an ed25519 keypair SHALL be generated
- **AND** the public key SHALL be added to the allowlist
- **AND** the stored key SHALL carry a comment with the user's name
- **AND** a configuration file SHALL be produced containing the server URL and the private seed

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

#### Scenario: Register an externally created public key
- **WHEN** an operator provides an app's public key directly without server issuance
- **THEN** the key SHALL still be added to the allowlist
- **AND** it SHALL be registrable with or without a user-name comment