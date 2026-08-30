## MODIFIED Requirements

### Requirement: Authenticated requests

The server SHALL require every protected endpoint request to be authenticated by an ed25519 signature produced with an authorized private key. The signature SHALL cover the request method, the request path, the request query string, the hash of the request body, and a client-supplied timestamp. To avoid verifying against every allowlisted key on each request, the client MAY send its key identifier in the `X-Elisdb-Key` header; when present, the server SHALL verify the signature against only that key (and SHALL reject if the header names a key not in the allowlist). When the header is absent, the server SHALL fall back to trying each allowlisted key. The authenticated identity (key id and, when present, user name) SHALL be stored on the per-request `request.state`.

#### Scenario: Request signed with an authorized key is accepted
- **WHEN** a request to a protected endpoint arrives with a signature produced by an authorized private key, over the method, path, query string, body hash and timestamp
- **THEN** the server SHALL process the request normally

#### Scenario: Request without a signature is rejected
- **WHEN** a request to a protected endpoint arrives without a signature
- **THEN** the server SHALL reject it with an unauthorized error

#### Scenario: Request signed with an unknown key is rejected
- **WHEN** a request carries a signature from a private key whose public key is not in the allowlist
- **THEN** the server SHALL reject it with an unauthorized error

#### Scenario: Key header short-circuits verification
- **WHEN** a request carries a valid `X-Elisdb-Key` header naming an allowlisted key
- **THEN** the server SHALL verify the signature against only that key

#### Scenario: Key header naming an unknown key is rejected
- **WHEN** a request carries an `X-Elisdb-Key` header naming a key not in the allowlist
- **THEN** the server SHALL reject the request with an unauthorized error

#### Scenario: Missing key header falls back to full allowlist
- **WHEN** a request has no `X-Elisdb-Key` header
- **THEN** the server SHALL verify the signature against the allowlisted keys as before
