## Context

Today the client record's location is mandatory at every layer (see proposal.md — Why): the form blocks save without coordinates, the Room entity and schema use `REAL NOT NULL`, the server Pydantic schemas require `float`, and the detail screen constructs `GeoPoint` unconditionally. There is no "no location" representation anywhere, so making it optional is a cross-cutting nullable-field change touching storage, API and UI.

## Goals / Non-Goals

**Goals:**
- A client can be created and edited without coordinates, on the app and through the server API.
- Room, sync DTOs, server schema and Pydantic models all represent absent coordinates as null.
- Detail screen renders correctly (no map, no navigate action) for location-less clients.
- Push/pull carry null coordinates without breaking the sync engine.

**Non-Goals:**
- Changing the location picking UX ("use current location", draggable marker) — that behavior is unchanged.
- A sentinel value (e.g. `0.0`) for "no location" — null is used explicitly.
- Bulk-backfilling existing clients.

## Decisions

**D1 — Nullable coordinates throughout, using `null` for absence.**
- App entity `Cliente.latitude`/`longitude` become `Double?`; `ClienteDraft` likewise.
- Room schema migrates 6→7 changing `latitude REAL NOT NULL, longitude REAL NOT NULL` to nullable columns (default `NULL`).
- Server `ClienteIn`/`ClienteOut` change `float` → `float | None = None`; SQLite columns become nullable.
- *Rationale*: null is the honest representation of "no location" and keeps existing records (which all have coordinates) untouched.
- *Alternative considered*: sentinel `0.0`/`-999` — rejected because it leaks into the map, navigation and search, and misleads conflict resolution.

**D2 — Form: remove the location-required gate, keep everything else.**
- `ClientFormViewModel.save()` drops the `latitude == null || longitude == null` early return (message "Escolhe a localização no mapa.") and passes the nullable coordinates through.
- The UI already renders a null preview correctly (`ClientFormScreen.kt` null-safe branch), so no form layout change is needed.
- *Rationale*: minimal change; the map preview and "use current location" already handle null.

**D3 — Detail screen: conditionally render map and navigate action.**
- `ClientDetailContent` renders the map only when `cliente.latitude != null && cliente.longitude != null`; otherwise it shows the location section with a "no location" hint and no map.
- "Navegar até aqui" is shown only when coordinates exist.
- `openInMap()` guards on null before building the geo URI.
- *Rationale*: avoids a `GeoPoint(null)` crash and keeps navigation meaningful.

**D4 — Sync: nullable pass-through, optional wire fields.**
- `ClienteInDto`/`ClienteOutDto` lat/lon become `Double?`.
- `SyncMapper.toRecord()` copies nullable values as-is.
- `SyncCoordinator.applyServerItem()` builds `Cliente` with nullable coordinates; no special handling needed beyond the type change.
- Server push stores null when absent; pull returns null. The existing `IntegrityError`-as-duplicate path in `save_client()` must be reviewed so a null coordinate never misroutes to `duplicate` (nullable columns mean no NOT NULL violation, so this is naturally avoided).
- *Rationale*: wire and storage both represent absence as null, keeping the sync engine logic unchanged.
- *Breaking note*: older app builds with non-nullable DTOs will reject pulled records carrying null coordinates; this is acceptable for a single-user/small-team deployment but documented in the proposal as BREAKING.

**D5 — Server: optional fields without custom 422→400 handling.**
- Pydantic `float | None = None` accepts missing/null coordinates; absent coordinates are stored as NULL.
- No change to blank-name handling (that 400 stays).
- *Rationale*: consistent with other optional fields (contract, notes).

## Risks / Trade-offs

- [Detail screen NPE with a location-less client] → D3 conditional rendering; covered by a new instrumentation test.
- [Older clients reject null coordinates on pull] → BREAKING flagged in proposal; server is shared by the same app fleet in practice.
- [Sync DTO deserialization of null floats] → D4 makes them nullable; covered by ApiClient/sync tests.
- [Room migration 6→7 breaking existing installs] → Standard exported-schema + migration test added before rollout.

## Migration Plan

1. Server first: make Pydantic fields optional and DB columns nullable (additive — old payloads still accepted).
2. App: nullable entity + DTOs + form gate removal + detail conditional rendering; add Room migration 6→7.
3. Deploy app; run migration test and end-to-end check (create client without location, sync push/pull, view detail).
4. Rollback: revert app (schema stays v7 with nullable columns; old app tolerates) and server (columns stay nullable, harmless).

## Open Questions

None.