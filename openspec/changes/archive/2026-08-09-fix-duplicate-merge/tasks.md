## 1. Model and outcome

- [x] 1.1 Add `DuplicateMerged(nome: String)` to `SyncOutcome` (convert sealed class as needed) and thread it through `SyncManager.syncNow()` so it can be surfaced.
- [x] 1.2 Add a one-shot `lastResult` (or `_fault`/`_status` extension) to `SyncManager` that carries the merged record name for the UI.

## 2. Coordinator duplicate handling

- [x] 2.1 In `pushEdits()`, branch on `status == "duplicate" && existingId != null`: re-attribute the local record to `existing_id` (id swap), keep `pending = true`, and record a `DuplicateMerged` outcome instead of clearing pending.
- [x] 2.2 Keep the existing behaviour for `accepted`/`stale` statuses unchanged.
- [x] 2.3 Ensure the old record id is never left behind after re-attachment (delete-old + insert-new within one Room transaction, `INSERT OR REPLACE` semantics).
- [x] 2.4 Re-associate local photo rows to the new client id within the same transaction so photos re-push next cycle.
- [x] 2.5 Bump the merged record's `updated_at` to `now` (max of local and canonical timestamps) so a subsequent push wins by LWW.
- [x] 2.6 Confirm `pull()` no longer deletes the merged record: with `pending = true`, line `SyncCoordinator.kt:109` already protects it; add a regression test proving a `duplicate`-merged record survives the following `pull`.

## 3. UI surfacing

- [x] 3.1 Show a one-shot message in `SettingsScreen`/`SyncStatusBar` naming the merged record ("Registo «X» fundido com um registo existente.").
- [x] 3.2 Wire `SyncManager.lastResult` / fault surface into `SettingsViewModel` so the message appears after sync and clears on next sync.

## 4. Tests and docs

- [x] 4.1 Add a `SyncCoordinator` unit test: duplicate ack re-attaches local record to `existing_id`, keeps photos, keeps pending, and the following pull does NOT delete it.
- [x] 4.2 Add a test for id collision when `existing_id` already exists locally.
- [x] 4.3 Record the duplicate-merge behaviour in `docs/uso.md`.
- [x] 4.4 Run existing test suite and `./gradlew testDebugUnitTest`; confirm green.