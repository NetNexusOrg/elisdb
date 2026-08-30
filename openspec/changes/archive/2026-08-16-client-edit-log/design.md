## Context

Users want to know who created or edited a client and when. The app's detail screen shows only current data; there is no history. The server already authenticates every push with a per-device ed25519 key and attaches the allowlist record (with `comment` = user name and `id` = key id) to the request state (`request.app.state.authenticated_key`). So the actor is known at save time but is never recorded.

## Goals / Non-Goals

**Goals:**
- The server records, per persisted client change, the actor (user name when available, always the key id), the timestamp, and whether it was a create or an edit.
- The app shows that history from the client detail screen via a "Log" action.

**Non-Goals:**
- Recording per-field diffs of what changed (only who and when).
- Editing or deleting audit entries after the fact.
- Recording offline local edits that have not been pushed (the server is the audit source of truth).

## Decisions

**D1 — Dedicated audit table, appended on save.**
- New table `client_audit (id INTEGER PRIMARY KEY AUTOINCREMENT, cliente_id TEXT NOT NULL, key_id TEXT NOT NULL, username TEXT, action TEXT NOT NULL CHECK(action IN ('create','edit')), ts INTEGER NOT NULL)`, indexed by `cliente_id`.
- `save_client` gains an optional `actor: dict | None` parameter (the authenticated key record) and records one row per accepted create/update (not on `stale`/`duplicate`-refused). `create` when inserting, `edit` when updating. `username` is null when the key has no comment.
- *Rationale*: the audit is append-only and independent of the client row's `created_at`/`updated_at`, so it survives merges and LWW without corrupting the schema.
- *Alternative considered*: storing only `created_by`/`updated_by` columns on `clientes` — rejected because it loses the edit history (only the last editor survives).

**D2 — Actor comes from the authenticated request, not the payload.**
- `main.py`'s `push` reads `request.app.state.authenticated_key` and passes it to `save_client(..., actor=key)`. The push payload is unchanged.
- *Rationale*: the actor is the authenticated device, not a client-supplied field; this prevents spoofing.

**D3 — Per-client history endpoint.**
- `GET /api/clients/{id}/history` (authenticated) returns the audit rows for that client ordered by `ts`, `id`; empty list when the client or entries do not exist.
- DTOs: `AuditEntryOut { action, username, key_id, ts }`.
- *Rationale*: a small, explicit endpoint the app can call on demand.

**D4 — App fetches on demand and shows a view.**
- `ApiClient.fetchClientHistory(id)` returns the entries. `ClientDetailViewModel` exposes a `log` state and a `loadLog()`; the detail screen's top-bar menu gains a "Log" item opening a dialog/sheet listing create + edit entries (actor + formatted timestamp). Fetch failure shows "não foi possível carregar o histórico" and leaves the detail intact.
- *Rationale*: keep the detail screen lightweight — the log is fetched only when requested.

**D5 — Files touched.**
- Server: `database.py` (table + `save_client` audit recording + `client_history` query + migration), `main.py` (pass actor, add endpoint), `models.py` (DTOs), `test_main.py`.
- App: `remote/RemoteDto.kt` (DTOs), `remote/ApiClient.kt`/`sync/SyncApi.kt` (fetch), `ui/detalhe/ClientDetailViewModel.kt` + `ClientDetailScreen.kt` (Log action + view), tests.

## Risks / Trade-offs

- [Audit growth] → Bounded per client (a few rows per edit); acceptable at this scale.
- [Legacy clients have no history] → Existing records simply show an empty history; new changes are recorded from deployment onward.
- [User name may be absent] → Entries without a name show the key id (or "desconhecido"); the spec covers the unnamed-key case.

## Migration Plan

1. Deploy the server change first (additive table + endpoint; existing push payload unchanged).
2. Ship the app update (Log action + fetch).

## Open Questions

None.
