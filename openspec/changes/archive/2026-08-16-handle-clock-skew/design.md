## Context

Multi-device sync resolves conflicts by last-write-wins on `updated_at`, which every device stamps from its own wall clock (`System.currentTimeMillis()`). Two consequences follow when device clocks drift:

1. **Unfair LWW**: a device whose clock runs ahead stamps newer timestamps and wins conflicts it did not actually win; the losing device's edit is rejected as `stale`.
2. **Silent loss + hard blocker**: the app currently treats any non-duplicate status (`accepted`/`stale`) as an ack and clears `pending`; a `stale` edit is silently discarded via pull. And the server's auth rejects timestamps outside a 300 s window, so a device whose clock drifted more than 5 minutes cannot sync at all (`ClockFailure`, `SyncFault.CLOCK`).

The server already owns conflict resolution; the gap is that the *clock* — the reference used to decide conflicts — is per-device, not shared.

## Goals / Non-Goals

**Goals:**
- All devices stamp records on the server's timescale, so LWW compares like-for-like.
- A lost local edit is reported to the user, never silently dropped.
- A device whose clock drifted (but synced recently) keeps authenticating within the auth window.

**Non-Goals:**
- Changing the server's LWW or auth-window semantics (they stay; clients normalize instead).
- Replacing the wall clock with a logical/HLC clock (overkill for this scope).
- Bulk-rewriting existing stored `updated_at` values (convergence over normal use).

## Decisions

**D1 — Server exposes current time via a public endpoint.**
- Add `GET /api/time` returning `{"time": <unix seconds>}`, no authentication (like `/health`).
- *Rationale*: a dedicated unauthenticated endpoint lets the app bootstrap the offset even when its clock is already wrong (an authenticated response would be unreachable). Seconds scale matches the auth timestamps and is sufficient; `updated_at` normalization reuses the same offset in milliseconds.
- *Alternative considered*: response header on authenticated endpoints — rejected because a clock-skewed device could not reach any authenticated endpoint to read it.

**D2 — App measures and persists a device↔server clock offset.**
- New `ClockOffsetStore` (SharedPreferences-backed) storing `offset = server_time_sec − device_time_sec`.
- After a successful sync (or when `server_time` is obtainable), the offset is computed and persisted; absent a stored offset, the app uses the device clock as-is.
- `updated_at` stamps use `device_now + offset`.
- *Rationale*: keeps stamping cheap (no network on the write path) and correct once measured; persisted so offline-created records still get the last-known-good timescale.

**D3 — Auth timestamps use the offset-normalized time.**
- The signed timestamp sent with push/pull/delete becomes `(device_now + offset) / 1000`.
- *Rationale*: a device whose clock drifted but synced recently produces timestamps inside the server's 300 s window and keeps working.
- *Note*: this does not weaken the window — the offset comes from the server itself and the signature still binds the timestamp.

**D4 — `stale` becomes user-visible.**
- `SyncCoordinator.pushEdits` keeps clearing `pending` on `stale` (the server owns the data), but aggregates the affected client names into the outcome.
- `SyncOutcome` already has `SuccessWithStatuses(accepted, stale)`; the coordinator now returns it when any record is `stale`, and `SyncManager.mapOutcome` renders a result message naming the lost edits.
- *Rationale*: data correctness (server wins) and transparency (user is told) without changing the LWW model.

**D5 — Files touched.**
- Server: `app/main.py` (new `/api/time` route).
- App: `SyncApi`/`ApiClient` (time fetch, normalized auth timestamp), new `ClockOffsetStore`, `SyncCoordinator` (offset capture + stale aggregation), `SyncManager` (stale message), `AppContainer` wiring.
- No DB schema change; offset lives in SharedPreferences.

## Risks / Trade-offs

- [Offset goes stale after long offline periods] → Convergence: refreshed on every successful sync; until then the last-known offset still beats a raw device clock for fairness.
- [First-ever sync with a badly wrong clock] → The `/api/time` endpoint is unauthenticated, so the offset can still be bootstrapped; if even that fails, the device falls back to today's behavior.
- [`stale` notification noise] → Only surfaced when a real conflict is lost; duplicate merges keep their existing distinct message.

## Migration Plan

1. Deploy the server change first (adds `/api/time`; additive, no wire-format break).
2. Ship the app update. No data migration; existing records converge as they are next edited/pulled.

## Open Questions

None.
