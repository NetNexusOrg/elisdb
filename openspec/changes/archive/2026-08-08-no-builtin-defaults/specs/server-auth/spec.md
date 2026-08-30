## MODIFIED Requirements

### Requirement: Public key allowlist

The server SHALL maintain an allowlist of trusted public keys, SHALL authenticate requests only against keys present in that allowlist, and SHALL NOT trust any built-in or default key: no key SHALL be trusted until an operator explicitly adds it.

#### Scenario: Authorized key is accepted
- **WHEN** a request is signed by a private key corresponding to a public key in the allowlist
- **THEN** the request SHALL be accepted

#### Scenario: Removed key is no longer accepted
- **WHEN** a public key is removed from the allowlist
- **THEN** requests signed by its private key SHALL no longer be accepted

#### Scenario: No built-in key is trusted on first run
- **WHEN** the server starts with a fresh allowlist and no operator has added a key
- **THEN** the server SHALL reject every signed request
- **AND** the server SHALL NOT authenticate against any default, test, or generated key not explicitly added