"""Regression tests for the data-integrity fixes in `sync-integrity`.

Each test pins one of the failures that could lose or hide data:

* the coordinate rebuild used to cascade-delete every photo row;
* the orphan sweep used to delete referenced photos when the directory was
  relative;
* deletions stamped before another device's watermark never travelled;
* the delivery clock leaked into `updated_at`, so a slow device's edits were
  rejected as stale;
* photos referenced by a push but missing on the server were dropped silently
  (now kept as an unavailable placeholder so clients do not delete their copy).
"""

import os
import sqlite3

from elisdb.database import Database
from elisdb.models import ClienteIn, FotoIn

LEGACY_SCHEMA = """
CREATE TABLE clientes (
    id TEXT PRIMARY KEY,
    nome TEXT NOT NULL,
    contrato TEXT,
    latitude REAL NOT NULL,
    longitude REAL NOT NULL,
    notas TEXT,
    updated_at INTEGER NOT NULL
);
CREATE UNIQUE INDEX idx_clientes_nome ON clientes (nome COLLATE NOCASE);
CREATE TABLE fotos (
    id TEXT PRIMARY KEY,
    cliente_id TEXT NOT NULL REFERENCES clientes(id) ON DELETE CASCADE,
    mime TEXT NOT NULL,
    caminho TEXT NOT NULL,
    ordem INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX idx_fotos_cliente ON fotos (cliente_id);
"""


def _cliente(**kwargs) -> ClienteIn:
    base = dict(id="1", nome="Ana", updated_at=1000)
    base.update(kwargs)
    return ClienteIn(**base)


def _db(tmp_path) -> Database:
    db = Database(str(tmp_path / "elisdb.db"), fotos_dir=str(tmp_path / "photos"))
    db.init()
    return db


def test_coordinate_rebuild_keeps_the_photos(tmp_path):
    """Upgrading a schema with NOT NULL coordinates must not drop photo rows."""
    db_path = tmp_path / "legacy.db"
    photos = tmp_path / "photos"
    photos.mkdir()
    stored = photos / "p1"
    stored.write_bytes(b"photo-bytes")

    raw = sqlite3.connect(db_path)
    raw.executescript(LEGACY_SCHEMA)
    raw.execute(
        "INSERT INTO clientes (id, nome, contrato, latitude, longitude, notas, updated_at) "
        "VALUES ('c1', 'ANA', NULL, 1.5, -2.5, NULL, 1000)"
    )
    raw.execute(
        "INSERT INTO fotos (id, cliente_id, mime, caminho, ordem) VALUES (?, ?, ?, ?, ?)",
        ("p1", "c1", "image/jpeg", str(stored), 0),
    )
    raw.commit()
    raw.close()

    db = Database(str(db_path), fotos_dir=str(photos))
    db.init()

    assert db.client_count() == 1
    record = db.get_client_with_photos("c1")
    assert record is not None
    assert [foto["id"] for foto in record["fotos"]] == ["p1"]
    assert db.get_photo("p1") == ("image/jpeg", b"photo-bytes")
    assert db.gc_orphan_photos() == 0


def test_sweep_keeps_photos_when_the_directory_is_relative(tmp_path, monkeypatch):
    """Stored paths are absolute, so a relative config must not look orphaned."""
    monkeypatch.chdir(tmp_path)
    db = Database("elisdb.db", fotos_dir="photos")
    db.init()
    db.write_photo("referenced", b"data")
    db.save_client(_cliente(id="1"), [FotoIn(id="referenced", mime="image/jpeg")])

    assert db.gc_orphan_photos() == 0
    assert db.get_photo("referenced") == ("image/jpeg", b"data")


def test_sweep_ignores_directories(tmp_path):
    """A subdirectory in the photos folder is not an orphaned file."""
    db = _db(tmp_path)
    (tmp_path / "photos" / "nested").mkdir()
    db.write_photo("orphan", b"data")

    assert db.gc_orphan_photos() == 1
    assert (tmp_path / "photos" / "nested").is_dir()


def test_deletion_from_a_slow_device_is_delivered(tmp_path):
    """A tombstone stamped before the watermark still reaches other devices."""
    db = _db(tmp_path)
    db.save_client(_cliente(id="a", updated_at=1000), [])
    db.save_client(_cliente(id="b", nome="B", updated_at=9000), [])

    assert db.delete_client("a", deleted_at=1001) is True

    records, tombstones, _, _ = db.pull_all(since=9000)
    assert records == []
    assert [tombstone["id"] for tombstone in tombstones] == ["a"]
    assert tombstones[0]["deleted_at"] == 1001
    assert tombstones[0]["delivery_ts"] > 9000


def test_slow_clock_edit_is_accepted_and_keeps_its_timestamp(tmp_path):
    """The delivery clock must not overwrite updated_at and cause a false stale."""
    db = _db(tmp_path)
    db.save_client(_cliente(id="a", updated_at=1000), [])
    # Another device has already pushed far ahead; the watermark is now 9000.
    db.save_client(_cliente(id="b", nome="B", updated_at=9000), [])

    status, _, _, _ = db.save_client(_cliente(id="a", updated_at=2000), [])

    assert status == "accepted"
    stored = db.get_client_with_photos("a")["client"]
    assert stored["updated_at"] == 2000
    assert stored["delivery_ts"] > 9000


def test_push_reports_photos_missing_on_the_server(tmp_path):
    """A referenced photo without a file must be reported, not silently dropped."""
    db = _db(tmp_path)

    status, _, _, missing = db.save_client(
        _cliente(id="1"), [FotoIn(id="absent", mime="image/jpeg")]
    )

    assert status == "accepted"
    assert missing == ["absent"]
    stored = db.get_client_with_photos("1")
    assert stored["photos_complete"] is True
    assert stored["fotos"] == [
        {
            "id": "absent",
            "mime": "image/jpeg",
            "ordem": 0,
            "hash": None,
            "available": False,
        }
    ]


def test_latest_timestamp_covers_deletions(tmp_path):
    """Clients compare this value with their watermark, so tombstones count."""
    db = _db(tmp_path)
    db.save_client(_cliente(id="a", updated_at=1000), [])

    db.delete_client("a", deleted_at=5000)

    assert db.latest_updated_at() >= 5000


def test_pull_records_use_the_delivery_cursor(tmp_path):
    """Pull pagination and filtering follow delivery_ts, not the device clock."""
    db = _db(tmp_path)
    db.save_client(_cliente(id="a", updated_at=1000), [])
    db.save_client(_cliente(id="b", nome="B", updated_at=2000), [])

    records, _tombstones, next_since, next_since_id = db.pull_all(limit=1)

    assert len(records) == 1
    assert next_since is not None and next_since > 0
    assert next_since_id == records[0]["client"]["id"]
    assert records[0]["client"]["delivery_ts"] == next_since

    rest, _, _, _ = db.pull_all(since=next_since, since_id=next_since_id)
    assert [record["client"]["id"] for record in rest] == ["b"]


def test_database_normalises_the_photos_directory(tmp_path, monkeypatch):
    """The stored directory is absolute so path comparisons are meaningful."""
    monkeypatch.chdir(tmp_path)
    db = Database("elisdb.db", fotos_dir="photos")

    assert os.path.isabs(db.fotos_dir)
