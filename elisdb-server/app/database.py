"""SQLite persistence layer. Owns schema, duplicate detection and LWW conflict
resolution. The server is the single authority over all data management.

Photo binary content lives in a filesystem directory (self.fotos_dir); the
fotos table stores only the file path (caminho) referencing each photo file.
"""

import hashlib
import os
import sqlite3
import threading
import time

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
    ordem INTEGER NOT NULL DEFAULT 0
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
    ts INTEGER NOT NULL
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


def _migrate_clients_nullable_coords(conn: sqlite3.Connection) -> None:
    """Idempotently make clientes.latitude/longitude nullable.

    SQLite cannot ALTER a column's nullability, so the table is rebuilt.
    Existing NOT NULL data is preserved; new rows may store NULL coordinates.
    """
    columns = [
        dict(row) for row in conn.execute("PRAGMA table_info(clientes)").fetchall()
    ]
    latitude = next((c for c in columns if c["name"] == "latitude"), None)
    if latitude is None or latitude["notnull"] != 1:
        return
    conn.execute("PRAGMA foreign_keys = OFF")
    try:
        conn.executescript(
            """
            CREATE TABLE clientes_new (
                id TEXT PRIMARY KEY,
                nome TEXT NOT NULL,
                contrato TEXT,
                latitude REAL,
                longitude REAL,
                notas TEXT,
                updated_at INTEGER NOT NULL,
                created_at INTEGER NOT NULL DEFAULT 0
            );
            INSERT INTO clientes_new (id, nome, contrato, latitude, longitude, notas, updated_at, created_at)
                SELECT id, nome, contrato, latitude, longitude, notas, updated_at, created_at FROM clientes;
            DROP TABLE clientes;
            ALTER TABLE clientes_new RENAME TO clientes;
            CREATE UNIQUE INDEX IF NOT EXISTS idx_clientes_nome
                ON clientes (nome COLLATE NOCASE);
            """
        )
    finally:
        conn.execute("PRAGMA foreign_keys = ON")


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
    conn.execute(
        "UPDATE client_audit SET ts = ts / 1000 WHERE ts > 100000000000"
    )


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
    _migrate_clients_nullable_coords(conn)
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
        self.fotos_dir = fotos_dir

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
    ) -> tuple[str, str | None, dict | None]:
        """Insert or update a client, applying last-write-wins.

        When `actor` (the authenticated key record: `id`, `comment`) is given and
        the record is accepted, an audit entry is appended to client_audit
        (`create` on insert, `edit` on update).

        Returns (status, existing_id, canonical) where status is:
          - "accepted": the record (and photos) were persisted
          - "duplicate": a different client already holds this normalized name
          - "stale": the incoming updated_at is older than the stored value

        On "duplicate", `canonical` carries the stored canonical record (client
        + photos) so the client can merge without a separate round-trip; it is
        None for any other status.
        """
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
            tombstone = conn.execute(
                "SELECT deleted_at FROM tombstones WHERE id = ?", (cliente.id,)
            ).fetchone()
            if tombstone is not None and cliente.updated_at <= tombstone["deleted_at"]:
                return "stale", None, None
            if tombstone is not None:
                # The pushed record is newer than the deletion: resurrect the
                # client and clear the tombstone so it persists as a new record.
                conn.execute("DELETE FROM tombstones WHERE id = ?", (cliente.id,))

            # Pulls are timestamp-cursored: a device only receives records newer
            # than its watermark. A push can be accepted while carrying an older
            # updated_at (device clock behind, or an edit made days ago and only
            # synced now); storing it as-is would leave it invisible to every
            # device that already pulled past that timestamp. Give accepted
            # writes the next watermark value instead, so they are delivered.
            # The audit entry keeps the original timestamp: that is when the
            # change was actually made.
            watermark = self._delivery_watermark(conn)
            delivery_ts = max(cliente.updated_at, watermark + 1)

            existing = conn.execute(
                "SELECT id, updated_at FROM clientes WHERE id = ?", (cliente.id,)
            ).fetchone()

            if existing is not None:
                if cliente.updated_at < existing["updated_at"]:
                    return "stale", None, None
                other = conn.execute(
                    "SELECT id FROM clientes WHERE nome = ? COLLATE NOCASE AND id != ?",
                    (nome, cliente.id),
                ).fetchone()
                if other is not None:
                    canonical = self._canonical_from_conn(conn, other["id"])
                    return "duplicate", other["id"], canonical
                conn.execute(
                    "UPDATE clientes SET nome=?, contrato=?, latitude=?, longitude=?, notas=?, updated_at=? WHERE id=?",
                    (
                        nome,
                        cliente.contrato,
                        cliente.latitude,
                        cliente.longitude,
                        cliente.notas,
                        delivery_ts,
                        cliente.id,
                    ),
                )
                self._replace_fotos(conn, cliente.id, fotos)
                self._record_audit(conn, cliente.id, actor, "edit", cliente.updated_at)
                conn.commit()
                return "accepted", None, None

            try:
                conn.execute(
                    "INSERT INTO clientes (id, nome, contrato, latitude, longitude, notas, updated_at, created_at) "
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    (
                        cliente.id,
                        nome,
                        cliente.contrato,
                        cliente.latitude,
                        cliente.longitude,
                        cliente.notas,
                        delivery_ts,
                        cliente.updated_at,
                    ),
                )
            except sqlite3.IntegrityError:
                conn.rollback()
                other = conn.execute(
                    "SELECT id FROM clientes WHERE nome = ? COLLATE NOCASE", (nome,)
                ).fetchone()
                if other is None:
                    return "duplicate", None, None
                canonical = self._canonical_from_conn(conn, other["id"])
                return "duplicate", other["id"], canonical
            self._replace_fotos(conn, cliente.id, fotos)
            self._record_audit(conn, cliente.id, actor, "create", cliente.updated_at)
            conn.commit()
            return "accepted", None, None
        finally:
            conn.close()

    def _record_audit(
        self,
        conn: sqlite3.Connection,
        cliente_id: str,
        actor: dict | None,
        action: str,
        ts: int,
    ) -> None:
        """Append an audit entry for an accepted client persistence."""
        if actor is None:
            return
        conn.execute(
            "INSERT INTO client_audit (cliente_id, key_id, username, action, ts) "
            "VALUES (?, ?, ?, ?, ?)",
            (cliente_id, actor.get("id", ""), actor.get("comment") or None, action, ts),
        )

    def _replace_fotos(self, conn: sqlite3.Connection, cliente_id: str, fotos: list[FotoIn]):
        """Merge a client's photos into the store.

        Photos are referenced by id and their binary content is uploaded to the
        photo endpoint separately; only rows pointing at an existing file are
        recorded. Photos owned by another client are rejected rather than
        reassigned. Stored photos the push no longer lists are removed as before.
        """
        incoming = sorted(fotos, key=lambda f: f.ordem)
        kept_ids: list[str] = []
        for foto in incoming:
            owner = conn.execute(
                "SELECT cliente_id FROM fotos WHERE id = ?", (foto.id,)
            ).fetchone()
            if owner is not None and owner["cliente_id"] != cliente_id:
                raise ValueError(
                    f"photo id {foto.id} already belongs to another client"
                )
            if os.path.exists(self._foto(foto.id)):
                kept_ids.append(foto.id)

        # The client's canonical photo set is exactly what the push lists.
        # Remove rows no longer present, deleting their files from disk too so
        # storage does not grow from orphaned photo files.
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
        for row in remove_rows:
            self._remove_file(row["caminho"])
        if kept_ids:
            conn.execute(
                "DELETE FROM fotos WHERE cliente_id = ? AND id NOT IN (%s)"
                % ",".join("?" * len(kept_ids)),
                [cliente_id, *kept_ids],
            )
        else:
            conn.execute("DELETE FROM fotos WHERE cliente_id = ?", (cliente_id,))

        for foto in incoming:
            if foto.id not in kept_ids:
                continue
            conn.execute(
                "INSERT INTO fotos (id, cliente_id, mime, caminho, ordem) "
                "VALUES (?, ?, ?, ?, ?) "
                "ON CONFLICT(id) DO UPDATE SET "
                "cliente_id=excluded.cliente_id, mime=excluded.mime, caminho=excluded.caminho, ordem=excluded.ordem",
                (foto.id, cliente_id, foto.mime, self._foto(foto.id), foto.ordem),
            )

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
            stored = {row["caminho"] for row in conn.execute("SELECT caminho FROM fotos").fetchall()}
        finally:
            conn.close()
        removed = 0
        for name in os.listdir(self.fotos_dir):
            if name.endswith(".tmp"):
                continue
            path = os.path.join(self.fotos_dir, name)
            if path not in stored:
                try:
                    os.remove(path)
                    removed += 1
                except FileNotFoundError:
                    pass
        return removed

    @staticmethod
    def _delivery_watermark(conn: sqlite3.Connection) -> int:
        """Newest timestamp a device may already hold (clients + tombstones)."""
        max_clients = conn.execute(
            "SELECT COALESCE(MAX(updated_at), 0) FROM clientes"
        ).fetchone()[0]
        max_tombstones = conn.execute(
            "SELECT COALESCE(MAX(deleted_at), 0) FROM tombstones"
        ).fetchone()[0]
        return max(max_clients, max_tombstones)

    def latest_updated_at(self) -> int:
        """Largest `updated_at` over stored clients and `deleted_at` over tombstones (0 when empty)."""
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
                "SELECT action, username, key_id, ts FROM client_audit "
                "WHERE cliente_id = ? ORDER BY ts, id",
                (cliente_id,),
            ).fetchall()
            return [dict(r) for r in rows]
        finally:
            conn.close()

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
        """Read one client with its photos from an open connection.

        Returns None when the client does not exist.
        """
        row = conn.execute(
            "SELECT id, nome, contrato, latitude, longitude, notas, updated_at, created_at "
            "FROM clientes WHERE id = ?",
            (cliente_id,),
        ).fetchone()
        if row is None:
            return None
        fotos = conn.execute(
            "SELECT id, mime, caminho, ordem FROM fotos WHERE cliente_id = ? ORDER BY ordem",
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
        return {
            "client": dict(row),
            "fotos": foto_out,
        }

    def pull_all(
        self,
        since: int | None = None,
        limit: int | None = None,
        since_id: str | None = None,
    ) -> tuple[list[dict], list[dict], int | None, str | None]:
        """Returns (records, tombstones, next_since, next_since_id).

        Filters records to `updated_at > since` when `since` is given. When
        `since_id` is also given, the filter is a keyset over `(updated_at, id)`:
        `updated_at > since OR (updated_at = since AND id > since_id)`, so records
        sharing the same `updated_at` are never skipped at a page boundary.

        With `limit`, at most `limit` records are returned per call and the cursor
        (`next_since`, `next_since_id`) is the `(updated_at, id)` of the last
        record of the returned page, to be used as `(since, since_id)` for the
        next call (a next page exists only when `next_since` is not None).
        Tombstones are filtered to `deleted_at > since` when `since` is given so
        incremental pulls carry only new deletions.
        """
        conn = self._conn()
        try:
            sql = "SELECT id FROM clientes"
            params: list = []
            if since is not None:
                if since_id is not None:
                    sql += " WHERE updated_at > ? OR (updated_at = ? AND id > ?)"
                    params += [since, since, since_id]
                else:
                    sql += " WHERE updated_at > ?"
                    params.append(since)
            # Paginated pulls order by updated_at so the cursor is a monotonic
            # keyset; unrestricted pulls keep the historical name ordering.
            if limit is not None:
                sql += " ORDER BY updated_at, id LIMIT ?"
                params.append(limit + 1)
            else:
                sql += " ORDER BY nome COLLATE NOCASE"
            rows = conn.execute(sql, params).fetchall()
            fetched = [r["id"] for r in rows]
            has_more = limit is not None and len(fetched) > limit
            cliente_ids = fetched[:limit] if has_more else fetched
            next_since = None
            next_since_id = None
            if has_more and cliente_ids:
                last_id = cliente_ids[-1]
                last = conn.execute(
                    "SELECT updated_at FROM clientes WHERE id = ?",
                    (last_id,),
                ).fetchone()
                if last is not None:
                    next_since = last["updated_at"]
                    next_since_id = last_id

            if since is not None:
                tombstones = [
                    dict(r)
                    for r in conn.execute(
                        "SELECT id, deleted_at FROM tombstones WHERE deleted_at > ? ORDER BY deleted_at",
                        (since,),
                    ).fetchall()
                ]
            else:
                tombstones = [
                    dict(r)
                    for r in conn.execute(
                        "SELECT id, deleted_at FROM tombstones ORDER BY deleted_at"
                    ).fetchall()
                ]
        finally:
            conn.close()
        return (
            [self.get_client_with_photos(i) for i in cliente_ids],
            tombstones,
            next_since,
            next_since_id,
        )

    def delete_client(self, cliente_id: str, deleted_at: int | None = None) -> bool:
        conn = self._conn()
        try:
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
                    "INSERT OR REPLACE INTO tombstones (id, deleted_at) VALUES (?, ?)",
                    (cliente_id, deleted_at),
                )
            conn.commit()
            if cur.rowcount > 0:
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