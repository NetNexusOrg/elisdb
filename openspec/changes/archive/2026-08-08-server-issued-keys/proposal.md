## Why

Key issuance is app-centric today: the device generates its own ed25519 keypair and an operator registers the public key on the server, so the server cannot track who owns which key. The server should be responsible for issuing authentication keys — it generates the keypair, stores it with a comment identifying the user, and hands the private seed to the operator for the device. The Android settings screen then needs a way to import that issued key (and confirm the server URL), completing a server-side controlled onboarding.

## What Changes

- **Server issues keys**: replace the client-side "generate your own key" flow with a server-managed issuance flow: the server generates an ed25519 keypair, records the **public** key in the allowlist together with a **comment that names the user**, and returns the **private seed** for the operator to give to the app.
- **Comment on keys**: the key allowlist records and surfaces a user-name comment (`nome`) per key alongside id, public key, and creation time.
- **App imports the key**: the Android settings screen gains the ability to import a server-issued authentication key (paste/load the issued seed) and to set the server URL (URL config is covered by the `add-settings-page` change; this change adds the import action and the server-side issuance behind it).

## Capabilities

### New Capabilities
- `client-key-import`: importing a server-issued ed25519 authentication key into the app settings so the device can sign requests with the issued identity.

### Modified Capabilities
- `server-key-provisioning`: the server generates (issues) keypairs with a user-name comment instead of only registering externally created keys; issued keys are returned as a private seed + registered public key.

## Impact

- `elisdb-server/app/database.py` — `keys` table gains a `comment` column; `add_key` accepts a comment; `list_keys` returns it.
- `elisdb-server/app/keys.py` — new `issue <user>` (and `accept`) command: generate keypair, store public key + comment, print private seed + public key.
- `elisdb-server/app/main.py` — `GET /api/keys` includes the comment.
- `elisdb-server/app/models.py` / tasks tests — updated accordingly.
- Android: `ApiClientProvider`/settings reuse the import store; new `SettingsScreen` import action (see `add-settings-page` for the screen shell).
- `openspec/specs/server-key-provisioning` — modified requirements.