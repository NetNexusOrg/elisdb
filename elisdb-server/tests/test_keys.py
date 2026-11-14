"""Unit tests for the server-issued configuration file written by the issue flow."""

import json
import os

from elisdb import keys
from elisdb.database import Database


def _db(tmp_path) -> Database:
    db = Database(str(tmp_path / "elisdb.db"), fotos_dir=str(tmp_path / "photos"))
    db.init()
    return db


def test_issue_only_generates_key_material(tmp_path):
    """`issue` must not register anything; only `issue_and_register` does."""
    db = _db(tmp_path)

    seed, public, key_id = keys.issue("maria")

    assert seed and public and key_id.startswith("issue-")
    assert db.list_keys() == []


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


def test_default_output_goes_to_the_configs_directory(tmp_path, monkeypatch):
    """Without --output the file lands in ELISDB_CONFIGS_DIR, never the CWD."""
    db = _db(tmp_path)
    configs = tmp_path / "configs"
    monkeypatch.setenv("ELISDB_CONFIGS_DIR", str(configs))

    assert keys.issue_and_register(db, "maria", "https://db.example.com", None) == 0

    output = configs / "maria.config.json"
    assert output.is_file()
    assert os.stat(output).st_mode & 0o777 == 0o600


def test_issue_does_not_print_the_private_seed_by_default(tmp_path, capsys):
    db = _db(tmp_path)
    output = tmp_path / "maria.config.json"

    keys.issue_and_register(db, "maria", "https://db.example.com", str(output))

    printed = capsys.readouterr()
    seed = json.loads(output.read_text(encoding="utf-8"))["private_seed"]
    assert seed not in printed.out
    assert seed not in printed.err
    assert "private seed" not in printed.out


def test_print_seed_flag_opts_in_to_showing_it(tmp_path, capsys):
    db = _db(tmp_path)
    output = tmp_path / "maria.config.json"

    keys._run_args(
        [
            "issue",
            "maria",
            "--server-url",
            "https://db.example.com",
            "--output",
            str(output),
            "--print-seed",
        ],
        db,
    )

    printed = capsys.readouterr()
    seed = json.loads(output.read_text(encoding="utf-8"))["private_seed"]
    assert seed in printed.out
    assert "registered public key" in printed.out


def test_safe_config_path_rejects_traversal_and_separators(tmp_path):
    for name in ("../escape", "sub/escape", "a\\b", "", ".", ".."):
        try:
            keys.safe_config_path(str(tmp_path), name)
        except ValueError:
            pass
        else:
            raise AssertionError(f"expected {name!r} to be rejected")


def test_safe_config_path_accepts_a_plain_user_name(tmp_path):
    path = keys.safe_config_path(str(tmp_path), "maria-01")
    assert path == os.path.realpath(str(tmp_path / "maria-01.config.json"))


def test_list_json_is_machine_readable(tmp_path, capsys):
    db = _db(tmp_path)
    keys.issue_and_register(db, "maria", "https://db.example.com", str(tmp_path / "m.json"))
    capsys.readouterr()

    assert keys._run_args(["list", "--json"], db) == 0

    rows = json.loads(capsys.readouterr().out)
    assert len(rows) == 1
    assert rows[0]["comment"] == "maria"
    assert rows[0]["revoked_at"] is None
    assert set(rows[0]) >= {"id", "comment", "created_at", "revoked_at", "provisioned"}


def test_revoke_by_id_soft_revokes_and_keeps_the_row(tmp_path, capsys):
    db = _db(tmp_path)
    keys.issue_and_register(db, "maria", "https://db.example.com", str(tmp_path / "m.json"))
    key_id = db.list_keys()[0]["id"]

    assert keys._run_args(["revoke", "--id", key_id], db) == 0

    rows = db.list_keys()
    assert rows[0]["revoked_at"] is not None
    assert key_id in capsys.readouterr().out


def test_revoke_by_unknown_id_fails(tmp_path, capsys):
    db = _db(tmp_path)

    assert keys._run_args(["revoke", "--id", "issue-nope"], db) == 1
    assert "no active key" in capsys.readouterr().err
