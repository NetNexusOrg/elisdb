"""SQLite persistence layer. Owns schema, duplicate detection and LWW conflict
resolution. The server is the single authority over all data management.

Photo binary content lives in a filesystem directory (self.fotos_dir); the
fotos table stores only the file path (caminho) referencing each photo file.
"""

import hashlib
import json
import os
import sqlite3
import tempfile
import threading
import time
import uuid

from .models import ClienteIn, FotoIn, normalize_nome

# Reject data timestamps further in the future than this many seconds, so a
# malicious or broken client timestamp cannot permanently win last-write-wins.
# Data timestamps (`updated_at`, `deleted_at`) are epoch seconds.
MAX_FUTURE_SKEW_SECONDS = 3600

SCHEMA = """
CREATE TABLE IF NOT EXISTS clientes (
    id TEXT PRIMARY KEY,
    nome TEXT NOT NULL,
    contrato TEXT,
    tag TEXT,
    latitude REAL,
    longitude REAL,
    notas TEXT,
    updated_at INTEGER NOT NULL,
    created_at INTEGER NOT NULL DEFAULT 0,
    ativo INTEGER NOT NULL DEFAULT 1
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_clientes_nome
    ON clientes (nome COLLATE NOCASE);
CREATE TABLE IF NOT EXISTS fotos (
    id TEXT PRIMARY KEY,
    cliente_id TEXT NOT NULL REFERENCES clientes(id) ON DELETE CASCADE,
    mime TEXT NOT NULL,
    caminho TEXT NOT NULL,
    ordem INTEGER NOT NULL DEFAULT 0,
    hash TEXT,
    uploaded_by_key TEXT,
    size_bytes INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_fotos_cliente ON fotos (cliente_id);
CREATE TABLE IF NOT EXISTS tombstones (
    id TEXT PRIMARY KEY,
    deleted_at INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_tombstones_deleted_at ON tombstones (deleted_at);
CREATE TABLE IF NOT EXISTS client_audit (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    cliente_id TEXT NOT NULL,
    key_id TEXT NOT NULL,
    username TEXT,
    action TEXT NOT NULL CHECK(action IN ('create','edit','delete')),
    ts INTEGER NOT NULL,
    details TEXT
);
CREATE INDEX IF NOT EXISTS idx_client_audit_cliente ON client_audit (cliente_id);
CREATE INDEX IF NOT EXISTS idx_client_audit_cliente_id ON client_audit (cliente_id, id);
CREATE INDEX IF NOT EXISTS idx_client_audit_ts ON client_audit (ts);
CREATE TABLE IF NOT EXISTS push_idempotency (
    key TEXT PRIMARY KEY,
    payload_hash TEXT NOT NULL,
    response_json TEXT NOT NULL,
    created_at INTEGER NOT NULL
);
CREATE TABLE IF NOT EXISTS keys (
    id TEXT PRIMARY KEY,
    public_key TEXT NOT NULL,
    comment TEXT,
    provisioned INTEGER NOT NULL DEFAULT 0,
    created_at INTEGER NOT NULL
);
CREATE TABLE IF NOT EXISTS server_meta (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL
);
"""


def _migrate_keys_comment(conn: sqlite3.Connection) -> None:
    """Idempotently add the keys.comment column to pre-comment databases."""
    columns = [row["name"] for row in conn.execute("PRAGMA table_info(keys)").fetchall()]
    if "comment" not in columns:
        conn.execute("ALTER TABLE keys ADD COLUMN comment TEXT")


def _migrate_keys_provisioned(conn: sqlite3.Connection) -> None:
    """Idempotently add the keys.provisioned flag to pre-existing databases.

    Existing keys default to 0 (manually issued), so provisioning never revokes
    keys that predate the flag.
    """
    columns = [row["name"] for row in conn.execute("PRAGMA table_info(keys)").fetchall()]
    if "provisioned" not in columns:
        conn.execute("ALTER TABLE keys ADD COLUMN provisioned INTEGER NOT NULL DEFAULT 0")


def _migrate_keys_revoked(conn: sqlite3.Connection) -> None:
    """Idempotently add keys.revoked_at so old credentials can be disabled.

    NULL means the key is active; an epoch-seconds value revokes it without
    deleting the row, preserving the audit trail that references key ids.
    """
    columns = [row["name"] for row in conn.execute("PRAGMA table_info(keys)").fetchall()]
    if "revoked_at" not in columns:
        conn.execute("ALTER TABLE keys ADD COLUMN revoked_at INTEGER")


def _migrate_fotos_hash(conn: sqlite3.Connection) -> None:
    """Idempotently add the fotos.hash column for cached content hashes."""
    columns = [row["name"] for row in conn.execute("PRAGMA table_info(fotos)").fetchall()]
    if "hash" not in columns:
        conn.execute("ALTER TABLE fotos ADD COLUMN hash TEXT")


def _migrate_fotos_upload_metadata(conn: sqlite3.Connection) -> None:
    """Idempotently add upload ownership/size metadata and backfill sizes.

    Existing photo rows predate quotas, so their stored bytes are counted from
    the file on disk. A missing file leaves ``size_bytes`` at zero; the row is
    still a placeholder and the client can upload the bytes later.
    """
    columns = [row["name"] for row in conn.execute("PRAGMA table_info(fotos)").fetchall()]
    if "uploaded_by_key" not in columns:
        conn.execute("ALTER TABLE fotos ADD COLUMN uploaded_by_key TEXT")
    if "size_bytes" not in columns:
        conn.execute("ALTER TABLE fotos ADD COLUMN size_bytes INTEGER NOT NULL DEFAULT 0")

    rows = conn.execute("SELECT id, caminho, size_bytes FROM fotos").fetchall()
    for row in rows:
        if row["size_bytes"]:
            continue
        try:
            size = os.path.getsize(row["caminho"])
        except OSError:
            continue
        conn.execute("UPDATE fotos SET size_bytes = ? WHERE id = ?", (size, row["id"]))


def _migrate_clients_created_at(conn: sqlite3.Connection) -> None:
    """Idempotently add the clientes.created_at column.

    Legacy rows get the column backfilled from updated_at so their creation
    date degrades gracefully to the last update time known before the upgrade.
    """
    columns = [
        row["name"] for row in conn.execute("PRAGMA table_info(clientes)").fetchall()
    ]
    if "created_at" not in columns:
        conn.execute(
            "ALTER TABLE clientes ADD COLUMN created_at INTEGER NOT NULL DEFAULT 0"
        )
        conn.execute("UPDATE clientes SET created_at = updated_at")


def _migrate_clients_tag(conn: sqlite3.Connection) -> None:
    """Idempotently add the optional clientes.tag column.

    Existing rows get no tag (NULL), which is the same as an empty tag.
    """
    columns = [
        row["name"] for row in conn.execute("PRAGMA table_info(clientes)").fetchall()
    ]
    if "tag" not in columns:
        conn.execute("ALTER TABLE clientes ADD COLUMN tag TEXT")


def _migrate_delivery_ts(conn: sqlite3.Connection) -> None:
    """Idempotently add the delivery clock used to hand changes to devices.

    `updated_at`/`deleted_at` carry the device clock and decide last-write-wins.
    They cannot also drive the pull cursor: the server has to be able to deliver
    a write stamped *behind* the watermark (a slow device) without pretending it
    happened later. `delivery_ts` is assigned by the server, strictly above
    everything already delivered, and is the only cursor the pull uses.

    Existing rows are backfilled from the timestamp they already had, so no
    device loses sight of data it has not pulled yet.
    """
    columns = [
        row["name"] for row in conn.execute("PRAGMA table_info(clientes)").fetchall()
    ]
    if "delivery_ts" not in columns:
        conn.execute("ALTER TABLE clientes ADD COLUMN delivery_ts INTEGER NOT NULL DEFAULT 0")
    conn.execute("UPDATE clientes SET delivery_ts = updated_at WHERE delivery_ts = 0")

    columns = [
        row["name"] for row in conn.execute("PRAGMA table_info(tombstones)").fetchall()
    ]
    if "delivery_ts" not in columns:
        conn.execute(
            "ALTER TABLE tombstones ADD COLUMN delivery_ts INTEGER NOT NULL DEFAULT 0"
        )
    conn.execute("UPDATE tombstones SET delivery_ts = deleted_at WHERE delivery_ts = 0")

    # Created here (not in SCHEMA) because the columns only exist after the
    # ALTERs above.
    conn.execute(
        "CREATE INDEX IF NOT EXISTS idx_clientes_delivery ON clientes (delivery_ts, id)"
    )
    conn.execute(
        "CREATE INDEX IF NOT EXISTS idx_tombstones_delivery ON tombstones (delivery_ts)"
    )


def _migrate_clients_ativo(conn: sqlite3.Connection) -> None:
    """Idempotently add clientes.ativo, defaulting every existing client to active."""
    columns = [
        row["name"] for row in conn.execute("PRAGMA table_info(clientes)").fetchall()
    ]
    if "ativo" not in columns:
        conn.execute(
            "ALTER TABLE clientes ADD COLUMN ativo INTEGER NOT NULL DEFAULT 1"
        )


def _migrate_clients_nullable_coords(conn: sqlite3.Connection) -> None:
    """Idempotently make clientes.latitude/longitude nullable.

    SQLite cannot ALTER a column's nullability, so the table is rebuilt.
    Existing NOT NULL data is preserved; new rows may store NULL coordinates.

    The rebuild drops `clientes`, and `fotos` references it with ON DELETE
    CASCADE, so foreign-key enforcement has to be off for real. The PRAGMA is a
    silent no-op inside a transaction, and an earlier migration may have left one
    open; committing first and reading the pragma back turns that failure mode
    into an explicit error instead of deleting every photo row.
    """
    columns = [
        dict(row) for row in conn.execute("PRAGMA table_info(clientes)").fetchall()
    ]
    latitude = next((c for c in columns if c["name"] == "latitude"), None)
    if latitude is None or latitude["notnull"] != 1:
        return

    conn.commit()
    conn.execute("PRAGMA foreign_keys = OFF")
    try:
        if conn.execute("PRAGMA foreign_keys").fetchone()[0] != 0:
            raise RuntimeError(
                "cannot disable foreign keys for the clientes rebuild; "
                "refusing to risk the photos that cascade with it"
            )
        photos_before = conn.execute("SELECT COUNT(*) FROM fotos").fetchone()[0]
        conn.execute("BEGIN IMMEDIATE")
        conn.execute(
            """
            CREATE TABLE clientes_new (
                id TEXT PRIMARY KEY,
                nome TEXT NOT NULL,
                contrato TEXT,
                tag TEXT,
                latitude REAL,
                longitude REAL,
                notas TEXT,
                updated_at INTEGER NOT NULL,
                created_at INTEGER NOT NULL DEFAULT 0,
                delivery_ts INTEGER NOT NULL DEFAULT 0,
                ativo INTEGER NOT NULL DEFAULT 1
            )
            """
        )
        conn.execute(
            """
            INSERT INTO clientes_new
                (id, nome, contrato, tag, latitude, longitude, notas, updated_at, created_at, delivery_ts, ativo)
            SELECT id, nome, contrato, tag, latitude, longitude, notas, updated_at, created_at, delivery_ts, ativo
            FROM clientes
            """
        )
        conn.execute("DROP TABLE clientes")
        conn.execute("ALTER TABLE clientes_new RENAME TO clientes")
        conn.execute(
            "CREATE UNIQUE INDEX IF NOT EXISTS idx_clientes_nome "
            "ON clientes (nome COLLATE NOCASE)"
        )
        conn.execute(
            "CREATE INDEX IF NOT EXISTS idx_clientes_delivery ON clientes (delivery_ts, id)"
        )
        photos_after = conn.execute("SELECT COUNT(*) FROM fotos").fetchone()[0]
        if photos_after != photos_before:
            raise RuntimeError(
                "clientes rebuild would lose photo rows "
                f"({photos_before} -> {photos_after}); aborting"
            )
        conn.commit()
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.execute("PRAGMA foreign_keys = ON")


def _migrate_client_audit_details(conn: sqlite3.Connection) -> None:
    """Idempotently add the client_audit.details column.

    Entries written before change tracking keep NULL details and are presented
    with the actor-only view.
    """
    columns = [
        row["name"] for row in conn.execute("PRAGMA table_info(client_audit)").fetchall()
    ]
    if "details" not in columns:
        conn.execute("ALTER TABLE client_audit ADD COLUMN details TEXT")


def _migrate_client_audit_actions(conn: sqlite3.Connection) -> None:
    """Widen the client_audit action constraint to include deletes.

    SQLite cannot alter a CHECK constraint in place, so rebuild the table while
    preserving every existing audit row. The migration is idempotent: a table
    whose SQL already mentions the delete action is left untouched.
    """
    row = conn.execute(
        "SELECT sql FROM sqlite_master WHERE type = 'table' AND name = 'client_audit'"
    ).fetchone()
    if row is None or (row["sql"] and "'delete'" in row["sql"]):
        return
    conn.execute("ALTER TABLE client_audit RENAME TO client_audit_legacy")
    conn.execute(
        """
        CREATE TABLE client_audit (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            cliente_id TEXT NOT NULL,
            key_id TEXT NOT NULL,
            username TEXT,
            action TEXT NOT NULL CHECK(action IN ('create','edit','delete')),
            ts INTEGER NOT NULL,
            details TEXT
        )
        """
    )
    conn.execute(
        """
        INSERT INTO client_audit (id, cliente_id, key_id, username, action, ts, details)
        SELECT id, cliente_id, key_id, username, action, ts, details
        FROM client_audit_legacy
        """
    )
    conn.execute("DROP TABLE client_audit_legacy")
    conn.execute(
        "CREATE INDEX IF NOT EXISTS idx_client_audit_cliente ON client_audit (cliente_id)"
    )
    conn.execute(
        "CREATE INDEX IF NOT EXISTS idx_client_audit_cliente_id ON client_audit (cliente_id, id)"
    )
    conn.execute("CREATE INDEX IF NOT EXISTS idx_client_audit_ts ON client_audit (ts)")


def _migrate_timestamps_to_seconds(conn: sqlite3.Connection) -> None:
    """Idempotently convert legacy millisecond timestamps to epoch seconds.

    Before the seconds normalization, client `updated_at`/`created_at` and audit
    `ts` were stored in milliseconds while `deleted_at` and `keys.created_at`
    were already seconds. Values above the threshold are milliseconds (epoch
    ~1.7e12 in 2026) and are divided by 1000; second-scale values are left
    untouched, so the migration is naturally idempotent across restarts.
    """
    conn.execute(
        "UPDATE clientes SET updated_at = updated_at / 1000 WHERE updated_at > 100000000000"
    )
    conn.execute(
        "UPDATE clientes SET created_at = created_at / 1000 WHERE created_at > 100000000000"
    )
    # delivery_ts is backfilled from updated_at, so a legacy millisecond value
    # would otherwise survive as a cursor far in the future.
    conn.execute(
        "UPDATE clientes SET delivery_ts = delivery_ts / 1000 WHERE delivery_ts > 100000000000"
    )
    conn.execute(
        "UPDATE tombstones SET delivery_ts = delivery_ts / 1000 WHERE delivery_ts > 100000000000"
    )
    conn.execute(
        "UPDATE client_audit SET ts = ts / 1000 WHERE ts > 100000000000"
    )


# Audit details keep at most this many characters per changed text field, so a
# long notes/contract body cannot bloat the history (the record itself keeps the
# full value).
AUDIT_TEXT_LIMIT = 200


def _audit_text(value: str | None) -> tuple[str | None, bool]:
    """Shortens an audit text value, reporting whether it was truncated."""
    if value is None or len(value) <= AUDIT_TEXT_LIMIT:
        return value, False
    return value[:AUDIT_TEXT_LIMIT], True


def _audit_coord(value: float | None) -> str | None:
    """Formats a coordinate so sub-millimetre float noise is not a change."""
    if value is None:
        return None
    return f"{value:.6f}"


def _audit_location(latitude: str | None, longitude: str | None) -> str | None:
    """Joins formatted coordinates into the single value shown for "Localização"."""
    parts = [part for part in (latitude, longitude) if part is not None]
    return ", ".join(parts) if parts else None


def build_audit_details(
    before: dict | None,
    after: dict,
    before_photos: list[str],
    after_photos: list[str],
) -> dict | None:
    """Describe what an accepted write changed, or None when nothing did.

    ``before`` is the stored client row (None on insert); the photo arguments are
    the ordered ids of the canonical sets before and after the write. Text values
    are shortened to ``AUDIT_TEXT_LIMIT`` and flagged; coordinates are compared
    with six decimals and presented as a single "localizacao" field.
    """
    if before is None:
        return (
            {
                "fields": [],
                "photos": {"added": list(after_photos), "removed": [], "reordered": False},
            }
            if after_photos
            else None
        )

    fields: list[dict] = []
    for name in ("nome", "contrato", "tag", "notas"):
        old = before.get(name)
        new = after.get(name)
        if name in ("contrato", "tag", "notas"):
            # Empty text and NULL mean the same thing to the user.
            old = old or None
            new = new or None
        if old == new:
            continue
        old_text, old_truncated = _audit_text(old)
        new_text, new_truncated = _audit_text(new)
        fields.append(
            {
                "field": name,
                "old": old_text,
                "new": new_text,
                "truncated": old_truncated or new_truncated,
            }
        )

    old_lat = _audit_coord(before.get("latitude"))
    old_lon = _audit_coord(before.get("longitude"))
    new_lat = _audit_coord(after.get("latitude"))
    new_lon = _audit_coord(after.get("longitude"))
    if (old_lat, old_lon) != (new_lat, new_lon):
        fields.append(
            {
                "field": "localizacao",
                "old": _audit_location(old_lat, old_lon),
                "new": _audit_location(new_lat, new_lon),
                "truncated": False,
            }
        )

    old_ativo = bool(before.get("ativo", 1))
    new_ativo = bool(after.get("ativo", 1))
    if old_ativo != new_ativo:
        fields.append(
            {
                "field": "ativo",
                "old": "Ativo" if old_ativo else "Inativo",
                "new": "Ativo" if new_ativo else "Inativo",
                "truncated": False,
            }
        )

    added = [foto_id for foto_id in after_photos if foto_id not in before_photos]
    removed = [foto_id for foto_id in before_photos if foto_id not in after_photos]
    reordered = not added and not removed and before_photos != after_photos
    photos = (
        {"added": added, "removed": removed, "reordered": reordered}
        if added or removed or reordered
        else None
    )

    if not fields and photos is None:
        return None
    details: dict = {"fields": fields}
    if photos is not None:
        details["photos"] = photos
    return details


# Databases whose schema + migrations have already been applied in this process.
# Schema/migrations run once per database file (at first connection / startup)
# instead of on every request connection, while the per-connection PRAGMAs below
# stay cheap and idempotent.
_INITIALIZED: set[str] = set()
_INIT_LOCK = threading.Lock()


def _ensure_schema(conn: sqlite3.Connection, db_path: str) -> None:
    """Run schema + migrations at most once per database file.

    In-memory databases are fresh on every connection, so they are always
    initialized. File databases are initialized exactly once per process.
    """
    if db_path == ":memory:":
        _apply_schema(conn)
        return
    with _INIT_LOCK:
        if db_path in _INITIALIZED:
            return
        _apply_schema(conn)
        _INITIALIZED.add(db_path)


def _apply_schema(conn: sqlite3.Connection) -> None:
    conn.executescript(SCHEMA)
    # A stable server identity lives in the database so it survives restarts but
    # changes when the database/volume is recreated. Clients use it to tell the
    # same server apart from a new or restored one before reconciling data.
    conn.execute(
        "INSERT OR IGNORE INTO server_meta (key, value) VALUES ('instance_id', ?)",
        (str(uuid.uuid4()),),
    )
    _migrate_keys_comment(conn)
    _migrate_keys_provisioned(conn)
    _migrate_keys_revoked(conn)
    _migrate_clients_created_at(conn)
    _migrate_clients_tag(conn)
    _migrate_fotos_hash(conn)
    _migrate_fotos_upload_metadata(conn)
    # delivery_ts must exist before the coordinate rebuild, which copies it.
    _migrate_delivery_ts(conn)
    # ativo must exist before the coordinate rebuild too, which preserves it.
    _migrate_clients_ativo(conn)
    _migrate_clients_nullable_coords(conn)
    _migrate_client_audit_details(conn)
    _migrate_client_audit_actions(conn)
    _migrate_timestamps_to_seconds(conn)
    conn.commit()


def connect(db_path: str) -> sqlite3.Connection:
    if db_path != ":memory:":
        os.makedirs(os.path.dirname(os.path.abspath(db_path)), exist_ok=True)
    conn = sqlite3.connect(db_path)
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA foreign_keys = ON")
    # busy_timeout makes a contended write wait instead of raising
    # "database is locked"; it is a per-connection setting and must be applied
    # on every connection.
    conn.execute("PRAGMA busy_timeout=5000")
    # WAL lets concurrent readers proceed while a writer is active and avoids
    # the full-database lock of the default DELETE journal. journal_mode is a
    # persistent property of the file, so setting it here is idempotent.
    try:
        conn.execute("PRAGMA journal_mode=WAL")
    except sqlite3.DatabaseError:
        pass
    _ensure_schema(conn, db_path)
    return conn


class DuplicateError(Exception):
    """Raised when a create collides with an existing client name."""

    def __init__(self, existing_id: str):
        super().__init__(f"duplicate client name (existing id {existing_id})")
        self.existing_id = existing_id


class PhotoNotReferenced(Exception):
    """Raised when an upload targets a photo id with no stored photo row."""


class PhotoQuotaExceeded(Exception):
    """Raised when an upload would exceed a storage quota."""


class StaleDeleteError(Exception):
    """Raised when a deletion timestamp is not newer than the stored record."""


class IdempotencyConflict(Exception):
    """Raised when a push reuses an idempotency key with different content."""


class Database:
    def __init__(self, db_path: str, fotos_dir: str | None = None):
        self.db_path = db_path
        if fotos_dir is None:
            dirname = os.path.dirname(os.path.abspath(db_path))
            fotos_dir = os.path.join(dirname, "photos")
        # Stored paths are absolute; normalising here keeps the orphan sweep
        # comparing like with like when the caller passes a relative directory.
        self.fotos_dir = os.path.abspath(fotos_dir)

    def _conn(self) -> sqlite3.Connection:
        return connect(self.db_path)

    def init(self) -> None:
        """Apply schema and migrations once at startup (idempotent).

        Also creates the photos directory here (not in ``__init__``) so merely
        constructing or importing the database object has no filesystem side
        effects.
        """
        os.makedirs(self.fotos_dir, exist_ok=True)
        conn = self._conn()
        try:
            _ensure_schema(conn, self.db_path)
        finally:
            conn.close()

    def save_client(
        self, cliente: ClienteIn, fotos: list[FotoIn], actor: dict | None = None
    ) -> tuple[str, str | None, dict | None, list[str]]:
        """Insert or update a client, applying last-write-wins.

        When `actor` (the authenticated key record: `id`, `comment`) is given and
        the record is accepted, an audit entry is appended to client_audit
        (`create` on insert, `edit` on update).

        Returns (status, existing_id, canonical, missing_photos) where status is:
          - "accepted": the record (and photos) were persisted
          - "duplicate": a different client already holds this normalized name
          - "stale": the incoming updated_at is older than the stored value
        """
        conn = self._conn()
        removals: list[str] = []
        try:
            conn.execute("BEGIN IMMEDIATE")
            result = self._save_client_on_conn(conn, cliente, fotos, actor, removals)
            if result[0] == "accepted":
                conn.commit()
            else:
                conn.rollback()
        except BaseException:
            conn.rollback()
            raise
        finally:
            conn.close()
        # Files are removed only after the commit: a rollback must not leave
        # fotos rows pointing at missing files.
        for path in removals:
            self._remove_file(path)
        return result

    def _save_client_on_conn(
        self,
        conn: sqlite3.Connection,
        cliente: ClienteIn,
        fotos: list[FotoIn],
        actor: dict | None,
        deferred_removals: list[str],
    ) -> tuple[str, str | None, dict | None, list[str]]:
        """Persist a client inside an existing transaction.

        The caller owns BEGIN/COMMIT/ROLLBACK and must run this under
        ``BEGIN IMMEDIATE``. No files are deleted here; paths are appended to
        ``deferred_removals`` so the caller can remove them only after commit.
        All early returns happen before any mutation, so a shared batch
        transaction can roll back a single failed record with a savepoint.
        """
        nome = normalize_nome(cliente.nome)
        if not nome:
            raise ValueError("nome cannot be blank")

        # updated_at is epoch seconds; reject a far-future value so a malicious
        # or broken timestamp cannot permanently win last-write-wins.
        if cliente.updated_at > int(time.time()) + MAX_FUTURE_SKEW_SECONDS:
            raise ValueError("updated_at is too far in the future")

        # Read the delivery clock before touching any row: a resurrection
        # deletes the tombstone, and the new record still has to be handed
        # out above everything already delivered.
        watermark = self._delivery_watermark(conn)
        tombstone = conn.execute(
            "SELECT deleted_at FROM tombstones WHERE id = ?", (cliente.id,)
        ).fetchone()
        if tombstone is not None and cliente.updated_at <= tombstone["deleted_at"]:
            return "stale", None, None, []

        # `updated_at` keeps the device timestamp (it is what last-write-wins
        # compares) while `delivery_ts` is assigned here so the change is
        # visible to every device pulling from a watermark at or behind it,
        # even when the pushing device's clock is behind.
        delivery_ts = max(cliente.updated_at, watermark + 1)

        existing = conn.execute(
            "SELECT id, updated_at, nome, contrato, tag, latitude, longitude, notas, ativo "
            "FROM clientes WHERE id = ?",
            (cliente.id,),
        ).fetchone()

        if existing is not None:
            if cliente.updated_at < existing["updated_at"]:
                return "stale", None, None, []
            other = conn.execute(
                "SELECT id FROM clientes WHERE nome = ? COLLATE NOCASE AND id != ?",
                (nome, cliente.id),
            ).fetchone()
            if other is not None:
                canonical = self._canonical_from_conn(conn, other["id"])
                return "duplicate", other["id"], canonical, []
            # All checks passed: it is safe to clear a resurrection tombstone.
            if tombstone is not None:
                conn.execute("DELETE FROM tombstones WHERE id = ?", (cliente.id,))
            before = dict(existing)
            before_photos = self._photo_ids(conn, cliente.id)
            conn.execute(
                "UPDATE clientes SET nome=?, contrato=?, tag=?, latitude=?, longitude=?, notas=?, ativo=?, updated_at=?, delivery_ts=? WHERE id=?",
                (
                    nome,
                    cliente.contrato,
                    cliente.tag,
                    cliente.latitude,
                    cliente.longitude,
                    cliente.notas,
                    cliente.ativo,
                    cliente.updated_at,
                    delivery_ts,
                    cliente.id,
                ),
            )
            kept_ids, missing_ids, files_to_remove = self._replace_fotos(
                conn, cliente.id, fotos
            )
            details = build_audit_details(
                before,
                {
                    "nome": nome,
                    "contrato": cliente.contrato,
                    "tag": cliente.tag,
                    "latitude": cliente.latitude,
                    "longitude": cliente.longitude,
                    "notas": cliente.notas,
                    "ativo": cliente.ativo,
                },
                before_photos,
                kept_ids,
            )
            # A push that changes nothing records no entry: "edited" with no
            # visible difference is noise in the history.
            if details is not None:
                self._record_audit(
                    conn, cliente.id, actor, "edit", cliente.updated_at, details
                )
            deferred_removals.extend(files_to_remove)
            return "accepted", None, None, missing_ids

        # Pre-check the unique name before mutating anything, so a duplicate in
        # a shared batch transaction never requires a destructive rollback.
        other = conn.execute(
            "SELECT id FROM clientes WHERE nome = ? COLLATE NOCASE", (nome,)
        ).fetchone()
        if other is not None:
            canonical = self._canonical_from_conn(conn, other["id"])
            return "duplicate", other["id"], canonical, []
        if tombstone is not None:
            conn.execute("DELETE FROM tombstones WHERE id = ?", (cliente.id,))
        conn.execute(
            "INSERT INTO clientes (id, nome, contrato, tag, latitude, longitude, notas, ativo, updated_at, created_at, delivery_ts) "
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            (
                cliente.id,
                nome,
                cliente.contrato,
                cliente.tag,
                cliente.latitude,
                cliente.longitude,
                cliente.notas,
                cliente.ativo,
                cliente.updated_at,
                cliente.updated_at,
                delivery_ts,
            ),
        )
        kept_ids, missing_ids, files_to_remove = self._replace_fotos(
            conn, cliente.id, fotos
        )
        details = build_audit_details(
            None,
            {
                "nome": nome,
                "contrato": cliente.contrato,
                "tag": cliente.tag,
                "latitude": cliente.latitude,
                "longitude": cliente.longitude,
                "notas": cliente.notas,
                "ativo": cliente.ativo,
            },
            [],
            kept_ids,
        )
        self._record_audit(
            conn, cliente.id, actor, "create", cliente.updated_at, details
        )
        deferred_removals.extend(files_to_remove)
        return "accepted", None, None, missing_ids

    def push_batch(
        self,
        records: list,
        actor: dict | None = None,
        idempotency_key: str | None = None,
        payload_hash: str | None = None,
    ) -> list[dict]:
        """Persist a push batch, optionally replaying an idempotent request.

        Without an idempotency key each record is saved independently, matching
        the historical per-record behavior. With a key the whole batch commits
        atomically and the response is stored; replaying the same key and
        payload returns that response without applying the records again. A key
        reused with different content raises IdempotencyConflict.
        """
        if not idempotency_key:
            results: list[dict] = []
            for record in records:
                try:
                    status, existing_id, canonical, missing = self.save_client(
                        record.client, record.fotos, actor=actor
                    )
                except ValueError:
                    results.append({"id": record.client.id, "status": "error"})
                else:
                    results.append(
                        self._push_result(
                            record, status, existing_id, canonical, missing
                        )
                    )
            return results

        conn = self._conn()
        removals: list[str] = []
        try:
            conn.execute("BEGIN IMMEDIATE")
            row = conn.execute(
                "SELECT payload_hash, response_json FROM push_idempotency WHERE key = ?",
                (idempotency_key,),
            ).fetchone()
            if row is not None:
                if row["payload_hash"] != (payload_hash or ""):
                    raise IdempotencyConflict(
                        "idempotency key reused with a different payload"
                    )
                replay = json.loads(row["response_json"])
                conn.rollback()
                return replay

            results = []
            for record in records:
                conn.execute("SAVEPOINT push_record")
                try:
                    status, existing_id, canonical, missing = self._save_client_on_conn(
                        conn, record.client, record.fotos, actor, removals
                    )
                except ValueError:
                    conn.execute("ROLLBACK TO push_record")
                    conn.execute("RELEASE push_record")
                    results.append({"id": record.client.id, "status": "error"})
                else:
                    conn.execute("RELEASE push_record")
                    results.append(
                        self._push_result(
                            record, status, existing_id, canonical, missing
                        )
                    )
            conn.execute(
                "INSERT INTO push_idempotency (key, payload_hash, response_json, created_at) "
                "VALUES (?, ?, ?, ?)",
                (
                    idempotency_key,
                    payload_hash or "",
                    json.dumps(results, ensure_ascii=False),
                    int(time.time()),
                ),
            )
            conn.commit()
        except BaseException:
            conn.rollback()
            raise
        finally:
            conn.close()
        for path in removals:
            self._remove_file(path)
        return results

    @staticmethod
    def _push_result(
        record,
        status: str,
        existing_id: str | None,
        canonical: dict | None,
        missing: list[str],
    ) -> dict:
        return {
            "id": record.client.id,
            "status": status,
            "existing_id": existing_id,
            "client": canonical["client"] if canonical else None,
            "fotos": canonical["fotos"] if canonical else [],
            "missing_photos": missing,
        }

    def _record_audit(
        self,
        conn: sqlite3.Connection,
        cliente_id: str,
        actor: dict | None,
        action: str,
        ts: int,
        details: dict | None = None,
    ) -> None:
        """Append an audit entry for an accepted client persistence."""
        if actor is None:
            return
        conn.execute(
            "INSERT INTO client_audit (cliente_id, key_id, username, action, ts, details) "
            "VALUES (?, ?, ?, ?, ?, ?)",
            (
                cliente_id,
                actor.get("id", ""),
                actor.get("comment") or None,
                action,
                ts,
                json.dumps(details, ensure_ascii=False) if details is not None else None,
            ),
        )

    def _photo_ids(self, conn: sqlite3.Connection, cliente_id: str) -> list[str]:
        """Ordered photo ids currently stored for a client."""
        return [
            row["id"]
            for row in conn.execute(
                "SELECT id FROM fotos WHERE cliente_id = ? ORDER BY ordem", (cliente_id,)
            ).fetchall()
        ]

    def _replace_fotos(
        self, conn: sqlite3.Connection, cliente_id: str, fotos: list[FotoIn]
    ) -> tuple[list[str], list[str], list[str]]:
        """Merge a client's photos into the store.

        Photos are referenced by id and their binary content is uploaded to the
        photo endpoint separately. Every photo the push lists gets a row, even
        when its file is missing: the row is a placeholder so the photo is not
        silently forgotten, and its id is reported in `missing_ids` so the
        client uploads the bytes again. Photos owned by another client are
        rejected rather than reassigned. Stored photos the push no longer lists
        are removed from the database; their files are returned to the caller so
        they can be deleted only after the transaction commits.

        Returns (kept_ids, missing_ids, files_to_remove): the canonical photo ids
        in display order, the ids whose file is absent, and the paths that are no
        longer referenced.
        """
        incoming = sorted(fotos, key=lambda f: f.ordem)
        kept_ids: list[str] = []
        missing_ids: list[str] = []
        for foto in incoming:
            owner = conn.execute(
                "SELECT cliente_id FROM fotos WHERE id = ?", (foto.id,)
            ).fetchone()
            if owner is not None and owner["cliente_id"] != cliente_id:
                raise ValueError(
                    f"photo id {foto.id} already belongs to another client"
                )
            kept_ids.append(foto.id)
            if not os.path.exists(self._foto(foto.id)):
                missing_ids.append(foto.id)

        # The client's canonical photo set is exactly what the push lists.
        # Remove rows no longer present, but defer deleting their files until the
        # caller commits: a rollback must not leave rows pointing at missing
        # files.
        if kept_ids:
            remove_rows = conn.execute(
                "SELECT caminho FROM fotos WHERE cliente_id = ? AND id NOT IN (%s)"
                % ",".join("?" * len(kept_ids)),
                [cliente_id, *kept_ids],
            ).fetchall()
        else:
            remove_rows = conn.execute(
                "SELECT caminho FROM fotos WHERE cliente_id = ?", (cliente_id,)
            ).fetchall()
        files_to_remove = [row["caminho"] for row in remove_rows]
        if kept_ids:
            conn.execute(
                "DELETE FROM fotos WHERE cliente_id = ? AND id NOT IN (%s)"
                % ",".join("?" * len(kept_ids)),
                [cliente_id, *kept_ids],
            )
        else:
            conn.execute("DELETE FROM fotos WHERE cliente_id = ?", (cliente_id,))

        for foto in incoming:
            path = self._foto(foto.id)
            digest = self._hash_file(path)
            conn.execute(
                "INSERT INTO fotos (id, cliente_id, mime, caminho, ordem, hash) "
                "VALUES (?, ?, ?, ?, ?, ?) "
                "ON CONFLICT(id) DO UPDATE SET "
                "cliente_id=excluded.cliente_id, mime=excluded.mime, caminho=excluded.caminho, ordem=excluded.ordem, hash=excluded.hash",
                (foto.id, cliente_id, foto.mime, path, foto.ordem, digest),
            )
        return kept_ids, missing_ids, files_to_remove

    def _foto(self, foto_id: str) -> str:
        candidate = os.path.abspath(os.path.join(self.fotos_dir, foto_id))
        root = os.path.abspath(self.fotos_dir)
        # Defense in depth: even if an id bypasses model validation, the resolved
        # path must stay inside the photos directory.
        if candidate != root and not candidate.startswith(root + os.sep):
            raise ValueError("photo path escapes the photos directory")
        return candidate

    def _remove_file(self, path: str) -> None:
        try:
            os.remove(path)
        except FileNotFoundError:
            pass

    def _read_foto(self, path: str) -> bytes | None:
        try:
            with open(path, "rb") as fh:
                return fh.read()
        except FileNotFoundError:
            return None

    def _hash_file(self, path: str) -> str | None:
        data = self._read_foto(path)
        return hashlib.sha256(data).hexdigest() if data is not None else None

    def get_photo(self, foto_id: str) -> tuple[str, bytes] | None:
        """Return (mime, bytes) for a stored photo, or None when absent."""
        conn = self._conn()
        try:
            row = conn.execute(
                "SELECT mime, caminho FROM fotos WHERE id = ?", (foto_id,)
            ).fetchone()
        finally:
            conn.close()
        if row is None:
            return None
        data = self._read_foto(row["caminho"])
        if data is None:
            return None
        return row["mime"], data

    def get_photo_path(self, foto_id: str) -> tuple[str, str] | None:
        """Return (mime, path) for a stored photo, without reading its bytes."""
        conn = self._conn()
        try:
            row = conn.execute(
                "SELECT mime, caminho FROM fotos WHERE id = ?", (foto_id,)
            ).fetchone()
        finally:
            conn.close()
        if row is None or not os.path.isfile(row["caminho"]):
            return None
        return row["mime"], row["caminho"]

    def write_photo(
        self,
        foto_id: str,
        data: bytes,
        *,
        uploaded_by_key: str | None = None,
        require_reference: bool = False,
        max_total_bytes: int | None = None,
        max_key_bytes: int | None = None,
    ) -> None:
        """Atomically write a photo's binary content to the managed directory.

        The content is staged in a unique temporary file next to the destination
        and moved into place with ``os.replace``. A fixed ``<path>.tmp`` would
        let two concurrent uploads of the same photo overwrite each other's
        partial write, leaving truncated bytes for a reader that wins the race.

        When ``require_reference`` is true the upload must match an existing
        ``fotos`` row (created by an accepted push); unknown identifiers are
        rejected before a file is written. Quotas are checked against
        ``size_bytes`` under the same write lock, before the file is replaced.
        """
        path = self._foto(foto_id)
        conn = self._conn()
        tmp: str | None = None
        try:
            conn.execute("BEGIN IMMEDIATE")
            row = conn.execute(
                "SELECT size_bytes, uploaded_by_key FROM fotos WHERE id = ?", (foto_id,)
            ).fetchone()
            if require_reference and row is None:
                raise PhotoNotReferenced(f"photo {foto_id} is not referenced")

            old_size = int(row["size_bytes"] or 0) if row is not None else 0
            old_key = row["uploaded_by_key"] if row is not None else None
            if max_total_bytes is not None:
                total = conn.execute(
                    "SELECT COALESCE(SUM(size_bytes), 0) FROM fotos"
                ).fetchone()[0]
                if int(total) - old_size + len(data) > max_total_bytes:
                    raise PhotoQuotaExceeded("instance photo storage quota exceeded")
            if max_key_bytes is not None and uploaded_by_key is not None:
                key_total = conn.execute(
                    "SELECT COALESCE(SUM(size_bytes), 0) FROM fotos "
                    "WHERE uploaded_by_key = ?",
                    (uploaded_by_key,),
                ).fetchone()[0]
                key_total = int(key_total)
                if old_key == uploaded_by_key:
                    key_total -= old_size
                if key_total + len(data) > max_key_bytes:
                    raise PhotoQuotaExceeded("key photo storage quota exceeded")

            fd, tmp = tempfile.mkstemp(
                dir=os.path.dirname(path),
                prefix=f".{os.path.basename(path)}.",
                suffix=".tmp",
            )
            with os.fdopen(fd, "wb") as fh:
                fh.write(data)
            os.replace(tmp, path)
            tmp = None
            digest = hashlib.sha256(data).hexdigest()
            conn.execute(
                "UPDATE fotos SET hash = ?, size_bytes = ?, uploaded_by_key = ? "
                "WHERE id = ?",
                (digest, len(data), uploaded_by_key, foto_id),
            )
            conn.commit()
        except BaseException:
            # Never leave a half-written staging file behind (the orphan sweep
            # ignores .tmp files, so it would linger until the next restart).
            if tmp is not None:
                try:
                    os.remove(tmp)
                except FileNotFoundError:
                    pass
            conn.rollback()
            raise
        finally:
            conn.close()

    def gc_orphan_photos(self, min_age_seconds: int = 0) -> int:
        """Delete photo files on disk that have no matching ``fotos`` row.

        Reclaims space from photos removed via client edits and from
        ``PUT /api/photos/{id}`` uploads that were never referenced by a push.
        Returns the number of files removed.

        Only files inside the managed photos directory are considered, and
        temporary ``.tmp`` files are ignored. ``min_age_seconds`` skips files
        modified more recently than that: a periodic sweep can run while an
        upload is in flight and its ``fotos`` row is not committed yet, so
        young files get a grace period before they can be treated as orphans.
        """
        if not os.path.isdir(self.fotos_dir):
            return 0
        conn = self._conn()
        try:
            stored = {
                os.path.abspath(row["caminho"])
                for row in conn.execute("SELECT caminho FROM fotos").fetchall()
            }
        finally:
            conn.close()
        now = time.time()
        removed = 0
        for name in os.listdir(self.fotos_dir):
            if name.endswith(".tmp"):
                continue
            path = os.path.join(self.fotos_dir, name)
            # A subdirectory is not an orphaned photo; removing it would also
            # mean deleting a whole tree, which this pass must never do.
            if not os.path.isfile(path):
                continue
            if path in stored:
                continue
            if min_age_seconds > 0:
                try:
                    if now - os.path.getmtime(path) < min_age_seconds:
                        continue
                except FileNotFoundError:
                    continue
            try:
                os.remove(path)
                removed += 1
            except FileNotFoundError:
                pass
        return removed

    def prune_old_records(
        self,
        tombstone_retention_days: int = 0,
        audit_retention_days: int = 0,
        now: int | None = None,
    ) -> tuple[int, int]:
        """Delete tombstones and audit entries older than the retention windows.

        Retention is opt-in per table: a value of 0 (or negative) disables that
        table, which is the default. Nothing is ever discarded unless an
        operator configures a window through the environment.

        Tombstone age is measured by ``delivery_ts`` (assigned by the server)
        rather than ``deleted_at`` (sent by the device): a device with a lagging
        clock must not make a tombstone expire before it had time to propagate.
        Audit age uses ``ts``, the device edit time stored with the entry.

        Returns ``(tombstones_removed, audit_entries_removed)``. The operation
        is idempotent: a second run with the same clock removes nothing.
        """
        if tombstone_retention_days <= 0 and audit_retention_days <= 0:
            return 0, 0
        now = int(time.time()) if now is None else now
        tombstones_removed = 0
        audit_removed = 0
        conn = self._conn()
        try:
            if tombstone_retention_days > 0:
                cutoff = now - tombstone_retention_days * 86400
                cur = conn.execute(
                    "DELETE FROM tombstones WHERE delivery_ts < ?", (cutoff,)
                )
                tombstones_removed = cur.rowcount
            if audit_retention_days > 0:
                cutoff = now - audit_retention_days * 86400
                cur = conn.execute("DELETE FROM client_audit WHERE ts < ?", (cutoff,))
                audit_removed = cur.rowcount
            conn.commit()
        finally:
            conn.close()
        return tombstones_removed, audit_removed

    def prune_idempotency(self, retention_days: int, now: int | None = None) -> int:
        """Delete push idempotency records older than the retention window.

        A key only needs to survive as long as a client may retry the same push;
        keeping every response forever makes the table grow without bound.
        Returns the number of rows removed.
        """
        if retention_days <= 0:
            return 0
        now = int(time.time()) if now is None else now
        cutoff = now - retention_days * 86400
        conn = self._conn()
        try:
            cur = conn.execute(
                "DELETE FROM push_idempotency WHERE created_at < ?", (cutoff,)
            )
            conn.commit()
            return cur.rowcount
        finally:
            conn.close()

    @staticmethod
    def _delivery_watermark(conn: sqlite3.Connection) -> int:
        """Highest delivery timestamp handed out so far (clients + tombstones).

        Reads `delivery_ts`, not `updated_at`: the two clocks are independent and
        only the delivery clock orders the pull cursor.
        """
        max_clients = conn.execute(
            "SELECT COALESCE(MAX(delivery_ts), 0) FROM clientes"
        ).fetchone()[0]
        max_tombstones = conn.execute(
            "SELECT COALESCE(MAX(delivery_ts), 0) FROM tombstones"
        ).fetchone()[0]
        return max(max_clients, max_tombstones)

    def latest_updated_at(self) -> int:
        """Largest delivery timestamp over clients and tombstones (0 when empty)."""
        conn = self._conn()
        try:
            return self._delivery_watermark(conn)
        finally:
            conn.close()

    def instance_id(self) -> str:
        """Stable identifier for this server database.

        Generated once when the schema is first created and stored in
        ``server_meta``. It survives restarts but changes with a recreated
        database, so clients can avoid reconciling against a different server.
        """
        conn = self._conn()
        try:
            row = conn.execute(
                "SELECT value FROM server_meta WHERE key = 'instance_id'"
            ).fetchone()
            if row is None:
                # Defensive: a database created by a version before the table
                # existed still gets an identity on first read.
                value = str(uuid.uuid4())
                conn.execute(
                    "INSERT INTO server_meta (key, value) VALUES ('instance_id', ?)",
                    (value,),
                )
                conn.commit()
                return value
            return row["value"]
        finally:
            conn.close()

    def client_count(self) -> int:
        """Number of live client records (tombstoned deletions are not clients)."""
        conn = self._conn()
        try:
            return conn.execute("SELECT COUNT(*) FROM clientes").fetchone()[0]
        finally:
            conn.close()

    def client_history(self, cliente_id: str) -> list[dict]:
        conn = self._conn()
        try:
            rows = conn.execute(
                "SELECT action, username, key_id, ts, details FROM client_audit "
                "WHERE cliente_id = ? ORDER BY ts, id",
                (cliente_id,),
            ).fetchall()
        finally:
            conn.close()
        history = []
        for row in rows:
            entry = dict(row)
            raw = entry.get("details")
            # Entries written before change tracking carry no details.
            entry["details"] = json.loads(raw) if raw else None
            history.append(entry)
        return history

    def client_history_page(
        self,
        cliente_id: str,
        limit: int,
        after_id: int | None = None,
    ) -> tuple[list[dict], int | None]:
        """Return one history page ordered by insertion id, plus the next cursor."""
        conn = self._conn()
        try:
            if after_id is None:
                rows = conn.execute(
                    "SELECT id, action, username, key_id, ts, details FROM client_audit "
                    "WHERE cliente_id = ? ORDER BY id LIMIT ?",
                    (cliente_id, limit + 1),
                ).fetchall()
            else:
                rows = conn.execute(
                    "SELECT id, action, username, key_id, ts, details FROM client_audit "
                    "WHERE cliente_id = ? AND id > ? ORDER BY id LIMIT ?",
                    (cliente_id, after_id, limit + 1),
                ).fetchall()
        finally:
            conn.close()
        has_more = len(rows) > limit
        kept = rows[:limit]
        next_after_id = kept[-1]["id"] if has_more and kept else None
        history = []
        for row in kept:
            entry = dict(row)
            entry.pop("id", None)
            raw = entry.get("details")
            entry["details"] = json.loads(raw) if raw else None
            history.append(entry)
        return history, next_after_id

    def list_clients(self) -> list[dict]:
        conn = self._conn()
        try:
            rows = conn.execute(
                "SELECT id, nome, contrato, tag, latitude, longitude, notas, ativo, updated_at, created_at "
                "FROM clientes ORDER BY nome COLLATE NOCASE"
            ).fetchall()
            return [dict(r) for r in rows]
        finally:
            conn.close()

    def get_client_with_photos(self, cliente_id: str) -> dict | None:
        conn = self._conn()
        try:
            record = self._canonical_from_conn(conn, cliente_id)
            # _canonical_from_conn persists a missing photo hash when it has to
            # read the file. Commit here so the next read can reuse it instead
            # of hashing the same bytes again.
            conn.commit()
            return record
        finally:
            conn.close()

    def _canonical_batch_from_conn(
        self, conn: sqlite3.Connection, cliente_ids: list[str]
    ) -> list[dict]:
        """Read a pull page with its photos using bounded queries.

        Pull pages can carry up to 1000 records. Loading each one with a new
        connection (and one photo query per client) made the pull cost grow
        with the page size. This reads the clients and photos in ID sub-batches
        on the caller's connection, preserving the page order, and computes any
        missing photo hash once, committing it before the connection closes.
        """
        if not cliente_ids:
            return []

        clients: dict[str, sqlite3.Row] = {}
        photos: dict[str, list[sqlite3.Row]] = {}
        chunk_size = 500
        for start in range(0, len(cliente_ids), chunk_size):
            chunk = cliente_ids[start : start + chunk_size]
            placeholders = ",".join("?" for _ in chunk)
            rows = conn.execute(
                "SELECT id, nome, contrato, tag, latitude, longitude, notas, ativo, "
                "updated_at, created_at, delivery_ts FROM clientes "
                f"WHERE id IN ({placeholders})",
                chunk,
            ).fetchall()
            clients.update({row["id"]: row for row in rows})
            for row in conn.execute(
                "SELECT cliente_id, id, mime, caminho, ordem, hash FROM fotos "
                f"WHERE cliente_id IN ({placeholders}) "
                "ORDER BY cliente_id, ordem, id",
                chunk,
            ).fetchall():
                photos.setdefault(row["cliente_id"], []).append(row)

        hash_updates: list[tuple[str, str]] = []
        records: list[dict] = []
        for cliente_id in cliente_ids:
            row = clients.get(cliente_id)
            # A record deleted between the page query and this read is simply
            # not part of the page; returning None would break the response.
            if row is None:
                continue
            foto_out = []
            for f in photos.get(cliente_id, []):
                digest = f["hash"]
                available = os.path.exists(f["caminho"])
                if not available:
                    digest = None
                elif digest is None:
                    digest = self._hash_file(f["caminho"])
                    if digest is not None:
                        hash_updates.append((digest, f["id"]))
                foto_out.append(
                    {
                        "id": f["id"],
                        "mime": f["mime"],
                        "hash": digest,
                        "ordem": f["ordem"],
                        "available": available,
                    }
                )
            records.append(
                {
                    "client": dict(row),
                    "fotos": foto_out,
                    "photos_complete": True,
                }
            )
        if hash_updates:
            conn.executemany(
                "UPDATE fotos SET hash = ? WHERE id = ?", hash_updates
            )
            conn.commit()
        return records

    def _canonical_from_conn(self, conn: sqlite3.Connection, cliente_id: str) -> dict | None:
        """Read one client with its photos from an open connection.

        Returns None when the client does not exist.
        """
        row = conn.execute(
            "SELECT id, nome, contrato, tag, latitude, longitude, notas, ativo, updated_at, created_at, delivery_ts "
            "FROM clientes WHERE id = ?",
            (cliente_id,),
        ).fetchone()
        if row is None:
            return None
        fotos = conn.execute(
            "SELECT id, mime, caminho, ordem, hash FROM fotos WHERE cliente_id = ? ORDER BY ordem",
            (cliente_id,),
        ).fetchall()
        foto_out = []
        for f in fotos:
            digest = f["hash"]
            available = os.path.exists(f["caminho"])
            if not available:
                # A row whose file is missing is still returned. Omitting it
                # would make every device interpret the absence as a deletion
                # and drop its own copy; `available=False` tells clients the
                # metadata exists but the bytes need repair/upload.
                digest = None
            elif digest is None:
                digest = self._hash_file(f["caminho"])
                if digest is not None:
                    conn.execute(
                        "UPDATE fotos SET hash = ? WHERE id = ?", (digest, f["id"])
                    )
            foto_out.append(
                {
                    "id": f["id"],
                    "mime": f["mime"],
                    "hash": digest,
                    "ordem": f["ordem"],
                    "available": available,
                }
            )
        return {
            "client": dict(row),
            "fotos": foto_out,
            "photos_complete": True,
        }

    def pull_all(
        self,
        since: int | None = None,
        limit: int | None = None,
        since_id: str | None = None,
    ) -> tuple[list[dict], list[dict], int | None, str | None]:
        """Returns (records, tombstones, next_since, next_since_id).

        The cursor is the *delivery* timestamp, not `updated_at`: the server
        hands out delivery timestamps so a write stamped behind the current
        watermark is still delivered, while `updated_at` alone decides
        last-write-wins.

        Filters records to `delivery_ts > since` when `since` is given. When
        `since_id` is also given, the filter is a keyset over `(delivery_ts, id)`:
        `delivery_ts > since OR (delivery_ts = since AND id > since_id)`, so
        records sharing the same delivery timestamp are never skipped at a page
        boundary.

        With `limit`, at most `limit` records are returned per call and the cursor
        (`next_since`, `next_since_id`) is the `(delivery_ts, id)` of the last
        record of the returned page, to be used as `(since, since_id)` for the
        next call (a next page exists only when `next_since` is not None).
        Tombstones are filtered to `delivery_ts > since` when `since` is given so
        incremental pulls carry only new deletions.
        """
        conn = self._conn()
        try:
            sql = "SELECT id, delivery_ts FROM clientes"
            params: list = []
            if since is not None:
                if since_id is not None:
                    sql += " WHERE delivery_ts > ? OR (delivery_ts = ? AND id > ?)"
                    params += [since, since, since_id]
                else:
                    sql += " WHERE delivery_ts > ?"
                    params.append(since)
            # Paginated pulls order by delivery_ts so the cursor is a monotonic
            # keyset; unrestricted pulls keep the historical name ordering.
            if limit is not None:
                sql += " ORDER BY delivery_ts, id LIMIT ?"
                params.append(limit + 1)
            else:
                sql += " ORDER BY nome COLLATE NOCASE"
            rows = conn.execute(sql, params).fetchall()
            fetched = [(r["id"], r["delivery_ts"]) for r in rows]
            has_more = limit is not None and len(fetched) > limit
            page = fetched[:limit] if has_more else fetched
            cliente_ids = [cliente_id for cliente_id, _ in page]
            next_since = None
            next_since_id = None
            if has_more and page:
                # Taken from the row already read: re-querying could observe a
                # newer timestamp written meanwhile and make the client skip
                # everything between the two positions.
                last_id, last_delivery = page[-1]
                next_since, next_since_id = last_delivery, last_id

            if since is not None:
                tombstones = [
                    dict(r)
                    for r in conn.execute(
                        "SELECT id, deleted_at, delivery_ts FROM tombstones "
                        "WHERE delivery_ts > ? ORDER BY delivery_ts",
                        (since,),
                    ).fetchall()
                ]
            else:
                tombstones = [
                    dict(r)
                    for r in conn.execute(
                        "SELECT id, deleted_at, delivery_ts FROM tombstones "
                        "ORDER BY delivery_ts"
                    ).fetchall()
                ]
            records = self._canonical_batch_from_conn(conn, cliente_ids)
        finally:
            conn.close()
        return records, tombstones, next_since, next_since_id

    def delete_client(
        self,
        cliente_id: str,
        deleted_at: int | None = None,
        actor: dict | None = None,
    ) -> bool:
        conn = self._conn()
        try:
            # Serialize with pushes and read the delivery clock before the row
            # disappears, so the tombstone is handed out above everything already
            # delivered — a deletion stamped behind a device's watermark would
            # otherwise never reach it.
            conn.execute("BEGIN IMMEDIATE")
            existing = conn.execute(
                "SELECT id, nome, contrato, tag, latitude, longitude, notas, ativo, "
                "updated_at, created_at FROM clientes WHERE id = ?",
                (cliente_id,),
            ).fetchone()
            if existing is None:
                return False
            watermark = self._delivery_watermark(conn)
            if deleted_at is None:
                deleted_at = int(time.time())
            # Deletion is a write and competes in LWW with edits. A delete
            # stamped at or before the stored update is stale and must not
            # remove the newer record or leave a tombstone behind.
            if deleted_at <= existing["updated_at"]:
                raise StaleDeleteError(
                    f"deleted_at {deleted_at} is not newer than updated_at "
                    f"{existing['updated_at']}"
                )
            paths = [
                r["caminho"]
                for r in conn.execute(
                    "SELECT caminho FROM fotos WHERE cliente_id = ?", (cliente_id,)
                ).fetchall()
            ]
            cur = conn.execute("DELETE FROM clientes WHERE id = ?", (cliente_id,))
            if cur.rowcount > 0:
                conn.execute(
                    "INSERT OR REPLACE INTO tombstones (id, deleted_at, delivery_ts) "
                    "VALUES (?, ?, ?)",
                    (cliente_id, deleted_at, max(deleted_at, watermark + 1)),
                )
                self._record_audit(
                    conn,
                    cliente_id,
                    actor,
                    "delete",
                    deleted_at,
                    {"deleted_client": dict(existing)},
                )
            conn.commit()
            if cur.rowcount > 0:
                # Files are removed only after the commit: a rollback must not
                # leave rows pointing at content that no longer exists.
                for path in paths:
                    self._remove_file(path)
            return cur.rowcount > 0
        finally:
            conn.close()

    def add_key(
        self, key_id: str, public_key: str, comment: str = "", provisioned: bool = False
    ) -> None:
        conn = self._conn()
        try:
            conn.execute(
                "INSERT OR REPLACE INTO keys (id, public_key, comment, provisioned, created_at) "
                "VALUES (?, ?, ?, ?, ?)",
                (key_id, public_key, comment, 1 if provisioned else 0, int(time.time())),
            )
            conn.commit()
        finally:
            conn.close()

    def remove_key(self, key_id: str) -> bool:
        conn = self._conn()
        try:
            cur = conn.execute("DELETE FROM keys WHERE id = ?", (key_id,))
            conn.commit()
            return cur.rowcount > 0
        finally:
            conn.close()

    def list_keys(self) -> list[dict]:
        conn = self._conn()
        try:
            rows = conn.execute(
                "SELECT id, public_key, comment, provisioned, created_at, revoked_at FROM keys ORDER BY created_at"
            ).fetchall()
            return [dict(r) for r in rows]
        finally:
            conn.close()

    def get_all_public_keys(self) -> list[str]:
        conn = self._conn()
        try:
            rows = conn.execute(
                "SELECT public_key FROM keys WHERE revoked_at IS NULL"
            ).fetchall()
            return [r["public_key"] for r in rows]
        finally:
            conn.close()

    def get_key_by_public_key(self, public_key: str) -> dict | None:
        conn = self._conn()
        try:
            row = conn.execute(
                "SELECT id, public_key, comment, created_at FROM keys WHERE public_key = ? AND revoked_at IS NULL",
                (public_key,),
            ).fetchone()
            return dict(row) if row is not None else None
        finally:
            conn.close()

    def revoke_keys_by_comment(self, comment: str, now: int | None = None) -> int:
        """Revokes every active key whose comment matches, returning how many changed.

        The rows are kept so existing audit entries keep pointing at a valid key id.
        """
        conn = self._conn()
        try:
            with conn:
                cursor = conn.execute(
                    "UPDATE keys SET revoked_at = ? WHERE comment = ? AND revoked_at IS NULL",
                    (int(now if now is not None else time.time()), comment),
                )
                return cursor.rowcount
        finally:
            conn.close()
    def revoke_key(self, key_id: str, now: int | None = None) -> int:
        """Revokes one active key by id, returning 1 when it changed and 0 otherwise."""
        conn = self._conn()
        try:
            with conn:
                cursor = conn.execute(
                    "UPDATE keys SET revoked_at = ? WHERE id = ? AND revoked_at IS NULL",
                    (int(now if now is not None else time.time()), key_id),
                )
                return cursor.rowcount
        finally:
            conn.close()
