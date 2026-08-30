## Context

The app has no networking layer. The server exposes authed endpoints push/pull/delete over HTTP with an ed25519 request signature. See proposal.md and the `server-sync`/`server-auth` specs for the API contract.

## Goals / Non-Goals

**Goals:**
- Provide a thin mobile client transport: push, pull, delete-by-id, authed.
- Mirror server DTOs (PushRequest/PushResult, Cliente/Foto with base64).
- Sign every request per `server-auth`.

**Non-Goals:**
- Conflict handling, retry/queueing logic, deciding accepted vs stale (these belong to `client-sync-engine`).
- Implementing the server.

## Decisions

**1. Use Ktor client + kotlinx-serialization.**
Project already uses kotlinx-serialization. Rationale: idiomatic Android networking, coroutine/Flow-friendly, minimal VnO. Alternative: Retrofit + Moshi — heavier, less consistent with existing stack.

**2. Sign in an HttpClient plugin (request interceptor).**
Computes timestamp, SHA256(body), and builds the signature message for every non-health API call; injects `X-Elisdb-Timestamp`/`X-Elisdb-Signature`. Rationale: single place, matches `server-auth` "sign header+body". Key is embedded in app (BuildConfig or keystore-style secret per the server-auth choice).

**3. DTOs in a `remote/` package mirroring server models.**
`ClienteDto`, `FotoDto(dataB64)`, `PushRecord{PushDoc}`, `PushResult`, mapped to/from local entities by the sync layer, not this layer.

**4. Base URL via a build-config property.**
Default to a documented dev server; override per build flavor/env.

## Risks / Trade-offs

- **Secret key embedded in APK is extractable** → matches server-auth's accepted decision (single embedded key); rotation limited. Not mitigated here by design.
- **Base64 photos inflate request size** → acceptable at current scale; if large, uploads could stream later (out of scope).
- **401 handling** → surfaced as typed error so the sync layer can mark auth-failed rather than retry forever.