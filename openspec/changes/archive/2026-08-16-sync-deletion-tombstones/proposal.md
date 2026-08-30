## Why

Deletes have no last-write-wins semantics, so they race with concurrent edits from other devices:

- Device A deletes "Maria" while offline; Device B edits "Maria" while offline.
- When B pushes, the server no longer holds that id, so it simply accepts the record as new — "Maria" is re-created and A's delete is silently undone.
- Deletes also propagate to other devices only via a full pull (the delta pull omits older records), so a deleted client can linger on devices that sync incrementally.

The result: deletions are not convergent in a multi-device setup.

## What Changes

- The server keeps a lightweight **tombstone** (id + deletion timestamp) when a client is deleted, instead of forgetting the id.
- Pushing a record whose id has a tombstone is rejected as `stale` (the delete wins), unless the pushed `updated_at` is newer than the tombstone — in which case the record is resurrected. Delete and edit then participate in the same LWW rule.
- Deletes carry the deleting device's timestamp so they compete fairly in LWW.
- Pull returns tombstones so other devices drop the deleted record locally, including on incremental pulls.

## Capabilities

### New Capabilities
- `sync-tombstones`: server-side deletion markers with LWW participation, propagated on pull.

### Modified Capabilities
- `server-sync`: delete endpoint writes a tombstone; push honours tombstones; pull returns them.
- `client-sync`: delete marks the record with a timestamp; push handles tombstone `stale`/resurrection.
- `sync-incremental-pull`: deletions propagate on delta pulls, not only full pulls.

## Impact

- **Server**: `database.py` — tombstone table/lifecycle, `save_client` and `delete_client` semantics, `pull_all` returning tombstones.
- **App**: `SyncCoordinator` — delete carries timestamp; push reconciles tombstone responses; DTOs for tombstone records.
- **Behavior**: a concurrent edit no longer resurrects a deleted client; deletions converge across devices.
- **Wire format**: breaking change for older clients (delete semantics and pull payload). Older app versions should be considered against the deployment.
- **Tests**: server pytest for tombstone push/reject/resurrection and pull propagation; app sync tests for delete-vs-edit races.
