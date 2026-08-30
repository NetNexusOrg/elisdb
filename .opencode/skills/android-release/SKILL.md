---
name: android-release
description: How to produce an Android release for ElisDB (versioning, signing, APK, tags, GitHub Releases). Use when preparing or shipping a release, or touching versionCode/versionName/signing.
---

# Android Release

Base facts in `project-context` and `gradle-build`.

## Versioning (verified)

- Version lives in `elisdb-android/gradle.properties`: `app.versionCode` (monotonic int) and `app.versionName` (SemVer-ish).
- Release strategy is Semantic Versioning `MAJOR.MINOR.PATCH`; a release tag is `vX.Y.Z` and must match `app.versionName`.
- Bump `versionCode` and `versionName` **only when a release is intended** and only after confirming with the user; never bump as a side effect.

## Artifacts

- APK output: `elisdb-android/app/build/outputs/apk/release/app-release.apk`.
- Local signed build helper: `elisdb-android/scripts/build-release.sh` (reads `~/.keystores/elisdb-release.env`).

## Signing (never in Git)

- Keystore/passwords come from `keystore.properties` (git-ignored) or env vars
  `KEYSTORE_PATH` / `KEYSTORE_PASSWORD` / `KEY_ALIAS` / `KEY_PASSWORD`.
- **Never commit** `*.jks`, `*.keystore`, `keystore.properties`, or passwords.
- **Never generate or replace a signing key without explicit confirmation** — losing the key strands existing installs (they can no longer update).
- CI signs via GitHub Secrets (`ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`); do not invent or change these values.

## Release flow

1. Bump `versionCode`/`versionName` in `gradle.properties`.
2. Run local checks: `./gradlew :app:testDebugUnitTest` (and lint).
3. Build the signed APK locally to verify signing works (`scripts/build-release.sh`).
4. Commit, push to `main`.
5. Tag the release: `git tag vX.Y.Z && git push origin vX.Y.Z`.
6. CI builds the signed APK and creates the GitHub Release automatically (see `github-actions`).

## Rules

- Do not create production tags just to test CI; test via the non-tag `test` job.
- Release notes are auto-generated from commits; do not put secrets or photo data in them.
