"""ElisDB server — minimal FastAPI app that owns all data management.

Duplicates are refused by normalized name, conflicts are resolved by
last-write-wins on updated_at, and clients/photos live in a single SQLite file.
"""

import logging
import os
from pathlib import Path

import time

from fastapi import Depends, FastAPI, HTTPException, Request, Response

from . import auth
from .database import Database
from .models import (
    AuditEntryOut,
    ClienteComFotosOut,
    ClienteOut,
    PushRequest,
    PushResult,
    TombstoneOut,
)

logger = logging.getLogger("elisdb.server")


def _read_version() -> str:
    """Read version from VERSION file or ELISDB_VERSION env var, with fallback."""
    env_version = os.environ.get("ELISDB_VERSION")
    if env_version:
        return env_version
    version_file = Path(__file__).resolve().parent.parent / "VERSION"
    try:
        return version_file.read_text().strip()
    except FileNotFoundError:
        return "0.0.0-dev"

# Max accepted photo upload size in bytes (20 MB). Overridden by
# ELISDB_MAX_PHOTO_BYTES. Rejects oversized uploads with 413 instead of
# buffering the whole body in memory.
MAX_PHOTO_BYTES = int(os.environ.get("ELISDB_MAX_PHOTO_BYTES", "20971520"))

db = Database(
    os.environ.get("ELISDB_DB", "elisdb.db"),
    fotos_dir=os.environ.get("ELISDB_PHOTOS"),
)


async def require_auth(request: Request) -> None:
    """FastAPI dependency verifying the ed25519 request signature.

    On success the authenticated allowlist key record (`id`, `public_key`,
    `comment`) is attached to the per-request ``request.state`` (never the
    shared ``app.state``) so concurrent requests cannot overwrite each other's
    identity.

    The client may send its public key in the ``X-Elisdb-Key`` header to skip
    verifying against every allowlisted key. A captured signature is rejected
    if replayed within its validity window.
    """
    store = request.app.state.store
    body = await request.body()

    timestamp_str = request.headers.get(auth.TIMESTAMP_HEADER)
    signature = request.headers.get(auth.SIGNATURE_HEADER)
    if timestamp_str is None or signature is None:
        raise HTTPException(status_code=401, detail="missing signature headers")

    try:
        timestamp = int(timestamp_str)
    except ValueError:
        raise HTTPException(status_code=401, detail="malformed timestamp") from None

    if not auth.is_timestamp_fresh(timestamp):
        raise HTTPException(status_code=401, detail="stale or future timestamp")

    if auth.is_replay(signature):
        raise HTTPException(status_code=401, detail="replay detected")

    raw_path = request.url.path
    query = request.url.query
    path_with_query = raw_path if not query else f"{raw_path}?{query}"
    message = auth.build_message(request.method, path_with_query, body, timestamp)

    key_header = request.headers.get(auth.KEY_HEADER)
    if key_header is not None:
        key_record = store.get_key_by_public_key(key_header)
        if key_record is not None and auth.verify_signature(key_header, signature, message):
            auth.record_replay(signature)
            request.state.authenticated_key = key_record
            return
        raise HTTPException(status_code=401, detail="invalid signature")

    for public_key in store.get_all_public_keys():
        if auth.verify_signature(public_key, signature, message):
            auth.record_replay(signature)
            request.state.authenticated_key = store.get_key_by_public_key(public_key)
            return
    raise HTTPException(status_code=401, detail="invalid signature")


def create_app(database: Database | None = None) -> FastAPI:
    version = _read_version()
    app = FastAPI(title="ElisDB Server", version=version)
    store = database if database is not None else db
    store.init()
    app.state.store = store
    auth_dep = Depends(require_auth)

    logging.basicConfig(
        level=getattr(logging, os.environ.get("ELISDB_LOG_LEVEL", "INFO").upper(), logging.INFO),
        format="%(asctime)s %(levelname)s %(name)s %(message)s",
    )
    logger.info("ElisDB server starting (version=%s)", version)

    @app.middleware("http")
    async def _log_requests(request: Request, call_next):
        response = await call_next(request)
        # Log only method, path and status — never signatures, bodies or keys.
        if response.status_code >= 500:
            logger.error(
                "server error %s %s -> %s",
                request.method,
                request.url.path,
                response.status_code,
            )
        elif response.status_code in (401, 403):
            logger.warning(
                "auth rejected %s %s -> %s",
                request.method,
                request.url.path,
                response.status_code,
            )
        return response

    @app.get("/health")
    def health() -> dict:
        return {"status": "ok", "version": version}

    @app.get("/api/time")
    def time_now() -> dict:
        return {"time": int(time.time())}

    @app.get("/api/latest", dependencies=[auth_dep])
    def latest() -> dict:
        return {"latest_updated_at": store.latest_updated_at()}

    @app.post("/api/push", response_model=list[PushResult], dependencies=[auth_dep])
    def push(request: Request, req: PushRequest) -> list[PushResult]:
        actor = getattr(request.state, "authenticated_key", None)
        results: list[PushResult] = []
        for record in req.records:
            try:
                status, existing_id, canonical = store.save_client(record.client, record.fotos, actor=actor)
            except ValueError as exc:
                raise HTTPException(status_code=400, detail=str(exc)) from exc
            results.append(
                PushResult(
                    id=record.client.id,
                    status=status,
                    existing_id=existing_id,
                    client=canonical["client"] if canonical is not None else None,
                    fotos=canonical["fotos"] if canonical is not None else [],
                )
            )
        return results

    @app.get("/api/pull", response_model=None, dependencies=[auth_dep])
    def pull(
        since: int | None = None,
        limit: int | None = None,
        since_id: str | None = None,
    ) -> dict:
        records, tombstones, next_since, next_since_id = store.pull_all(
            since=since, limit=limit, since_id=since_id
        )
        items = [ClienteComFotosOut.model_validate(item) for item in records]
        return {
            "records": items,
            "tombstones": [TombstoneOut.model_validate(t) for t in tombstones],
            "next_since": next_since,
            "next_since_id": next_since_id,
        }

    @app.get("/api/clients", response_model=list[ClienteOut], dependencies=[auth_dep])
    def list_clients() -> list[ClienteOut]:
        return [ClienteOut.model_validate(c) for c in store.list_clients()]

    @app.get("/api/photos/{foto_id}", dependencies=[auth_dep])
    def get_photo(foto_id: str) -> Response:
        result = store.get_photo(foto_id)
        if result is None:
            raise HTTPException(status_code=404, detail="photo not found")
        mime, data = result
        return Response(content=data, media_type=mime)

    @app.put("/api/photos/{foto_id}", status_code=204, dependencies=[auth_dep])
    async def put_photo(foto_id: str, request: Request) -> Response:
        content_length = request.headers.get("content-length")
        if content_length is not None and content_length.isdigit():
            if int(content_length) > MAX_PHOTO_BYTES:
                raise HTTPException(status_code=413, detail="photo too large")
        data = await request.body()
        if len(data) > MAX_PHOTO_BYTES:
            raise HTTPException(status_code=413, detail="photo too large")
        store.write_photo(foto_id, data)
        return Response(status_code=204)

    @app.delete("/api/clients/{cliente_id}", status_code=204, dependencies=[auth_dep])
    def delete_client(cliente_id: str, deleted_at: int | None = None) -> None:
        if not store.delete_client(cliente_id, deleted_at=deleted_at):
            raise HTTPException(status_code=404, detail="client not found")

    @app.get(
        "/api/clients/{cliente_id}/history",
        response_model=list[AuditEntryOut],
        dependencies=[auth_dep],
    )
    def client_history(cliente_id: str) -> list[AuditEntryOut]:
        return [AuditEntryOut.model_validate(e) for e in store.client_history(cliente_id)]

    @app.get("/api/keys", dependencies=[auth_dep])
    def list_keys() -> list[dict]:
        # Key listing is an operator action (CLI), not exposed to client
        # devices, so a compromised or curious device cannot enumerate or
        # tamper with other devices' keys.
        raise HTTPException(status_code=403, detail="key listing is operator-only")

    @app.get("/api/keys/me", dependencies=[auth_dep])
    def me(request: Request) -> dict:
        key = getattr(request.state, "authenticated_key", None)
        if key is None:
            raise HTTPException(status_code=401, detail="not authenticated")
        return {"key_id": key["id"], "username": key["comment"] or None}

    return app


app = create_app()
