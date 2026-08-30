## Why

A release-ready deployment needs a clear, documented way to register the app's public key with the server and to configure the server connection on both sides. Today the flow is undocumented and error-prone: key management exists only as an ad-hoc CLI module, there is no way to generate a keypair and wire it from the app build to a server allowlist entry, and misconfiguration surfaces as an opaque 401/auth failure.

## What Changes

- Provide a documented, end-to-end key provisioning flow: generate an ed25519 keypair, expose the app's public key, and register it on the server via an existing management surface (`python -m app.keys add <id> <public_key_b64>`).
- Add a server-side health/configuration check that surfaces whether any trusted key is registered, to aid operators in first-run setup.
- Clarify server env config (`ELISDB_DB`, `ELISDB_PHOTOS`) and the fact that there is no trusted key on first start.
- Keep the allowlist management CLI (`add`/`remove`/`list`) — this change hardens and documents it, not redesigns the auth protocol.

## Capabilities

### New Capabilities
- `server-key-provisioning`: documented, end-to-end registration of an app public key in the server allowlist, and operator-facing visibility of whether trusted keys exist.

### Modified Capabilities
- `server-auth`: adds first-run configuration health feedback (surfacing when no trusted key is registered) to the key-allowlist management capabilities.

## Impact

- `server/app/keys.py` — a generation/health helper if added; otherwise documentation plus a diagnostic.
- Server startup/health — clearer first-run guidance when no trusted keys exist.
- `README` / ops documentation for the server- and client-side setup.
- `openspec/specs/server-auth` — first-run/key-management documentation requirements.