"""SQLite persistence layer. Owns schema, duplicate detection and LWW conflict
resolution. The server is the single authority over all data management.

Photo binary content lives in a filesystem directory (self.fotos_dir); the
fotos table stores only the file path (caminho) referencing each photo file.
"""

import hashlib
import json
import os
import sqlite3
import threading
import time

from .models import (
    ALLOWED_DOCUMENT_MIMES,
    ALLOWED_PHOTO_MIMES,
    ClienteIn,
    DocumentoIn,
    FotoIn,
    normalize_nome,
)

# Reject data timestamps further in the future than this many seconds, so a
# malicious or broken client timestamp cannot permanently win last-write-wins.
# Data timestamps (`updated_at`, `deleted_at`) are epoch seconds.
MAX_FUTURE_SKEW_SECONDS = 3600

SCHEMA = """
CREATE TABLE IF NOT EXISTS clientes (
    id TEXT PRIMARY KEY,
    nome TEXT NOT NULL,
    contrato TEXT,
    latitude REAL,
    longitude REAL,
    notas TEXT,
    updated_at INTEGER NOT NULL,
    created_at INTEGER NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_clientes_nome
    ON clientes (nome COLLATE NOCASE);
CREATE TABLE IF NOT EXISTS fotos (
    id TEXT PRIMARY KEY,
    cliente_id TEXT NOT NULL REFERENCES clientes(id) ON DELETE CASCADE,
    mime TEXT NOT NULL,
    caminho TEXT NOT NULL,
    ordem INTEGER NOT NULL DEFAULT 0,
    nome TEXT,
    kind TEXT NOT NULL DEFAULT 'photo'
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
    action TEXT NOT NULL CHECK(action IN ('create','edit')),
    ts INTEGER NOT NULL,
    details TEXT
);
CREATE INDEX IF NOT EXISTS idx_client_audit_cliente ON client_audit (cliente_id);
CREATE TABLE IF NOT EXISTS keys (
    id TEXT PRIMARY KEY,
    public_key TEXT NOT NULL,
    comment TEXT,
    provisioned INTEGER NOT NULL DEFAULT 0,
    created_at INTEGER NOT NULL
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
                latitude REAL,
                longitude REAL,
                notas TEXT,
                updated_at INTEGER NOT NULL,
                created_at INTEGER NOT NULL DEFAULT 0,
                delivery_ts INTEGER NOT NULL DEFAULT 0
            )
            """
        )
        conn.execute(
            """
            INSERT INTO clientes_new
                (id, nome, contrato, latitude, longitude, notas, updated_at, created_at, delivery_ts)
            SELECT id, nome, contrato, latitude, longitude, notas, updated_at, created_at, delivery_ts
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


def _migrate_fotos_documents(conn: sqlite3.Connection) -> None:
    """Idempotently turn the photos table into the attachment table.

    Device-map attachments (map images and PDF datasheets) live in the same
    table and directory as photos so the storage, hashing, cascade delete and
    orphan sweep stay one code path. `kind` separates the two lists and `nome`
    carries the original file name (null for photos, which only need an index).
    """
    columns = [row["name"] for row in conn.execute("PRAGMA table_info(fotos)").fetchall()]
    if "nome" not in columns:
        conn.execute("ALTER TABLE fotos ADD COLUMN nome TEXT")
    if "kind" not in columns:
        conn.execute("ALTER TABLE fotos ADD COLUMN kind TEXT NOT NULL DEFAULT 'photo'")
    # Created here (not in SCHEMA) because the column only exists after the
    # ALTER above on pre-existing databases.
    conn.execute(
        "CREATE INDEX IF NOT EXISTS idx_fotos_cliente_kind ON fotos (cliente_id, kind)"
    )


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
    before_documents: list[tuple[str, str]] = (),
    after_documents: list[tuple[str, str]] = (),
) -> dict | None:
    """Describe what an accepted write changed, or None when nothing did.

    ``before`` is the stored client row (None on insert); the photo arguments are
    the ordered ids of the canonical sets before and after the write, while the
    document arguments are ``(id, nome)`` pairs so the history can name the files
    the user added or removed. Text values are shortened to ``AUDIT_TEXT_LIMIT``
    and flagged; coordinates are compared with six decimals and presented as a
    single "localizacao" field.
    """
    before_document_ids = {doc_id for doc_id, _ in before_documents}
    after_document_ids = {doc_id for doc_id, _ in after_documents}
    added_names = [
        name for doc_id, name in after_documents if doc_id not in before_document_ids
    ]
    removed_names = [
        name for doc_id, name in before_documents if doc_id not in after_document_ids
    ]
    documents = None
    if added_names or removed_names:
        documents = {
            "added": [name or "(sem nome)" for name in added_names],
            "removed": [name or "(sem nome)" for name in removed_names],
        }

    if before is None:
        details: dict = {"fields": []}
        if after_photos:
            details["photos"] = {
                "added": list(after_photos),
                "removed": [],
                "reordered": False,
            }
        if documents is not None:
            details["documents"] = documents
        return details if len(details) > 1 else None

    fields: list[dict] = []
    for name in ("nome", "contrato", "notas"):
        old = before.get(name)
        new = after.get(name)
        if name in ("contrato", "notas"):
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

    added = [foto_id for foto_id in after_photos if foto_id not in before_photos]
    removed = [foto_id for foto_id in before_photos if foto_id not in after_photos]
    reordered = not added and not removed and before_photos != after_photos
    photos = (
        {"added": added, "removed": removed, "reordered": reordered}
        if added or removed or reordered
        else None
    )

    if not fields and photos is None and documents is None:
        return None
    details: dict = {"fields": fields}
    if photos is not None:
        details["photos"] = photos
    if documents is not None:
        details["documents"] = documents
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
    _migrate_keys_comment(conn)
    _migrate_keys_provisioned(conn)
    _migrate_clients_created_at(conn)
    # delivery_ts must exist before the coordinate rebuild, which copies it.
    _migrate_delivery_ts(conn)
    _migrate_clients_nullable_coords(conn)
    _migrate_client_audit_details(conn)
    _migrate_fotos_documents(conn)
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
        self,
        cliente: ClienteIn,
        fotos: list[FotoIn],
        actor: dict | None = None,
        documentos: list[DocumentoIn] | None = None,
    ) -> tuple[str, str | None, dict | None, list[str], list[str]]:
        """Insert or update a client, applying last-write-wins.

        When `actor` (the authenticated key record: `id`, `comment`) is given and
        the record is accepted, an audit entry is appended to client_audit
        (`create` on insert, `edit` on update).

        Returns (status, existing_id, canonical, missing_photos, missing_documents)
        where status is:
          - "accepted": the record (and attachments) were persisted
          - "duplicate": a different client already holds this normalized name
          - "stale": the incoming updated_at is older than the stored value

        On "duplicate", `canonical` carries the stored canonical record (client
        + photos + documents) so the client can merge without a separate
        round-trip; it is None for any other status.

        The two ``missing_*`` lists hold the attachment ids the push referenced
        but whose bytes are not on the server. They are not stored, and the
        client is told so it uploads them again instead of assuming they are safe.
        """
        documentos = documentos or []
        nome = normalize_nome(cliente.nome)
        if not nome:
            raise ValueError("nome cannot be blank")

        # updated_at is epoch seconds; reject a far-future value so a malicious
        # or broken timestamp cannot permanently win last-write-wins.
        if cliente.updated_at > int(time.time()) + MAX_FUTURE_SKEW_SECONDS:
            raise ValueError("updated_at is too far in the future")

        conn = self._conn()
        try:
            # Serialize writers before reading the delivery watermark below, so
            # two concurrent pushes cannot both pick the same bumped timestamp
            # and hide each other from devices pulling in between.
            conn.execute("BEGIN IMMEDIATE")
            # Read the delivery clock before touching any row: a resurrection
            # deletes the tombstone, and the new record still has to be handed
            # out above everything already delivered.
            watermark = self._delivery_watermark(conn)
            tombstone = conn.execute(
                "SELECT deleted_at FROM tombstones WHERE id = ?", (cliente.id,)
            ).fetchone()
            if tombstone is not None and cliente.updated_at <= tombstone["deleted_at"]:
                return "stale", None, None, [], []
            if tombstone is not None:
                # The pushed record is newer than the deletion: resurrect the
                # client and clear the tombstone so it persists as a new record.
                conn.execute("DELETE FROM tombstones WHERE id = ?", (cliente.id,))

            # A record cannot move an existing attachment to the other kind, nor
            # list the same id as both a photo and a document: ids are unique
            # across the attachment table and the kind decides which endpoint
            # serves the file, so a silent flip would make the file unreachable.
            self._reject_kind_collisions(conn, fotos, documentos)

            # `updated_at` keeps the device timestamp (it is what last-write-wins
            # compares) while `delivery_ts` is assigned here so the change is
            # visible to every device pulling from a watermark at or behind it,
            # even when the pushing device's clock is behind.
            delivery_ts = max(cliente.updated_at, watermark + 1)

            existing = conn.execute(
                "SELECT id, updated_at, nome, contrato, latitude, longitude, notas "
                "FROM clientes WHERE id = ?",
                (cliente.id,),
            ).fetchone()

            if existing is not None:
                if cliente.updated_at < existing["updated_at"]:
                    return "stale", None, None, [], []
                other = conn.execute(
                    "SELECT id FROM clientes WHERE nome = ? COLLATE NOCASE AND id != ?",
                    (nome, cliente.id),
                ).fetchone()
                if other is not None:
                    canonical = self._canonical_from_conn(conn, other["id"])
                    return "duplicate", other["id"], canonical, [], []
                before = dict(existing)
                before_photos = self._attachment_ids(conn, cliente.id, "photo")
                before_documents = self._attachment_names(conn, cliente.id, "document")
                conn.execute(
                    "UPDATE clientes SET nome=?, contrato=?, latitude=?, longitude=?, notas=?, updated_at=?, delivery_ts=? WHERE id=?",
                    (
                        nome,
                        cliente.contrato,
                        cliente.latitude,
                        cliente.longitude,
                        cliente.notas,
                        cliente.updated_at,
                        delivery_ts,
                        cliente.id,
                    ),
                )
                kept_ids, missing_ids = self._replace_attachments(
                    conn, cliente.id, fotos, "photo", ALLOWED_PHOTO_MIMES
                )
                _kept_docs, missing_docs = self._replace_attachments(
                    conn, cliente.id, documentos, "document", ALLOWED_DOCUMENT_MIMES
                )
                details = build_audit_details(
                    before,
                    {
                        "nome": nome,
                        "contrato": cliente.contrato,
                        "latitude": cliente.latitude,
                        "longitude": cliente.longitude,
                        "notas": cliente.notas,
                    },
                    before_photos,
                    kept_ids,
                    before_documents,
                    self._attachment_names(conn, cliente.id, "document"),
                )
                # A push that changes nothing records no entry: "edited" with no
                # visible difference is noise in the history.
                if details is not None:
                    self._record_audit(
                        conn, cliente.id, actor, "edit", cliente.updated_at, details
                    )
                conn.commit()
                return "accepted", None, None, missing_ids, missing_docs

            try:
                conn.execute(
                    "INSERT INTO clientes (id, nome, contrato, latitude, longitude, notas, updated_at, created_at, delivery_ts) "
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    (
                        cliente.id,
                        nome,
                        cliente.contrato,
                        cliente.latitude,
                        cliente.longitude,
                        cliente.notas,
                        cliente.updated_at,
                        cliente.updated_at,
                        delivery_ts,
                    ),
                )
            except sqlite3.IntegrityError:
                conn.rollback()
                other = conn.execute(
                    "SELECT id FROM clientes WHERE nome = ? COLLATE NOCASE", (nome,)
                ).fetchone()
                if other is None:
                    return "duplicate", None, None, [], []
                canonical = self._canonical_from_conn(conn, other["id"])
                return "duplicate", other["id"], canonical, [], []
            kept_ids, missing_ids = self._replace_attachments(
                conn, cliente.id, fotos, "photo", ALLOWED_PHOTO_MIMES
            )
            _kept_docs, missing_docs = self._replace_attachments(
                conn, cliente.id, documentos, "document", ALLOWED_DOCUMENT_MIMES
            )
            details = build_audit_details(
                None,
                {
                    "nome": nome,
                    "contrato": cliente.contrato,
                    "latitude": cliente.latitude,
                    "longitude": cliente.longitude,
                    "notas": cliente.notas,
                },
                [],
                kept_ids,
                [],
                self._attachment_names(conn, cliente.id, "document"),
            )
            self._record_audit(
                conn, cliente.id, actor, "create", cliente.updated_at, details
            )
            conn.commit()
            return "accepted", None, None, missing_ids, missing_docs
        finally:
            conn.close()

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

    def _reject_kind_collisions(
        self,
        conn: sqlite3.Connection,
        fotos: list[FotoIn],
        documentos: list[DocumentoIn],
    ) -> None:
        """Reject ids that would exist as both kinds or change kind in place."""
        photo_ids = {f.id for f in fotos}
        document_ids = {d.id for d in documentos}
        overlap = photo_ids & document_ids
        if overlap:
            raise ValueError(
                f"attachment id {sorted(overlap)[0]} is listed as both photo and document"
            )
        for ids, kind, other in (
            (photo_ids, "photo", "document"),
            (document_ids, "document", "photo"),
        ):
            if not ids:
                continue
            rows = conn.execute(
                "SELECT id, kind FROM fotos WHERE id IN (%s)" % ",".join("?" * len(ids)),
                sorted(ids),
            ).fetchall()
            for row in rows:
                if row["kind"] == other:
                    raise ValueError(
                        f"attachment id {row['id']} already belongs to a {other}"
                    )

    def _attachment_ids(
        self, conn: sqlite3.Connection, cliente_id: str, kind: str
    ) -> list[str]:
        """Ordered attachment ids of one kind currently stored for a client."""
        return [
            row["id"]
            for row in conn.execute(
                "SELECT id FROM fotos WHERE cliente_id = ? AND kind = ? "
                "ORDER BY ordem, id",
                (cliente_id, kind),
            ).fetchall()
        ]

    def _attachment_names(
        self, conn: sqlite3.Connection, cliente_id: str, kind: str
    ) -> list[tuple[str, str]]:
        """Ordered ``(id, nome)`` pairs of one attachment kind for a client."""
        return [
            (row["id"], row["nome"] or "")
            for row in conn.execute(
                "SELECT id, nome FROM fotos WHERE cliente_id = ? AND kind = ? "
                "ORDER BY ordem, id",
                (cliente_id, kind),
            ).fetchall()
        ]

    def _replace_attachments(
        self,
        conn: sqlite3.Connection,
        cliente_id: str,
        items: list[FotoIn] | list[DocumentoIn],
        kind: str,
        allowed_mimes: frozenset[str],
    ) -> tuple[list[str], list[str]]:
        """Merge one kind of attachment (photos or documents) into the store.

        Attachments are referenced by id and their binary content is uploaded to
        the matching endpoint separately; only rows pointing at an existing file
        are recorded. An attachment owned by another client, already stored as
        the other kind, or carrying a mime outside ``allowed_mimes`` rejects the
        whole record. Stored attachments of this kind the push no longer lists
        are removed.

        Returns (kept_ids, missing_ids): the kept ids in display order (the
        canonical set, which the audit uses to describe what was added or
        removed) and the ids the push referenced whose file is absent. The
        missing ones are reported back so the client uploads them again instead
        of recording them as already pushed.
        """
        incoming = sorted(items, key=lambda a: a.ordem)
        kept_ids: list[str] = []
        missing_ids: list[str] = []
        seen: set[str] = set()
        for item in incoming:
            if item.id in seen:
                continue
            seen.add(item.id)
            if item.mime not in allowed_mimes:
                raise ValueError(f"{kind} mime {item.mime!r} is not allowed")
            owner = conn.execute(
                "SELECT cliente_id, kind FROM fotos WHERE id = ?", (item.id,)
            ).fetchone()
            if owner is not None and owner["cliente_id"] != cliente_id:
                raise ValueError(
                    f"attachment id {item.id} already belongs to another client"
                )
            if owner is not None and owner["kind"] != kind:
                raise ValueError(
                    f"attachment id {item.id} already belongs to a {owner['kind']}"
                )
            if os.path.exists(self._foto(item.id)):
                kept_ids.append(item.id)
            else:
                missing_ids.append(item.id)

        # The client's canonical set of this kind is exactly what the push lists.
        # Remove rows no longer present, deleting their files from disk too so
        # storage does not grow from orphaned files.
        if kept_ids:
            remove_rows = conn.execute(
                "SELECT caminho FROM fotos WHERE cliente_id = ? AND kind = ? "
                "AND id NOT IN (%s)" % ",".join("?" * len(kept_ids)),
                [cliente_id, kind, *kept_ids],
            ).fetchall()
        else:
            remove_rows = conn.execute(
                "SELECT caminho FROM fotos WHERE cliente_id = ? AND kind = ?",
                (cliente_id, kind),
            ).fetchall()
        for row in remove_rows:
            self._remove_file(row["caminho"])
        if kept_ids:
            conn.execute(
                "DELETE FROM fotos WHERE cliente_id = ? AND kind = ? "
                "AND id NOT IN (%s)" % ",".join("?" * len(kept_ids)),
                [cliente_id, kind, *kept_ids],
            )
        else:
            conn.execute(
                "DELETE FROM fotos WHERE cliente_id = ? AND kind = ?",
                (cliente_id, kind),
            )

        for item in incoming:
            if item.id not in kept_ids:
                continue
            nome = getattr(item, "nome", None)
            conn.execute(
                "INSERT INTO fotos (id, cliente_id, mime, caminho, ordem, nome, kind) "
                "VALUES (?, ?, ?, ?, ?, ?, ?) "
                "ON CONFLICT(id) DO UPDATE SET "
                "cliente_id=excluded.cliente_id, mime=excluded.mime, "
                "caminho=excluded.caminho, ordem=excluded.ordem, "
                "nome=excluded.nome, kind=excluded.kind",
                (item.id, cliente_id, item.mime, self._foto(item.id), item.ordem, nome, kind),
            )
        return kept_ids, missing_ids

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

    def get_photo(self, foto_id: str) -> tuple[str, bytes] | None:
        """Return (mime, bytes) for a stored photo, or None when absent."""
        return self._get_attachment(foto_id, "photo")

    def get_document(self, documento_id: str) -> tuple[str, bytes] | None:
        """Return (mime, bytes) for a stored document, or None when absent.

        The kind is checked so a photo id cannot be downloaded as a document
        (and vice versa), even though ids live in one table.
        """
        return self._get_attachment(documento_id, "document")

    def _get_attachment(self, attachment_id: str, kind: str) -> tuple[str, bytes] | None:
        conn = self._conn()
        try:
            row = conn.execute(
                "SELECT mime, caminho FROM fotos WHERE id = ? AND kind = ?",
                (attachment_id, kind),
            ).fetchone()
        finally:
            conn.close()
        if row is None:
            return None
        data = self._read_foto(row["caminho"])
        if data is None:
            return None
        return row["mime"], data

    def write_photo(self, foto_id: str, data: bytes) -> None:
        """Atomically write a photo's binary content to the managed directory."""
        path = self._foto(foto_id)
        tmp = f"{path}.tmp"
        with open(tmp, "wb") as fh:
            fh.write(data)
        os.replace(tmp, path)

    def gc_orphan_photos(self) -> int:
        """Delete photo files on disk that have no matching ``fotos`` row.

        Reclaims space from photos removed via client edits and from
        ``PUT /api/photos/{id}`` uploads that were never referenced by a push.
        Returns the number of files removed.

        Only files inside the managed photos directory are considered, and
        temporary ``.tmp`` files are ignored.
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
        removed = 0
        for name in os.listdir(self.fotos_dir):
            if name.endswith(".tmp"):
                continue
            path = os.path.join(self.fotos_dir, name)
            # A subdirectory is not an orphaned photo; removing it would also
            # mean deleting a whole tree, which this pass must never do.
            if not os.path.isfile(path):
                continue
            if path not in stored:
                try:
                    os.remove(path)
                    removed += 1
                except FileNotFoundError:
                    pass
        return removed

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

    def list_clients(self) -> list[dict]:
        conn = self._conn()
        try:
            rows = conn.execute(
                "SELECT id, nome, contrato, latitude, longitude, notas, updated_at, created_at "
                "FROM clientes ORDER BY nome COLLATE NOCASE"
            ).fetchall()
            return [dict(r) for r in rows]
        finally:
            conn.close()

    def get_client_with_photos(self, cliente_id: str) -> dict | None:
        conn = self._conn()
        try:
            return self._canonical_from_conn(conn, cliente_id)
        finally:
            conn.close()

    def _canonical_from_conn(self, conn: sqlite3.Connection, cliente_id: str) -> dict | None:
        """Read one client with its two attachment lists from an open connection.

        Photos and documents are returned as separate lists so the app's gallery
        and pager never see a document. Attachments whose file vanished are
        skipped rather than returned with a hash of nothing.

        Returns None when the client does not exist.
        """
        row = conn.execute(
            "SELECT id, nome, contrato, latitude, longitude, notas, updated_at, created_at, delivery_ts "
            "FROM clientes WHERE id = ?",
            (cliente_id,),
        ).fetchone()
        if row is None:
            return None
        fotos = conn.execute(
            "SELECT id, mime, caminho, ordem FROM fotos "
            "WHERE cliente_id = ? AND kind = 'photo' ORDER BY ordem, id",
            (cliente_id,),
        ).fetchall()
        foto_out = []
        for f in fotos:
            data = self._read_foto(f["caminho"])
            if data is None:
                continue
            foto_out.append(
                {
                    "id": f["id"],
                    "mime": f["mime"],
                    "hash": hashlib.sha256(data).hexdigest(),
                    "ordem": f["ordem"],
                }
            )
        documentos = conn.execute(
            "SELECT id, nome, mime, caminho, ordem FROM fotos "
            "WHERE cliente_id = ? AND kind = 'document' ORDER BY ordem, id",
            (cliente_id,),
        ).fetchall()
        documento_out = []
        for d in documentos:
            data = self._read_foto(d["caminho"])
            if data is None:
                continue
            documento_out.append(
                {
                    "id": d["id"],
                    "nome": d["nome"] or "",
                    "mime": d["mime"],
                    "hash": hashlib.sha256(data).hexdigest(),
                    "ordem": d["ordem"],
                }
            )
        return {
            "client": dict(row),
            "fotos": foto_out,
            "documentos": documento_out,
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
        finally:
            conn.close()
        records = [
            record
            for record in (self.get_client_with_photos(i) for i in cliente_ids)
            # A record deleted between the page query and this read is simply not
            # part of the page; returning None would break the response.
            if record is not None
        ]
        return records, tombstones, next_since, next_since_id

    def delete_client(self, cliente_id: str, deleted_at: int | None = None) -> bool:
        conn = self._conn()
        try:
            # Serialize with pushes and read the delivery clock before the row
            # disappears, so the tombstone is handed out above everything already
            # delivered — a deletion stamped behind a device's watermark would
            # otherwise never reach it.
            conn.execute("BEGIN IMMEDIATE")
            watermark = self._delivery_watermark(conn)
            paths = [
                r["caminho"]
                for r in conn.execute(
                    "SELECT caminho FROM fotos WHERE cliente_id = ?", (cliente_id,)
                ).fetchall()
            ]
            cur = conn.execute("DELETE FROM clientes WHERE id = ?", (cliente_id,))
            if cur.rowcount > 0:
                deleted_at = int(time.time()) if deleted_at is None else deleted_at
                conn.execute(
                    "INSERT OR REPLACE INTO tombstones (id, deleted_at, delivery_ts) "
                    "VALUES (?, ?, ?)",
                    (cliente_id, deleted_at, max(deleted_at, watermark + 1)),
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
                "SELECT id, public_key, comment, provisioned, created_at FROM keys ORDER BY created_at"
            ).fetchall()
            return [dict(r) for r in rows]
        finally:
            conn.close()

    def get_all_public_keys(self) -> list[str]:
        conn = self._conn()
        try:
            rows = conn.execute("SELECT public_key FROM keys").fetchall()
            return [r["public_key"] for r in rows]
        finally:
            conn.close()

    def get_key_by_public_key(self, public_key: str) -> dict | None:
        conn = self._conn()
        try:
            row = conn.execute(
                "SELECT id, public_key, comment, created_at FROM keys WHERE public_key = ?",
                (public_key,),
            ).fetchone()
            return dict(row) if row is not None else None
        finally:
            conn.close()