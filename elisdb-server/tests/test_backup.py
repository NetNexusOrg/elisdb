"""Tests for consistent database + photo backup and restore."""

import json
import threading
from pathlib import Path

import pytest

from elisdb.backup import BackupError, create_backup, restore_backup, verify_backup
from elisdb.models import ClienteIn, FotoIn


def _seed_photo(db, payload: bytes = b"\xff\xd8\xffphoto"):
    db.save_client(
        ClienteIn(id="1", nome="Ana", updated_at=1000),
        [FotoIn(id="p1", mime="image/jpeg", ordem=0)],
    )
    Path(db._foto("p1")).write_bytes(payload)
    db.write_photo("p1", payload)
    return payload


def test_backup_verifies_and_restores_database_and_photo(tmp_path, server):
    _client, db = server
    payload = _seed_photo(db)
    backup = create_backup(db.db_path, db.fotos_dir, str(tmp_path / "backup"))

    manifest = verify_backup(str(backup))
    assert [photo["id"] for photo in manifest["photos"]] == ["p1"]

    restore_backup(str(backup), db.db_path, db.fotos_dir)
    record = db.get_client_with_photos("1")
    assert record is not None
    assert record["client"]["nome"] == "ANA"
    assert Path(db._foto("p1")).read_bytes() == payload


def test_corrupt_checksum_is_rejected_without_touching_live_data(tmp_path, server):
    _client, db = server
    _seed_photo(db)
    original = Path(db._foto("p1")).read_bytes()
    backup = create_backup(db.db_path, db.fotos_dir, str(tmp_path / "backup"))
    manifest_path = backup / "manifest.json"
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    manifest["photos"][0]["sha256"] = "0" * 64
    manifest_path.write_text(json.dumps(manifest), encoding="utf-8")

    with pytest.raises(BackupError):
        verify_backup(str(backup))
    with pytest.raises(BackupError):
        restore_backup(str(backup), db.db_path, db.fotos_dir)

    assert Path(db._foto("p1")).read_bytes() == original
    assert db.get_client_with_photos("1") is not None


def test_missing_backup_photo_is_rejected(tmp_path, server):
    _client, db = server
    _seed_photo(db)
    backup = create_backup(db.db_path, db.fotos_dir, str(tmp_path / "backup"))
    next((backup / "photos").iterdir()).unlink()

    with pytest.raises(BackupError):
        verify_backup(str(backup))


def test_backup_is_consistent_while_writes_are_active(tmp_path, server):
    _client, db = server
    _seed_photo(db)

    def writer():
        for index in range(20):
            db.save_client(
                ClienteIn(id=f"w{index}", nome=f"W{index}", updated_at=1100 + index),
                [],
            )

    thread = threading.Thread(target=writer)
    thread.start()
    backup = create_backup(db.db_path, db.fotos_dir, str(tmp_path / "backup"))
    thread.join()

    manifest = verify_backup(str(backup))
    assert any(photo["id"] == "p1" for photo in manifest["photos"])


def test_restore_failure_after_database_swap_rolls_back(tmp_path, server, monkeypatch):
    _client, db = server
    payload = _seed_photo(db)
    backup = create_backup(db.db_path, db.fotos_dir, str(tmp_path / "backup"))
    import elisdb.backup as backup_module

    real_replace = backup_module.os.replace
    calls = {"count": 0}

    def flaky_replace(source, destination):
        calls["count"] += 1
        if calls["count"] == 4:
            raise OSError("simulated failure while swapping photos")
        real_replace(source, destination)

    monkeypatch.setattr(backup_module.os, "replace", flaky_replace)
    with pytest.raises(OSError):
        restore_backup(str(backup), db.db_path, db.fotos_dir)

    assert Path(db._foto("p1")).read_bytes() == payload
    assert db.get_client_with_photos("1") is not None
