---
name: github-actions
description: Guide to the ElisDB CI/CD workflow (.github/workflows/android-release.yml) — jobs, triggers, secrets, permissions, caching. Use when editing or debugging the GitHub Actions pipeline.
---

# GitHub Actions (CI/CD)

## Workflow (verified)

- File: `.github/workflows/android-release.yml` — name `Android CI / Release`.
- Triggers: `push` to `main`, `push` of tags `v*`, `pull_request` to `main`, `workflow_dispatch`.

## Jobs

- `test` (always): checkout → `actions/setup-java` (Temurin 17) → `android-actions/setup-android`
  → `gradle/actions/setup-gradle` → `:app:testDebugUnitTest` → `:app:lintDebug` → `:app:assembleDebug`
  → uploads `app-debug` APK + test reports.
- `release` (only on `v*` tags, `needs: test`): decodes keystore from secret, `:app:assembleRelease`,
  uploads `app-release` APK, creates the GitHub Release via `softprops/action-gh-release`.

## Toolchain

- Java 17, Android SDK via `android-actions/setup-android` (AGP downloads the platform/build-tools it needs).
- Gradle wrapper is used as-is (`elisdb-android/gradlew`); `working-directory: elisdb-android`.

## Secrets

- Required for release: `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`,
  `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`.
- Referenced only via `${{ secrets.NAME }}` and mapped to the env vars the build reads
  (`KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`).
- The keystore is decoded to `$RUNNER_TEMP` and never uploaded as an artifact.

To configure (once, from a machine with the real keystore):

```bash
gh secret set ANDROID_KEYSTORE_BASE64 --repo NetNexusOrg/elisdb --body "$(base64 -w0 <keystore-path>)"
gh secret set ANDROID_KEYSTORE_PASSWORD --repo NetNexusOrg/elisdb
gh secret set ANDROID_KEY_ALIAS --repo NetNexusOrg/elisdb
gh secret set ANDROID_KEY_PASSWORD --repo NetNexusOrg/elisdb
```

`ANDROID_KEYSTORE_BASE64` is the base64 of the real `.jks`; the other three are the
keystore password, key alias, and key password. Use the actual values — never
commit them or put them in any file in the repo.

## Rules

- Use minimal permissions: `contents: read` globally; `contents: write` only on the `release` job.
- **Never print secrets to logs** (no `echo $SECRET`); never commit secrets to the workflow file.
- Do not cache or upload keystores, temp files with passwords, or decoded key material.
- Pin action versions to stable major tags; do not introduce arbitrary tool versions.
- If a run fails, inspect `gh run view <id> --log` and the artifact/test reports; do not
  blindly bump Gradle/AGP or disable steps to "fix" a failure.
