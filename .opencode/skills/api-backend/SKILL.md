---
name: api-backend
description: Development guide for the ElisDB server (Python, FastAPI, ed25519 auth, SQLite). Use when changing elisdb-server/ — endpoints, auth, models, provisioning, or server config.
---

# API Backend (FastAPI)

Base facts in `project-context`. This skill covers server development rules.

## Stack (verified)

- Python 3.14, FastAPI, uvicorn, Pydantic v2, `cryptography` (ed25519), SQLite.
- Entry point `app/main.py`; persistence `app/database.py`; models `app/models.py`;
  auth `app/auth.py`; key management `app/keys.py`; auto-provision `app/provision.py`.

## Endpoints (defined in `app/main.py`)

- `GET /health` — liveness, unauthenticated.
- `GET /api/time` — server epoch time (for clock offset), unauthenticated.
- `GET /api/latest` — `latest_updated_at` (max `delivery_ts` over clients **and** tombstones) plus `client_count`/`api_version`.
- `POST /api/push` — upsert records (LWW); returns per-record `accepted|duplicate|stale`.
- `GET /api/pull` — incremental pull with `since`/`since_id`/`limit` keyset pagination; returns records + tombstones.
- `GET /api/clients` — list all.
- `GET/PUT /api/photos/{id}` — photo binary by id (PUT 204, capped by `ELISDB_MAX_PHOTO_BYTES`).
- `DELETE /api/clients/{id}` — soft delete (tombstone).
- `GET /api/clients/{id}/history` — audit log (`ts` is the device `updated_at`, not server receipt time).
- `GET /api/keys` — operator-only: always `403` to authenticated clients; list keys via the CLI.
- `GET /api/keys/me` — identity of the authenticated key.

## Authentication

- Every protected route uses `require_auth` dependency (ed25519 request signature).
- Headers: `X-Elisdb-Timestamp` (epoch), `X-Elisdb-Signature` (b64), `X-Elisdb-Key` (optional public key to skip allowlist scan).
- Message signed: `METHOD\nPATH[?QUERY]\nSHA256(body)\nTIMESTAMP`.
- Enforce timestamp freshness (±300s) and replay cache. See `app/auth.py`.
- The authenticated key record is stored on `request.state.authenticated_key`, never on shared `app.state`.

## Error handling / validation

- Auth failures → `401` with a short `detail`. Validation errors → `400`.
- Oversized photos → `413` (checked from `content-length` before reading the body).
- Pydantic validators reject unsafe photo ids (path traversal) — keep this invariant.
- Return models are in `app/models.py` (`ClienteOut`, `ClienteComFotosOut`, `PushResult`, `TombstoneOut`, `AuditEntryOut`).

## Configuration (env vars)

- `ELISDB_DB`, `ELISDB_PHOTOS`, `ELISDB_LOG_LEVEL` (default INFO),
  `ELISDB_MAX_PHOTO_BYTES` (default 20 MiB), `ELISDB_SERVER_URL`,
  `ELISDB_USERS` (comma-separated auto-provision), `ELISDB_CONFIGS_DIR`.
- Push limits: `ELISDB_MAX_PUSH_RECORDS` (default 200) and
  `ELISDB_MAX_PHOTOS_PER_CLIENT` (default 500); excess gets `422`.
- Rate limiting: `ELISDB_RATE_LIMIT_REQUESTS` (default 300) and
  `ELISDB_RATE_LIMIT_WINDOW_SECONDS` (default 60); excess requests get `429`.
  Keyed by source IP. `ELISDB_TRUST_PROXY` (default false) makes it use the
  first `X-Forwarded-For` hop, only valid behind a trusted reverse proxy.
- Retention: `ELISDB_TOMBSTONE_RETENTION_DAYS` / `ELISDB_AUDIT_RETENTION_DAYS`
  (default 0 = disabled); orphan-photo GC after pushes every
  `ELISDB_PHOTO_GC_INTERVAL_SECONDS` (default 3600).

## Logging rules

- Structured logging at startup; 500s and 401/403s are logged with method/path/status only.
- Never log signatures, bodies, photo bytes, or private seeds (see `server-observability` spec).

## Conventions

- The server has an in-repo `pytest` suite under `elisdb-server/tests/`. Run it with
  `python -m pytest` from `elisdb-server/` (after installing `requirements-dev.txt`).
  CI job: `.github/workflows/server-ci.yml`. For manual end-to-end checks, run the
  server in Docker (`docker compose up -d`) and exercise endpoints (see `references/commands.md`).
- Model changes must stay consistent with the Android `remote/` + `sync/` clients.
