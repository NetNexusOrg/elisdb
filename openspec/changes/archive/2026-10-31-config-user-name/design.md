# Design

## Context

A configuration file is emitted by `app/keys.py::issue_and_register`, used by both the CLI (`python -m app.keys issue`) and startup provisioning (`app/provision.py`). It currently holds `server_url` and `private_seed`. On the app side, `parseServerConfig` reads those two fields, `PrefsSettingsStore` persists them (encrypted), and `ServerConfigViewModel` resolves the associated user by calling `GET /api/keys/me` through the `fetchKeyIdentity` lambda wired in `Navigation.kt`.

## Goals / Non-Goals

**Goals:**
- The displayed user name comes from the configuration file when present.
- Files issued before this change keep importing and still show the name via the server lookup.
- One source of truth per device: the name is stored next to the URL and key and cleared with them.

**Non-Goals:**
- No change to request signing or to the server allowlist (the name is already the key's comment).
- No new endpoint and no change to `/api/keys/me`, which remains the fallback and the operator-visible truth.
- No re-validation of the name against the server on every open.

## Decisions

- **Optional field, not required.** `user_name` is parsed with `runCatching` and treated as optional so a file without it is still a valid configuration. Rejecting it would break every file issued so far and every hand-written file.
- **The file wins, the server is the fallback.** `ServerConfigViewModel` resolves the identity from the stored name in `init` and after import, and only calls `fetchKeyIdentity` when no name is stored. The sync-success re-resolve hook is unchanged, so it only runs while the identity is unresolved.
- **Store it with the settings.** `ServerSettings` gains `userName` and `SettingsStore.save` a third argument (default `""`), persisted in the same encrypted preferences as the seed, including in the plain-text migration path. This keeps URL, key and name consistent: `clearConfig` writes all three empty in one step.
- **Server emits the same name used as the allowlist comment**, so the file, `/api/keys/me` and the CLI listing agree.

## Risks / Trade-offs

- A file whose `user_name` was edited by hand would display that name until re-issued. The value is informational only (signing does not use it), and the server lookup is not consulted while it is present; `keys` still authorises by public key.
- Files already on devices keep relying on the lookup until re-issued with the new field.
