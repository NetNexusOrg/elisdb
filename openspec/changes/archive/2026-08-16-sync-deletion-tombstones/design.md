## Context

Deletes currently have no last-write-wins semantics. When device A deletes a client offline and device B edits the same client offline, B's later push finds the id absent on the server and simply creates the record again — silently undoing A's delete. Deletes also only propagate to other devices via a full pull (the delta pull omits older records), so a deleted client can linger on devices that sync incrementally.

## Goals / Non-Goals

**Goals:**
- A delete competes with concurrent edits under the same LWW rule (newest timestamp wins), so a delete is not silently undone.
- Deletions propagate to other devices on incremental pulls, not only full pulls.

**Non-Goals:**
- Historical audit/tombstone retention beyond the data needed for LWW and propagation.
- Changing the duplicate-by-name merge (independent mechanism).
- Backfilling tombstones for previously deleted ids (no record of them exists).

## Decisions

**D1 — Server keeps a `tombstones` table (id + deleted_at).**
- `DELETE /api/clients/{id}` now writes `tombstones(id, deleted_at)` before/alongside removing the client row and its photos. `deleted_at` is taken from the request query parameter `deleted_at` when supplied, else the server's current time.
- *Rationale*: a dedicated table keeps the `clientes` schema untouched (D3 in the location/photo changes already rebuilt that table; adding tombstones there would risk another rebuild).
- *Alternative considered*: a soft-delete flag on the client row — rejected because it would resurface deleted rows in the normal pull and complicate the LWW update path.

**D2 — Push honours tombstones.**
- In `save_client`, before the normal create/update logic, look up the id in `tombstones`. If a tombstone exists and `cliente.updated_at <= deleted_at`, return `stale` (delete wins). If `cliente.updated_at > deleted_at`, remove the tombstone and proceed as a normal accepted create (resurrection).
- *Rationale*: this is exactly the LWW comparison used for edits, extended to deletes. Resurrection is deliberate re-creation by a newer timestamp.

**D3 — Pull returns tombstones.**
- `pull_all` additionally returns tombstones (`id`, `deleted_at`), filtered by `deleted_at > since` when `since` is supplied so incremental pulls carry only new deletions.
- The response shape becomes an object `{"records": [...], "tombstones": [...], "next_since": ...}` in all cases (previously a flat list for unlimited pulls). This is a **wire-format break** for older app versions; a coordinated release is required.
- *Rationale*: the flat-list form has no room for a second array. The app is the only client; the change is accepted per the proposal.

**D4 — App reconciles tombstones on pull.**
- `SyncCoordinator.pull` processes `tombstones`: for each tombstone, if a local record exists with that id and no pending edits, delete it (rows + photos). Pending records are kept so the next push re-judges under LWW.
- Delete requests now send `deleted_at = device timestamp` (reusing the clock-offset normalization from `handle-clock-skew`).

**D5 — Stale from tombstone is reported like a stale edit.**
- `pushEdits` already aggregates `stale` names into the outcome (from `handle-clock-skew`); a tombstone rejection flows through the same path, so the user sees the lost conflict.

## Risks / Trade-offs

- [Wire-format break on pull] → Coordinated release; the proposal flags it as breaking.
- [Tombstone growth] → Bounded: one row per deleted id, reused by resurrection; acceptable at this scale.
- [Delete-then-recreate race between devices] → Resolved deterministically by LWW on timestamps; the newer action wins.

## Migration Plan

1. Deploy server first (tombstones table is additive; the pull shape change is breaking, so the server must be deployed together with the app update).
2. Ship the app update that parses the new pull shape and sends `deleted_at`.
3. No data migration; old ids deleted before this change have no tombstones and behave as before.

## Open Questions

None.
