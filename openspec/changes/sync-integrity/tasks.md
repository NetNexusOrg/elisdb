# Tasks

## 1. Server — schema and delivery clock

- [x] 1.1 Add `delivery_ts` to `clientes` and `tombstones` (declared in the schema and added by a migration that backfills it from `updated_at`/`deleted_at`, creating the `(delivery_ts, id)` indexes).
- [x] 1.2 Run the migration before the coordinate rebuild and include `delivery_ts` in the rebuilt table.
- [x] 1.3 Assign `delivery_ts = max(device timestamp, watermark + 1)` in `save_client` and in `delete_client` (the latter under `BEGIN IMMEDIATE`), keeping `updated_at`/`deleted_at` exactly as received.
- [x] 1.4 Filter, order and paginate `pull` by `delivery_ts`; return it with each record and tombstone; take the cursor from the row already read; make `/api/latest` report the maximum delivery timestamp.

## 2. Server — data safety

- [x] 2.1 Make the coordinate migration commit before `PRAGMA foreign_keys = OFF`, verify the pragma took effect, and compare the photo count before and after (abort on mismatch).
- [x] 2.2 Resolve the photos directory to an absolute path and skip non-files in the orphan sweep.
- [x] 2.3 Return the identifiers of photos that could not be stored (`_replace_fotos` → `save_client` → `PushResult.missing_photos`).

## 3. Server — tests

- [x] 3.1 Migration test: a legacy database with a client and a photo keeps the photo after `init()`.
- [x] 3.2 Garbage collector test with a relative photos directory (and a stray subdirectory) keeps the referenced files.
- [x] 3.3 Tombstone is delivered to a client whose watermark is newer than the deletion timestamp.
- [x] 3.4 A push from a clock that is 60 s behind is accepted and keeps its `updated_at` while `delivery_ts` advances.
- [x] 3.5 A push referencing a photo with no file reports it in `missing_photos` and does not create a row.
- [x] 3.6 Existing tests updated for the new return shape; `pytest -q` green.

## 4. Android — protocol

- [x] 4.1 `ClienteOutDto.deliveryTs`, `TombstoneDto.deliveryTs`, `PushResultDto.missingPhotos` (optional, additive).
- [x] 4.2 `pull()` advances the watermark with `deliveryTs`, clamped below the oldest skipped item, and applies tombstones by delivery timestamp.
- [x] 4.3 `applyServerItem` re-reads the row inside the transaction and skips the item when it became pending.

## 5. Android — acknowledgements

- [x] 5.1 `ClienteDao.markSynced(id, updatedAt): Int` (`UPDATE ... WHERE id AND updated_at`), used for both `accepted` and `stale` instead of writing the snapshot back.
- [x] 5.2 Do not record hashes for photos reported in `missingPhotos`, so they are uploaded again.

## 6. Android — migrations

- [x] 6.1 Rewrite `MIGRATION_2_3` with the table-recreate pattern (no `DROP COLUMN`).
- [x] 6.2 Migration test for 2→3 covering the resulting schema.

## 7. Verification

- [x] 7.1 Unit tests for the new acknowledgement and watermark behaviour; `:app:testDebugUnitTest` and `:app:lintDebug` green.
- [x] 7.2 `openspec validate sync-integrity --strict`.

## 8. Docs

- [x] 8.1 Note the `delivery_ts` contract in `elisdb-server/docs/configuracao.md` (sync fields) and the app-side consequence in `elisdb-android/docs/uso.md` if the behaviour is visible to the user.
