## Why

The app currently stores all data only on-device with no way to back it up or access it from other devices, and clients/locations duplicate manually across installations. A small, dedicated database server (in `../elisdb-server`) becomes the authoritative store, owning duplicate detection, conflict resolution and data management, while the Android app keeps working offline-first and does no heavy lifting.

## What Changes

- Introduce a new, standalone database server at `../elisdb-server`, implemented in Python (FastAPI) with a SQLite store — kept intentionally simple.
- The server owns all data management responsibilities: detecting and refusing duplicates, resolving sync conflicts (last-write-wins by timestamp), and persisting records + photos.
- Define a small HTTP sync API (push changes, pull changes) that the Android app and other clients can call. This change covers the **server and the protocol only**; wiring the app to the API is a separate future change.
- The app and all clients remain offline-first: they work locally and sync with the server when reachable.

## Capabilities

### New Capabilities
- `server-sync`: a minimal HTTP database server that persists clients and photos, rejects duplicates by a natural key, resolves edit conflicts by last-write-wins, and exposes push/pull endpoints for offline-first clients.

### Modified Capabilities
<!-- None - the Android app behavior is unchanged in this change. A future change wires the app to this server. -->

## Impact

- New standalone codebase in `../elisdb-server` (Python, FastAPI, SQLite) — does not touch the existing Android code in this repo.
- No changes to the Android app, its Room database, or existing capabilities.
- Introduces a network API contract (protocol) that clients must follow; documented in the server-sync capability spec.