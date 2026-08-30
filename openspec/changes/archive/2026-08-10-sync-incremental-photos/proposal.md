## Why

On every `pull()`, `applyServerItem` deletes each client's existing photos from disk and DB and re-decodes/re-stores the full base64 payload, even for photos that did not change (SyncCoordinator.kt:132-149). This causes full-catalog I/O and traffic each sync, and — worse — if `storeBase64` fails midway, the old photos are already deleted while the transaction only rolls back DB rows, leaving database rows with no files and orphaned files on disk. Review finding R3 (Grave).

## What Changes

- Pull entry point SHALL skip re-writing photos whose content did not change between server and local (content-based comparison), eliminating pointless delete/re-store churn.
- Photos SHALL be written to disk outside the Room transaction so a DB rollback never leaves rows referencing missing files (write file first, then DB row; remove old files only after the row commit succeeds).
- Failure mid-pull (decode/write error) SHALL leave prior photo files intact and consistent with DB rows («keep the last good version»).
- A pull failure SHALL be retriable and idempotent; partial cleanup of orphan files is handled lazily (see R8 future) or in this change.

## Capabilities

### New Capabilities

- `photo-content-hashing`: content-based identity/comparison for photos so unchanged photos are not rewritten.

### Modified Capabilities

- `client-sync`: the "Pull returns all records with photos" / "decoded to photos files" behavior changes to "only changed photos are re-materialized".
- `server-sync` (pull delta): pull currently always returns the full dataset w/ base64; without server `since`, comparison is client-side only. No protocol change required for correctness of this change; full-base64 bandwidth reduction is covered in R4–R8 (sync-polish-and-scale).

## Impact

- `app/src/main/java/com/example/elisdb/sync/SyncCoordinator.kt` — `applyServerItem` rewrite logic.
- `app/src/main/java/com/example/elisdb/sync/SyncMapper.kt` — photo compare helper.
- `app/src/main/java/com/example/elisdb/data/photo/PhotoStore.kt` / `FilePhotoStore` — API for a "compare & materialize" operation.
- `app/src/main/java/com/example/elisdb/data/local/FotoDao.kt` — transactional write helpers.
- Tests: pull-with-unchanged-photos does not rewrite; pull failure keeps last good photo.