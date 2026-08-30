## Why

The server currently stores photo bytes directly in the SQLite `fotos.data` column as BLOBs, which bloats the database file with large binary payloads. Storing photos as files on disk and pointing to them by path keeps the DB lean, scales better, and follows the same pattern already used by the Android app.

## What Changes

- **BREAKING (internal storage, not API):** Server photos are no longer stored as BLOBs inside SQLite. Photo binary content moves to a server-managed filesystem directory, and the `fotos` table stores a stable file reference (path) instead of the bytes.
- Photo files are written/overwritten when a client is saved (push), and deleted when a client is deleted (cascading, matching `ON DELETE CASCADE`).
- The public HTTP API contract is unchanged: `push` still accepts photos as base64 (`dataB64`) and `pull`/`get` still returns them as `dataB64`. Only server storage changes.
- A configurable base directory for photo files (via environment variable), defaulting relative to the DB location.

## Capabilities

### New Capabilities
- `server-photo-storage`: how the server persists photo binary content as files on disk, referenced by path, surviving restarts and cascading deletion.

### Modified Capabilities
<!-- Existing capabilities whose REQUIREMENTS are changing (not just implementation).
     The public server-sync behavior is unchanged (API stays base64-based); only
     server storage changes, so no server-sync requirement is modified. -->

## Impact

- `../elisdb-server/app/database.py` — `fotos` schema (drop `data`, add path/reference column), `_replace_fotos`, `get_client_with_photos`, `delete_client`.
- `../elisdb-server/app/main.py` — wire an environment variable (e.g. `ELISDB_PHOTOS`) to the `Database`.
- `../elisdb-server/app/models.py` — unchanged (API still base64).
- `../elisdb-server/tests` — update photo round-trip tests; add tests for file persistence and cascade deletion of files.
- No dependency changes.