## Why

The pull path already deduplicates photos by content (`photo-content-identity`, `PhotoHashes`), but the push path sends every photo of a client as base64 on every edit. Editing only a client's name re-uploads all of its photos:

- With a client holding 10 photos and two devices on mobile data, each small edit costs the full photo payload over the network.
- The server unconditionally replaces the client's photos (`_replace_fotos`) on each push, so it cannot reuse stored files.

The result: bandwidth and storage churn disproportionate to the actual change.

## What Changes

- On push, the client sends only **new or changed** photos with their base64 data; unchanged photos are referenced by id (and optionally by content hash) so the server can keep its stored files.
- The server's save path merges photo updates instead of deleting and rewriting all rows/files, keeping files whose content hash matches what it already stores.
- Reuses the existing content-identity mechanism so "changed" is decided by content, not by any client-side flag.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `client-sync`: push payload carries partial photos (id + data only for changed photos).
- `client-photos`: edit flow tracks which photos changed since last sync.
- `photo-content-identity`: content hash reused on the push path to decide which photos need re-upload.
- `server-photo-storage`: saves photos by merge (keep unchanged files), not full replace.
- `server-sync`: push accepts records where some photos carry no data payload.

## Impact

- **App**: `SyncMapper.toRecord` (partial photo payloads), `SyncCoordinator`/repository to track changed photos, DTOs.
- **Server**: `database.py` `save_client`/`_replace_fotos` → merge semantics keyed by content hash; `main.py` unchanged contract.
- **Behavior**: editing a client's name no longer re-uploads its photos; unchanged photo files are reused server-side.
- **Wire format**: `FotoInDto.dataB64` becomes optional in the push payload — compatible with the existing client only if the app also stops sending full data; a coordinated release is required.
- **Tests**: app unit tests for partial push payloads; server pytest for photo merge/keep-unchanged.
