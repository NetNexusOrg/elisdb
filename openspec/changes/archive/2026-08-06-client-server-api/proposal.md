## Why

The app must exchange data with the server's push/pull API, but it currently has no HTTP networking layer. For offline-first operation the app needs a client that talks to the server endpoints, marshals the push/pull payloads (including base64 photos), and authenticates every request with the server's ed25519 signature scheme.

## What Changes

- Add an HTTP client that calls `POST /api/push`, `GET /api/pull`, and `DELETE /api/clients/{id}`.
- **BREAKING:** The app embeds an ed25519 private key and signs every API request (timestamped `METHOD\nPATH\nSHA256(body)\nTIMESTAMP` message, ±300s window), matching the server's auth contract. The matching public key is registered on the server.
- DTOs mirror the server models: `Cliente`, `Foto` with `dataB64`, `PushRecord`, `PushResult` (statuses `accepted`/`duplicate`/`stale`), `PullItem`.
- Photos are base64-encoded when pushed and decoded when pulled using the app-private photo files.
- Server base URL is configurable (e.g., `BuildConfig`/env property with a sensible default).

## Capabilities

### New Capabilities
- `client-sync`: the client-side capability to connect to the server's push/pull data API and exchange client and photo records (this proposal covers the transport/DTO/auth layer).

### Modified Capabilities
<!-- The app's data behavior (client-data) is unchanged by this layer; only transport is added. -->

## Impact

- `app/build.gradle.kts` — add HTTP client + JSON (Ktor/Retrofit + kotlinx-serialization), ed25519 (e.g. `org.bouncycastle` or java.security + jose4j).
- New package `.../data/remote/` — API DTOs, HTTP client, authenticator/signer.
- `app/src/main/AndroidManifest.xml` — network security config (cleartext for dev if http), INTERNET permission already present.
- `app/src/main/java/com/example/elisdb/ElisdbApp.kt` — wire repository/service construction.
- No changes to `client-data` behavior/specs.