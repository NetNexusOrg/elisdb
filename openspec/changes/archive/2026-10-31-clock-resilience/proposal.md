# Proposal

## Why

Records are ordered by the timestamp the sending device writes into `updated_at`. If the device clock is wrong when an edit is made, the record carries a timestamp far in the past: the server accepts it, but every device whose pull cursor is already past that date never receives it, and `/api/latest` does not change, so nothing looks pending. This is not hypothetical — a device whose clock was ~22 days behind pushed four edits that stayed invisible to the rest of the team.

The offset between device and server clocks was only measured at the start of a sync, so edits made before that first sync (or after the clock changed) carried the raw — and possibly wrong — device time. Nothing in the app told the user the clock was off, and a rejected timestamp produced a generic sync failure.

## What Changes

- The clock offset is measured from the `Date` header of every server response, so it stays fresh without extra requests.
- The offset is also measured at app start, before the first sync, so early edits are stamped on the server's timescale.
- A signed request rejected with `stale or future timestamp` is retried once: the app corrects the offset from the same response (or from `/api/time`), re-signs with the corrected timestamp and repeats the request.
- The configuration screen warns when the device clock differs from the server by 120 seconds or more, stating how far off and in which direction, with a "Corrigir" action that re-measures the offset immediately.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `client-sync`: the clock-offset requirement now covers continuous observation of the server clock, measurement at startup, a single retry after a stale-timestamp rejection, and the user-visible skew warning with an explicit correction action.

## Impact

- **App**: new `sync/ClockSync.kt` (offset store + observed skew), `remote/ApiClient.kt` (Date-header observation and one-retry correction in a shared `execute` helper), `remote/ApiClientProvider.kt` (accepts the clock), `AppContainer.kt` (owns `ClockSync`, `refreshClock()`), `ElisdbApp.kt` (refresh at start), `ui/settings/SyncViewModel.kt` (exposes skew and a correction action), `ui/settings/ConfigScreen.kt` (skew banner).
- **Server**: unchanged — the existing `Date` header and `/api/time` are enough.
- **Tests**: `ClockSyncTest`, `ApiClientTest` (retry, invalid key, Date observation), instrumented `SyncViewModelTest` and `ConfigScreenTest`.
- **Docs**: `elisdb-android/docs/uso.md` — the clock warning and its correction.
- **Not affected**: record format, pull/push protocol, server behaviour.
