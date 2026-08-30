## 1. Runtime config store

- [x] 1.1 Add a `SettingsStore` (SharedPreferences-backed) holding server base URL and signing-key seed, seeded from `BuildConfig.SERVER_BASE_URL`/`SIGNER_PRIVATE_KEY_B64` when no saved value exists.
- [x] 1.2 Expose `settings()` (current effective values) and `save(baseUrl, key)` on `SettingsStore`, persisting blank values verbatim.

## 2. Config resolution

- [x] 2.1 Change `ApiClientProvider.create()` to resolve the base URL and key at runtime: saved override when present, else `BuildConfig` fallback.
- [x] 2.2 Keep `ConfigException` raising for blank resolved base URL or signing key so the sync layer maps them to `SyncFault.CONFIG` unchanged.

## 3. Apply configuration on save

- [x] 3.1 Wire `SettingsStore` into `AppContainer` and rebuild the API client/signer when settings change (config-aware holder or re-invocable provider).
- [x] 3.2 Add the save action that persists settings and triggers `SyncManager.syncNow()` so connectivity against the new server is attempted right away.

## 4. Settings screen + navigation

- [x] 4.1 Add a `Settings` navigation destination and an entry point from the client list screen (settings action) and from the config-fault state.
- [x] 4.2 Add `SettingsScreen` + ViewModel: two fields (base URL, signing key) pre-filled from current settings, save/back actions, inline save confirmation, and a note that the matching public key must be registered on the server after a key change.

## 5. Home screen message removal

- [x] 5.1 Remove the idle "Não sincronizado" mapping in `ClientListScreen.syncMessage` so an idle state renders no config-gap message.
- [x] 5.2 Replace the CONFIG fault message with a prompt/entry point that opens Settings instead of showing the missing-config error text.
- [x] 5.3 Keep the existing NETWORK/AUTH messages and the manual sync trigger intact.

## 6. Tests and verification

- [x] 6.1 Add unit tests for `SettingsStore` seeding, override, and blank persistence.
- [x] 6.2 Add tests for `ApiClientProvider` resolution (runtime override wins over BuildConfig; blank resolved config throws).
- [x] 6.3 Add/adjust UI tests for the client list screen: idle shows no "Não sincronizado", CONFIG fault shows a settings prompt, and a manual sync still works.
- [x] 6.4 Build the app and run the test suite to confirm no regressions.