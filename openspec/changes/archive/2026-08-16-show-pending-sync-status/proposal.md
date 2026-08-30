## Why

After creating or editing a client, the settings screen shows "Sincronizado" even when the change has not been sent to the server yet. The app marks local changes as `pending` in the database, but no UI exposes that state: the sync status bar only reacts to the last sync attempt (idle/in-progress/success/error), and the `SyncStatus` value persists globally, so a stale "Sincronizado" is shown while pending work is outstanding. Conversely, when another device has pushed changes the server holds records newer than this device's last pull, the app has no way to indicate that there is work to receive. Users cannot tell whether synchronization is still to do in either direction, which is confusing in an offline-first app.

## What Changes

- The settings sync status bar gains a "pending uploads" state shown whenever there are local client records (or pending deletions) not yet acknowledged by the server, instead of a stale success/error from an earlier sync.
- The settings sync status bar gains a "pending downloads" state shown whenever the server holds data newer than this device's last pull (work to receive).
- Both states clear once a sync acknowledges/retrieves the outstanding work.
- Both pending signals are observable and derive from authoritative sources: the pending markers the sync engine already uses for uploads, and a server-reported latest timestamp compared with the persisted pull watermark for downloads.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `client-sync-state`: the observable sync status reflects pending local uploads and pending server downloads, not just the last sync attempt.
- `client-settings`: the sync status row shows the pending-uploads and pending-downloads states alongside the existing states.
- `server-sync`: the server reports its latest data timestamp so clients can detect work to receive without a full pull.

## Impact

- **App**: `SyncManager` (expose pending-uploads from the pending DAO and pending-downloads from comparing the server's latest timestamp with the pull watermark), `ApiClient` (fetch the server's latest timestamp), `SettingsViewModel` (observe and expose), `SyncStatusBar` (render the pending states), and the `ClienteDao` pending queries already used by the sync engine.
- **Server**: a lightweight endpoint (e.g. `GET /api/latest`) returning the maximum `updated_at` over stored clients and tombstones.
- **Behavior**: opening settings after a local save shows a pending sync; opening settings when the server has newer data shows work to receive; after a sync the status shows the acknowledged result.
- **Tests**: app unit tests for the pending-state derivation and rendering; server pytest for the latest-timestamp endpoint.
