## Why

The db-server currently exposes an open data API with no way to verify who is calling it. Adding ed25519 request signing lets the server authenticate legitimate clients cryptographically (the app ships a private key; the server keeps the matching public key) so that only trusted devices can push, pull and manage data.

## What Changes

- Protect the server's `/api/*` endpoints with ed25519 request signing: every protected request must carry a signature over its request line (method, path), body hash, and a timestamp, produced with the app's embedded private key.
- The server verifies the signature against the public key stored in its allowlist and rejects unsigned, wrongly signed, and replayed (stale timestamp) requests.
- Add a setup/management surface to register and check public keys in the allowlist.
- **BREAKING** for the db-server protocol: `/api/push`, `/api/pull`, `/api/clients`, and `DELETE /api/clients/{id}` now require a signed request. Existing unauthenticated clients will be rejected with `401`.
- This change covers the server and the auth protocol only; embedding the private key and signing in the Android app is a separate future change.

## Capabilities

### New Capabilities
- `server-auth`: ed25519 request authentication for the database server — signing/verification of each protected request, an allowlist of trusted public keys, timestamp anti-replay, and management affordances to add/remove keys.

### Modified Capabilities
<!-- None in this repo's specs (server-side behavior and the auth protocol live entirely under the new `server-auth` capability; the db server protocol is implementation of the existing server, not a spec change here). Scope of this change is the new capability only.

## Impact

- New auth code in `../elisdb-server` (a signing verification module, middleware/dependency, and an allowlist store with a CLI/management endpoint).
- Existing protected endpoints in `app/main.py` gain an auth dependency; unauthenticated requests now answer `401`.
- Relies on the `cryptography` dependency (ed25519) added to `requirements.txt`.
- Does not touch the Android app in this repo.