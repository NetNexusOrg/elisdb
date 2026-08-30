## Why

The sync button/icon on the Settings screen jumps horizontally depending on the sync state: when idle it sits at the left of the status row, but as soon as a status message appears ("A sincronizar…", "Sincronizado", or an error) the weighted message text pushes the button to the right. This unstable position is confusing for the user.

## What Changes

- The sync button/icon in the settings sync status row SHALL keep a fixed position regardless of the sync state (idle, in progress, success, error).
- The sync status message (icon + text) SHALL be laid out so it never displaces the button; the button occupies a reserved, stable slot in the row.
- While a sync runs, the button slot continues to show the in-progress spinner in the same position.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `client-settings`: the sync status row shows the sync action at a stable position across all sync states.

## Impact

- **App**: `ui/settings/SyncStatusBar.kt` — rework the row layout so the button keeps a fixed slot (e.g. trailing slot, with the message area constrained so it cannot push the button).
- **Tests**: `SyncStatusBarTest.kt` gains position/stable-layout assertions for the button across IDLE, IN_PROGRESS, SUCCESS and ERROR.
- **No breaking changes**: purely a layout fix; no API, schema, or sync behavior changes.