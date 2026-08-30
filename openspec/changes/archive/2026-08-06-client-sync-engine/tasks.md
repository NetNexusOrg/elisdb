## 1. Sync coordinator

- [x] 1.1 Add a `SyncCoordinator` that orchestrates push-then-pull ordering
- [x] 1.2 Implement push of pending (created/edited) records, mapping DTOs and decoding handled by transport layer; keep pending until per-record ack
- [x] 1.3 Implement push of pending deletions; on server confirm, permanently delete the local record and its photos
- [x] 1.4 Implement pull application: overwrite non-pending records with server state; keep unseen pending records for a later push; drop records the server-clear ledger says are gone
- [x] 1.5 Handle auth-failure distinct from transient errors (stop on auth-fail rather than retry)

## 2. Scheduling & retry

- [x] 2.1 Wire `SyncManager` with WorkManager: network constraint, backoff/retry, periodic triggers
- [x] 2.2 Register the coordinator lifetime in `ElisdbApp` (start on launch)
- [x] 2.3 Surface a live `syncStatus` flow (idle / in-progress / error) for the UI
- [x] 2.4 Expose `syncNow()` for manual triggering and refresh

## 3. Repository integration

- [x] 3.1 `ClienteRepository` exposes the sync status flow and `syncNow()`
- [x] 3.2 Update list/details to refresh immediately after a successful pull
- [x] 3.3 Ensure observe flows in the UI react to post-sync local changes

## 4. Tests & verification

- [x] 4.1 Unit test: push of pending records, cleared only after ack
- [x] 4.2 Unit test: offline push keeps data and status reports error; later retry succeeds
- [x] 4.3 Unit test: delete transmitted and local record finalized after confirm
- [x] 4.4 Unit test: pull overwrites non-pending local data; does not overwrite pending edits
- [x] 4.5 Verify photo file lifecycle (decode on pull, remove on confirmed delete)
- [x] 4.6 Run Android unit tests