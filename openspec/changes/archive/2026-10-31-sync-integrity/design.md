# Design

## Context

The server resolves conflicts by last-write-wins on `updated_at` and lets clients pull changes incrementally with `since=`. To make incremental pulls reliable it was re-stamping accepted writes to `max(updated_at, previous_delivery + 1)` — using the same column it compares for last-write-wins. That conflates two different clocks: the *device's* modification time (what decides who wins) and the *server's* delivery order (what decides what a device has already seen). Every defect below follows from that conflation or from a path that bypasses it.

## Decisions

### Separate `delivery_ts` from `updated_at`

`clientes` and `tombstones` gain `delivery_ts INTEGER NOT NULL DEFAULT 0`:

- `updated_at` / `deleted_at` are stored **exactly as the device sent them** and are the only input to last-write-wins and to the tombstone comparison.
- `delivery_ts = max(device timestamp, previous delivery watermark + 1)` is assigned inside the same `BEGIN IMMEDIATE` transaction and is the **only** input to `pull(since)` filtering, pagination and the client watermark.

This removes the failure mode where a device whose clock is a minute slow (well inside the 5-minute signature window) pushes an edit that the server has already "delivered" at a higher timestamp: the edit is judged on `updated_at` alone, so it is accepted, and its `delivery_ts` is still newer than anything issued before, so every device receives it.

A single monotonically increasing value also keeps the existing keyset pagination `(timestamp, id)` valid: `delivery_ts` is unique per write, so two records can share it only in the backfilled legacy data.

**Backfill.** The migration sets `delivery_ts = updated_at` (and `= deleted_at` for tombstones). Old app versions keep their watermark in `updated_at` space; since every future delivery timestamp is greater than all timestamps already issued, those clients never skip a record. The migration creates the `(delivery_ts, id)` indexes itself, because the base schema declares columns only — the columns do not exist until the `ALTER TABLE` runs.

**Ordering.** The new migration must run *before* `_migrate_clients_nullable_coords`, which rebuilds `clientes` from an explicit column list. The rebuild includes `delivery_ts`, otherwise the column would be dropped again on the very databases that need the coordinate fix.

### Deletions are deliveries too

`delete_client` previously inserted the tombstone with the device timestamp and committed without taking the write lock. It now uses the same `BEGIN IMMEDIATE` + watermark + `max(...)` sequence as `save_client`, storing `deleted_at` (device time, used for last-write-wins and resurrection) and `delivery_ts` (server order, used by `pull`). A deletion can therefore never be older than what other devices have already consumed.

**Tombstones are never garbage collected.** Keeping them is what makes "delete wins over a stale edit" work; the pull already accepts a `since` so they do not grow the incremental payload.

### The migration must prove it did not lose photos

`PRAGMA foreign_keys = OFF` is a silent no-op inside a transaction. The fix is not just a `commit()` before it:

1. commit any open transaction;
2. run `PRAGMA foreign_keys = OFF` and **read it back**, aborting if it is still on;
3. open an explicit transaction, rebuild the table, copy rows;
4. read `PRAGMA foreign_key_check` and compare the photo count before and after; abort on mismatch;
5. restore `PRAGMA foreign_keys = ON` in a `finally`.

Aborting is better than proceeding: a failed startup is recoverable, a silent photo wipe is not. The regression test builds a legacy database (non-nullable coordinates, no `created_at`, one photo) and asserts the photo row survives `Database.init()`.

### The garbage collector compares resolved paths

`fotos.caminho` is always written absolute (`_foto()` resolves it). The collector must resolve the directory the same way, otherwise a relative `ELISDB_PHOTOS` makes every stored path differ from the candidate and every file look orphaned. `Database.__init__` stores `os.path.abspath(fotos_dir)`; the collector also skips anything that is not a regular file, so a stray subdirectory cannot abort the sweep at startup.

### A push reports photos it could not store

`_replace_fotos` silently skipped photos whose file was missing: no row, but the record was `accepted`, so the app recorded the hash as "already uploaded" and never sent the bytes again. Now `_replace_fotos` returns `(kept, missing)`, `save_client` returns it, and `PushResult.missing_photos` carries the identifiers. The app excludes them from the uploaded-hash bookkeeping, so the next sync uploads them again.

This is deliberately *not* an error status for the record: the client data itself was stored correctly, and failing the whole record would make the app retry an edit the server already accepted.

### Acknowledgements are conditional

`pushEdits()` used to write back the entity it had read before the network call. The write is replaced by a targeted update:

```sql
UPDATE clientes SET pending = 0 WHERE id = :id AND updated_at = :sent
```

The row count tells the app whether the record is still the one it sent. Zero means the user edited it during the sync: the row keeps `pending = 1` and the new edit is pushed next time. Nothing is read-modify-written from a stale snapshot.

The same rule applies to photo hashes: `recordPushedHashes` only records the photos the server confirmed.

### The watermark stops below anything skipped

If a pull skips a record because it has pending edits, advancing the watermark past it means the server version is never fetched again. The pull therefore tracks the smallest `delivery_ts` among skipped records and advances the watermark to `min(that - 1, highest delivery_ts seen)`. The next pull re-requests the skipped range, which is small (one record), and converges as soon as the push clears the pending flag.

### Revalidate `pending` inside the write transaction

`applyServerItem` downloads photos before writing, so the `pending` check happens seconds before the write. The upsert now re-reads the row inside the same transaction and skips the item if it became pending in the meantime; it is then handled by the watermark clamp above.

### The 2→3 migration recreates the table

`ALTER TABLE ... DROP COLUMN` requires SQLite 3.35 (Android 14). The app supports Android 7 (API 24), where the statement fails at startup. The migration is rewritten with the same table-recreate pattern already used for 3→4: create `clientes_new` without `telefone`, copy the columns, drop the old table, rename. `@Upsert` and the photo cascade are unaffected because the rebuild copies rows rather than deleting clients.

## Risks / trade-offs

- **Two timestamps to reason about.** Every future change touching sync must know that `updated_at` is conflict time and `delivery_ts` is delivery order. This is documented in the schema comments and in the spec deltas.
- **Backfilled timestamps can repeat.** Legacy rows keep `delivery_ts = updated_at`, so several rows may share one value; the keyset cursor `(delivery_ts, id)` already handles ties.
- **A skipped record delays the watermark.** An aggressively pending record pins the watermark until it is pushed. That is the correct trade-off: re-fetching a small range is cheaper than permanent divergence.
- **`missing_photos` costs an extra round-trip.** The photo bytes are uploaded again on the next sync; the alternative is losing the photo.
- **The migration aborts instead of continuing.** A database in an unexpected state stops the server at startup with a clear error instead of starting with photos silently gone.

## Alternatives considered

- **Bump `deleted_at` on the server** (the minimal fix for deletions only): it would make the tombstone "newer" than a concurrent edit and break last-write-wins, exactly the bug this change removes for pushes.
- **Send a full pull every time**: simple, but O(catalog) per sync and it removes the reason incremental pulls exist.
- **A monotonically increasing integer sequence per write** instead of a timestamp: cleaner in theory, but it changes the wire contract for every record and complicates the existing `since`-by-time clients; `max(device, watermark + 1)` gives the same ordering guarantee within the existing format.
- **Clearing `pending` by re-reading and comparing in Kotlin** before the update: still a read-modify-write race; the conditional `UPDATE` is atomic in SQLite.
