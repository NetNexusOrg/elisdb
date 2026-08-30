## Why

The OpenStreetMap tile server rejects the app's tile requests with `403 Access blocked`, so every map shows no tiles. The User-Agent is currently set to the bare application id (`com.example.elisdb`), which OSM's tile usage policy treats as generic and blocks; the map must present a descriptive, policy-compliant User-Agent to load tiles.

## What Changes

- Replace the bare application-id User-Agent with a descriptive, policy-compliant value (e.g. `ElisDB/<version>` + contact) in `ElisdbApp.onCreate` before any map is created.
- Verify the osmdroid MAPNIK tile endpoint and TLS configuration are correct so requests reach the tile server over HTTPS with the compliant User-Agent.
- Confirm map tiles load on all three screens (client detail, coordinate picker, form preview) instead of showing `403 Access blocked`.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

None. This is a bug fix that restores behavior already specified by the `client-location` capability (scenario "Map renders map tiles"). No requirement text changes, so the change opts out of specs via `skip_specs: true`.

## Impact

- `app/src/main/java/com/example/elisdb/ElisdbApp.kt` — User-Agent configuration.
- `app/src/main/java/com/example/elisdb/ui/map/OsmMap.kt` — tile source/endpoint verification (no structural change expected).
- Dependency: `org.osmdroid:osmdroid-android` (unchanged).
- Behavior: map tiles render on the detail screen, fullscreen picker, and form preview when the device has network access.
