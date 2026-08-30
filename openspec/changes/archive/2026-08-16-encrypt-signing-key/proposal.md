## Why

The app stores the ed25519 signing key (the secret that authenticates against the server) in `SharedPreferences` in plain text (`PrefsSettingsStore.save`). On a device that is lost, rooted, or whose app data is readable, the key can be extracted and used to impersonate the user until the key is revoked server-side. The user identified this as a real scenario (lost device) for their small team.

## What Changes

- The signing key (and server base URL) are stored with `EncryptedSharedPreferences` from AndroidX Security, so the secret is encrypted at rest with a key held in the Android Keystore.
- Existing stored values migrate to the encrypted store on first read/write (the plain-text prefs are cleared).

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `client-settings`: the stored server connection (base URL + signing key) is encrypted at rest.

## Impact

- **App**: `PrefsSettingsStore` uses `EncryptedSharedPreferences`; add the `androidx.security:security-crypto` dependency; migrate/clear the old plain-text prefs.
- **Behavior**: existing configurations keep working after upgrade (values are read from the old store and re-saved encrypted); new saves are encrypted.
- **Tests**: `SettingsStoreTest` and `SettingsViewModelTest` continue to pass (they use the same interface); a migration test for the plain→encrypted transition.
