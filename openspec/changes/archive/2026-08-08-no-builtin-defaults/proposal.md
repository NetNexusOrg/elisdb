## Why

Configuration is drifting back toward baked-in values: the Android build falls back to a hard-coded server URL (`http://10.0.2.2:8000`) when `ELISDB_SERVER_URL` is unset, and the app README advertises that default. No key or URL should be "builtin" in either the app or the server — the server must only trust keys an operator explicitly adds, and the app must only talk to a server it was explicitly given. Everything else should stay unconfigured until manually set.

## What Changes

- **App: no default server URL.** Remove the `http://10.0.2.2:8000` fallback in `app/build.gradle.kts`; an unset `ELISDB_SERVER_URL` yields a blank `SERVER_BASE_URL` instead of an implicit default. Update the README to remove the documented default.
- **App config is explicit.** With no server URL or signing key baked in, an app that is not configured fails sync locally with a configuration fault (already surfaced as `SyncFault.CONFIG`) instead of silently targeting an implicit host.
- **Server: no built-in trust keys.** The server SHALL NOT ship with or implicitly trust any default/test key; only keys added by an operator through `python -m app.keys add` are trusted. Add/gated by the existing empty-allowlist path (already implemented) and confirmed by tests.
- **(Scoped out)** changing runtime settings UI — `add-settings-page` handles on-device configuration; this change only removes baked-in defaults so configuration must be explicit (build-time env for the app, `app.keys add` for the server).

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `app-build`: build-time server configuration must not embed an implicit default URL; unconfigured builds must be blank (no fallback).
- `server-auth`: guarantee that only operator-added keys are trusted and no default/built-in key is accepted.

## Impact

- `app/build.gradle.kts` — `serverBaseUrl` reads env var without fallback (blank default).
- `app/src/main/java/com/example/elisdb/remote/ApiClientProvider.kt` — already raises `ConfigException` on blank; unchanged behaviour, now reachable because there is no built-in default.
- `openspec/specs/app-build` and `openspec/specs/server-auth` — requirement/language updates.
- `elisdb-server/app/keys.py` — `app.keys add` remains the single trust path; no default key provisioning.
- README (both app and server) — remove advertised default URL/keys; document that config is explicit.
- Server tests — confirm no key is trusted on a fresh DB and only added keys authenticate.