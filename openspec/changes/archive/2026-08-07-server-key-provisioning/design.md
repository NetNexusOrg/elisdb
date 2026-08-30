## Context

See `proposal.md`. The server (`elys-server`) already manages an allowlist via `python -m app.keys` (add/remove/list) and `GET /api/keys` on `Database.list_keys()`. The auth protocol and signing are unchanged. The gap is repeatable end-to-end provisioning and observability of an empty first-run allowlist.

## Goals / Non-Goals

- **Goals**: document/automate keypair generation → app embed → server registration; signal an empty allowlist on first run; document `ELISDB_DB`/`ELISDB_PHOTOS`.
- **Non-Goals**: changing the ed25519 protocol, auth headers, or conflict semantics; changing who holds the private key.

## Approach

1. **Keypair generation**: use the same ed25519 curve as the app. Document (or add a helper) that generates a keypair and prints the base64 public key plus the base64 private seed for the app build (`ELISDB_PRIVATE_KEY`).

2. **Server registration**: use the existing CLI `python -m app.keys add <id> <public_key_b64>` (backed by `Database.add_key`) against `ELISDB_DB`. Keep it the single path so the server-auth.allowlist semantics are unchanged.

3. **Empty-config observability**: in the keys read path (`Database.list_keys` / `GET /api/keys`), ensure the response distinguishes an empty allowlist; and in `keys list`/eg health, surface "no trusted keys configured" guidance for operators on first start.

4. **Env docs**: document `ELISDB_DB` and `ELISDB_PHOTOS`, and state that no key is trusted before the first `add`.

## Alternatives Considered

- Generating keys entirely server-side: rejected — the private key must live in the app build, not the server.
- Overloading the health endpoint with auth state: rejected — keep reading keys explicit to avoid leaking operator state.