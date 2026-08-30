## Why

The server writes photo files at `photos/<foto.id>` without validating the id. A pushed photo with an id containing `../` or `/` resolves to a path outside the photos directory, letting an authenticated client write files anywhere on the server filesystem (path traversal). This was confirmed in testing: a photo id of `../../../../tmp/ELISDB_TRAV_TEST` wrote a file outside the photos folder. Even among trusted users, a buggy client could accidentally do the same.

## What Changes

- The server rejects or neutralizes photo ids that contain path separators or traversal sequences, so photo files can only ever be written inside the configured photos directory.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `server-photo-storage`: photo file paths are constrained to the configured photos directory regardless of the supplied photo id.

## Impact

- **Server**: `app/database.py` (`_foto` / `_replace_fotos`), `app/models.py` (validate `FotoIn.id`).
- **Behavior**: a push with an unsafe photo id is rejected (or the id is neutralized), preventing writes outside `photos/`.
- **Tests**: server tests for traversal ids (rejected/neutralized) and for normal ids still working.
