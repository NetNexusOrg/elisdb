# Proposal

## Why

The configuration card shows "Utilizador associado", but the name is not part of the configuration file: the app only learns it by asking the server (`GET /api/keys/me`) after import. That has three consequences:

- the identity is unknown until the server answers, so a fresh import shows "A identificar utilizador…" and, offline, "Não foi possível determinar o utilizador";
- the server already knows the name — it is the same name it stores as the key's allowlist comment — so the round trip repeats information the file could carry;
- a device configured against a temporarily unreachable server looks unconfigured from the user's point of view, even though the key and URL are valid.

## What Changes

- The server-issued configuration file gains `user_name` alongside `server_url` and `private_seed`, for both the CLI issue flow and the `ELISDB_USERS` startup provisioning.
- The app parses `user_name` as an optional field, stores it with the URL and the key, and shows it as the associated user immediately after import — no server round trip.
- Old configuration files (without `user_name`) keep working: the app falls back to the existing `GET /api/keys/me` lookup.
- Removing the configuration clears the stored user name with the rest of the settings.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities

- `client-key-import`: the imported configuration carries the user name; the settings screen shows it from the file, with the server lookup kept as a fallback.
- `server-key-provisioning`: issued and provisioned configuration files include the user name.
