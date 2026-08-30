## 1. Shared sync entry point

- [x] 1.1 Move `SyncCoordinator` ownership into `SyncManager` (hold `coordinatorProvider` result lazily or inject once); keep construction out of `SyncWorker`.
- [x] 1.2 Add `suspend fun sync(): SyncOutcome` to `SyncManager` guarded by a shared `Mutex` (single-flight); map outcome → `_status`/`_fault` inside it.
- [x] 1.3 Refactor `syncNow()` to call `sync()` while keeping its signature/behavior for manual callers.

## 2. Periodic worker parity

- [x] 2.1 Rebuild `SyncWorker.doWork()` to obtain the app's `SyncManager` and `return mapToResult(syncManager.sync())` — no local `SyncCoordinator` construction.
- [x] 2.2 Keep WorkManager mapping identical to today (Success→`Result.success()`, Auth/Config→`Result.failure()`, Transient→`Result.retry()`).
- [x] 2.3 Make `SyncStatus`/`SyncFault` update on the periodic path so background failures surface in `SettingsScreen`/`SyncStatusBar`.

## 3. Tests

- [x] 3.1 Unit test: `sync()` with a concurrent second call does not start a second coordinator run (single-flight).
- [x] 3.2 Unit test: periodic-path failure (transient) resolves to `Result.retry()` AND updates `_status`/`_fault`.
- [x] 3.3 Unit test: manual join while periodic in-flight produces exactly one push (fake API call counter == 1).
- [x] 3.4 Run `./gradlew testDebugUnitTest`; confirm green.