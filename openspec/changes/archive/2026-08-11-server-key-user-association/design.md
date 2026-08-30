## Context

The server authenticates requests by verifying the ed25519 signature against every public key in the allowlist (`auth.py`, `main.py:require_auth`); currently it returns a boolean pass/fail and cannot say *which* key authenticated. Keys live in SQLite with `id, public_key, comment, created_at`; `issue <user-name>` already stores the name in `comment`. See proposal.md - Why.

## Goals / Non-Goals

**Goals**
- Expose the issuing key holder's identity (`GET /api/keys/me`) so the app can display the associated username.
- Make a user name mandatory when the server issues a key.

**Non-Goals**
- No change to the request signing format.
- No multi-user authorization model or per-key permissions — this is identity lookup, not authorization.
- No UI changes in this change (handled by `app-show-imported-key-user`).

## Decisions

**D1: `require_auth` resolves and stores the authenticated key, not just pass/fail.**
Currently `require_auth` returns early on success. Change it to resolve the matching allowlist record and attach it (e.g. `request.app.state` or a request-scoped value) so the `me` endpoint can read `comment`/`id`. Alternative considered: re-verify in the endpoint — rejected (duplicated work and a second security-sensitive code path).

**D2: Add `Database.get_key_by_public_key(public_key)` instead of scanning.**
`get_all_public_keys()` today returns bare strings. Add a lookup that returns the key record (`id`, `public_key`, `comment`) for the one matching key. Alternative: extend `require_auth` to iterate full records — acceptable but keeps the list loaded; a single lookup is cleaner.

**D3: `GET /api/keys/me` returns `{key_id, username}` where username may be null.**
The response includes the key id always; `username` is null when no `comment` is stored (externally registered keys). This matches the "Unknown key identity" scenario without inventing a sentinel value.

**D4: Refuse blank user names in `keys.py issue`.**
`issue` already takes `<user-name>` as a positional arg; add a guard (and same for a blank `add` comment path is left permissive — external registration still allows no comment). This satisfies "issuance without a user name is refused" with minimal surface.

## Risks / Trade-offs

- [Changing `require_auth`'s returned value could break other callers] → It retains its signature (returns `None`); the resolved key is carried out-of-band via the request object, so `push`/`pull`/`clients` are unaffected.
- [`me` endpoint leaks identity] → It is authenticated: only the key holder can query their own identity, and the data is the same `comment` the operator already stored.
- [Existing databases have keys with blank comments] → The endpoint reports `username: null` for those; newly issued keys are always named.

## Migration Plan

Additive endpoint + a stricter issuance path. Existing keys and clients are untouched. Rollback: remove the endpoint and relax the name check; the allowlist and signature checks are unchanged.

## Open Questions

None.