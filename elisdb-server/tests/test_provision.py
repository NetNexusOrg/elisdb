"""Provisioning must stay inside the configs directory and never log seeds."""

import os

from elisdb import provision
from elisdb.database import Database


def _db(tmp_path) -> Database:
    db = Database(str(tmp_path / "elisdb.db"), fotos_dir=str(tmp_path / "photos"))
    db.init()
    return db


def test_provisioning_writes_a_confined_config_without_printing_the_seed(
    tmp_path, capsys
):
    db = _db(tmp_path)
    configs = tmp_path / "configs"

    provisioned, revoked = provision.provision_users(
        db, ["maria"], "https://db.example.com", str(configs)
    )

    assert provisioned == ["maria"]
    assert revoked == []
    config = configs / "maria.config.json"
    assert config.exists()
    printed = capsys.readouterr()
    assert "private seed" not in printed.out
    assert "private seed" not in printed.err


def test_invalid_user_names_are_skipped_without_escaping(tmp_path, capsys):
    db = _db(tmp_path)
    configs = tmp_path / "configs"

    provisioned, revoked = provision.provision_users(
        db, ["../../escape", "maria"], "https://db.example.com", str(configs)
    )

    assert provisioned == ["maria"]
    assert revoked == []
    assert not (tmp_path / "escape.config.json").exists()
    assert "skipping user" in capsys.readouterr().err


def test_revocation_removes_the_key_but_not_a_path_outside_configs(tmp_path, capsys):
    db = _db(tmp_path)
    configs = tmp_path / "configs"
    outside = tmp_path / "outside.config.json"
    outside.write_text("keep me", encoding="utf-8")
    db.add_key(
        "issue-bad",
        "public-key",
        comment="../outside",
        provisioned=True,
    )

    provisioned, revoked = provision.provision_users(
        db, [], "https://db.example.com", str(configs)
    )

    assert provisioned == []
    assert revoked == ["../outside"]
    assert db.list_keys() == []
    assert outside.exists()
    assert "not deleting config" in capsys.readouterr().err


def test_valid_config_file_is_owner_only(tmp_path):
    db = _db(tmp_path)
    configs = tmp_path / "configs"

    provision.provision_users(db, ["maria"], "https://db.example.com", str(configs))

    config = configs / "maria.config.json"
    assert os.stat(config).st_mode & 0o777 == 0o600
