## Purpose

Adds ed25519 request signing so the database server can authenticate clients cryptographically: each protected request carries a signature over its method, path, body hash and timestamp, verified against an allowlist of trusted public keys to reject unsigned, invalid and replayed requests.

## Requirements
### Requirement: Authenticated requests

The server SHALL require every protected endpoint request to be authenticated by an ed25519 signature produced with an authorized private key. The signature SHALL cover the request method, the request path, the request query string, the hash of the request body, and a client-supplied timestamp. To avoid verifying against every allowlisted key on each request, the client MAY send its key identifier in the `X-Elisdb-Key` header; when present, the server SHALL verify the signature against only that key (and SHALL reject if the header names a key not in the allowlist). When the header is absent, the server SHALL fall back to trying each allowlisted key. The authenticated identity (key id and, when present, user name) derived from the verified signature SHALL be stored on the per-request `request.state` object, NOT on shared application state (`app.state`), so that concurrent requests can never overwrite one another's identity.

#### Scenario: Request signed with an authorized key is accepted
- **WHEN** a request to a protected endpoint arrives with a signature produced by an authorized private key, over the method, path, query string, body hash and timestamp
- **THEN** the server SHALL process the request normally

#### Scenario: Request without a signature is rejected
- **WHEN** a request to a protected endpoint arrives without a signature
- **THEN** the server SHALL reject it with an unauthorized error

#### Scenario: Request signed with an unknown key is rejected
- **WHEN** a request carries a signature from a private key whose public key is not in the allowlist
- **THEN** the server SHALL reject it with an unauthorized error

#### Scenario: Query string is covered by the signature
- **WHEN** a request to a protected endpoint carries query parameters and its signature does not cover the query string
- **THEN** the server SHALL reject it with an unauthorized error

#### Scenario: Tampered query string is rejected
- **WHEN** the query string of a signed request is altered after signing
- **THEN** the signature SHALL no longer verify
- **AND** the server SHALL reject the request with an unauthorized error

#### Scenario: Key header short-circuits verification
- **WHEN** a request carries a valid `X-Elisdb-Key` header naming an allowlisted key
- **THEN** the server SHALL verify the signature against only that key

#### Scenario: Key header naming an unknown key is rejected
- **WHEN** a request carries an `X-Elisdb-Key` header naming a key not in the allowlist
- **THEN** the server SHALL reject the request with an unauthorized error

#### Scenario: Missing key header falls back to full allowlist
- **WHEN** a request has no `X-Elisdb-Key` header
- **THEN** the server SHALL verify the signature against the allowlisted keys as before

#### Scenario: Authenticated identity is isolated per request
- **WHEN** two authenticated requests are processed concurrently
- **THEN** each request's authenticated identity SHALL be stored on its own `request.state`
- **AND** the server SHALL NOT read an identity written by another concurrent request

### Requirement: Anti-replay protection

The server SHALL reject replayed requests by checking the signed timestamp; a request whose timestamp is too old or too far in the future SHALL be rejected. Within the accepted timestamp window, the server SHALL additionally reject a request whose signature has already been seen (replay), using a short-TTL nonce or a cache of consumed signatures, so a captured signed request cannot be replayed while its timestamp remains valid.

#### Scenario: Request with a stale timestamp is rejected
- **WHEN** a signed request has a timestamp older than the allowed window
- **THEN** the server SHALL reject it with an unauthorized error

#### Scenario: Request with a future timestamp is rejected
- **WHEN** a signed request has a timestamp ahead of the server clock beyond the allowed window
- **THEN** the server SHALL reject it with an unauthorized error

#### Scenario: Replayed request is rejected
- **WHEN** a signed request is replayed within its valid timestamp window
- **THEN** the server SHALL reject the replay (the signature has already been consumed) with an unauthorized error

#### Scenario: Distinct requests within the window are accepted
- **WHEN** two different signed requests arrive within the window with unique signatures
- **THEN** both SHALL be processed normally

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

### Requirement: Management of trusted keys

The server SHALL add trusted public keys exclusively through the server-issue flow (`python -m app.keys`), which SHALL be available both with arguments (`issue <user-name> --server-url <url>`) and interactively, and registers each issued public key with the user name as comment. The server SHALL also provision trusted keys automatically at container startup for each user listed in the `ELISDB_USERS` environment variable, using the same server-issue flow. The CLI SHALL expose interactive commands to LIST and REMOVE trusted keys in addition to issuing them, picked from a numbered menu when the CLI runs without arguments. The allowlist SHALL remain observable so operators can confirm configuration, and an empty/first-run allowlist SHALL be distinguishable from a listing that contains keys.

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

### Requirement: Public health endpoint stays open

The health endpoint SHALL remain accessible without a signature so monitors and orchestrators can probe the server.

#### Scenario: Unauthenticated health check
- **WHEN** an unauthenticated request reaches the health endpoint
- **THEN** the server SHALL respond normally without requiring a signature