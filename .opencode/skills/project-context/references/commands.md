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

## Server (run from `elisdb-server/`)

```bash
python3 -m venv .venv
.venv/bin/pip install -r requirements.txt
.venv/bin/uvicorn app.main:app --host 0.0.0.0   # run the server
.venv/bin/python -m pytest                      # run tests
python -m app.keys issue <user> --server-url <url>  # issue a device key (writes <user>.config.json)
```

Server env vars: `ELISDB_DB`, `ELISDB_PHOTOS`, `ELISDB_LOG_LEVEL`,
`ELISDB_MAX_PHOTO_BYTES`, `ELISDB_SERVER_URL`, `ELISDB_USERS`, `ELISDB_CONFIGS_DIR`.

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
git tag vX.Y.Z && git push origin vX.Y.Z   # triggers the CI release (see android-release)
```

## OpenSpec

```bash
openspec list                     # active changes
openspec validate <change>
openspec status --change <name>
```
