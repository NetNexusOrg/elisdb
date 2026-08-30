## Why

All maps in the app (client form preview, fullscreen picker, client detail) render as a blank grid because osmdroid cannot download map tiles. Confirmed root cause: the app manifest declares no `android.permission.INTERNET`, so every tile request fails silently. Verified empirically — with the permission added, osmdroid begins caching tiles immediately ("Tile cache increased", cache.db grows).

## What Changes

- Add the `android.permission.INTERNET` permission to the app manifest so osmdroid can download map tiles from the tile server.

## Capabilities

### New Capabilities

- None.

### Modified Capabilities

- `client-location`: the "Display a location on a map" requirement gains a scenario pinning that the map actually renders map tiles (visible content), not just an empty view.

## Impact

- `app/src/main/AndroidManifest.xml`: one new `<uses-permission>` entry.
- No dependency, API, or data changes.
