## 1. Schema

- [x] 1.1 Update `fotos` table schema: drop `data` column, add `caminho` (path) column
- [x] 1.2 Update `Database` to accept a configurable photo storage directory (env `ELISDB_PHOTOS`), defaulting to `photos/` subdir of the DB location

## 2. Photo persistence

- [x] 2.1 In `_replace_fotos`, write each photo's bytes to `<photos_dir>/<photo_id>` and store the path in the row instead of the BLOB
- [x] 2.2 In `get_client_with_photos`, read each photo file and re-encode to `dataB64`; gracefully skip files that are missing
- [x] 2.3 In `delete_client`, also remove the client's photo files from disk

## 3. Wiring & robustness

- [x] 3.1 Wire `ELISDB_PHOTOS` env var into the `Database` construction in `app/main.py`
- [x] 3.2 Best-effort cleanup of orphaned photo files when a push's DB write fails

## 4. Tests & verification

- [x] 4.1 Add tests: push photo persists a file on disk and returns it as base64 on pull
- [x] 4.2 Add test: photo file survives server restart (new Database instance against same dir)
- [x] 4.3 Add test: deleting a client removes its photo files from the directory
- [x] 4.4 Add test: configured photo directory is honored, default is relative to DB location
- [x] 4.5 Update any existing tests that asserted photo storage inside the DB
- [x] 4.6 Run the full pytest suite and confirm all tests pass