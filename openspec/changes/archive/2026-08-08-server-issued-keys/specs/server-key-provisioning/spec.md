## MODIFIED Requirements

### Requirement: Public key registration

The system SHALL expose a server-managed flow that ISSUES an app's ed25519 keypair: it generates the keypair, records the public key in the server allowlist together with a comment identifying the user, and returns the private seed for the operator to import into the app.

#### Scenario: Issue a key for a user
- **WHEN** an operator issues a key for a named user
- **THEN** an ed25519 keypair SHALL be generated
- **AND** the public key SHALL be added to the allowlist
- **AND** the stored key SHALL carry a comment with the user's name
- **AND** the private seed SHALL be returned for the operator to hand to the app

#### Scenario: Issued key authenticates requests
- **WHEN** a request is signed with the private seed of an issued key
- **THEN** the request SHALL be authenticated against the allowlisted public key

#### Scenario: Register an externally created public key
- **WHEN** an operator provides an app's public key directly without server issuance
- **THEN** the key SHALL still be added to the allowlist
- **AND** it SHALL be registrable with or without a user-name comment

### Requirement: Trusted-key configuration visibility

The system SHALL let an operator determine whether any trusted public keys are registered, so an allowlist misconfigured at first run is detectable before clients sign requests.

#### Scenario: No trusted keys on first run
- **WHEN** an operator checks key configuration and no trusted keys are registered
- **THEN** the system SHALL indicate that no trusted keys exist, distinct from a successful listing of keys

#### Scenario: Key listing shows user comments
- **WHEN** an operator lists the trusted keys
- **THEN** each listing entry SHALL include the key's user-name comment alongside its id, public key, and creation time