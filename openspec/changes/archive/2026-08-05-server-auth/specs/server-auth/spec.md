## Purpose

Adds ed25519 request signing so the database server can authenticate clients cryptographically: each protected request carries a signature over its method, path, body hash and timestamp, verified against an allowlist of trusted public keys to reject unsigned, invalid and replayed requests.

## ADDED Requirements

### Requirement: Authenticated requests
The server SHALL require every protected endpoint request to be authenticated by an ed25519 signature produced with an authorized private key. The signature SHALL cover the request method, the request path, the hash of the request body, and a client-supplied timestamp.

#### Scenario: Request signed with an authorized key is accepted
- **WHEN** a request to a protected endpoint arrives with a signature produced by an authorized private key, over the method, path, body hash and timestamp
- **THEN** the server SHALL process the request normally

#### Scenario: Request without a signature is rejected
- **WHEN** a request to a protected endpoint arrives without a signature
- **THEN** the server SHALL reject it with an unauthorized error

#### Scenario: Request signed with an unknown key is rejected
- **WHEN** a request carries a signature from a private key whose public key is not in the allowlist
- **THEN** the server SHALL reject it with an unauthorized error

### Requirement: Anti-replay protection
The server SHALL reject replayed requests by checking the signed timestamp; a request whose timestamp is too old or too far in the future SHALL be rejected.

#### Scenario: Request with a stale timestamp is rejected
- **WHEN** a signed request has a timestamp older than the allowed window
- **THEN** the server SHALL reject it with an unauthorized error

#### Scenario: Request with a future timestamp is rejected
- **WHEN** a signed request has a timestamp ahead of the server clock beyond the allowed window
- **THEN** the server SHALL reject it with an unauthorized error

#### Scenario: Replayed request is rejected
- **WHEN** a signed request is replayed after its timestamp becomes stale
- **THEN** the server SHALL reject it as it would any stale request

### Requirement: Public key allowlist
The server SHALL maintain an allowlist of trusted public keys and SHALL authenticate requests only against keys present in that allowlist.

#### Scenario: Authorized key is accepted
- **WHEN** a request is signed by a private key corresponding to a public key in the allowlist
- **THEN** the request SHALL be accepted

#### Scenario: Removed key is no longer accepted
- **WHEN** a public key is removed from the allowlist
- **THEN** requests signed by its private key SHALL no longer be accepted

### Requirement: Management of trusted keys
The server SHALL expose a management surface to add a public key to the allowlist, remove a public key from the allowlist, and list the public keys currently trusted.

#### Scenario: Add a trusted key
- **WHEN** an operator adds a public key
- **THEN** the key SHALL be stored in the allowlist and usable for authentication

#### Scenario: Remove a trusted key
- **WHEN** an operator removes a public key
- **THEN** the key SHALL be removed from the allowlist

#### Scenario: List trusted keys
- **WHEN** an operator lists the trusted keys
- **THEN** the allowlist SHALL be returned

### Requirement: Public health endpoint stays open
The health endpoint SHALL remain accessible without a signature so monitors and orchestrators can probe the server.

#### Scenario: Unauthenticated health check
- **WHEN** an unauthenticated request reaches the health endpoint
- **THEN** the server SHALL respond normally without requiring a signature