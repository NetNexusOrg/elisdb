## Context

See proposal.md - Why. The osmdroid configuration in `ElisdbApp.onCreate` sets `Configuration.getInstance().userAgentValue = packageName` (a bare Java-style application id). The OpenStreetMap tile server rejects such generic User-Agents with `403 Access blocked`, so all map tiles fail to load. The main `client-location` spec already requires tiles to render (scenario "Map renders map tiles"); this change makes the app comply with it.

## Goals / Non-Goals

**Goals**

- Send a descriptive, policy-compliant User-Agent on every osmdroid tile request.
- Keep the change localized to map configuration; no UI or navigation changes.
- Verify tiles actually render on the device across all three map surfaces.

**Non-Goals**

- Not switching tile providers or adding a fallback tile source.
- Not changing the tile source/endpoint (osmdroid 6.1.20 `TileSourceFactory.MAPNIK` already uses the HTTPS `tile.openstreetmap.org` endpoint).
- Not altering app network permissions or other osmdroid settings.

## Decisions

**1. Replace the bare package-name User-Agent with a descriptive value.**

- Current: `userAgentValue = packageName` → `com.example.elisdb` (rejected by OSM).
- New: `"ElisDB/<versionName> (Android)"`, e.g. `ElisDB/1.0 (Android)`. This matches OSM's tile usage policy, which requires a User-Agent that identifies the application by name and version.
- Read `<versionName>` at runtime from `PackageManager.getPackageInfo(packageName, 0).versionName` in `onCreate`, keeping the version in sync with the manifest.
- Rationale: OSM blocks generic or empty User-Agents; a named app + version is the documented-compliant form. Alternatives considered: `buildConfig = true` + `BuildConfig.VERSION_NAME` — rejected to avoid changing the build configuration; a hard-coded string — rejected because it drifts from the manifest version.

**2. Keep the configuration in `ElisdbApp.onCreate`, before any `MapView` is created.**

- osmdroid reads `Configuration.userAgentValue` when the HTTP client for tile downloads is built. Since maps are only created after `Application.onCreate`, this single assignment is sufficient and already correctly placed — only its value changes.

**3. Verify end-to-end on the emulator/device.**

- OSM's `403 Access blocked` is server-enforced, so verification must happen against the live tile server: confirm tiles render on the detail screen, the fullscreen picker, and the form preview, and that `cache.db` grows with tile bytes (the same signal used to confirm the `fix-blank-map` change).

## Risks / Trade-offs

- [OSM still rejects the request for a different reason (network/TLS/rate limiting) and 403 persists] → The verification task distinguishes "requests rejected" (check the exact HTTP status and User-Agent header in osmdroid's `TileLoader` logs) from "tiles blocked for other reasons"; the emulator smoke test gives a quick pass/fail.
- [A hard-coded app name in the User-Agent becomes stale if the app is renamed] → The value is composed in one place in `ElisdbApp.onCreate`, so a rename is a one-line change; the version is derived from `PackageManager`, not hard-coded.

## Migration Plan

- Single-commit change to `ElisdbApp.kt`; no data migration, no schema change.
- Rollback: revert the User-Agent assignment to `packageName`; behavior returns to the pre-fix state.

## Open Questions

None.
