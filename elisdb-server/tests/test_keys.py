"""Unit tests for the server-issued configuration file written by the issue flow."""

import json
import os

from app import keys
from app.database import Database


def _db(tmp_path) -> Database:
    db = Database(str(tmp_path / "elisdb.db"), fotos_dir=str(tmp_path / "photos"))
    db.init()
    return db


def test_issue_and_register_writes_url_seed_and_user_name(tmp_path):
    db = _db(tmp_path)
    output = tmp_path / "maria.config.json"

    assert keys.issue_and_register(db, "maria", "https://db.example.com", str(output)) == 0

    config = json.loads(output.read_text(encoding="utf-8"))
    assert config["server_url"] == "https://db.example.com"
    assert config["user_name"] == "maria"
    assert config["private_seed"]


def test_issued_config_file_is_owner_only(tmp_path):
    db = _db(tmp_path)
    output = tmp_path / "maria.config.json"

    keys.issue_and_register(db, "maria", "https://db.example.com", str(output))

    assert os.stat(output).st_mode & 0o777 == 0o600
