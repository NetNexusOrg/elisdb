## 1. End-to-end provisioning flow

- [x] 1.1 Add a documented/generate helper producing an ed25519 keypair whose base64 public key and base64 private seed match what the app expects (`ELISDB_PRIVATE_KEY`).
- [x] 1.2 Document the full flow: generate keypair → embed private seed in the app build → `python -m app.keys add <id> <public_key_b64>` on the server → verify with a signed request.

## 2. Empty-config observability

- [x] 2.1 Make `keys list` / `GET /api/keys` distinguish an empty allowlist from a populated one (e.g. explicit empty result) and add first-run guidance when no keys are registered.
- [x] 2.2 Verify no trusted-key-first-start behaviour with the CLI and the keys endpoint.

## 3. Environment documentation

- [x] 3.1 Document `ELISDB_DB` and `ELISDB_PHOTOS` and the fact that no key is trusted before the first `add`.

## 4. Verification

- [x] 4.1 Run the full provisioning flow end-to-end against a local server and confirm authenticated sync works with the registered key.
- [x] 4.2 Confirm requests signed by an unregistered key are still rejected (no regression).