"""Unit tests for the SQLite persistence layer (LWW, duplicates, tombstones,
migrations, photo hygiene and the timestamp-unit normalization)."""

import pytest

from app.database import Database, _apply_schema, connect
from app.models import ClienteIn, FotoIn, normalize_nome


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
    status, existing_id, canonical = db.save_client(
        _cliente(id="2", nome="  ana ", updated_at=1000), []
    )
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
    # Accepted writes are bumped onto the next watermark value, so rewind the
    # timestamps here to exercise the keyset over records sharing updated_at.
    conn = connect(db.db_path)
    try:
        conn.execute("UPDATE clientes SET updated_at = 100 WHERE id IN ('a', 'b', 'c')")
        conn.execute("UPDATE clientes SET updated_at = 200 WHERE id = 'd'")
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


def test_old_timestamp_is_bumped_so_the_write_is_deliverable(tmp_path):
    db = _db(tmp_path)
    assert db.save_client(_cliente(id="a", updated_at=2000), [])[0] == "accepted"

    # An edit made with a lagging clock (or synced days later) arrives after a
    # newer record exists: storing its old timestamp verbatim would leave it
    # invisible to every device that already pulled past that point.
    assert db.save_client(_cliente(id="b", nome="Bea", updated_at=1500), [])[0] == "accepted"

    assert db.get_client_with_photos("b")["client"]["updated_at"] == 2001
    records, _tombstones, _since, _since_id = db.pull_all(2000)
    assert [record["client"]["id"] for record in records] == ["b"]


def test_equal_timestamp_is_also_bumped(tmp_path):
    db = _db(tmp_path)
    db.save_client(_cliente(id="a", updated_at=2000), [])
    db.save_client(_cliente(id="b", nome="Bea", updated_at=2000), [])

    assert db.get_client_with_photos("b")["client"]["updated_at"] == 2001


def test_audit_entry_keeps_the_original_edit_time(tmp_path):
    db = _db(tmp_path)
    actor = {"id": "key-1", "comment": "Joao"}
    db.save_client(_cliente(id="a", updated_at=2000), [], actor)
    db.save_client(_cliente(id="b", nome="Bea", updated_at=1500), [], actor)

    history = db.client_history("b")
    assert history[-1]["action"] == "create"
    assert history[-1]["ts"] == 1500
