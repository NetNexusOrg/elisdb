## Why

Multi-device sync resolves conflicts by last-write-wins on `updated_at`, which each device stamps from its own wall clock (`System.currentTimeMillis()`). This makes the system depend on device clocks being roughly aligned:

- A device whose clock runs **ahead** stamps newer timestamps than it should, so it wins conflicts it didn't actually win — and the other device's edit is discarded.
- When the server rejects a push as `stale`, the app silently clears `pending` and pulls, so the losing user never learns their edit was dropped.
- Clock drift beyond the 300 s auth window blocks sync entirely, with no recovery path for a device whose clock is simply wrong.

The result: a user with "data e hora automática" off (or a miscalibrated clock) experiences silent data loss or a hard sync blocker.

## What Changes

- The app measures the offset between its clock and the server clock on each successful sync, persists it, and uses it to normalize `updated_at` stamps on new and edited records, so all devices write on the server's timescale.
- Auth request timestamps use the same normalized time, so a device whose clock drifted (but synced recently) keeps authenticating instead of hitting the 300 s window.
- When the server rejects a push as `stale`, the app surfaces a visible warning instead of silently discarding the edit, so a lost conflict is never silent.
- The server reports its current time (e.g., a response header or a field in push/pull responses) so the offset can be measured.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `client-sync`: `updated_at` stamped on the server's timescale; `stale` becomes user-visible.
- `client-sync-state`: device↔server clock offset persisted locally.
- `server-sync`: exposes the server's current time to clients.
- `sync-observability`: stale/lost-conflict outcome visible to the user.

> Note: `server-auth` requires no behavioral change — the freshness window stays
> 300 s; clients simply normalize their timestamps against the server clock, so
> the window is exercised correctly even for a device whose clock drifted.

## Impact

- **App**: `SyncCoordinator` (offset on stamp, stale handling), `SyncManager` (outcome display), `ApiClient` (auth timestamps, reading server time), a new persisted offset store.
- **Server**: `main.py`/`auth.py` — expose current time; auth validation unchanged in substance but exercised against normalized timestamps.
- **Behavior**: edits from a clock-skewed device no longer win by accident; a rejected edit is reported, not dropped silently; the auth window no longer blocks a device that synced recently.
- **Tests**: app unit tests for offset normalization and stale warning; server pytest for time reporting.
