"""Unit tests for the SQLite persistence layer (LWW, duplicates, tombstones,
migrations, photo hygiene and the timestamp-unit normalization)."""

import hashlib
import os
import sqlite3

import pytest

from elisdb.database import (
    Database,
    PhotoQuotaExceeded,
    StaleDeleteError,
    _apply_schema,
    _migrate_fotos_upload_metadata,
    connect,
)
from elisdb.models import ClienteIn, FotoIn, normalize_nome


def _cliente(**kwargs) -> ClienteIn:
    base = dict(id="1", nome="Ana", updated_at=1000)
    base.update(kwargs)
    return ClienteIn(**base)


def _db(tmp_path) -> Database:
    db = Database(str(tmp_path / "elisdb.db"), fotos_dir=str(tmp_path / "photos"))
    db.init()
    return db


def test_insert_then_lww(tmp_path):
    db = _db(tmp_path)
    assert db.save_client(_cliente(updated_at=1000, nome="Ana"), [])[0] == "accepted"

    # Older timestamp loses (stale), newer wins.
    assert db.save_client(_cliente(updated_at=900, nome="Old"), [])[0] == "stale"
    assert db.save_client(_cliente(updated_at=1100, nome="New"), [])[0] == "accepted"

    stored = db.get_client_with_photos("1")
    assert stored["client"]["nome"] == "NEW"


def test_blank_name_rejected(tmp_path):
    db = _db(tmp_path)
    with pytest.raises(ValueError):
        db.save_client(_cliente(nome="   "), [])


def test_future_updated_at_rejected(tmp_path):
    db = _db(tmp_path)
    far_future = 9_999_999_999_999
    with pytest.raises(ValueError):
        db.save_client(_cliente(updated_at=far_future), [])


def test_duplicate_name_returns_canonical(tmp_path):
    db = _db(tmp_path)
    db.save_client(_cliente(id="1", nome="Ana", updated_at=1000), [])
    status, existing_id, canonical, missing = db.save_client(
        _cliente(id="2", nome="  ana ", updated_at=1000), []
    )
    assert missing == []
    assert status == "duplicate"
    assert existing_id == "1"
    assert canonical["client"]["id"] == "1"


def test_tombstone_blocks_older_and_allows_newer(tmp_path):
    db = _db(tmp_path)
    db.save_client(_cliente(id="1", updated_at=1000), [])
    assert db.delete_client("1", deleted_at=2000) is True
    assert db.delete_client("1", deleted_at=2000) is False

    # Older than the tombstone -> stale (stays deleted).
    assert db.save_client(_cliente(id="1", updated_at=1500), [])[0] == "stale"
    # Newer than the tombstone -> resurrected.
    assert db.save_client(_cliente(id="1", updated_at=2500), [])[0] == "accepted"
    assert db.get_client_with_photos("1") is not None


def test_pull_pagination_keyset(tmp_path):
    db = _db(tmp_path)
    # Three records sharing one updated_at, plus a newer one.
    for i in ("a", "b", "c"):
        db.save_client(_cliente(id=i, nome=i.upper(), updated_at=100), [])
    db.save_client(_cliente(id="d", nome="D", updated_at=200), [])
    # Accepted writes get increasing delivery timestamps, so rewind both clocks
    # here to exercise the keyset over records sharing one value.
    conn = connect(db.db_path)
    try:
        conn.execute(
            "UPDATE clientes SET updated_at = 100, delivery_ts = 100 "
            "WHERE id IN ('a', 'b', 'c')"
        )
        conn.execute("UPDATE clientes SET updated_at = 200, delivery_ts = 200 WHERE id = 'd'")
        conn.commit()
    finally:
        conn.close()

    page1 = db.pull_all(limit=2)
    ids1 = [r["client"]["id"] for r in page1[0]]
    assert ids1 == ["a", "b"]
    assert page1[2] == 100 and page1[3] == "b"

    page2 = db.pull_all(since=page1[2], since_id=page1[3], limit=2)
    ids2 = [r["client"]["id"] for r in page2[0]]
    assert ids2 == ["c", "d"]
    assert page2[2] is None  # last page


def test_gc_orphan_photos_removes_unreferenced(tmp_path):
    db = _db(tmp_path)
    db.write_photo("referenced", b"data")
    db.save_client(_cliente(id="1"), [FotoIn(id="referenced", mime="image/jpeg")])
    db.write_photo("orphan", b"data")

    removed = db.gc_orphan_photos()
    assert removed == 1
    assert db.get_photo("referenced") is not None
    assert db.get_photo("orphan") is None


def test_gc_orphan_photos_matches_container_paths_by_name(tmp_path):
    db = _db(tmp_path)
    db.write_photo("referenced", b"data")
    db.save_client(_cliente(id="1"), [FotoIn(id="referenced", mime="image/jpeg")])

    # A photo written inside the container and read by a local process: the
    # stored path is absolute to /app/data, the file sits in the host dir.
    conn = db._conn()
    conn.execute(
        "UPDATE fotos SET caminho = ? WHERE id = ?",
        ("/app/data/photos/referenced", "referenced"),
    )
    conn.commit()
    conn.close()

    assert db.gc_orphan_photos() == 0
    assert os.path.exists(os.path.join(db.fotos_dir, "referenced"))


def test_gc_orphan_photos_never_wipes_a_foreign_layout(tmp_path):
    db = _db(tmp_path)
    db.write_photo("orphan", b"data")
    db.save_client(_cliente(id="1"), [FotoIn(id="elsewhere", mime="image/jpeg")])

    # No stored name matches anything on disk: the mounted layout is foreign,
    # so the sweep must keep every file.
    assert db.gc_orphan_photos() == 0
    assert os.path.exists(os.path.join(db.fotos_dir, "orphan"))


def test_photo_id_path_traversal_rejected(tmp_path):
    db = _db(tmp_path)
    with pytest.raises(ValueError):
        db.write_photo("../escape", b"x")


def test_normalize_nome_trims_and_uppercases():
    assert normalize_nome("  ana   maria ") == "ANA MARIA"


def test_migration_converts_milliseconds_and_is_idempotent(tmp_path):
    db_path = str(tmp_path / "legacy.db")
    db = Database(db_path, fotos_dir=str(tmp_path / "photos"))
    db.init()  # creates the full schema (empty)

    # Simulate a legacy database holding millisecond timestamps.
    conn = connect(db_path)
    conn.execute(
        "INSERT INTO clientes (id, nome, updated_at, created_at) VALUES ('1','A', 1758000000000, 1758000000000)"
    )
    conn.execute(
        "INSERT INTO client_audit (cliente_id, key_id, action, ts) VALUES ('1','k','create', 1758000000000)"
    )
    conn.commit()

    _apply_schema(conn)  # runs the ms->s migration
    assert tuple(
        conn.execute("SELECT updated_at, created_at FROM clientes WHERE id='1'").fetchone()
    ) == (1758000000, 1758000000)
    assert conn.execute("SELECT ts FROM client_audit").fetchone()[0] == 1758000000

    _apply_schema(conn)  # idempotent: second run must not divide again
    assert conn.execute("SELECT updated_at FROM clientes WHERE id='1'").fetchone()[0] == 1758000000
    conn.close()


def test_old_timestamp_is_delivered_without_rewriting_updated_at(tmp_path):
    db = _db(tmp_path)
    assert db.save_client(_cliente(id="a", updated_at=2000), [])[0] == "accepted"

    # An edit made with a lagging clock (or synced days later) arrives after a
    # newer record exists. It still has to reach devices that already pulled
    # past 1500 — without pretending the edit happened at 2001, which would let
    # a server stamp win a conflict against a newer device edit.
    assert db.save_client(_cliente(id="b", nome="Bea", updated_at=1500), [])[0] == "accepted"

    stored = db.get_client_with_photos("b")["client"]
    assert stored["updated_at"] == 1500
    assert stored["delivery_ts"] == 2001
    records, _tombstones, _since, _since_id = db.pull_all(2000)
    assert [record["client"]["id"] for record in records] == ["b"]


def test_equal_timestamp_gets_the_next_delivery_value(tmp_path):
    db = _db(tmp_path)
    db.save_client(_cliente(id="a", updated_at=2000), [])
    db.save_client(_cliente(id="b", nome="Bea", updated_at=2000), [])

    stored = db.get_client_with_photos("b")["client"]
    assert stored["updated_at"] == 2000
    assert stored["delivery_ts"] == 2001


def test_audit_entry_keeps_the_original_edit_time(tmp_path):
    db = _db(tmp_path)
    actor = {"id": "key-1", "comment": "Joao"}
    db.save_client(_cliente(id="a", updated_at=2000), [], actor)
    db.save_client(_cliente(id="b", nome="Bea", updated_at=1500), [], actor)

    history = db.client_history("b")
    assert history[-1]["action"] == "create"
    assert history[-1]["ts"] == 1500


def test_edit_details_describe_the_changed_fields(tmp_path):
    db = _db(tmp_path)
    actor = {"id": "key-1", "comment": "Joao"}
    db.save_client(_cliente(id="a", contrato="C1", updated_at=1000), [], actor)
    db.save_client(
        _cliente(id="a", nome="Ana Silva", contrato="", notas="Entregar", updated_at=1001),
        [],
        actor,
    )

    details = db.client_history("a")[-1]["details"]
    fields = {field["field"]: field for field in details["fields"]}
    assert fields["nome"] == {
        "field": "nome",
        "old": "ANA",
        "new": "ANA SILVA",
        "truncated": False,
    }
    assert fields["contrato"]["old"] == "C1"
    assert fields["contrato"]["new"] is None
    assert fields["notas"]["old"] is None
    assert fields["notas"]["new"] == "Entregar"
    assert "photos" not in details


def test_tag_change_appears_in_the_edit_details(tmp_path):
    db = _db(tmp_path)
    actor = {"id": "key-1", "comment": "Joao"}
    db.save_client(_cliente(id="a", tag="Premium", updated_at=1000), [], actor)
    db.save_client(_cliente(id="a", tag="", updated_at=1001), [], actor)

    fields = {field["field"]: field for field in db.client_history("a")[-1]["details"]["fields"]}
    assert fields["tag"]["old"] == "PREMIUM"
    assert fields["tag"]["new"] is None


def test_multiple_tags_are_normalized_in_the_edit_details(tmp_path):
    db = _db(tmp_path)
    actor = {"id": "key-1", "comment": "Joao"}
    db.save_client(_cliente(id="a", tag="gold;premium", updated_at=1000), [], actor)
    db.save_client(_cliente(id="a", tag="premium", updated_at=1001), [], actor)

    fields = {field["field"]: field for field in db.client_history("a")[-1]["details"]["fields"]}
    assert fields["tag"]["old"] == "GOLD;PREMIUM"
    assert fields["tag"]["new"] == "PREMIUM"


def test_tag_survives_the_coordinate_rebuild(tmp_path):
    db_path = str(tmp_path / "legacy-tag.db")
    conn = connect(db_path)
    # A legacy table without the tag and with NOT NULL coordinates forces the
    # rebuild; the tag column must be added first and copied through it.
    conn.executescript(
        """
        CREATE TABLE clientes_old (
            id TEXT PRIMARY KEY,
            nome TEXT NOT NULL,
            contrato TEXT,
            latitude REAL NOT NULL,
            longitude REAL NOT NULL,
            notas TEXT,
            updated_at INTEGER NOT NULL
        );
        DROP TABLE clientes;
        ALTER TABLE clientes_old RENAME TO clientes;
        INSERT INTO clientes (id, nome, latitude, longitude, updated_at)
            VALUES ('1', 'ANA', 1.0, 2.0, 1000);
        """
    )
    conn.commit()

    _apply_schema(conn)
    columns = [row["name"] for row in conn.execute("PRAGMA table_info(clientes)").fetchall()]
    assert "tag" in columns
    assert conn.execute("SELECT tag FROM clientes WHERE id='1'").fetchone()[0] is None

    conn.execute("UPDATE clientes SET tag = 'Premium' WHERE id='1'")
    conn.commit()
    _apply_schema(conn)  # idempotent: the value is not reset
    assert conn.execute("SELECT tag FROM clientes WHERE id='1'").fetchone()[0] == "Premium"
    conn.close()


def test_location_change_is_a_single_field(tmp_path):
    db = _db(tmp_path)
    actor = {"id": "key-1", "comment": "Joao"}
    db.save_client(_cliente(id="a", latitude=1.0, longitude=2.0, updated_at=1000), [], actor)

    # Sub-millimetre float noise is not a change, so no entry is recorded.
    db.save_client(_cliente(id="a", latitude=1.0000001, longitude=2.0, updated_at=1001), [], actor)
    assert len(db.client_history("a")) == 1

    db.save_client(_cliente(id="a", latitude=1.5, longitude=-2.25, updated_at=1002), [], actor)
    details = db.client_history("a")[-1]["details"]
    assert details["fields"] == [
        {
            "field": "localizacao",
            "old": "1.000000, 2.000000",
            "new": "1.500000, -2.250000",
            "truncated": False,
        }
    ]


def test_photo_changes_are_recorded(tmp_path):
    db = _db(tmp_path)
    actor = {"id": "key-1", "comment": "Joao"}
    db.write_photo("p1", b"1")
    db.write_photo("p2", b"2")

    db.save_client(
        _cliente(id="a", updated_at=1000), [FotoIn(id="p1", mime="image/jpeg")], actor
    )
    assert db.client_history("a")[-1]["details"] == {
        "fields": [],
        "photos": {"added": ["p1"], "removed": [], "reordered": False},
    }

    db.save_client(
        _cliente(id="a", updated_at=1001),
        [
            FotoIn(id="p1", mime="image/jpeg"),
            FotoIn(id="p2", mime="image/jpeg", ordem=1),
        ],
        actor,
    )
    assert db.client_history("a")[-1]["details"]["photos"] == {
        "added": ["p2"],
        "removed": [],
        "reordered": False,
    }

    db.save_client(
        _cliente(id="a", updated_at=1002), [FotoIn(id="p2", mime="image/jpeg")], actor
    )
    assert db.client_history("a")[-1]["details"]["photos"] == {
        "added": [],
        "removed": ["p1"],
        "reordered": False,
    }


def test_reordering_photos_is_recorded(tmp_path):
    db = _db(tmp_path)
    actor = {"id": "key-1", "comment": "Joao"}
    db.write_photo("p1", b"1")
    db.write_photo("p2", b"2")
    db.save_client(
        _cliente(id="a", updated_at=1000),
        [
            FotoIn(id="p1", mime="image/jpeg"),
            FotoIn(id="p2", mime="image/jpeg", ordem=1),
        ],
        actor,
    )
    db.save_client(
        _cliente(id="a", updated_at=1001),
        [
            FotoIn(id="p2", mime="image/jpeg"),
            FotoIn(id="p1", mime="image/jpeg", ordem=1),
        ],
        actor,
    )

    assert db.client_history("a")[-1]["details"]["photos"] == {
        "added": [],
        "removed": [],
        "reordered": True,
    }


def test_unchanged_push_records_no_entry(tmp_path):
    db = _db(tmp_path)
    actor = {"id": "key-1", "comment": "Joao"}
    db.save_client(_cliente(id="a", updated_at=1000), [], actor)

    status, _existing, _canonical, _missing = db.save_client(
        _cliente(id="a", nome="  ana ", updated_at=1001), [], actor
    )

    assert status == "accepted"
    assert len(db.client_history("a")) == 1
    # The record is still re-delivered under a newer timestamp.
    assert db.get_client_with_photos("a")["client"]["updated_at"] == 1001


def test_long_text_is_truncated_in_the_details(tmp_path):
    db = _db(tmp_path)
    actor = {"id": "key-1", "comment": "Joao"}
    db.save_client(_cliente(id="a", updated_at=1000), [], actor)
    db.save_client(_cliente(id="a", notas="x" * 250, updated_at=1001), [], actor)

    change = db.client_history("a")[-1]["details"]["fields"][0]
    assert change["field"] == "notas"
    assert change["new"] == "x" * 200
    assert change["truncated"] is True


def test_history_entries_written_before_details_stay_compatible(tmp_path):
    db = _db(tmp_path)
    conn = connect(str(tmp_path / "elisdb.db"))
    conn.execute(
        "INSERT INTO client_audit (cliente_id, key_id, action, ts) "
        "VALUES ('a', 'k', 'create', 1000)"
    )
    conn.commit()
    conn.close()

    assert db.client_history("a")[0]["details"] is None


def test_legacy_client_without_status_becomes_active(tmp_path):
    db_path = str(tmp_path / "legacy-status.db")
    conn = connect(db_path)
    conn.execute("ALTER TABLE clientes DROP COLUMN ativo")
    conn.execute("INSERT INTO clientes (id, nome, updated_at) VALUES ('1', 'ANA', 1000)")
    conn.commit()

    _apply_schema(conn)
    assert conn.execute("SELECT ativo FROM clientes WHERE id='1'").fetchone()[0] == 1

    _apply_schema(conn)  # idempotent: the column is not re-added or reset
    conn.execute("UPDATE clientes SET ativo = 0 WHERE id='1'")
    conn.commit()
    _apply_schema(conn)
    assert conn.execute("SELECT ativo FROM clientes WHERE id='1'").fetchone()[0] == 0
    conn.close()


def test_status_survives_the_coordinate_rebuild(tmp_path):
    db_path = str(tmp_path / "legacy-coords.db")
    conn = connect(db_path)
    conn.execute("ALTER TABLE clientes DROP COLUMN ativo")
    # A legacy table that still declares the coordinates as NOT NULL forces the
    # rebuild migration; the status must be preserved across it.
    conn.executescript(
        """
        CREATE TABLE clientes_old (
            id TEXT PRIMARY KEY,
            nome TEXT NOT NULL,
            contrato TEXT,
            latitude REAL NOT NULL,
            longitude REAL NOT NULL,
            notas TEXT,
            updated_at INTEGER NOT NULL
        );
        DROP TABLE clientes;
        ALTER TABLE clientes_old RENAME TO clientes;
        CREATE UNIQUE INDEX idx_clientes_nome ON clientes (nome COLLATE NOCASE);
        INSERT INTO clientes (id, nome, latitude, longitude, updated_at)
            VALUES ('1', 'ANA', 1.5, 2.5, 1000);
        """
    )
    conn.commit()

    _apply_schema(conn)
    row = conn.execute(
        "SELECT latitude, longitude, ativo FROM clientes WHERE id='1'"
    ).fetchone()
    assert tuple(row) == (1.5, 2.5, 1)
    conn.close()


def test_pull_reports_missing_photo_file_as_unavailable(tmp_path):
    db = _db(tmp_path)
    db.write_photo("p1", b"1")
    db.save_client(
        _cliente(id="a", updated_at=1000),
        [FotoIn(id="p1", mime="image/jpeg")],
    )
    os.remove(db._foto("p1"))

    stored = db.get_client_with_photos("a")

    assert stored["photos_complete"] is True
    assert stored["fotos"] == [
        {
            "id": "p1",
            "mime": "image/jpeg",
            "ordem": 0,
            "hash": None,
            "available": False,
        }
    ]


def test_push_with_missing_photo_file_keeps_placeholder_row(tmp_path):
    db = _db(tmp_path)

    status, _, _, missing = db.save_client(
        _cliente(id="a"), [FotoIn(id="p1", mime="image/jpeg")]
    )

    assert status == "accepted"
    assert missing == ["p1"]
    conn = connect(db.db_path)
    assert conn.execute("SELECT id FROM fotos WHERE id = 'p1'").fetchone() is not None
    conn.close()

    # A later push still reports the missing bytes but keeps the row alive.
    status, _, _, missing = db.save_client(
        _cliente(id="a", updated_at=1001), [FotoIn(id="p1", mime="image/jpeg")]
    )
    assert status == "accepted"
    assert missing == ["p1"]
    assert db.get_client_with_photos("a")["fotos"][0]["available"] is False


def test_removed_photo_file_survives_a_failed_commit(tmp_path, monkeypatch):
    db = _db(tmp_path)
    db.write_photo("p1", b"1")
    db.save_client(
        _cliente(id="a", updated_at=1000),
        [FotoIn(id="p1", mime="image/jpeg")],
    )
    path = db._foto("p1")

    def boom(*_args, **_kwargs):
        raise RuntimeError("audit failed")

    monkeypatch.setattr(db, "_record_audit", boom)
    with pytest.raises(RuntimeError):
        db.save_client(
            _cliente(id="a", updated_at=1001), [], {"id": "k", "comment": "u"}
        )

    assert os.path.exists(path)
    assert [f["id"] for f in db.get_client_with_photos("a")["fotos"]] == ["p1"]


def test_removed_photo_file_is_deleted_after_commit(tmp_path):
    db = _db(tmp_path)
    db.write_photo("p1", b"1")
    db.save_client(
        _cliente(id="a", updated_at=1000),
        [FotoIn(id="p1", mime="image/jpeg")],
    )
    path = db._foto("p1")

    db.save_client(_cliente(id="a", updated_at=1001), [])

    assert not os.path.exists(path)
    assert db.get_client_with_photos("a")["fotos"] == []


def test_photo_hash_is_cached_and_used_without_reading_the_file(tmp_path, monkeypatch):
    db = _db(tmp_path)
    payload = b"\xff\xd8\xff\xe0hash-me"
    db.write_photo("p1", payload)
    db.save_client(
        _cliente(id="a", updated_at=1000),
        [FotoIn(id="p1", mime="image/jpeg")],
    )

    conn = connect(db.db_path)
    row = conn.execute("SELECT hash FROM fotos WHERE id = 'p1'").fetchone()
    conn.close()
    assert row["hash"] == hashlib.sha256(payload).hexdigest()

    def fail_read(_path):
        raise AssertionError("pull must use the cached hash")

    monkeypatch.setattr(db, "_read_foto", fail_read)
    stored = db.get_client_with_photos("a")
    assert stored["fotos"][0]["hash"] == hashlib.sha256(payload).hexdigest()


def test_upload_backfills_hash_for_an_existing_placeholder(tmp_path):
    db = _db(tmp_path)
    payload = b"\xff\xd8\xff\xe0late-upload"
    db.save_client(
        _cliente(id="a", updated_at=1000),
        [FotoIn(id="p1", mime="image/jpeg")],
    )
    db.write_photo("p1", payload)

    conn = connect(db.db_path)
    row = conn.execute("SELECT hash FROM fotos WHERE id = 'p1'").fetchone()
    conn.close()
    assert row["hash"] == hashlib.sha256(payload).hexdigest()
    assert db.get_client_with_photos("a")["fotos"][0]["available"] is True


def test_retention_is_disabled_by_default(tmp_path):
    db = _db(tmp_path)
    actor = {"id": "key-1", "comment": "Joao"}
    db.save_client(_cliente(id="a", updated_at=1000), [], actor)
    db.delete_client("a", deleted_at=1001)

    # Both windows default to 0: history must survive an unconfigured server.
    assert db.prune_old_records() == (0, 0)
    conn = connect(db.db_path)
    assert conn.execute("SELECT COUNT(*) FROM tombstones").fetchone()[0] == 1
    assert conn.execute("SELECT COUNT(*) FROM client_audit").fetchone()[0] == 1
    conn.close()


def test_idempotency_retention_prunes_old_rows(tmp_path):
    db = _db(tmp_path)
    now = 1_000_000_000
    conn = connect(db.db_path)
    conn.execute(
        "INSERT INTO push_idempotency (key, payload_hash, response_json, created_at) "
        "VALUES (?, ?, ?, ?)",
        ("old", "hash", "{}", now - 31 * 86400),
    )
    conn.execute(
        "INSERT INTO push_idempotency (key, payload_hash, response_json, created_at) "
        "VALUES (?, ?, ?, ?)",
        ("recent", "hash", "{}", now - 29 * 86400),
    )
    conn.commit()
    conn.close()

    assert db.prune_idempotency(30, now=now) == 1
    assert db.prune_idempotency(30, now=now) == 0
    # A zero window disables pruning, like the other retention settings.
    assert db.prune_idempotency(0, now=now) == 0
    conn = connect(db.db_path)
    remaining = conn.execute("SELECT key FROM push_idempotency").fetchall()
    conn.close()
    assert [row[0] for row in remaining] == ["recent"]


def test_retention_prunes_only_rows_older_than_the_window(tmp_path):
    db = _db(tmp_path)
    actor = {"id": "key-1", "comment": "Joao"}
    now = 1_000_000_000
    day = 86400

    # Ancient client + audit entry + tombstone.
    db.save_client(_cliente(id="old", updated_at=1000), [], actor)
    db.delete_client("old", deleted_at=1001)
    # Recent client + audit entry + tombstone (one day old).
    db.save_client(
        _cliente(id="recent", nome="Recent", updated_at=now - day), [], actor
    )
    db.delete_client("recent", deleted_at=now - day + 1)

    assert db.prune_old_records(30, 30, now=now) == (1, 1)

    conn = connect(db.db_path)
    assert [r["id"] for r in conn.execute("SELECT id FROM tombstones")] == ["recent"]
    assert conn.execute("SELECT COUNT(*) FROM client_audit").fetchone()[0] == 1
    conn.close()

    # Idempotent: nothing else is old enough.
    assert db.prune_old_records(30, 30, now=now) == (0, 0)


def test_tombstone_retention_uses_the_server_delivery_clock(tmp_path):
    db = _db(tmp_path)
    now = 1_000_000_000
    db.save_client(_cliente(id="a", updated_at=1000), [])
    # A newer record pushes the delivery watermark forward; the deletion of "a"
    # carries a lagging deleted_at but a recent delivery_ts.
    db.save_client(_cliente(id="b", nome="B", updated_at=now - 100), [])
    db.delete_client("a", deleted_at=1001)

    # Using deleted_at would expire it immediately; delivery_ts keeps it.
    assert db.prune_old_records(tombstone_retention_days=30, now=now) == (0, 0)


def test_write_photo_uses_a_unique_temporary_file(tmp_path, monkeypatch):
    db = _db(tmp_path)
    path = db._foto("p1")
    payload = b"\xff\xd8\xff\xe0unique-temp"
    seen = {}
    real_replace = os.replace

    def checking_replace(src, dst):
        seen["src"] = src
        assert os.path.dirname(src) == os.path.dirname(path)
        assert src.endswith(".tmp")
        assert src != f"{path}.tmp"
        assert os.path.exists(src)
        real_replace(src, dst)

    monkeypatch.setattr(os, "replace", checking_replace)
    db.write_photo("p1", payload)

    assert seen["src"] != f"{path}.tmp"
    with open(path, "rb") as fh:
        assert fh.read() == payload
    assert not [name for name in os.listdir(db.fotos_dir) if name.endswith(".tmp")]


def test_write_photo_removes_the_temporary_file_when_replace_fails(tmp_path, monkeypatch):
    db = _db(tmp_path)

    def boom(_src, _dst):
        raise OSError("replace failed")

    monkeypatch.setattr(os, "replace", boom)
    with pytest.raises(OSError):
        db.write_photo("p1", b"\xff\xd8\xff\xe0temp")

    assert not [name for name in os.listdir(db.fotos_dir) if name.endswith(".tmp")]


def test_fotos_upload_metadata_migration_backfills_existing_files(tmp_path):
    photos = tmp_path / "legacy-photos"
    photos.mkdir()
    payload = b"\xff\xd8\xff\xe0legacy"
    path = photos / "p1.jpg"
    path.write_bytes(payload)
    db_path = tmp_path / "legacy.db"
    conn = sqlite3.connect(db_path)
    conn.row_factory = sqlite3.Row
    conn.execute(
        "CREATE TABLE fotos ("
        "id TEXT PRIMARY KEY, cliente_id TEXT, mime TEXT, caminho TEXT, "
        "ordem INTEGER, hash TEXT)"
    )
    conn.execute(
        "INSERT INTO fotos (id, cliente_id, mime, caminho, ordem, hash) "
        "VALUES ('p1', '1', 'image/jpeg', ?, 0, NULL)",
        (str(path),),
    )
    conn.commit()

    _migrate_fotos_upload_metadata(conn)
    columns = {row[1] for row in conn.execute("PRAGMA table_info(fotos)")}
    assert {"uploaded_by_key", "size_bytes"} <= columns
    row = conn.execute(
        "SELECT uploaded_by_key, size_bytes FROM fotos WHERE id = 'p1'"
    ).fetchone()
    assert tuple(row) == (None, len(payload))
    conn.close()


def test_photo_quota_rejects_before_writing_and_keeps_the_old_file(tmp_path):
    db = _db(tmp_path)
    db.save_client(
        _cliente(id="1"),
        [
            FotoIn(id="p1", mime="image/jpeg"),
            FotoIn(id="p2", mime="image/jpeg"),
        ],
    )
    db.write_photo("p1", b"12345", uploaded_by_key="k1", max_total_bytes=10)

    with pytest.raises(PhotoQuotaExceeded):
        db.write_photo("p2", b"123456", uploaded_by_key="k1", max_total_bytes=10)

    assert os.path.exists(db._foto("p1"))
    assert not os.path.exists(db._foto("p2"))


def test_photo_quota_is_tracked_per_uploading_key(tmp_path):
    db = _db(tmp_path)
    db.save_client(
        _cliente(id="1"),
        [
            FotoIn(id="p1", mime="image/jpeg"),
            FotoIn(id="p2", mime="image/jpeg"),
        ],
    )
    db.write_photo("p1", b"12345", uploaded_by_key="k1", max_key_bytes=5)

    with pytest.raises(PhotoQuotaExceeded):
        db.write_photo("p2", b"1", uploaded_by_key="k1", max_key_bytes=5)

    db.write_photo("p2", b"1", uploaded_by_key="k2", max_key_bytes=5)
    assert os.path.exists(db._foto("p2"))


def test_photo_quota_replace_subtracts_the_old_size(tmp_path):
    db = _db(tmp_path)
    db.save_client(_cliente(id="1"), [FotoIn(id="p1", mime="image/jpeg")])
    db.write_photo("p1", b"12345", uploaded_by_key="k1", max_total_bytes=5)

    db.write_photo("p1", b"1234", uploaded_by_key="k1", max_total_bytes=5)

    with open(db._foto("p1"), "rb") as fh:
        assert fh.read() == b"1234"


def test_delete_older_than_the_stored_edit_is_rejected_and_keeps_the_row(tmp_path):
    db = _db(tmp_path)
    db.save_client(_cliente(id="1", updated_at=2000), [])

    with pytest.raises(StaleDeleteError):
        db.delete_client("1", deleted_at=500)

    assert db.get_client_with_photos("1")["client"]["id"] == "1"
    conn = connect(db.db_path)
    assert conn.execute("SELECT COUNT(*) FROM tombstones").fetchone()[0] == 0
    conn.close()


def test_delete_newer_than_the_stored_edit_is_accepted(tmp_path):
    db = _db(tmp_path)
    db.save_client(_cliente(id="1", updated_at=1000), [])

    assert db.delete_client("1", deleted_at=1001) is True
    assert db.get_client_with_photos("1") is None


def test_gc_orphan_photos_can_skip_recent_uploads(tmp_path):
    db = _db(tmp_path)
    db.write_photo("just-uploaded", b"data")

    # A periodic sweep must not delete an upload whose push has not committed.
    assert db.gc_orphan_photos(min_age_seconds=3600) == 0
    assert os.path.exists(db._foto("just-uploaded"))

    assert db.gc_orphan_photos(min_age_seconds=0) == 1
    assert not os.path.exists(db._foto("just-uploaded"))


def test_pull_persists_a_missing_photo_hash_and_reuses_it(tmp_path, monkeypatch):
    db = _db(tmp_path)
    payload = b"\xff\xd8\xff\xe0computed-on-pull"
    db.save_client(_cliente(id="a", updated_at=1000), [FotoIn(id="p1", mime="image/jpeg")])
    db.write_photo("p1", payload)
    conn = connect(db.db_path)
    conn.execute("UPDATE fotos SET hash = NULL WHERE id = 'p1'")
    conn.commit()
    conn.close()

    calls = 0
    original = db._hash_file

    def counting_hash(path):
        nonlocal calls
        calls += 1
        return original(path)

    monkeypatch.setattr(db, "_hash_file", counting_hash)

    first = db.pull_all(limit=10)
    second = db.pull_all(limit=10)

    assert first[0][0]["fotos"][0]["hash"] == hashlib.sha256(payload).hexdigest()
    assert second[0][0]["fotos"][0]["hash"] == hashlib.sha256(payload).hexdigest()
    assert calls == 1, "the hash must be committed on the first pull and reused"

    conn = connect(db.db_path)
    stored = conn.execute("SELECT hash FROM fotos WHERE id = 'p1'").fetchone()
    conn.close()
    assert stored["hash"] == hashlib.sha256(payload).hexdigest()


def test_pull_reads_a_page_with_one_connection(tmp_path, monkeypatch):
    db = _db(tmp_path)
    for i in range(3):
        db.save_client(_cliente(id=str(i), nome=f"C{i}", updated_at=1000 + i), [])

    calls = 0
    original = db._conn

    def counting_conn():
        nonlocal calls
        calls += 1
        return original()

    monkeypatch.setattr(db, "_conn", counting_conn)

    records, _tombstones, _since, _since_id = db.pull_all(limit=100)

    assert [record["client"]["id"] for record in records] == ["0", "1", "2"]
    assert calls == 1, "pull must not open one connection per record"


def test_pull_batches_a_thousand_records_and_their_photos(tmp_path):
    db = _db(tmp_path)
    conn = connect(db.db_path)
    conn.executemany(
        "INSERT INTO clientes(id, nome, updated_at, created_at, delivery_ts) "
        "VALUES (?, ?, ?, ?, ?)",
        [(f"c{i}", f"C{i}", 1000 + i, 1000 + i, 1000 + i) for i in range(1000)],
    )
    conn.executemany(
        "INSERT INTO fotos(id, cliente_id, mime, caminho, ordem) VALUES (?, ?, ?, ?, ?)",
        [
            (f"p{i}", f"c{i}", "image/jpeg", db._foto(f"p{i}"), 0)
            for i in range(0, 1000, 100)
        ],
    )
    conn.commit()
    conn.close()

    records, _tombstones, next_since, _next_id = db.pull_all(limit=1000)

    assert len(records) == 1000
    assert next_since is None
    assert records[0]["client"]["id"] == "c0"
    assert records[-1]["client"]["id"] == "c999"
    assert records[0]["fotos"][0]["available"] is False
    assert records[0]["fotos"][0]["hash"] is None


def test_revoked_keys_are_hidden_and_revoke_is_idempotent(tmp_path):
    db = _db(tmp_path)
    db.add_key("key-1", "public-1", comment="maria")
    db.add_key("key-2", "public-2", comment="maria")
    db.add_key("key-3", "public-3", comment="joao")

    assert "public-1" in db.get_all_public_keys()
    assert db.get_key_by_public_key("public-1") is not None

    # Revoking twice is safe: the second call has nothing left to update.
    assert db.revoke_keys_by_comment("maria") == 2
    assert db.revoke_keys_by_comment("maria") == 0

    assert db.get_all_public_keys() == ["public-3"]
    assert db.get_key_by_public_key("public-1") is None
    assert db.get_key_by_public_key("public-3") is not None

    rows = {row["id"]: row for row in db.list_keys()}
    assert rows["key-1"]["revoked_at"] is not None
    assert rows["key-3"]["revoked_at"] is None


def test_apply_schema_adds_revoked_at_idempotently(tmp_path):
    path = tmp_path / "legacy.db"
    conn = connect(str(path))

    _apply_schema(conn)
    columns = {row["name"] for row in conn.execute("PRAGMA table_info(keys)").fetchall()}
    assert "revoked_at" in columns

    # Running it again must not fail on the already-present column.
    _apply_schema(conn)
    conn.close()
