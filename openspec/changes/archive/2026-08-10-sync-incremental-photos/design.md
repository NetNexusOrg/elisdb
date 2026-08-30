## Context

See proposal.md. Today `applyServerItem` (SyncCoordinator.kt:116-150) unconditionally: (i) deletes existing foto files (`photoStore.delete`) + DB rows, (ii) inserts the client row, (iii) for every server foto calls `storeBase64` → new file and inserts a row. All inside the pull transaction. This is the R3 problem: delete-before-write (row/file mismatch on mid-failure) + full rewrite churn.

Local photo DB model (`Foto`: id, clienteId, uriLocal, mime, ordem) has no content hash today; photo content identity must be derived on the fly.

## Goals / Non-Goals

**Goals:**
- Never rewrite unchanged photos in `pull`.
- Sequence writes so DB rows never reference missing files after a mid-pull failure.
- Keep the pull outcome surface compatible: Success/Failure without a schema change.
- Idempotent retry: a retried pull converges to the same final state.

**Non-Goals:**
- Server protocol change for bandwidth (pull still returns full dataset with base64; bandwidth savings are tackled by `sync-polish-and-scale`).
- Historical photo deduplication or content-addressable storage layout beyond a per-photo hash value.
- Deleting orphaned files on every pull (deferred to a lazy sweep, R8).

## Decisions

**D1 — Compare by SHA-256 of the base64-decoded bytes.** Compute hash over photo bytes; if `hash(existing file)` equals the server photo's hash and `mime`/`ordem` match, keep the existing row/file and skip. This is a pure compare — no extra table. Overhead is one hash per foto per pull; negligible vs. re-writing.
- *Alternative:* server returns a per-photo content hash in the pull (`FotoOutDto`) → removes client-side hashing for the unchanged case but requires a protocol change; defer to the bandwidth change. Chosen: client-side hash now, wire hash later (R4–R8).

**Decision 2 — Two-phase materialization per photo:**
1. Decode server base64 in memory (already done by `FotoDto.dataB64` string).
2. Write the new file to disk via a temp path in the same dir, fsync, then rename to final URI.
3. Only then upsert/insert the DB row inside the photo-aware transaction.
4. Legacy cleanup: delete the replaced old file only AFTER the DB row commit; if no change (equal), never touch disk.
This removes delete-before-write; the window where rows reference missing files disappears because the old file is removed after the new row is committed. A leftover old file (if a crash occurs between step 3's commit and step 4's delete) is an orphan the lazy sweep (R8) reclaims.
- *Alternative:* wrap file ops in a compensating-teardown block inside the Room transaction — rejected because Room's rollback cannot undo filesystem deletes; the two-phase order is the reliable approach.

**Decision 3 — Out-of-transaction sequencing in `pull()`.** `pull()` iterates server items; each item materializes photos (file I/O + per-photo row insert) and then applies the client row, still within one Room transaction per item. To guarantee "write file before row", we break into: hash/compare → store file (outside transaction) → inner transaction { insert rows }. The `runInTransaction` boundary wraps only the DB mutations, with file I/O hoisted before it. Add a helper `PhotoMaterializer` encapsulating compare/store/commit/cleanup so `applyServerItem` reads cleanly.
- *Alternative:* keep everything in one transaction and rely on write-ahead ordering — insufficient because commit order inside a transaction is not a durable file-ordering guarantee across crash.

**Decision 4 — Failure semantics.** If any step throws (decode/write/DB), the item is considered failed, the pull is aborted with `TransientError` (matching today's catch-all), and previously committed items stand (room transaction boundary). Idempotent retry: unchanged photos are recognized again; changed photos are re-materialized to the same final path.

## Risks / Trade-offs

- **Two-phase rename needs atomic file ops**: use `Files.move(..., ATOMIC_MOVE)` with temp file in the same directory; fallback to copy+delete if unsupported. [Risk: non-atomic filesystem] → fallback documented.
- **Memory for big photos**: materializing one photo at a time (not the whole batch) keeps peak memory bounded. Note server-side payload limit.
- **Foto row 'hash'**: no schema change; hash computed transiently. Avoids Migration (Room). If a future delta uses server-side hash, migrating columns would be more involved; acceptable now.
- **Unchanged-photo skip trades CPU (hash) for I/O savings**: for small catalogs the win is small, but the correctness guarantee (no rewrite) is the real fix; performance scales up with catalog size.

## Migration Plan

- App-only. No DB schema change, no memoization, server untouched. Rollback: revert `applyServerItem` change or add a feature toggle (keep old path until validated).
- Docs: update `docs/uso.md` if user-visible message about failed pulls changes (unlikely). `client-sync` spec delta wording updated at archive.

## Open Questions

- Whether to also move the client-row insert (`clienteDao.insert`) inside the same per-item transaction — yes, it stays: only file ops and hash compare are hoisted outside; rows and client transplants remain in one Room transaction per item.
- Do we need a shared/named `PhotoMaterializer` in the app container for testability? Yes — private/DI-free; unit-test via a fake `PhotoStore`.