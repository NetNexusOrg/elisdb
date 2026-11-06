"""ElisDB server — minimal FastAPI app that owns all data management.

Duplicates are refused by normalized name, conflicts are resolved by
last-write-wins on updated_at, and clients/photos live in a single SQLite file.
"""

import asyncio
import hashlib
import logging
import os
import threading
import time
from contextlib import asynccontextmanager
from pathlib import Path

from fastapi import Depends, FastAPI, HTTPException, Query, Request, Response
from fastapi.responses import FileResponse

from . import auth, updates
from .database import (
    Database,
    IdempotencyConflict,
    MAX_FUTURE_SKEW_SECONDS,
    PhotoNotReferenced,
    PhotoQuotaExceeded,
    StaleDeleteError,
)
from .ratelimit import RateLimiter
from .models import (
    AppReleaseOut,
    AuditEntryOut,
    ClienteComFotosOut,
    ClienteOut,
    HistoryPageOut,
    PushRequest,
    PushResult,
    TombstoneOut,
    sniff_image_mime,
)

logger = logging.getLogger("elisdb.server")


API_VERSION = 2


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


def _env_int(name: str, default: int) -> int:
    """Read a positive integer env var, falling back to ``default`` when unset
    or invalid (with a warning) so a misconfiguration never crashes startup."""
    raw = os.environ.get(name)
    if raw is None:
        return default
    try:
        value = int(raw)
    except ValueError:
        logger.warning("invalid %s=%r; using default %d", name, raw, default)
        return default
    if value <= 0:
        logger.warning("invalid %s=%r; using default %d", name, raw, default)
        return default
    return value


def _env_non_negative_int(name: str, default: int = 0) -> int:
    """Read a non-negative integer env var (0 is meaningful: disabled)."""
    raw = os.environ.get(name)
    if raw is None or not raw.strip():
        return default
    try:
        value = int(raw)
    except ValueError:
        logger.warning("invalid %s=%r; using default %d", name, raw, default)
        return default
    if value < 0:
        logger.warning("invalid %s=%r; using default %d", name, raw, default)
        return default
    return value


def _env_bool(name: str, default: bool = False) -> bool:
    """Read a boolean env var (1/true/yes/on vs 0/false/no/off)."""
    raw = os.environ.get(name)
    if raw is None or not raw.strip():
        return default
    normalized = raw.strip().lower()
    if normalized in {"1", "true", "yes", "on"}:
        return True
    if normalized in {"0", "false", "no", "off"}:
        return False
    logger.warning("invalid %s=%r; using default %s", name, raw, default)
    return default


# Periodic maintenance intervals. The orphan-photo sweep scans the photos
# directory, so it runs in a background thread at most this often; retention
# pruning is indexed and runs synchronously but throttled to the same order of
# magnitude. Startup still performs both unconditionally.
PHOTO_GC_INTERVAL_SECONDS = 3600
MAINTENANCE_INTERVAL_SECONDS = 3600
# Periodic sweeps leave recently written files alone: a PUT upload may still be
# waiting for the push that creates its fotos row.
ORPHAN_PHOTO_GRACE_SECONDS = 300


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
    # Reject oversized bodies before buffering them for signature verification.
    # The photo limit is the largest legitimate body (JSON push bodies are tiny);
    # a client that declares a larger Content-Length is refused here instead of
    # exhausting memory during `request.body()`.
    content_length = request.headers.get("content-length")
    if content_length is not None and content_length.isdigit():
        if int(content_length) > MAX_PHOTO_BYTES:
            raise HTTPException(status_code=413, detail="request body too large")
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

    raw_path = request.url.path
    query = request.url.query
    path_with_query = raw_path if not query else f"{raw_path}?{query}"
    message = auth.build_message(request.method, path_with_query, body, timestamp)

    key_header = request.headers.get(auth.KEY_HEADER)
    if key_header is not None:
        key_record = store.get_key_by_public_key(key_header)
        if key_record is not None and auth.verify_signature(key_header, signature, message):
            if not auth.consume_replay(signature):
                raise HTTPException(status_code=401, detail="replay detected")
            request.state.authenticated_key = key_record
            return
        raise HTTPException(status_code=401, detail="invalid signature")

    for public_key in store.get_all_public_keys():
        if auth.verify_signature(public_key, signature, message):
            if not auth.consume_replay(signature):
                raise HTTPException(status_code=401, detail="replay detected")
            request.state.authenticated_key = store.get_key_by_public_key(public_key)
            return
    raise HTTPException(status_code=401, detail="invalid signature")


class _BodyLimitMiddleware:
    """Reject request bodies larger than MAX_PHOTO_BYTES before parsing.

    FastAPI parses Pydantic bodies before running endpoint dependencies, so the
    check in ``require_auth`` is too late for ``POST /api/push``. This pure-ASGI
    middleware buffers the body (bounded by the limit) and replays it to the app,
    so the cap applies even to chunked requests that omit Content-Length.
    """

    def __init__(self, app):
        self.app = app

    async def __call__(self, scope, receive, send):
        if scope["type"] != "http":
            await self.app(scope, receive, send)
            return

        headers = dict(scope.get("headers") or [])
        declared = headers.get(b"content-length")
        if declared is not None:
            try:
                declared_size = int(declared)
            except ValueError:
                declared_size = None
            if declared_size is not None and declared_size > MAX_PHOTO_BYTES:
                await Response(
                    status_code=413, content=b"request body too large"
                )(scope, receive, send)
                return

        body = bytearray()
        while True:
            message = await receive()
            if message["type"] == "http.disconnect":
                # The client went away; let the app observe the disconnect.
                async def replay_disconnect():
                    return message

                await self.app(scope, replay_disconnect, send)
                return
            if message["type"] != "http.request":
                continue
            body.extend(message.get("body", b""))
            if len(body) > MAX_PHOTO_BYTES:
                await Response(
                    status_code=413, content=b"request body too large"
                )(scope, receive, send)
                return
            if not message.get("more_body", False):
                break

        replayed = False
        # Starlette's streaming responses listen for a client disconnect while
        # sending. Returning `http.disconnect` here would cancel a FileResponse
        # immediately after the request body, so block instead; the listener is
        # cancelled when the response finishes.
        disconnected = asyncio.Event()

        async def replay():
            nonlocal replayed
            if not replayed:
                replayed = True
                return {"type": "http.request", "body": bytes(body), "more_body": False}
            await disconnected.wait()

        await self.app(scope, replay, send)


def create_app(database: Database | None = None) -> FastAPI:
    version = _read_version()
    app = FastAPI(title="ElisDB Server", version=version)
    store = database if database is not None else db
    store.init()
    # Reclaim photo files left without a database row (from edits or abandoned
    # uploads) so storage does not grow across restarts.
    removed = store.gc_orphan_photos()
    if removed:
        logger.info("startup cleanup removed %d orphaned photo file(s)", removed)

    # Retention is opt-in: both windows default to 0 (disabled) so an upgrade
    # never starts deleting history on its own.
    tombstone_retention_days = _env_non_negative_int(
        "ELISDB_TOMBSTONE_RETENTION_DAYS", 0
    )
    audit_retention_days = _env_non_negative_int("ELISDB_AUDIT_RETENTION_DAYS", 0)
    removed_tombstones, removed_audit = store.prune_old_records(
        tombstone_retention_days, audit_retention_days
    )
    if removed_tombstones or removed_audit:
        logger.info(
            "startup retention removed %d tombstone(s) and %d audit entries",
            removed_tombstones,
            removed_audit,
        )

    max_push_records = _env_int("ELISDB_MAX_PUSH_RECORDS", 200)
    max_photos_per_client = _env_int("ELISDB_MAX_PHOTOS_PER_CLIENT", 500)
    photo_gc_interval = _env_int("ELISDB_PHOTO_GC_INTERVAL_SECONDS", PHOTO_GC_INTERVAL_SECONDS)
    max_photo_bytes_total = _env_int(
        "ELISDB_MAX_PHOTO_BYTES_TOTAL", 20 * 1024**3
    )
    max_photo_bytes_per_key = _env_int(
        "ELISDB_MAX_PHOTO_BYTES_PER_KEY", 2 * 1024**3
    )

    app.state.store = store
    auth_dep = Depends(require_auth)

    # Startup already ran both maintenance passes; the throttles below only
    # decide when the next periodic run is due.
    maintenance_state = {"last": time.monotonic()}
    maintenance_lock = threading.Lock()
    gc_state = {"last": time.monotonic()}
    gc_lock = threading.Lock()

    def _maybe_prune() -> None:
        """Run retention pruning at most once per interval (cheap no-op when off)."""
        if tombstone_retention_days <= 0 and audit_retention_days <= 0:
            return
        now = time.monotonic()
        if now - maintenance_state["last"] < MAINTENANCE_INTERVAL_SECONDS:
            return
        maintenance_state["last"] = now
        if not maintenance_lock.acquire(blocking=False):
            return
        try:
            tombstones, audit = store.prune_old_records(
                tombstone_retention_days, audit_retention_days
            )
            if tombstones or audit:
                logger.info(
                    "retention removed %d tombstone(s) and %d audit entries",
                    tombstones,
                    audit,
                )
        except Exception:  # never fail a push because maintenance failed
            logger.exception("retention maintenance failed")
        finally:
            maintenance_lock.release()

    def _maybe_gc_photos() -> None:
        """Schedule the orphan-photo sweep at most once per interval.

        The sweep lists and stats the photos directory, so it runs in a daemon
        thread: a push never waits for it, and the lock prevents overlapping
        sweeps. A sweep racing an upload is safe because only files without a
        ``fotos`` row are removed.
        """
        now = time.monotonic()
        if now - gc_state["last"] < photo_gc_interval:
            return
        gc_state["last"] = now
        if not gc_lock.acquire(blocking=False):
            return

        def run() -> None:
            try:
                removed_files = store.gc_orphan_photos(
                    min_age_seconds=ORPHAN_PHOTO_GRACE_SECONDS
                )
                if removed_files:
                    logger.info(
                        "periodic cleanup removed %d orphaned photo file(s)",
                        removed_files,
                    )
            except Exception:
                logger.exception("orphan photo cleanup failed")
            finally:
                gc_lock.release()

        threading.Thread(target=run, name="elisdb-photo-gc", daemon=True).start()

    photo_gc_stop = threading.Event()

    def _photo_gc_loop() -> None:
        """Periodic orphan sweep, independent of pushes."""
        while not photo_gc_stop.wait(photo_gc_interval):
            if not gc_lock.acquire(blocking=False):
                continue
            try:
                removed_files = store.gc_orphan_photos(
                    min_age_seconds=ORPHAN_PHOTO_GRACE_SECONDS
                )
                if removed_files:
                    logger.info(
                        "periodic cleanup removed %d orphaned photo file(s)",
                        removed_files,
                    )
                gc_state["last"] = time.monotonic()
            except Exception:
                logger.exception("periodic orphan photo cleanup failed")
            finally:
                gc_lock.release()

    @asynccontextmanager
    async def lifespan(_app: FastAPI):
        thread = threading.Thread(
            target=_photo_gc_loop, name="elisdb-photo-gc", daemon=True
        )
        thread.start()
        try:
            yield
        finally:
            photo_gc_stop.set()
            thread.join(timeout=5)

    # Assigned after the helper definitions above; routes are added below.
    app.router.lifespan_context = lifespan

    rate_limiter = RateLimiter(
        max_requests=_env_int("ELISDB_RATE_LIMIT_REQUESTS", 300),
        window_seconds=_env_int("ELISDB_RATE_LIMIT_WINDOW_SECONDS", 60),
    )
    # Only trust X-Forwarded-For when an operator explicitly says a reverse
    # proxy sets it; otherwise any client could rotate the header to get a new
    # rate-limit budget on every request.
    trust_proxy = _env_bool("ELISDB_TRUST_PROXY", False)

    def _client_ip(request: Request) -> str:
        if trust_proxy:
            forwarded = request.headers.get("x-forwarded-for")
            if forwarded:
                first = forwarded.split(",", 1)[0].strip()
                if first:
                    return first
        return request.client.host if request.client else "unknown"

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

    @app.middleware("http")
    async def _rate_limit(request: Request, call_next):
        # Limit by source address only. The X-Elisdb-Key header is not verified
        # at this point (and is optional), so using it as a budget key would let
        # a client rotate keys to bypass the limit.
        identifier = f"ip:{_client_ip(request)}"
        if not rate_limiter.allow(identifier):
            logger.warning("rate limit exceeded for ip")
            return Response(status_code=429, content=b"too many requests")
        return await call_next(request)

    # Added last so it is the outermost middleware: the body is capped before
    # rate limiting, logging, or FastAPI body parsing can consume it.
    app.add_middleware(_BodyLimitMiddleware)

    @app.get("/health")
    def health() -> dict:
        return {
            "status": "ok",
            "version": version,
            "api_version": API_VERSION,
            "instance_id": store.instance_id(),
        }

    @app.get("/api/time")
    def time_now() -> dict:
        return {"time": int(time.time())}

    @app.get("/api/latest", dependencies=[auth_dep])
    def latest() -> dict:
        # client_count is additive: older clients ignore it, newer ones show the
        # server total next to the local one on the sync screen.
        return {
            "latest_updated_at": store.latest_updated_at(),
            "client_count": store.client_count(),
            # Additive too: an older client ignores it, a newer one warns when
            # the server predates a feature the app relies on.
            "api_version": API_VERSION,
            # Stable identity for this database; lets a client distinguish the
            # same server from a new or restored one before reconciling data.
            "instance_id": store.instance_id(),
            # The effective push limit, so clients can split large batches
            # instead of discovering the limit through a 422 response.
            "max_push_records": max_push_records,
        }

    @app.post("/api/push", response_model=list[PushResult], dependencies=[auth_dep])
    def push(request: Request, req: PushRequest) -> list[PushResult]:
        # Validate batch limits before writing anything: a partially applied
        # push followed by a 422 would be harder for the client to reason about.
        if len(req.records) > max_push_records:
            raise HTTPException(
                status_code=422,
                detail=(
                    f"too many records in one push "
                    f"({len(req.records)} > {max_push_records})"
                ),
            )
        for record in req.records:
            if len(record.fotos) > max_photos_per_client:
                raise HTTPException(
                    status_code=422,
                    detail=(
                        f"client {record.client.id} has too many photos "
                        f"({len(record.fotos)} > {max_photos_per_client})"
                    ),
                )

        actor = getattr(request.state, "authenticated_key", None)
        idempotency_key = request.headers.get("Idempotency-Key")
        payload_hash = (
            hashlib.sha256(req.model_dump_json().encode("utf-8")).hexdigest()
            if idempotency_key
            else None
        )
        try:
            raw_results = store.push_batch(
                req.records,
                actor=actor,
                idempotency_key=idempotency_key,
                payload_hash=payload_hash,
            )
        except IdempotencyConflict:
            raise HTTPException(
                status_code=409,
                detail="idempotency key reused with a different payload",
            ) from None
        # A successful push is a natural checkpoint: reclaim abandoned uploads
        # and expire history without adding work to every request.
        _maybe_prune()
        _maybe_gc_photos()
        return [PushResult.model_validate(result) for result in raw_results]

    @app.get("/api/pull", response_model=None, dependencies=[auth_dep])
    def pull(
        since: int | None = None,
        limit: int | None = Query(None, ge=1, le=1000),
        since_id: str | None = None,
    ) -> dict:
        if since_id is not None and since is None:
            raise HTTPException(status_code=400, detail="since_id requires since")
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
        return Response(
            content=data,
            media_type=mime,
            headers={
                "X-Content-Type-Options": "nosniff",
                "Content-Disposition": "attachment",
            },
        )

    @app.put("/api/photos/{foto_id}", status_code=204, dependencies=[auth_dep])
    async def put_photo(foto_id: str, request: Request) -> Response:
        content_length = request.headers.get("content-length")
        if content_length is not None and content_length.isdigit():
            if int(content_length) > MAX_PHOTO_BYTES:
                raise HTTPException(status_code=413, detail="photo too large")
        data = await request.body()
        if len(data) > MAX_PHOTO_BYTES:
            raise HTTPException(status_code=413, detail="photo too large")
        if sniff_image_mime(data) is None:
            raise HTTPException(status_code=400, detail="unsupported image content")
        key = getattr(request.state, "authenticated_key", None)
        key_id = key["id"] if key is not None else None
        try:
            store.write_photo(
                foto_id,
                data,
                uploaded_by_key=key_id,
                require_reference=True,
                max_total_bytes=max_photo_bytes_total,
                max_key_bytes=max_photo_bytes_per_key,
            )
        except PhotoNotReferenced:
            raise HTTPException(
                status_code=404,
                detail="photo is not referenced by a client; push the record first",
            ) from None
        except PhotoQuotaExceeded as exc:
            logger.warning("photo upload rejected for key %s: %s", key_id, exc)
            raise HTTPException(status_code=413, detail=str(exc)) from None
        return Response(status_code=204)

    @app.delete("/api/clients/{cliente_id}", status_code=204, dependencies=[auth_dep])
    def delete_client(
        request: Request, cliente_id: str, deleted_at: int | None = None
    ) -> None:
        # deleted_at is epoch seconds; reject implausible timestamps so they
        # cannot be accepted into a tombstone.
        if deleted_at is not None and deleted_at < 0:
            raise HTTPException(status_code=400, detail="deleted_at cannot be negative")
        if deleted_at is not None and deleted_at > int(time.time()) + MAX_FUTURE_SKEW_SECONDS:
            raise HTTPException(status_code=400, detail="deleted_at is in the future")
        try:
            deleted = store.delete_client(
                cliente_id,
                deleted_at=deleted_at,
                actor=getattr(request.state, "authenticated_key", None),
            )
        except StaleDeleteError:
            raise HTTPException(
                status_code=409,
                detail="deleted_at is not newer than the stored record",
            ) from None
        if not deleted:
            raise HTTPException(status_code=404, detail="client not found")

    @app.get(
        "/api/clients/{cliente_id}/history",
        response_model=None,
        dependencies=[auth_dep],
    )
    def client_history(
        cliente_id: str,
        limit: int | None = Query(None, ge=1, le=200),
        after_id: int | None = Query(None, ge=0),
    ) -> list[AuditEntryOut] | HistoryPageOut:
        if limit is None and after_id is None:
            return [
                AuditEntryOut.model_validate(e)
                for e in store.client_history(cliente_id)
            ]
        entries, next_after_id = store.client_history_page(
            cliente_id, limit or 50, after_id
        )
        return HistoryPageOut(
            entries=[AuditEntryOut.model_validate(e) for e in entries],
            next_after_id=next_after_id,
        )

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

    def _release_or_404() -> updates.Release:
        release = updates.find_release()
        if release is None:
            raise HTTPException(status_code=404, detail="no release available")
        return release

    @app.get(
        "/api/app/latest",
        response_model=AppReleaseOut,
        dependencies=[auth_dep],
    )
    def latest_app_release() -> AppReleaseOut:
        release = _release_or_404()
        return AppReleaseOut(
            version_name=release.version_name,
            version_code=release.version_code,
            size=release.size,
            sha256=release.sha256,
            filename=release.filename,
        )

    @app.get("/api/app/download", dependencies=[auth_dep])
    def download_app_release() -> FileResponse:
        release = _release_or_404()
        return FileResponse(
            updates.apk_dir() / release.filename,
            media_type=updates.APK_MEDIA_TYPE,
            filename=release.filename,
        )

    return app


app = create_app()
