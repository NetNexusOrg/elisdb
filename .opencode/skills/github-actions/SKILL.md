---
name: github-actions
description: Guide to the ElisDB CI/CD workflow (.github/workflows/android-release.yml) — jobs, triggers, secrets, permissions, caching. Use when editing or debugging the GitHub Actions pipeline.
---

# GitHub Actions (CI/CD)

## Workflow (verified)

- File: `.github/workflows/android-release.yml` — name `Android CI / Release`.
- Triggers: `push` to `main`, `push` of tags `v*`, `pull_request` to `main`, `workflow_dispatch`.
- File: `.github/workflows/server-ci.yml` — name `Server CI`. Runs the server `pytest`
  suite on `push`/`pull_request` to `main` when `elisdb-server/**` changes (path-filtered)
  and on `workflow_dispatch`.

## Jobs

- `test` (always): checkout → `actions/setup-java` (Temurin 17) → `android-actions/setup-android`
  → `gradle/actions/setup-gradle` → `:app:testDebugUnitTest` → `:app:lintDebug` → `:app:assembleDebug`
  → uploads test reports **only when the job fails** (3-day retention, best effort).
- `instrumented` (always, **release gate**): boots an emulator (API 34) and runs only
  `AppDatabaseMigrationTest`.
- `instrumented-suite` (always, **non-blocking**, `continue-on-error: true`): runs the
  full `:app:connectedDebugAndroidTest` suite on the emulator for visibility. It is not
  in `release.needs`, so a flaky UI test cannot block a release.
- `release` (only on `v*` tags, `needs: [test, instrumented]`): extracts the release body from
  `CHANGELOG.md` (step "Extract release notes": the `## [X.Y.Z]` section matching the tag;
  a tag without that section fails before the build), decodes keystore from secret,
  `:app:assembleRelease`,
  renames the APK to `elisdb-<versionName>-<versionCode>.apk` (`APK_NAME` in `$GITHUB_ENV`),
  creates the GitHub Release via `softprops/action-gh-release` with `body_path` (the
  extracted notes — never `generate_release_notes`, which appends a "Full Changelog" link),
  then publishes the same file to the server with `rsync` (step "Publish APK to the server";
  skipped with a warning when the `DEPLOY_*` secrets are absent, and always skipped
  without `DEPLOY_KNOWN_HOSTS`).
  The APK is **not** uploaded as a workflow artifact: Release assets have their own
  storage and do not consume the Actions artifact quota.

## Deploy guard (no branch protection)

`server-ci.yml` also has a `docker` job ("Docker image"): `docker build` plus an
import of the runtime modules inside the image, so a missing `requirements.txt`
entry or a broken Dockerfile fails on the push instead of on the VPS.

The repository is private on a plan without branch protection (required status
checks answer 403 "Upgrade to GitHub Pro or make this repository public"), so the
production-side guard is `scripts/deploy-server.sh`: it refuses to deploy unless
`elisdb-server/` is clean, local `main` equals `origin/main`, and the last commit
touching `elisdb-server/` has a completed, successful **Server CI** run. It then
rsyncs the code (never `compose.yaml`, which is host-specific) and runs
`docker compose up -d --build` on the VPS. `--check` verifies without deploying.

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

Deployment to the server (optional, step no-ops with a warning when missing):
`DEPLOY_SSH_KEY` (private ed25519 key), `DEPLOY_HOST` (a name or IP the runner can
resolve — local `~/.ssh/config` aliases do not exist there), `DEPLOY_USER`,
`DEPLOY_KNOWN_HOSTS` (`ssh-keyscan -H <host>` output) and optionally
`DEPLOY_PORT` (default 22), `DEPLOY_PATH` (default
`docker/elisdb-server/elisdb-data/apk/latest/`, relative to the deploy user's
home). `DEPLOY_KNOWN_HOSTS` is **required** for publication: without it the step
skips with a warning rather than trusting a runtime `ssh-keyscan`. Use a
dedicated key, not a personal one.
When the key is restricted with
`restrict,command="rrsync -wo <dir>"`, the path sent by rsync is **relative** to
`<dir>`, so set `DEPLOY_PATH=/`; an absolute path would be concatenated
(`<dir>/home/...`).

## Rules

- Use minimal permissions: `contents: read` globally; `contents: write` only on the `release` job.
- **Never print secrets to logs** (no `echo $SECRET`); never commit secrets to the workflow file.
- Do not cache or upload keystores, temp files with passwords, or decoded key material.
- Pin action versions to stable major tags; do not introduce arbitrary tool versions.
- If a run fails, inspect `gh run view <id> --log` and the artifact/test reports; do not
  blindly bump Gradle/AGP or disable steps to "fix" a failure.
