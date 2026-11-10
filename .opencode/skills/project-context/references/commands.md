# ElisDB — Commands

Verified command reference. Run from the directory shown.

## Android (run from `elisdb-android/`)

```bash
./gradlew :app:testDebugUnitTest          # local unit tests
./gradlew :app:connectedDebugAndroidTest  # instrumented tests (device/emulator)
./gradlew :app:lintDebug                  # lint
./gradlew :app:assembleDebug              # debug APK
./gradlew :app:assembleRelease            # signed release (needs KEYSTORE_* env vars)
./scripts/build-release.sh                # local signed release (reads ~/.keystores/elisdb-release.env)
```

Release APK: `app/build/outputs/apk/release/app-release.apk`.

### Machine setup (android CLI)

SDK/AVD managed with the official `android` CLI (no Android Studio required);
SDK defaults to `~/Android/Sdk` on Linux — `local.properties` already points there:

```bash
android sdk install platforms/android-37.0 build-tools/37.0.0 platform-tools
android sdk install system-images/android-37.0/google_apis/x86_64   # instrumented tests
android emulator create medium_phone    # creates an AVD from a profile
android emulator start medium_phone     # blocks until ready (e.g. emulator-5554)
android emulator stop medium_phone
```

`~/.bashrc`: `export ANDROID_HOME="$HOME/Android/Sdk"` and
`export PATH="$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator"`.

## Server (run from `elisdb-server/`)

```bash
docker compose up -d --build                         # build + start the server
docker compose logs -f                               # follow logs
docker compose down                                  # stop (keeps the data volume)
curl http://localhost:8000/health                    # liveness check
docker compose exec elisdb-server python -m app.keys issue <user> --server-url <url>  # issue a device key (writes <user>.config.json)
```

Tests (needs a venv with `pip install -r requirements-dev.txt`; the server itself is Docker-only):

```bash
python -m pytest                                     # server suite (from elisdb-server/)
```

Server env vars: `ELISDB_DB`, `ELISDB_PHOTOS`, `ELISDB_LOG_LEVEL`,
`ELISDB_MAX_PHOTO_BYTES`, `ELISDB_SERVER_URL`, `ELISDB_USERS`, `ELISDB_CONFIGS_DIR`,
`ELISDB_RATE_LIMIT_REQUESTS`, `ELISDB_RATE_LIMIT_WINDOW_SECONDS`,
`ELISDB_TRUST_PROXY`, `ELISDB_MAX_PUSH_RECORDS`, `ELISDB_MAX_PHOTOS_PER_CLIENT`,
`ELISDB_TOMBSTONE_RETENTION_DAYS`, `ELISDB_AUDIT_RETENTION_DAYS`,
`ELISDB_PHOTO_GC_INTERVAL_SECONDS`, `ELISDB_BIND_ADDRESS` (compose only).

## Docker (server)

```bash
docker compose up -d --build   # from elisdb-server/
```

## Git / GitHub

```bash
git status && git diff          # inspect before committing
gh auth status                  # GitHub authentication
gh run list                     # CI runs
gh repo view NetNexusOrg/elisdb --json visibility
```

## Release

```bash
git tag vX.Y.Z && git push origin vX.Y.Z   # triggers the CI release (see android-ci.yml)
```

## OpenSpec

```bash
openspec list                     # active changes
openspec validate <change>
openspec status --change <name>
```
