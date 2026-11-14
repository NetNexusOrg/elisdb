import os
import time

import pytest

from elisdb import backup
from elisdb.main import newest_auto_backup, should_run_backup


def _backup_dir(root, name):
    path = root / name
    path.mkdir()
    (path / backup.MANIFEST_NAME).write_text("{}")
    return path


def test_backup_name_is_sortable_utc(tmp_path):
    name = backup.backup_name(0)
    assert name == "auto-19700101-000000"


def test_list_backups_returns_manifest_dirs_oldest_first(tmp_path):
    _backup_dir(tmp_path, "auto-20260102-000000")
    _backup_dir(tmp_path, "auto-20260101-000000")
    (tmp_path / "not-a-backup").mkdir()

    names = [path.name for path in backup.list_backups(tmp_path)]

    assert names == ["auto-20260101-000000", "auto-20260102-000000"]


def test_list_backups_of_a_missing_directory_is_empty(tmp_path):
    assert backup.list_backups(tmp_path / "missing") == []


def test_prune_backups_removes_only_the_oldest_automatic_ones(tmp_path):
    for name in (
        "auto-20260101-000000",
        "auto-20260102-000000",
        "auto-20260103-000000",
        "pre-deploy-123",
    ):
        _backup_dir(tmp_path, name)

    removed = backup.prune_backups(tmp_path, 2)

    assert [path.name for path in removed] == ["auto-20260101-000000"]
    assert (tmp_path / "auto-20260102-000000").exists()
    assert (tmp_path / "auto-20260103-000000").exists()
    assert (tmp_path / "pre-deploy-123").exists()


def test_prune_backups_with_zero_keeps_everything(tmp_path):
    _backup_dir(tmp_path, "auto-20260101-000000")
    _backup_dir(tmp_path, "auto-20260102-000000")

    removed = backup.prune_backups(tmp_path, 0)

    assert removed == []
    assert len(backup.list_backups(tmp_path)) == 2


def test_prune_backups_rejects_a_negative_count(tmp_path):
    with pytest.raises(backup.BackupError):
        backup.prune_backups(tmp_path, -1)


def test_estimate_backup_bytes_counts_database_and_photos(tmp_path):
    db_path = tmp_path / "elisdb.db"
    db_path.write_bytes(b"x" * 100)
    photos = tmp_path / "photos"
    photos.mkdir()
    (photos / "one").write_bytes(b"y" * 50)
    (photos / "two").write_bytes(b"z" * 25)

    assert backup.estimate_backup_bytes(db_path, photos) == 175


def test_estimate_backup_bytes_tolerates_missing_paths(tmp_path):
    assert backup.estimate_backup_bytes(tmp_path / "db", tmp_path / "photos") == 0


def test_free_space_bytes_reports_a_positive_number(tmp_path):
    assert backup.free_space_bytes(tmp_path) > 0


def test_should_run_backup_needs_an_interval():
    assert not should_run_backup(None, 1000.0, 0)
    assert not should_run_backup(999.0, 1000.0, 0)


def test_should_run_backup_when_there_is_no_previous_backup():
    assert should_run_backup(None, 1000.0, 86400)


def test_should_run_backup_waits_for_the_interval():
    assert not should_run_backup(1000.0, 1000.0 + 3600, 86400)
    assert should_run_backup(1000.0, 1000.0 + 86400, 86400)


def test_newest_auto_backup_tracks_mtime_and_ignores_manual_backups(tmp_path):
    old = _backup_dir(tmp_path, "auto-20260101-000000")
    new = _backup_dir(tmp_path, "auto-20260102-000000")
    manual = _backup_dir(tmp_path, "pre-deploy-123")
    now = time.time()
    os.utime(old, (now - 200, now - 200))
    os.utime(new, (now - 100, now - 100))
    os.utime(manual, (now, now))

    newest = newest_auto_backup(tmp_path)

    assert newest is not None
    assert newest == pytest.approx(now - 100, abs=2)


def test_newest_auto_backup_without_backups(tmp_path):
    assert newest_auto_backup(tmp_path) is None


def test_summarize_backups_counts_only_automatic_backups(tmp_path):
    _backup_dir(tmp_path, "pre-deploy-123")

    empty = backup.summarize_backups(tmp_path)

    assert empty["count"] == 0
    assert empty["newest_auto"] is None
    assert empty["newest_auto_age_seconds"] is None
    assert empty["dir"] == str(tmp_path)


def test_summarize_backups_reports_the_newest_automatic(tmp_path):
    older = _backup_dir(tmp_path, "auto-20260101-000000")
    newer = _backup_dir(tmp_path, "auto-20260102-000000")
    os.utime(older, (1000, 1000))
    os.utime(newer, (2000, 2000))

    summary = backup.summarize_backups(tmp_path)

    assert summary["count"] == 2
    assert summary["newest_auto"] == "auto-20260102-000000"
    assert summary["newest_auto_age_seconds"] >= 0
