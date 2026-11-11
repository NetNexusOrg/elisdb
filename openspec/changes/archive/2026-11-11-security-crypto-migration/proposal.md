# Proposal: Migrate settings storage from security-crypto to Android Keystore

## Why

The app stores the device signing key in `EncryptedSharedPreferences` from
`androidx.security:security-crypto`, which is deprecated (Jetpack Security was
retired). The library still works, but it no longer receives security fixes.
Replacing it with a small Android Keystore AES-GCM implementation removes the
deprecated dependency without changing the `SettingsStore` contract.

## What Changes

- Add `KeystoreSettingsStore`: AES-256-GCM with a key in the Android Keystore,
  storing `v1:<iv>:<ciphertext>` values in its own preferences file.
- On first use, migrate existing data from the legacy plaintext keys and from
  the old `EncryptedSharedPreferences` store. New values are written and read
  back before the legacy copies are cleared; any failure keeps the legacy data
  readable and writes fail closed.
- Point `AppContainer` at the new store.
- Update the migration documentation and the Android/security skills.
- Keep `security-crypto` only for reading legacy data; it is no longer used for
  new writes.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

None. This is an internal storage implementation change with the same
`SettingsStore` behavior (no requirement-level change).

## Impact

- New: `app/src/main/java/com/example/elisdb/settings/KeystoreSettingsStore.kt`,
  `app/src/androidTest/java/com/example/elisdb/settings/KeystoreSettingsStoreTest.kt`.
- Modified: `AppContainer.kt`, `docs/migracao-security-crypto.md`, skills.
- No server, API, database schema, version or `CHANGELOG` changes.
