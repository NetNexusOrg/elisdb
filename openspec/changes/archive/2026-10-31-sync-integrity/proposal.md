# Proposal

## Why

A review of the app and the server found five defects that cause **data loss or permanently diverged data**, all on paths used every day:

1. **Deleting a client often never reaches the other devices.** The server stores the deletion timestamp exactly as the deleting device sent it, while every other device advances its pull watermark to the newest timestamp it has seen. A deletion stamped earlier than another device's watermark is never selected by `since=`, and because the watermark is never reset there is no full pull to recover from — the deleted client stays visible and editable forever.
2. **An acknowledgement can overwrite an edit made while the sync was running.** After the push returns, the app writes the record it read *before* the network round-trip back to the database with `pending = false`. If the user edited that client in the meantime, the edit is replaced by the older snapshot and is not even marked pending — silent loss.
3. **The pull watermark advances over records that were deliberately skipped.** Records with pending edits are skipped, but the watermark still moves past them, so the server version is never applied later and the two sides stay different forever.
4. **A schema migration deletes every stored photo.** The coordinate migration disables foreign keys with `PRAGMA foreign_keys = OFF`, but it runs inside an already-open transaction, where that pragma is a no-op; the subsequent `DROP TABLE clientes` therefore cascades and deletes all `fotos` rows, and the startup garbage collector deletes the files. Reproduced: a legacy database with one client and one photo ends with zero photos.
5. **The startup garbage collector deletes every photo when the photos directory is relative.** Paths stored in the database are absolute, while the collector compares them to a relative join, so nothing matches and every file is treated as an orphan.

Together with these, a migration in the Android app uses `ALTER TABLE ... DROP COLUMN`, which only exists in SQLite 3.35+ (Android 14); on Android 7–13 the app crashes at startup after updating.

## What Changes

- **A separate delivery clock.** Clients and tombstones gain a `delivery_ts` column, assigned by the server as `max(timestamp from the device, previous delivery timestamp + 1)`. Pull filters and paginates by it, and the app advances its watermark with it. `updated_at` stays exactly what the device wrote and remains the only input to last-write-wins, so a device with a slow clock can no longer lose an edit because the server re-stamped it.
- **Deletions are delivered.** `delete_client` assigns a `delivery_ts` like a push does, with the same `BEGIN IMMEDIATE` discipline, so a tombstone is always newer than everything already delivered.
- **The photo migration preserves photos.** The migration commits before disabling foreign keys, verifies that they really are disabled, and re-checks the photo count before committing.
- **The garbage collector compares real paths.** The photos directory is resolved to an absolute path at construction, and entries that are not regular files are ignored instead of aborting the sweep.
- **The push reports photos it could not store.** Instead of silently dropping a photo whose file is missing, the server returns its identifier in `missing_photos`; the app keeps such photos pending and uploads them again.
- **Acknowledgements are conditional.** The app clears `pending` with `UPDATE ... WHERE id = ? AND updated_at = ?` — the value it actually sent. If the row changed during the sync, it stays pending.
- **The watermark never passes a skipped record.** When a pull skips a record (pending edit) the watermark stops just below the oldest skipped delivery timestamp.
- **The pull re-checks `pending` inside the write transaction**, closing the window where an edit made while photos were downloading was overwritten.
- **The Android 2→3 migration recreates the table** instead of dropping a column, so it works on every supported Android version.

## Capabilities

### New Capabilities

<!-- none -->

### Modified Capabilities

- `sync-tombstones`: deletions carry a delivery timestamp and are therefore delivered to devices that are ahead of the deleting device's clock.
- `sync-incremental-pull`: the cursor and the watermark are the delivery timestamp; the watermark does not advance past skipped records.
- `server-sync`: the conflict timestamp and the delivery order are separate columns; pagination and the latest-timestamp endpoint use the delivery order.
- `server-photo-storage`: migrations preserve photo rows and the garbage collector works with relative directories.
- `client-sync`: acknowledgements are conditional, missing photos are reported and retried, and the pull revalidates pending state in the transaction.
- `client-data`: schema migrations run on every supported Android version.

## Impact

- **Server**: `app/database.py` (new `delivery_ts` columns + migration, `save_client`, `delete_client`, `pull_all`, `_delivery_watermark`, `_replace_fotos`, `gc_orphan_photos`, `_migrate_clients_nullable_coords`), `app/models.py` (`ClienteOut.delivery_ts`, `TombstoneOut.delivery_ts`, `PushResult.missing_photos`), `app/main.py` (push result, `/api/latest`).
- **Android**: `remote/RemoteDto.kt`, `data/local/ClienteDao.kt` (`markSynced`), `data/local/AppDatabase.kt` (`MIGRATION_2_3`), `sync/SyncCoordinator.kt` (ack, watermark, pull revalidation, missing photos).
- **Wire format**: additive only — new optional fields (`delivery_ts`, `missing_photos`). Older apps keep working: their watermark is bounded by the `updated_at` values they received, which never exceed the delivery timestamps already issued.
- **Database**: additive migration; existing rows are backfilled with their current timestamps.
- **Not affected**: authentication, the photo binary endpoints, the record fields, and the update flow.
