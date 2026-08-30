## 1. Dependencies & DTOs

- [x] 1.1 Add Ktor client and kotlinx-serialization deps to `app/build.gradle.kts`
- [x] 1.2 Add remote DTOs mirroring the server: `ClienteDto`, `FotoDto` (id, mime, `dataB64`, ordem), `PushRecordDto`, `PushResult` (status accepted/duplicate/stale)
- [x] 1.3 Add request/response serializers for the push/pull payloads and the delete path

## 2. Authentication

- [x] 2.1 Embed the ed25519 private key per the server-auth decision (BuildConfig secret)
- [x] 2.2 Implement `Signer` producing the auth headers (`X-Elisdb-Timestamp`, `X-Elisdb-Signature`) over `METHOD\nPATH\nSHA256(body)\nTIMESTAMP`
- [x] 2.3 Wire the signer into an HTTP client interceptor for authed endpoints

## 3. API client

- [x] 3.1 Implement `push(records)` → `List<PushResult>`
- [x] 3.2 Implement `pull()` → `List<ClienteDto>` with photos decoded from base64
- [x] 3.3 Implement `delete(id)` against the delete endpoint
- [x] 3.4 Surface 401 as a typed auth error
- [x] 3.5 Wire the base URL (build-config property / default) into the client

## 4. Tests & verification

- [x] 4.1 Unit test the signer message/header format against a reference vector
- [x] 4.2 Offline/mock test: push returns per-record statuses
- [x] 4.3 Update Android manifest/network security for the dev server
- [x] 4.4 Run Android unit tests