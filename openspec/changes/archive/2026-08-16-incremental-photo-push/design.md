## Context

The push path currently sends every photo of a client as base64 on every edit (`SyncMapper.toRecord` includes all `fotos` with `dataB64`), and the server's `_replace_fotos` deletes and rewrites all photo rows/files on each save. Editing only a client's name therefore re-uploads all of its photos over the network. The pull path already avoids this via content identity (`photo-content-identity`); the push path does not.

## Goals / Non-Goals

**Goals:**
- Editing a client's non-photo fields does not re-upload unchanged photos.
- The server reuses stored photo files for unchanged photos instead of deleting/rewriting them.

**Non-Goals:**
- Changing the pull content-identity mechanism (already in place).
- Changing photo deduplication or duplicate-merge behaviour.

## Decisions

**D1 — `FotoInDto.dataB64` becomes optional in the push payload.**
- A photo with `dataB64 == null` is a **reference**: the server must keep any stored file/row with that id. A photo with data is a full upload.
- The app decides which photos need data using the content hash of the last-pushed version.
- *Rationale*: minimal wire change; the server's save already keys photos by id.

**D2 — Server merges photo sets instead of replacing.**
- `_replace_fotos` becomes a merge: for each incoming photo, if it carries data, write/overwrite the file and row; if it is a reference, keep the existing file/row when present (and skip when absent). Rows/files the server stores but the push no longer lists are removed as today.
- *Rationale*: keeps the "server owns the photo set" contract while preserving unchanged files.

**D3 — App tracks last-pushed photo hashes.**
- New persisted store mapping `photoId -> contentHash` of the last successful push (SharedPreferences, mirroring the offset store pattern). On push, `SyncMapper` (or its caller) sends data only when a photo is new, its hash differs from the stored hash, or no hash is known yet.
- After a successful push, the hashes for that client's photos are updated to the current ones.
- *Rationale*: "changed since last push" is a content decision, consistent with the pull-side identity; a persisted hash store survives restarts.

**D4 — First push sends everything.**
- With no stored hashes, every photo is treated as new and sent with data — same as today.

**D5 — Files touched.**
- Server: `models.py` (`FotoIn.dataB64` optional), `database.py` (`_replace_fotos` merge).
- App: `RemoteDto` (`FotoInDto.dataB64` nullable), `SyncMapper` (reference vs data), a new `PhotoPushState` store, `SyncCoordinator` (capture/update hashes on ack), `AppContainer` wiring.
- No DB schema change; the hash store lives in SharedPreferences.

## Risks / Trade-offs

- [Wire-format change on push] → Coordinated release; the server tolerates both forms (data present or absent), so an older app that always sends data keeps working.
- [Stale hash after failed push] → Hashes update only on a successful ack; on failure the next push re-sends with data (safe).
- [Reference with missing server copy] → Ignored (no file/row created); the photo simply does not appear until a full-data push covers it.

## Migration Plan

1. Deploy the server change first (merge + optional dataB64; backward compatible with old clients).
2. Ship the app update (sends references for unchanged photos).
3. No data migration; the hash store starts empty, so the first post-update push re-sends all photos once, then the mechanism engages.

## Open Questions

None.
