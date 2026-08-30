## MODIFIED Requirements

### Requirement: Authenticated requests

The server SHALL require every protected endpoint request to be authenticated by an ed25519 signature produced with an authorized private key. The signature SHALL cover the request method, the request path, the request query string, the hash of the request body, and a client-supplied timestamp. The authenticated identity (key id and, when present, user name) derived from the verified signature SHALL be stored on the per-request `request.state` object, NOT on shared application state (`app.state`), so that concurrent requests can never overwrite one another's identity.

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

#### Scenario: Authenticated identity is isolated per request
- **WHEN** two authenticated requests are processed concurrently
- **THEN** each request's authenticated identity SHALL be stored on its own `request.state`
- **AND** the server SHALL NOT read an identity written by another concurrent request
