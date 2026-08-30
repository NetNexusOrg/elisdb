## Why

Client names are stored and displayed exactly as typed, so the same client can appear with different casings ("Maria", "maria", "MaRiA") across devices and entries. The names should be stored and presented consistently in uppercase regardless of how the user types them, avoiding visual inconsistency between the client list, detail screens and the server.

## What Changes

- The client name is normalized to uppercase whenever it is entered or edited, so the stored value is always uppercase.
- The create/edit form uppercases the name as the user types it, giving immediate visual feedback.
- The server stores client names in uppercase on push, so names remain uppercase across devices after synchronization.
- Existing stored names that are not yet uppercase become uppercase on the next edit, push or pull that touches them.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `client-data`: client name stored in uppercase regardless of input casing.
- `client-screens`: create/edit form uppercases the name as typed; name displayed in uppercase.
- `server-sync`: server persists client names in uppercase on push.

## Impact

- **App**: `ClientFormViewModel.onNomeChange`, `ClienteRepositoryImpl.save()` (create + edit), and `SyncCoordinator.applyServerItem()` (uppercase when storing pulled names).
- **Server**: `database.py` `save_client()` / `normalize_nome()` in `models.py`.
- **Behavior**: name casing normalization does not change duplicate detection, which already ignores case (`COLLATE NOCASE`); it only makes the stored/presented form consistent.
- **Tests**: app unit tests for form input and repository save; server pytest asserting the stored name is uppercased.