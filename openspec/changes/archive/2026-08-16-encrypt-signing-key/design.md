## Context

`PrefsSettingsStore` stores the base URL and signing key in a plain `SharedPreferences` file. The signing key is the secret authenticating against the server. On a lost or rooted device the file can be read, exposing the key until it is revoked server-side.

## Goals / Non-Goals

**Goals:**
- The signing key (and base URL) are encrypted at rest using `EncryptedSharedPreferences` (AndroidX Security), backed by a key in the Android Keystore.
- Existing installed configurations keep working after the upgrade.

**Non-Goals:**
- Changing the `SettingsStore` interface or the sync/API layer.
- Encrypting the other prefs stores (`ClockOffsetStore`, `PhotoPushState`) — non-sensitive metadata.

## Decisions

**D1 — Use `EncryptedSharedPreferences`.**
- `PrefsSettingsStore` switches to `EncryptedSharedPreferences.create(...)` with `AES256_SIV` (default) and the master key from `MasterKey`/Keystore.
- *Rationale*: the standard AndroidX approach; minimal code change, keys held in the hardware/software Keystore.
- *Alternative considered*: rolling our own AES with a Keystore key — rejected as more error-prone; the library is the ecosystem default.

**D2 — Migrate existing plain-text values.**
- On construction, if the old plain `SharedPreferences` file has values, read them and save them into the encrypted store, then clear the plain file.
- *Rationale*: users upgrading from a previous release keep their configured server without re-importing.
- *Note*: `android:allowBackup=false` is already set, so the encryption key not being backup-restorable is fine.

**D3 — Add the dependency.**
- Add `androidx.security:security-crypto` to `libs.versions.toml` and `app/build.gradle.kts`.

**D4 — Files touched.**
- `settings/SettingsStore.kt` (PrefsSettingsStore → encrypted), `gradle/libs.versions.toml`, `app/build.gradle.kts`, `SettingsStoreTest`/`SettingsViewModelTest` (still pass via the interface), a migration test.

## Risks / Trade-offs

- [EncryptedSharedPreferences is deprecated in newer AndroidX Security releases] → Still the standard stable approach; the data is small and access is infrequent, so overhead is negligible. If the project's AGP level requires a newer artifact, use the maintained fork/version accordingly.
- [Migration could fail on a fresh install] → No legacy values exist; the store just starts empty (no migration needed).

## Migration Plan

Ship as an app update; existing config migrates transparently on first access.

## Open Questions

None.
