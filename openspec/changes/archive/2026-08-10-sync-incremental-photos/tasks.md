## 1. Photo compare helper

- [x] 1.1 Add a photo content comparison: compute SHA-256 over decoded bytes and compare to the hash of an existing local file; expose via a `PhotoHashes`/`PhotoMaterializer` helper.
- [x] 1.2 Expose a `compareAndSkip(uriLocal, dataB64, mime, ordem): Boolean` (existing file matches) in the photo layer.
- [x] 1.3 Unit test: same bytes → skip; different bytes → materialize; `mime`/`ordem` mismatch → materialize.

## 2. Materialize (two-phase)

- [x] 2.1 Add a two-phase write: decode → temp file in same dir → fsync → atomic rename to final URI; return the final URI.
- [x] 2.2 Sequence inside a `PhotoMaterializer.materialize()`: return `MaterializeResult.Keep(existing)` or `MaterializeResult.Repointed(newUri)`.
- [x] 2.3 Wire fallback to copy+delete where `ATOMIC_MOVE` unsupported.

## 3. applyServerItem rewrite

- [x] 3.1 Rewrite `applyServerItem` (SyncCoordinator.kt:116-150) to: compare each server photo → skip unchanged; hoist file writes out of the DB transaction; insert rows within a per-item transaction; delete replaced old files only after the new row is committed.
- [x] 3.2 Keep client-row insert and photo-row inserts in the same per-item Room transaction.
- [x] 3.3 Confirm no rewrite when the matched local client has the same set of fotos in the same order (full skip).
- [x] 3.4 Preserve failure semantics: throw on decode/write failure → pull aborts with `TransientError` (existing catch-all).

## 4. Tests

- [x] 4.1 SyncCoordinator test: unchanged photo on pull — existing file/row intact and `storeBase64` not called.
- [x] 4.2 SyncCoordinator test: changed photo — old file removed only after new row committed; on mid-failure old file+row remain intact.
- [x] 4.3 Idempotency test: two consecutive pulls converge to identical files/row hashes.
- [x] 4.4 Run `./gradlew testDebugUnitTest`; confirm green.

## 5. Docs

- [x] 5.1 Update `docs/uso.md` and (at archive) `client-sync` spec wording: pulled photos are re-materialized only when unchanged.