## Context

The app currently bakes a fallback server URL (`http://10.0.2.2:8000`) into `BuildConfig.SERVER_BASE_URL` when `ELISDB_SERVER_URL` is unset (`app/build.gradle.kts`), and the app README documents it as the default. The server's allowlist already starts empty and the only path to trust a key is `python -m app.keys add` — no built-in key exists in code. The drift to fix is the app side's implicit URL (and any future implicit key default), plus making the "no builtin trust" guarantee explicit.

## Goals / Non-Goals

- **Goals**: remove the app's baked-in `SERVER_BASE_URL` fallback; keep all signing-key config explicit (no default key value anywhere); guarantee the server trusts only operator-added keys; document that config is explicit.
- **Non-Goals**: changing runtime settings UI, key issuance, the ed25519 protocol, or auth headers; adding a re-provision store for the defaults.

## Approach

### 1. App build: no default server URL

In `app/build.gradle.kts`, change:

```kotlin
val serverBaseUrl = providers.environmentVariable("ELISDB_SERVER_URL").orNull ?: "http://10.0.2.2:8000"
```

to read without a fallback:

```kotlin
val serverBaseUrl = providers.environmentVariable("ELISDB_SERVER_URL").orNull ?: ""
```

Consistent with `signerPrivateKey` which already defaults to blank. Result: an unconfigured build ships a blank `SERVER_BASE_URL`; `ApiClientProvider.create()` already throws `ConfigException` for a blank URL/key, which the sync layer already surfaces as `SyncFault.CONFIG` — no new app-side logic needed.

### 2. Server: verify no built-in trust

No code change is required server-side for behaviour (the allowlist is already empty and `get_all_public_keys()` returns only stored keys). This change adds spec/verification coverage:

- Confirm a fresh DB authenticates no requests (covered in tests).
- Confirm only keys passed through `python -m app.keys add` (or `Database.add_key`) authenticate.

### 3. Documentation

- `elisdb/README.md`: remove the `http://10.0.2.2:8000` default row/language; state that `SERVER_BASE_URL` and `SIGNER_PRIVATE_KEY_B64` are blank unless explicitly configured via `ELISDB_SERVER_URL` / `ELISDB_PRIVATE_KEY` at build time.
- `elisdb-server/README.md`: state there is no default key; the server trusts only keys added via `python -m app.keys add`; first-run guidance already documented.

## Alternatives Considered

- Keeping a dev-only default behind a `debug` build type: rejected — a default in one build type is still baked-in behaviour and the requirement is that the app holds no built-in URL.
- Moving defaults to `gradle.properties`: rejected — same implicit default, just relocated.
- Removing support for `ELISDB_SERVER_URL` at build time and requiring runtime-only config: rejected — build-time config is documented and useful for reproducible release builds; this change only removes the implicit fallback.