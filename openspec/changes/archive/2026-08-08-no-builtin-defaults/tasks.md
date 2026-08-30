## 1. Remove app built-in server URL

- [x] 1.1 Change `app/build.gradle.kts` so `SERVER_BASE_URL` has no fallback default (`ELISDB_SERVER_URL` unset → blank).
- [x] 1.2 Confirm `SIGNER_PRIVATE_KEY_B64` remains blank when `ELISDB_PRIVATE_KEY` is unset (no default key).

## 2. Verify app unconfigured behaviour

- [x] 2.1 Confirm the app built without `ELISDB_SERVER_URL`/`ELISDB_PRIVATE_KEY` raises `ConfigException` from `ApiClientProvider.create()` (blank URL/key → `SyncFault.CONFIG`), with no implicit host targeted.

## 3. Server no-builtin-trust guarantee

- [x] 3.1 Add tests proving a fresh server DB authenticates no requests (no default/generated key trusted).
- [x] 3.2 Confirm only keys added via `python -m app.keys add` / `Database.add_key` authenticate.

## 4. Documentation

- [x] 4.1 Update `elisdb/README.md`: remove the `http://10.0.2.2:8000` default; document that `SERVER_BASE_URL`/`SIGNER_PRIVATE_KEY_B64` are blank unless explicitly configured.
- [x] 4.2 Update `elisdb-server/README.md`: state there is no default key and only operator-added keys are trusted.

## 5. Build and regressions

- [x] 5.1 Build the Android app (debug) and run the app tests — no regressions from removing the default.
- [x] 5.2 Run the server test suite — auth tests green, empty allowlist rejects everything.