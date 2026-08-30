## Context

The db-server (change `db-server`, implemented in `../elisdb-server`) exposes an open FastAPI API. This change layers authentication on top. See proposal.md - Why for motivation; the required behavior is in specs/server-auth/spec.md. The server is Python + FastAPI + SQLite, single-file-oriented and minimal. Requirements that shape this design: signatures cover method + path + body hash + timestamp, an allowlist of public keys, stale/future timestamps rejected (anti-replay), and the health endpoint stays open.

## Goals / Non-Goals

**Goals:**
- A single private key embedded in the app signs every protected request; the server holds only the public key.
- Zero server-side session state: verification is stateless given the allowlist.
- Small, auditable auth layer that plugs into the existing FastAPI app without restructuring it.

**Non-Goals:**
- No per-user identity, roles, or multi-tenant auth.
- No TLS termination, token issuance, or OAuth-style flows.
- No changes to the Android app or its key embedding in this change.
- No key rotation automation beyond manual add/remove.

## Decisions

### D1: Signature scheme — ed25519 over a canonical string with body hash

Each protected request carries three headers: `X-Elisdb-Timestamp` (epoch seconds), `X-Elisdb-Signature` (hex/base64 ed25519 signature), and `X-Elisdb-Key` (optional key id so the allowlist can hold several keys). The signed message is: `METHOD\nPATH\nSHA256(body)\nTIMESTAMP`. The server recomputes it and verifies against each allowlisted public key. Rationale: covers method+path (binds the request), body hash (prevents tampering), and timestamp (enables anti-replay) per the spec. Alternatives considered: signing the raw body only (misses method/path binding, reusable across paths) and HTTP Message Signatures RFC (heavier, more spec surface than needed).

### D2: Allowlist stored in the same SQLite database

A `keys` table (`id TEXT PRIMARY KEY, public_key TEXT, created_at INTEGER`) persists trusted public keys (raw 32-byte ed25519 public key, base64-encoded). Rationale: the server already owns one SQLite file ("server owns all data management" principle) and keys survive restart for free. Alternatives considered: an on-disk JSON/YAML file (duplicating the persistence layer) and environment variables (not dynamically manageable).

### D3: Enforcement as a FastAPI dependency

All protected `/api/*` routes get an `auth` dependency that: (1) rejects missing/malformed headers with `401`, (2) verifies the timestamp is within a ±300-second window, (3) verifies the signature against the allowlist. `/health` stays outside it. Rationale: minimal intrusion into existing handlers, applied uniformly, easy to unit-test via FastAPI's dependency override. Alternatives considered: ASGI middleware (runs on every request including health; harder to test selectively) and per-handler checks (repetitive, error-prone).

### D4: Management surface — CLI and local-only endpoint

Key management is provided by a small CLI (`python -m app.keys add|remove|list <pubkey>`) backed by the same database, plus a `GET /api/keys` listing endpoint. Adding/removing keys goes through the CLI, which keeps the HTTP surface minimal. Rationale: operators manage keys, not clients. Alternatives considered: full CRUD HTTP endpoints for keys (widens the attack surface without need — the allowlist is an operator concern) and editing a file by hand (no validation).

### D5: `cryptography` dependency for ed25519

Use the `cryptography` package (`Ed25519PrivateKey`/`Ed25519PublicKey`, RFC 8032 raw keys). Rationale: battle-tested, pure-Python-usable, avoids hand-rolling signature code. Alternatives considered: `nacl`/PyNaCl (fine, but `cryptography` is the more common baseline) and stdlib-only (not possible — ed25519 is not in the Python stdlib).

### D6: Timestamp window of ±300 seconds

Reject requests whose timestamp is older than 5 minutes or ahead of the server clock by 5 minutes. Rationale: generous enough for clock skew and offline-first clients, tight enough to bound replay. Alternatives considered: no window (violates the anti-replay requirement) and a wider 24h window (long replay window).

## Risks / Trade-offs

- [A single embedded key shared by every app install means any extracted key authenticates fully] → Accepted per the chosen approach; the server is for private/local use, and the allowlist supports rotating the key by adding/removing entries.
- [Clock skew on the device can reject otherwise valid requests] → ±300s window absorbs realistic skew; documented so clients keep their clock sane.
- [Body hash must match exactly — clients re-encoding JSON can produce different bytes] → Protocol documents that the hash covers the exact bytes sent; verified against the same request body the server reads.
- [No auth on the health endpoint is by design] → Health remains an unauthenticated liveness probe only; it returns no data.

## Migration Plan

Breaking change for the db-server API: after deployment, `/api/*` requires signatures. Rollback: revert the auth dependency and remove the allowlist lookup; data and schema (keys table) remain compatible. Operators add the app's public key via the CLI before or at deployment.

## Open Questions

None.
