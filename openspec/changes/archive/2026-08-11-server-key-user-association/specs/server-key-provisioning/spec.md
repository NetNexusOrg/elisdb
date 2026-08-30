## MODIFIED Requirements

### Requirement: Public key registration

The system SHALL expose a server-managed flow that ISSUES an app's ed25519 keypair: it generates the keypair, records the public key in the server allowlist together with a mandatory comment identifying the user, and returns the private seed for the operator to import into the app.

#### Scenario: Issue a key for a user
- **WHEN** an operator issues a key for a named user
- **THEN** an ed25519 keypair SHALL be generated
- **AND** the public key SHALL be added to the allowlist
- **AND** the stored key SHALL carry a comment with the user's name
- **AND** the private seed SHALL be returned for the operator to hand to the app

#### Scenario: Issued key authenticates requests
- **WHEN** a request is signed with the private seed of an issued key
- **THEN** the request SHALL be authenticated against the allowlisted public key

#### Scenario: Issuance without a user name is refused
- **WHEN** an operator attempts to issue a key without providing a user name
- **THEN** the issuance SHALL be refused
- **AND** no key SHALL be added to the allowlist

#### Scenario: Register an externally created public key
- **WHEN** an operator provides an app's public key directly without server issuance
- **THEN** the key SHALL still be added to the allowlist
- **AND** it SHALL be registrable with or without a user-name comment

## ADDED Requirements

### Requirement: Key identity is discoverable by the key holder

The system SHALL let a signed request discover the user name associated with the key that signed it, so a client can identify itself without holding additional metadata.

#### Scenario: The holder looks up their own identity
- **WHEN** an authenticated request asks for the identity of its own signing key
- **THEN** the response SHALL identify the key and SHALL include the user name associated with it

#### Scenario: Unknown key identity
- **WHEN** an authenticated request asks for its identity and no user name is stored for the key
- **THEN** the response SHALL still identify the key
- **AND** SHALL indicate that no user name is associated