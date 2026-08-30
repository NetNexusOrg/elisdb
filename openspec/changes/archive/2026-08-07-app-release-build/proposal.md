## Why

The app cannot currently be shipped as a release: signing is only enabled when the environment variables happen to be present, minification/R8 is enabled but R8 rules are not verified against the new networking stack (Ktor, eddsa, kotlinx-serialization), the base URL and signing key are baked into the build from environment variables with no configuration story, versionCode/versionName are hard-coded, and there is no way to produce and validate installable release artifacts reproducibly.

## What Changes

- Make release signing explicit and reproducible: load keystore details from a non-version-controlled file/env with clear failure when absent.
- Replace hard-coded `versionCode = 1` / `versionName = "1.0"` with versioning derived from a single source (gradle properties or CI), keeping release alignment with the server.
- Verify and complete R8/ProGuard keep rules for the networking stack (Ktor client/CIO, kotlinx-serialization-generated serializers, eddsa, WorkManager/CoroutineWorker) so `assembleRelease` produces a working, minified app.
- Keep `SERVER_BASE_URL` and the signing key configurable per-build (environment/properties) with clear failure messaging when needed.
- Add a documented, reproducible release build step (e.g. `./gradlew assembleRelease`) and a validation pass against a real device/emulator.

## Constraints

- No source- or target-breaking API changes to the sync engine; this is build/configuration hardening.
- Secrets (keystore, private key) must NOT be committed to the repository.

## Impact

- `app/build.gradle.kts` — signing config, versioning, BuildConfig field provisioning.
- `app/proguard-rules.pro` — R8 rules for ktor/eddsa/serialization/WorkManager.
- Globally version catalog (`gradle/libs.versions.toml`) if versions are centralized.
- Release build validation (device/emulator) and possibly CI workflow.
- `README` / ops documentation for producing and validating a release.