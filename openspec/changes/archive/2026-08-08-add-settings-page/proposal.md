## Why

Today the server base URL and ed25519 signing key are compile-time values baked into `BuildConfig` (`ELISDB_SERVER_URL`, `ELISDB_PRIVATE_KEY`). There is no way to change the server target from the device, and when the build is missing that config the home screen shows a persistent "Não sincronizado" / "Servidor ou chave de assinatura não configurado" banner. Users need a place to configure the server connection on-device and a home screen that does not keep advertising configuration gaps.

## What Changes

- Add a **settings screen** where the operator can view and edit the server base URL and the ed25519 signing key seed, persisted locally at runtime, overriding the build-time defaults.
- Save applies the new configuration and triggers a sync against the new server so connectability is confirmed immediately.
- Remove the "Não sincronizado" idle message and the "Servidor ou chave de assinatura não configurada" config-fault message from the **client list (home) screen**; from the home, configuration gaps now lead the user toward the settings screen instead of being displayed as a sync error.

## Capabilities

### New Capabilities
- `client-settings`: a settings screen to view and edit the server base URL and signing key seed, persist them at runtime, and apply them to the sync client.

### Modified Capabilities
- `client-screens`: the home/client list screen no longer shows the "not synchronized" idle message or the "server or key not configured" config-fault message; sync-status display behaviour is adjusted accordingly.

## Impact

- `app/src/main/java/com/example/elisdb/remote/ApiClientProvider.kt` — read resolved config (runtime value falling back to build-time default) instead of only `BuildConfig`.
- New runtime config store holding server URL + signing key (defaults from `BuildConfig`).
- `app/src/main/java/com/example/elisdb/AppContainer.kt` — expose the config store and rebuild the API client when config changes.
- `app/src/main/java/com/example/elisdb/Navigation.kt` + `NavigationKeys.kt` — a new `Settings` destination.
- New `ui/settings/SettingsScreen.kt` (+ ViewModel) with server URL / signing key fields and a save action.
- `app/src/main/java/com/example/elisdb/ui/cliente/ClientListScreen.kt` — drop the two messages from `syncMessage`/idle states.
- Syncer top-level: `SyncManager`/`SyncCoordinator` reuse the resolved client; no protocol change.