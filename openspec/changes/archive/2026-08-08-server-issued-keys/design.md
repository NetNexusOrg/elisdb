## Context

Today the operator generates an ed25519 keypair themselves (`python -m app.keys generate`) and registers the public key with `python -m app.keys add <id> <public_key_b64>`. The app's private seed and the server's allowlist are loosely coupled, and nothing names the key's owner. On the Android side, the pending `add-settings-page` change introduces the settings shell with a server URL field; this change makes the server the issuer of keys and adds an import action with which a device adopts the issued identity.

## Goals / Non-Goals

- **Goals**: server issues keypairs (generate + allowlist-register in one step) with a user-name comment stored per key; `GET /api/keys` and `keys list` surface the comment; a new settings-screen import action lets the app take a server-issued seed.
- **Non-Goals**: changing the ed25519 protocol, request signing, or anti-replay; key rotation/revocation UX; changing who holds the private key (still the device, but now handed to it by the operator from a server-issued pair); multi-server profiles.

## Approach

### 1. Server: `keys` table gains an owner comment

Add a `comment` column to `keys`:

- Tracked in `SCHEMA` as optional (`comment TEXT`).
- Idempotent migration helper on connect: `ALTER TABLE keys ADD COLUMN comment TEXT` guarded by a pragma `PRAGMA table_info(keys)` check so existing DBs upgrade in place and fresh DBs get the column from `CREATE TABLE`.
- `add_key(key_id, public_key, comment="")`, `list_keys()` return all four fields.

### 2. Server: issuer command `python -m app.keys issue`

Replace the two-step generate-then-add with one step that registers the public key with a comment and prints the private seed:

```text
python -m app.keys issue <user-name>
# -> issued key for '<user>'
# -> private seed (import into app):
#    <base64-seed>
# -> registered public key:
#    <base64-public>
```

Implementation: reuse `generate()` for the pair; `db.add_key(id_for(user), public_b64, comment=user)`; print seed + public key. `add` remains for external/public-key registration (spec keeps that scenario) and becomes `python -m app.keys add <id> <public_key_b64> [comment]`.

### 3. Server: list surfaces comments

- `app/keys.py list` prints id + public key + comment + created_at.
- `GET /api/keys` returns the existing list dict, which now includes the `comment` field (stored in `list_keys`).

### 4. Android: settings import action

Build on `add-settings-page`'s settings screen:

- Import field/action in the same settings screen where the server URL is configured (`add-settings-page` provides the shell; this change adds the import affordance).
- Import flow: user pastes the issued seed (base64) → validate with `Signer.fromBase64` semantics (decodeable, 32 bytes) → persist via the `SettingsStore` signing-key override → the store's key now signs sync requests (resolved in `ApiClientProvider`).
- Invalid seed: show inline validation error, do not overwrite stored value.
- After import, a manual sync is triggerable (matches the existing sync UX); no silent automatic push.

## Alternatives Considered

- Keeping key generation client-side and only adding comments: rejected — the requirement is that the *server* is responsible for creating the keys.
- Storing the private seed on the server too: rejected — the server only authoritatively stores the public key; the private seed is returned once and held by the device.
- A dedicated HTTP issue endpoint: not needed — the allowlist CLI is the management surface and matches `server-key-provisioning`; a future change could expose it over HTTP.