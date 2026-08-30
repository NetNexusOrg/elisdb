## Context

See `proposal.md`. `app/build.gradle.kts` currently: enables minification in `release` with an R8 rules file that has no rules for the new networking stack; signs only when `KEYSTORE_PATH`/`KEYSTORE_PASSWORD`/`KEY_ALIAS`/`KEY_PASSWORD` env vars are set (silently producing an unsigned build otherwise); hardcodes `versionCode = 1`/`versionName = "1.0"`; and pumps `SERVER_BASE_URL` (default `http://10.0.2.2:8000`) and `SIGNER_PRIVATE_KEY_B64` (default empty) into `BuildConfig`.

## Goals / Non-Goals

- **Goals**: a reproducible signed minified release whose versioning, signing and server/app configuration are explicit; R8 rules verified for ktor/eddsa/serialization/WorkManager; a documented `assembleRelease` + validation run.
- **Non-Goals**: altering the sync/serialization contracts, changing runtime behavior, or migrating build systems; enabling Play AAB signing/config (out of scope unless requested).

## Approach

1. **Signing**: move keystore resolution to a `keystore.properties` file (git-ignored) with the four fields, plus env fallback. `registerSigningConfig` of `EXTENSION` should absent default to set the build FAIL (fail-fast) rather than skip — implement by checking config presence inside the `release` build type and throwing, or by only building `release` when credentials are configured. Add `keystore.properties` and `*.keystore` to `.gitignore`.

2. **Versioning**: introduce `versionCode`/`versionName` from a single source — read from `gradle.properties` (e.g. `app.versionCode`/`app.versionName`) or from an env/CI var, so the value lives in one place.

3. **R8/rules**: add keep rules for Ktor (client, CIO, content-negotiation, kotlinx-serialization), the eddsa library classes, and WorkManager `CoroutineWorker` subclasses (e.g. `SyncWorker`). `-keep` the app's `@Serializable` DTOs and generated serializers (may already be covered by existing `com.example.elisdb.**` rules — verify). Then run `./gradlew assembleRelease` and a smoke test.

4. **Config provisioning**: keep `SERVER_BASE_URL`/`SIGNER_PRIVATE_KEY_B64` via `BuildConfig`, sourced from env/properties; document them for the server's registered key. Keep the existing env defaults but ensure an empty key still fails loudly (already surfaced to sync status in `client-sync-status-ui`).

5. **Validation**: document and run `./gradlew clean assembleRelease`, install on a real device/emulator, verifying the release behaves like debug (sync, maps, photos).

## Alternatives Considered

- Continuing to rely solely on env vars for signing: rejected — silent unsigned releases are the failure mode this change removes.
- Hardcoded versions: rejected — source mismatch.
- Keeping minification off: rejected — release should ship minified.