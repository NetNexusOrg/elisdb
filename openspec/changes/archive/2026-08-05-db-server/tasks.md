## 1. Server setup

- [x] 1.1 Create `../elisdb-server` with a minimal Python project: `app/main.py`, `app/database.py`, `app/models.py`, `requirements.txt` (fastapi, uvicorn), `README.md`, and a `.gitignore` (for the `.db` file).
- [x] 1.2 Define the SQLite schema (idempotent `CREATE TABLE IF NOT EXISTS`): `clientes` with `id TEXT PRIMARY KEY`, `nome TEXT NOT NULL`, `contrato TEXT`, `latitude REAL`, `longitude REAL`, `notas TEXT`, `updated_at INTEGER NOT NULL`, and a `UNIQUE COLLATE NOCASE` index on `nome`; `fotos` with `id TEXT PRIMARY KEY`, `cliente_id TEXT NOT NULL REFERENCES clientes(id) ON DELETE CASCADE`, `mime TEXT`, `data BLOB`, `ordem INTEGER`.

## 2. Core behaviour

- [x] 2.1 Implement `save_client` with name normalization (trim + collapse inner whitespace), duplicate detection relying on the DB unique constraint (returns `duplicate` + existing id on collision), and last-write-wins: for an existing id keep the payload only when `updated_at >=` stored value (returns `accepted` or `stale`).
- [x] 2.2 Implement photo persistence alongside the client (insert `fotos` rows from base64 payloads) and cascade delete of a client's photos on client delete.
- [x] 2.3 Implement `list_clients` (ordered by normalized name) and `get_client_with_photos` used by the pull endpoint.

## 3. HTTP API

- [x] 3.1 Implement `POST /api/push` accepting `{ "records": [...] }` where each record has `client` (id, nome, contrato, latitude, longitude, notas, updated_at) and `fotos` (id, mime, dataB64, ordem); return per-record `accepted`/`duplicate`/`stale` status.
- [x] 3.2 Implement `GET /api/pull` returning all clients with their photos (base64) in one envelope ordered by name.
- [x] 3.3 Implement `GET /api/clients` (metadata only, no photo bytes) and `DELETE /api/clients/{id}` (cascades to photos); 404 for unknown id.

## 4. Verification

- [x] 4.1 Add pytest tests: duplicate create rejected (case/whitespace-insensitive), stale update loses, newer update wins, push persists client + photos, delete cascades, pull returns full data, restart preserves data.
- [x] 4.2 Run `python -m pytest` in `../elisdb-server` — all tests pass.
- [x] 4.3 Manual check: start `uvicorn` and confirm `GET /api/pull`, `POST /api/push`, `GET /api/clients`, and `DELETE /api/clients/{id}` behave per the spec on the running server.
