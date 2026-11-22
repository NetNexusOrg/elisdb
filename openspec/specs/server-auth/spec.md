## Purpose

Adds ed25519 request signing so the database server can authenticate clients cryptographically: each protected request carries a signature over its method, path, body hash and timestamp, verified against an allowlist of trusted public keys to reject unsigned, invalid and replayed requests.

## Requirements

### Requirement: Authenticated requests
The server SHALL authenticate protected requests with an ed25519 signature over the method, path, query string, body hash and a client timestamp. A client MAY send its key id in `X-Elisdb-Key`; when present the server SHALL verify only that allowlisted key and reject unknown ids, otherwise it SHALL try every allowlisted key. The verified identity SHALL be stored on `request.state`, never shared `app.state`, so concurrent requests cannot overwrite it.

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
The server SHALL reject requests whose signed timestamp is too old or too far in the future. Within the accepted window it SHALL also reject a signature already seen, using a short-TTL nonce or consumed-signature cache, so a captured request cannot be replayed while its timestamp remains valid. Cache entries SHALL outlive the signed timestamp's validity, protecting requests signed ahead of the server clock for their whole acceptance window.

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
The server SHALL add trusted keys only through `python -m elisdb.keys`, available with arguments (`issue <user-name> --server-url <url>`) or interactively, recording the user name as comment. It SHALL also provision keys at startup for each `ELISDB_USERS` user via the same flow. The CLI SHALL offer interactive LIST and REMOVE commands from a numbered menu. The allowlist SHALL be observable, and an empty list distinguishable from a populated one.

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
