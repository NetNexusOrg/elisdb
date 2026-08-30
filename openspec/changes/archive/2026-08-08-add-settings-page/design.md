## Context

Today the Android app gets its server base URL and ed25519 signing-key seed exclusively from build-time `BuildConfig` fields (`SERVER_BASE_URL`, `SIGNER_PRIVATE_KEY_B64`), resolved in `ApiClientProvider`. There is no way to change the server target from the device, and a build missing that config leaves the home screen stuck on "Não sincronizado" or a config-fault message. A settings screen must let the user override those at runtime and apply immediately, while the home screen drops those config-gap messages.

## Goals / Non-Goals

- **Goals**: runtime-viewable and editable server URL + signing key; persisted across runs; applies to sync immediately; home screen no longer advertises "Não sincronizado" or missing-config as a sync error; config gaps lead to the settings screen.
- **Non-Goals**: changing the ed25519 auth protocol, request format, or server behaviour; adding accounts/multiple profiles; editing anything besides the server URL and signing key.

## Approach

### 1. Runtime config store

Introduce a `SettingsStore` (SharedPreferences-backed, app-scoped) holding `SERVER_BASE_URL_OVERRIDE` and `SIGNER_KEY_OVERRIDE`:

- On first read, when no saved value exists, seed from `BuildConfig.SERVER_BASE_URL` / `BuildConfig.SIGNER_PRIVATE_KEY_B64` so existing builds keep working unchanged.
- Expose `settings(): ServerSettings`, `save(baseUrl, key)`, and an empty-blank handling.

### 2. Config resolution in `ApiClientProvider`

Change `ApiClientProvider.create()` to read resolved values: runtime override if set, else `BuildConfig`. This keeps `SyncCoordinator`, `SyncManager` and `SyncWorker` untouched (they already go through `AppContainer.apiClient`).

The `Signer` is built from the resolved key; blank key or blank base URL continue to raise `ConfigException`, which the sync layer already maps to `SyncFault.CONFIG`.

### 3. Applying a save

- `AppContainer` hosts the `SettingsStore`; replace the Lazy `apiClient` with a config-aware holder that rebuilds the `ApiClient`/`Signer` when settings change (e.g. a `MutableStateFlow<ServerSettings>` + `create()` built from latest value, or an `apiClientProvider` fun that sync can re-invoke).
- Settings screen's Save: `settingsStore.save(...)`, then trigger a sync via the existing `SyncManager.syncNow()` so the new connection is attempted immediately.

### 4. Settings screen + navigation

- New nav destination `Settings` (in `NavigationKeys.kt`) reached via an entry point on the client list screen (a settings/gear action) and from the config-fault state.
- `SettingsScreen` shows two fields (server base URL, signing key seed) pre-filled from current settings, saved/back actions, inline confirmation feedback, and a warning that changing the key means the matching public key must be registered on the server.

### 5. Home screen message removal

In `ClientListScreen`:

- Remove the idle "Não sincronizado" mapping — idle renders no config-gap message.
- Remove the CONFIG fault message from `syncMessage`; a CONFIG fault surfaces as a prompt/entry point to open Settings instead of a CloudOff/error message.
- Keep the existing NETWORK/AUTH messages and the sync trigger.

## Alternatives Considered

- Keeping config only in build-time `BuildConfig`: rejected — cannot be changed from the device.
- Storing nonsensitive values in a Room table: over-engineering; SharedPreferences suffices for two strings.
- Editing the release signing key at runtime: outside scope (non-goal); the signing key here is the app's ed25519 request key, not the APK keystore, and editing it is exactly what this change enables.
- No rebuild on save (requiring a manual sync): rejected — the spec requires an immediate sync after save to validate connectivity.