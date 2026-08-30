## Context

The Android app (this repo) keeps clients and photos in a local Room DB (`clientes`, `fotos`) and is offline-first. There is currently no server or sync. See proposal.md - Why for motivation; the requirements for the server are in specs/server-sync/spec.md. The server is a new standalone codebase in `../elisdb-server`, Python + FastAPI + SQLite.

## Goals / Non-Goals

**Goals:**
- A single-file-easy, dependency-light FastAPI server with SQLite as the durable store.
- Server owns all DB management: persistence, duplicate detection, LWW conflict resolution, cascade delete.
- A simple HTTP protocol for push/pull that offline-first clients can call; records carry a client id, client-generated `updatedAt` timestamp, and photo byte payloads.
- Server starts with one command, stores data in a single `.db` file, and is testable via HTTP and unit tests.

**Non-Goals:**
- No changes to the Android app or its Room schema in this change (a future change wires the app to this API).
- No user authentication, multi-tenant isolation, or TLS (local/single-operator tool). No photo storage on disk: photos are stored as BLOBs in SQLite to keep the server trivially simple.
- No server-side media thumbnailing, geospatial queries, or incremental delta sync beyond full pull.

## Decisions

### D1: FastAPI + SQLite via sqlite3 (stdlib)

Python 3 + FastAPI + uvicorn, with SQLite accessed through the standard-library `sqlite3` module (no ORM). Rationale: minimal dependencies, one process, durable file storage, trivially runnable. Alternatives considered: SQLAlchemy (heavier than needed), Node/Express (different ecosystem), a pure stdlib http server (loses FastAPI's validation/docs/path handling).

### D2: Single-table design with photos as BLOBs

Two tables: `clientes` (id TEXT, nome TEXT UNIQUE, contrato, latitude, longitude, notas, updated_at INTEGER) and `fotos` (id TEXT, cliente_id TEXT, mime TEXT, data BLOB, ordem INTEGER), photo bytes stored inline. Rationale: the "server is responsible for all DB management" and "as simple as possible" requirements favor storing everything in one file; deleting a client cascades to its photo rows. Alternatives considered: storing photo files on disk (adds file lifecycle, permissions, GC — rejected as unnecessary complexity).

### D3: Duplicate detection by normalized name

`nome` is UNIQUE COLLATE NOCASE in SQLite, and whitespace is normalized (trimmed, inner whitespace collapsed) before insert. A create that collides returns 409 with the existing client id. Rationale: the spec requires name-based dedup ignoring case/whitespace; a DB unique constraint makes it atomic and race-safe for the single-process server. Alternatives considered: application-level pre-check (racy), UUID-only identity (would not catch duplicates).

### D4: Conflict resolution by last-write-wins on `updated_at`

The client supplies `updated_at` (epoch millis). On push, for a known client id the server keeps the payload only if `updated_at` >= stored value; the response echoes which records won/lost. New records (unknown id) are inserted; if the name collides with an existing different-id record, the update is treated as a conflict per D3. Rationale: simplest deterministic policy that matches the chosen requirement; no merge logic. Alternatives considered: vector clocks / field-level merge (overkill for this data), version-and-reject (pushes conflict work to the client — explicitly ruled out by the user).

### D5: REST endpoint surface

- `POST /api/push` — body: `{ "records": [ { client: {...}, fotos: [{ id, mime, dataB64 }] } ] }`; returns per-record `accepted`/`duplicate`/`stale` status.
- `GET /api/pull` — returns all clients with photos (base64), one envelope, ordered by name.
- `GET /api/clients` — management listing (metadata only, no photo bytes).
- `DELETE /api/clients/{id}` — delete client + photos.
Rationale: four endpoints cover push/pull/management; matches the capability requirements. Alternatives considered: delta sync via `since` cursor (deferred — spec's pull is full snapshot).

### D6: Photo payloads as base64 in JSON

Photo bytes travel as base64 strings inside JSON. Rationale: one transport for the whole protocol, no multipart parsing, trivial for clients to build. Alternatives considered: multipart/form-data (faster but more client/server machinery), separate blob endpoints (more round trips).

## Risks / Trade-offs

- [Duplicate detection keyed only on name can merge distinct people sharing a name] → Accepted trade-off per the chosen requirement; the 409 response exposes the existing record id so an operator can intervene.
- [LWW can lose edits if two devices edit and the timestamps are equal] → `>=` comparison keeps the stored record on ties; documented in the protocol.
- [Photo BLOBs grow the single DB file] → Accepted for a single-operator local server; noted as a migration trigger if scale grows.
- [No auth means anyone with network access can read/write] → Server intended for local/private use; documented in the README.

## Migration Plan

Greenfield: nothing to migrate. The server owns a fresh SQLite file created on first start; schema created idempotently (`CREATE TABLE IF NOT EXISTS`). No rollback needed for the server itself; the Android app is untouched by this change.

## Open Questions

None.
