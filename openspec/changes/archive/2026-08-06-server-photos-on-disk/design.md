## Context

The server persists clients and photos in a single SQLite file (`app/database.py`). Photos are currently stored as BLOBs in the `fotos.data` column, decoded from the base64 `dataB64` payload on push and re-encoded on pull. The public API (`app/main.py`, `app/models.py`) accepts/returns photos as base64. See proposal.md - Why.

## Goals / Non-Goals

**Goals:**
- Move photo binary content out of the SQLite database and into files on a managed directory.
- Store a stable path reference in the `fotos` table instead of the bytes.
- Keep the HTTP API contract byte-for-byte unchanged (base64 in/out).
- Cascade-delete photo files when a client is deleted.
- Configurable photo directory via environment variable, defaulting to a `photos/` subdir next to the DB file.

**Non-Goals:**
- Changing the API schema, models, or response format.
- Adding photo compression, thumbnails, or content-type transforms.
- Migrating the Android client's local photo handling.
- A migration tool for existing BLOB data (backfill out of scope; treated fresh).

## Decisions

**1. Store files in `photos/` subdir of the DB location, configurable via `ELISDB_PHOTOS`.**
Alternative considered: `ELISDB_PHOTOS` as a separate required setting. Chose default-relative so out-of-the-box behavior mirrors today (single data dir). Matches "Photo storage configuration" spec requirement.

**2. Reference files by a stable filename derived from the photo id.**
Use the photo's `id` for the filename: `<photos_dir>/<photo_id>`. Rationale: id is already unique (TEXT PRIMARY KEY), idempotent rewrites, and makes orphan cleanup trivial. Alternative: store an arbitrary generated path — rejected as it adds bookkeeping without benefit. Store the path in `fotos.caminho` (path column) so the rewrite is a full replace.

**3. Rewrite photos as files during `_replace_fotos` (delete + re-insert).**
On `save_client`, write each photo's bytes to `<dir>/<id>` and store the path; drop the `data` column. Before the DB commit, write files; on DB failure, remove just-written files to avoid orphans (best-effort).

**4. DELETE removes files, not just rows.**
`delete_client` must remove each client's photo files from disk in addition to the DB-routed cascade. Fetch `fotos.caminho` first, DELETE the row, then remove files.

**5. Migration of the schema via `CREATE TABLE IF NOT EXISTS` won't change existing tables.**
`fotos` changes (drop `data`, add `caminho`). For a fresh DB the `CREATE TABLE` reflects the new shape. Existing databases with a `data` column are out of scope (Non-Goals) to avoid destructive ALTERs.

## Risks / Trade-offs

- **Orphan files on failed push (files written but DB rollback)** → Track just-written paths and best-effort delete them on exception.
- **Path column drift / unreadable file on pull (missing file)** → `get_client_with_photos` wraps file read; missing file yields empty list for that photo (or a skipped entry) without failing the whole pull.
- **Schema change vs existing DBs** → If the deployed DB predates this change, it keeps the `data` column; documenting it in the migration note covers this; applying to a fresh store introduces `caminho`.
- **File content not transactional with DB row** → Mitigated by cascade delete + best-effort orphan cleanup; acceptable because the server is the single authority.