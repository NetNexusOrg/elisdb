## Why

When a client created locally collides by name with an existing server record, the server rejects the push with `duplicate` + `existing_id`, but the app clears `pending` on any acknowledgement and later deletes the local record (and its photos) during `pull`. The user's newly created client silently disappears and its notes/contrato/photos are lost. Found during the sync system review (R1, blocking, `SyncCoordinator.kt:55-63` + `107-112`).

## What Changes

- On a `duplicate` push ack, the app SHALL reconcile the local record with the server's `existing_id` instead of clearing `pending` blindly and letting `pull` delete it.
- The reconciliation SHALL preserve the user's locally entered data by merging fields onto the existing server record (server remains authoritative for conflicts).
- Photos of the duplicate local record SHALL be transferred to the retained record, not deleted.
- The user SHALL be informed when their freshly created client was merged into an existing one.
- Remove the silent-loss path: a record acknowledged as `duplicate` must never result in `finalizeDeletion` on the next `pull`.

## Capabilities

### New Capabilities

- `duplicate-reconciliation`: how the app responds to a `duplicate` push acknowledgement, adopting the server's `existing_id` while preserving local data.

### Modified Capabilities

- `client-sync`: the push acknowledgement currently clears pending on any status; the new behaviour changes how `duplicate` statuses are processed.
- `server-sync`: the server already returns `existing_id` on duplicate; the sync-reconciliation contract may need clarity on which fields merge, but no server behavior change is expected unless the delta spec requires it.

## Impact

- `app/src/main/java/com/example/elisdb/sync/SyncCoordinator.kt` — duplicate ack handling and pull reconciliation.
- `app/src/main/java/com/example/elisdb/sync/SyncMapper.kt` — record/field mapping for the merge.
- `SyncOutcome`/`SyncManager`/`SyncStatusBar` — exposed UI message when a duplicate merge happens.
- Server: no code change expected; `database.py` already deduplicates on name and returns `existing_id`.
- Tests: `SyncCoordinator` tests for duplicate path; a scenario in `server-sync` spec if contract changes.