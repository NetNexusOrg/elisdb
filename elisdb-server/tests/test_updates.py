"""Discovery of the published Android package (``app/updates.py``)."""

import hashlib

from app import updates


def _publish(directory, name, data=b"apk-bytes"):
    path = directory / name
    path.write_bytes(data)
    return path


def test_parse_name_recognises_versioned_packages():
    assert updates.parse_name("elisdb-1.6.0-11.apk") == ("1.6.0", 11)
    assert updates.parse_name("elisdb-1.6.0-rc1-12.apk") == ("1.6.0-rc1", 12)


def test_parse_name_rejects_other_names():
    assert updates.parse_name("app-release.apk") is None
    assert updates.parse_name("elisdb-1.6.0.apk") is None
    assert updates.parse_name("elisdb-1.6.0-11.txt") is None


def test_find_release_selects_the_highest_version_code(tmp_path):
    _publish(tmp_path, "elisdb-1.5.2-10.apk", b"old")
    _publish(tmp_path, "elisdb-1.6.0-11.apk", b"new")
    _publish(tmp_path, "app-release.apk", b"ignored")

    release = updates.find_release(tmp_path)

    assert release is not None
    assert release.filename == "elisdb-1.6.0-11.apk"
    assert release.version_name == "1.6.0"
    assert release.version_code == 11
    assert release.size == 3
    assert release.sha256 == hashlib.sha256(b"new").hexdigest()


def test_find_release_without_a_valid_package(tmp_path):
    _publish(tmp_path, "app-release.apk")

    assert updates.find_release(tmp_path) is None


def test_find_release_in_a_missing_directory(tmp_path):
    assert updates.find_release(tmp_path / "missing") is None


def test_find_release_rejects_an_oversized_package(tmp_path, monkeypatch):
    monkeypatch.setattr(updates, "MAX_APK_BYTES", 4)
    _publish(tmp_path, "elisdb-1.6.0-11.apk", b"12345")

    assert updates.find_release(tmp_path) is None


def test_apk_dir_uses_the_environment(tmp_path, monkeypatch):
    monkeypatch.setenv("ELISDB_APK_DIR", str(tmp_path / "custom"))

    assert updates.apk_dir() == tmp_path / "custom"


def test_apk_dir_defaults_next_to_the_database(tmp_path, monkeypatch):
    monkeypatch.delenv("ELISDB_APK_DIR", raising=False)
    monkeypatch.setenv("ELISDB_DB", str(tmp_path / "elisdb.db"))

    assert updates.apk_dir() == tmp_path / "apk" / "latest"
