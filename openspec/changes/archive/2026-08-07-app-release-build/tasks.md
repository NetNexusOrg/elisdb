## 1. Versioning

- [x] 1.1 Move `versionCode`/`versionName` out of hardcoded values into a single source (`gradle.properties` and/or CI env) and wire `defaultConfig` to read them.
- [x] 1.2 Add a failing test/log path proving `BuildConfig`/`applicationId` version name is exposed (verify via `PackageInfo.versionName` if applicable).

## 2. Release signing

- [x] 2.1 Introduce a git-ignored `keystore.properties` (KEYSTORE_PATH/KEYSTORE_PASSWORD/KEY_ALIAS/KEY_PASSWORD) with env fallback, and register the `release` signing config from it.
- [x] 2.2 Make the release build FAIL clearly when signing credentials are missing instead of producing an unsigned artifact.
- [x] 2.3 Add `keystore.properties` and keystore files to `.gitignore`; confirm nothing secret is tracked.

## 3. R8 / ProGuard rules for the networking stack

- [x] 3.1 Add keep rules for the Ktor client stack (ktor-client, CIO engine, content-negotiation, kotlinx-serialization serializers/DTOs).
- [x] 3.2 Add keep rules for the eddsa (ed25519) signing classes.
- [x] 3.3 Add keep rules for WorkManager `CoroutineWorker` subclasses (e.g. `SyncWorker`).
- [x] 3.4 Verify/complete the existing rules for app `@Serializable` DTOs and generated serializers.

## 4. Server configuration provisioning

- [x] 4.1 Keep `SERVER_BASE_URL` and `SIGNER_PRIVATE_KEY_B64` configurable per-build via env/properties into `BuildConfig`, and document them (including the default emulator URL).
- [x] 4.2 Document how the configured signing key maps to a public key registered on the server.

## 5. Reproducible release build and validation

- [x] 5.1 Document and run `./gradlew clean assembleRelease` producing a signed APK.
- [x] 5.2 Install the release APK on a real device/emulator and smoke-test sync, maps, photos and persistence.