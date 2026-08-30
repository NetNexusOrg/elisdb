---
name: gradle-build
description: Gradle/AGP build configuration for the ElisDB Android project. Use when changing build files, running builds, troubleshooting Gradle, or touching versions/modules in elisdb-android/.
---

# Gradle Build

## Versions (verified — do not bump without being asked)

- Gradle Wrapper 9.1.0 (`gradle/wrapper/gradle-wrapper.properties`, SHA-256 pinned).
- Android Gradle Plugin 9.0.1, Kotlin 2.3.20, KSP 2.3.10 (see `gradle/libs.versions.toml`).
- Java 17 toolchain (`jvmToolchain(17)`); the foojay resolver auto-provisions JDK 17.
- App version is in `gradle.properties` (`app.versionCode`, `app.versionName`), NOT in `build.gradle.kts`.

## Structure

- Single module `:app`. Top-level `build.gradle.kts` only declares plugin aliases.
- `settings.gradle.kts`: `rootProject.name = "elisdb"`, `include(":app")`,
  `RepositoriesMode.FAIL_ON_PROJECT_REPOS` (repositories are centralized).
- `gradle/libs.versions.toml` is the version catalog — all deps/plugins are aliased.
- No product flavors. Two build types: `debug`, `release` (`isMinifyEnabled = true` + ProGuard).

## Common tasks (run from `elisdb-android/`)

```bash
./gradlew :app:assembleDebug          # debug APK
./gradlew :app:assembleRelease        # signed release APK (needs signing env vars)
./gradlew :app:testDebugUnitTest      # unit tests
./gradlew :app:connectedDebugAndroidTest  # instrumented tests
./gradlew :app:lintDebug              # lint
```

## Release build notes

- Release signing is resolved from `keystore.properties` (git-ignored) or env vars
  `KEYSTORE_PATH` / `KEYSTORE_PASSWORD` / `KEY_ALIAS` / `KEY_PASSWORD`.
- `app/build.gradle.kts` fails fast if a release task runs without signing configured
  (no unsigned release artifact is ever produced).
- `ELISDB_SERVER_URL` env var is embedded into `BuildConfig.SERVER_BASE_URL` at build time.

## Rules

- Never upgrade Gradle, AGP, Kotlin, or dependencies on your own initiative.
- Never edit `gradle-wrapper.jar` or regenerate the wrapper without being asked.
- Add new dependencies through the version catalog (`libs.versions.toml`), not inline strings.
- `local.properties` (`sdk.dir`) is machine-specific and git-ignored — never commit it.
